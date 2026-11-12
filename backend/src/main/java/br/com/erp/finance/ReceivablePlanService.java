package br.com.erp.finance;

import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.finance.ReceivablePlanContracts.*;

@Service
public class ReceivablePlanService {
 private final JdbcTemplate db;private final BranchAccess access;private final ReceivableService titles;private final Audit audit;
 public ReceivablePlanService(JdbcTemplate db,BranchAccess access,ReceivableService titles,Audit audit){this.db=db;this.access=access;this.titles=titles;this.audit=audit;}
 private ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private void scope(Actor actor,Input input){access.requireBranch(actor,input.branchId());}
 private Preview calculate(Input input){
  if(input.issuedOn().getYear()<1 || input.issuedOn().getYear()>9999)throw bad("Emissão fora do intervalo permitido.");
  if(input.firstDueOn().isBefore(input.issuedOn()))throw bad("Primeiro vencimento não pode anteceder emissão.");
  if(input.description().trim().length()<5)throw bad("Descrição deve conter pelo menos cinco caracteres.");
  BigInteger cents=input.amount().movePointRight(2).toBigIntegerExact();
  var division=cents.divideAndRemainder(BigInteger.valueOf(input.installmentCount()));
  if(division[0].signum()==0)throw bad("Cada parcela deve ter pelo menos um centavo.");
  List<Installment> result=new ArrayList<>();String reference=input.reference().toUpperCase(Locale.ROOT);
  for(int i=0;i<input.installmentCount();i++){
   LocalDate due;try{due=input.firstDueOn().plusMonths((long)i*input.intervalMonths());}catch(DateTimeException e){throw bad("Vencimento fora do intervalo permitido.");}
   if(due.getYear()<1 || due.getYear()>9999)throw bad("Vencimento fora do intervalo permitido.");
   BigInteger value=division[0].add(i<division[1].intValueExact()?BigInteger.ONE:BigInteger.ZERO);
   result.add(new Installment(i+1,reference+"."+String.format(Locale.ROOT,"%03d",i+1),due,new BigDecimal(value,2)));
  }
  return new Preview(input.amount().setScale(2),List.copyOf(result));
 }
 @Transactional(readOnly=true) public Preview preview(Actor actor,Input input){scope(actor,input);return calculate(input);}
 @Transactional(readOnly=true) public Plan get(Actor actor,UUID id){
  var rows=db.query("SELECT * FROM receivable_plan WHERE tenant_id=? AND id=?",(rs,i)->new Plan(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),rs.getObject("customer_id",UUID.class),rs.getString("reference"),rs.getBigDecimal("amount"),List.of()),actor.tenantId(),id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Parcelamento não encontrado.");
  var plan=rows.getFirst();access.requireBranch(actor,plan.branchId());
  var ids=db.query("SELECT receivable_id FROM receivable_installment WHERE tenant_id=? AND plan_id=? ORDER BY position",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),id);
  return new Plan(id,plan.branchId(),plan.customerId(),plan.reference(),plan.amount(),ids.stream().map(key->titles.get(actor,key)).toList());
 }
 @Transactional public Plan create(Actor actor,Input input){
  db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());scope(actor,input);var preview=calculate(input);
  String reference=input.reference().toUpperCase(Locale.ROOT),document=input.document().trim(),description=input.description().trim();
  String hash=StockService.fingerprint("RECEIVABLE_PLAN",input.branchId(),input.customerId(),reference,document,description,input.issuedOn(),input.firstDueOn(),input.amount(),input.installmentCount(),input.intervalMonths());
  var previous=db.query("SELECT id,fingerprint FROM receivable_plan WHERE tenant_id=? AND request_id=?",(rs,i)->Map.entry(rs.getObject(1,UUID.class),rs.getString(2)),actor.tenantId(),input.requestId());
  if(!previous.isEmpty()){
   if(!previous.getFirst().getValue().equals(hash))throw new ResponseStatusException(HttpStatus.CONFLICT,"Identificador do parcelamento reutilizado com outros dados.");
   return get(actor,previous.getFirst().getKey());
  }
  if(db.queryForObject("SELECT COUNT(*) FROM customer WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,actor.tenantId(),input.customerId())==0)throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Cliente ativo não encontrado.");
  UUID id=UUID.randomUUID();
  // Every title and its history link share this transaction; a late collision rolls back the entire plan.
  db.update("INSERT INTO receivable_plan(id,tenant_id,request_id,fingerprint,branch_id,customer_id,reference,amount,installment_count,first_due_on,interval_months,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,actor.tenantId(),input.requestId(),hash,input.branchId(),input.customerId(),reference,input.amount(),input.installmentCount(),input.firstDueOn(),input.intervalMonths(),actor.userId());
  for(var installment:preview.installments()){
   var title=titles.create(actor,new ReceivableContracts.Input(UUID.randomUUID(),input.branchId(),input.customerId(),installment.reference(),document,description,input.issuedOn(),installment.dueOn(),installment.amount()));
   db.update("INSERT INTO receivable_installment(tenant_id,plan_id,position,receivable_id) VALUES(?,?,?,?)",actor.tenantId(),id,installment.position(),title.id());
  }
  audit.record(actor,"RECEIVABLE_PLAN_CREATED",id);return get(actor,id);
 }
}
