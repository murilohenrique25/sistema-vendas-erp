package br.com.erp.pos;
import br.com.erp.auth.*;
import br.com.erp.finance.*;
import br.com.erp.shared.Audit;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Service
public class SaleCreditService {
 private final JdbcTemplate db;private final BranchAccess access;private final ReceivablePlanService plans;private final ReceivableService titles;private final Audit audit;private final ZoneId zone;
 public SaleCreditService(JdbcTemplate db,BranchAccess access,ReceivablePlanService plans,ReceivableService titles,Audit audit,@Value("${erp.finance-zone:America/Sao_Paulo}") String zone){this.db=db;this.access=access;this.plans=plans;this.titles=titles;this.audit=audit;this.zone=ZoneId.of(zone);}
 public record PolicyInput(@NotNull @Min(0) Long version,@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal creditLimit,@NotNull Boolean blocked){}
 public record Policy(UUID customerId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal creditLimit,boolean blocked,long version){}
 private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
 public void require(Actor a,String permission){if(db.queryForObject("SELECT COUNT(*) FROM user_permission WHERE tenant_id=? AND user_id=? AND permission=?",Long.class,a.tenantId(),a.userId(),permission)==0)throw error(HttpStatus.FORBIDDEN,"Operação exige permissão "+permission+".");}
 private void customer(Actor a,UUID id){if(db.queryForObject("SELECT COUNT(*) FROM customer WHERE tenant_id=? AND id=?",Long.class,a.tenantId(),id)==0)throw error(HttpStatus.NOT_FOUND,"Cliente não encontrado.");}
 private Policy read(Actor a,UUID id){var rows=db.query("SELECT * FROM customer_credit_policy WHERE tenant_id=? AND customer_id=?",(r,i)->new Policy(id,r.getBigDecimal("credit_limit"),r.getBoolean("blocked"),r.getLong("version")),a.tenantId(),id);return rows.isEmpty()?new Policy(id,BigDecimal.ZERO.setScale(2),true,0):rows.getFirst();}
 @Transactional(readOnly=true) public Policy policy(Actor a,UUID id){access.requireAll(a);customer(a,id);return read(a,id);}
 @Transactional public Policy configure(Actor a,UUID id,PolicyInput input){
  db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());access.requireAll(a);customer(a,id);var old=read(a,id);
  // Repeating a successful PUT with the original version and identical values is safe.
  if(old.version()==input.version()+1 && old.creditLimit().compareTo(input.creditLimit())==0 && old.blocked()==input.blocked())return old;
  if(old.version()!=input.version())throw error(HttpStatus.CONFLICT,"Política de crédito alterada. Atualize.");
  if(old.version()==0)db.update("INSERT INTO customer_credit_policy(tenant_id,customer_id,credit_limit,blocked,version) VALUES(?,?,?,?,1)",a.tenantId(),id,input.creditLimit(),input.blocked());
  else db.update("UPDATE customer_credit_policy SET credit_limit=?,blocked=?,version=version+1 WHERE tenant_id=? AND customer_id=?",input.creditLimit(),input.blocked(),a.tenantId(),id);
  audit.record(a,"CUSTOMER_CREDIT_CONFIGURED",id);return read(a,id);
 }
 public void permissions(Actor a){require(a,"pos:credit");require(a,"customer:read");require(a,"receivable:read");require(a,"receivable:write");}
 public UUID originate(Actor a,UUID sale,UUID branch,SaleContracts.CreditInput input,BigDecimal total){
  permissions(a);customer(a,input.customerId());
  if(input.amount().compareTo(total)>0)throw error(HttpStatus.BAD_REQUEST,"Crédito não pode exceder o total da venda.");
  var policy=read(a,input.customerId());
  BigDecimal exposure=db.queryForObject("SELECT COALESCE(SUM(amount-received_amount-credited_amount),0) FROM receivable WHERE tenant_id=? AND customer_id=? AND status IN ('OPEN','PARTIAL')",BigDecimal.class,a.tenantId(),input.customerId());
  boolean overdue=db.queryForObject("SELECT COUNT(*) FROM receivable WHERE tenant_id=? AND customer_id=? AND status IN ('OPEN','PARTIAL') AND due_on<?",Long.class,a.tenantId(),input.customerId(),LocalDate.now(zone))>0;
  if(policy.blocked()||overdue||exposure.add(input.amount()).compareTo(policy.creditLimit())>0)throw error(HttpStatus.CONFLICT,"Crédito indisponível para este cliente. Solicite revisão ao responsável.");
  var plan=plans.create(a,new ReceivablePlanContracts.Input(UUID.randomUUID(),branch,input.customerId(),"POS-"+sale,"","Venda a prazo "+sale,LocalDate.now(zone),input.firstDueOn(),input.amount(),input.installmentCount(),input.intervalMonths()));return plan.id();
 }
 public SaleContracts.Credit readSale(Actor a,UUID plan){
  if(plan==null)return null;
  return db.queryForObject("SELECT p.customer_id,p.amount,MIN(r.customer_name) AS customer_name,COALESCE(SUM(CASE WHEN r.status='CANCELLED' THEN 0 ELSE r.amount-r.received_amount-r.credited_amount END),0) AS outstanding,SUM(r.credited_amount) AS credited_amount FROM receivable_plan p JOIN receivable_installment i ON i.tenant_id=p.tenant_id AND i.plan_id=p.id JOIN receivable r ON r.tenant_id=i.tenant_id AND r.id=i.receivable_id WHERE p.tenant_id=? AND p.id=? GROUP BY p.customer_id,p.amount",(r,n)->new SaleContracts.Credit(r.getObject("customer_id",UUID.class),r.getString("customer_name"),plan,r.getBigDecimal("amount"),r.getBigDecimal("outstanding"),r.getBigDecimal("credited_amount"),db.queryForObject("SELECT COALESCE(SUM(f.amount),0) FROM pos_sale_return_receipt_refund f JOIN receivable_installment i ON i.tenant_id=f.tenant_id AND i.receivable_id=f.receivable_id WHERE i.tenant_id=? AND i.plan_id=?",BigDecimal.class,a.tenantId(),plan)),a.tenantId(),plan);
 }
 public void cancel(Actor a,UUID plan,String note){
  if(plan==null)return;permissions(a);
  var ids=db.query("SELECT r.id FROM receivable r JOIN receivable_installment i ON i.tenant_id=r.tenant_id AND i.receivable_id=r.id WHERE i.tenant_id=? AND i.plan_id=? ORDER BY i.position",(r,n)->r.getObject(1,UUID.class),a.tenantId(),plan);
  for(var id:ids){var title=titles.get(a,id);if(!title.status().equals("OPEN")||title.receivedAmount().signum()>0)throw error(HttpStatus.CONFLICT,"Estorne os recebimentos do crediário antes de cancelar a venda.");titles.cancelFromSale(a,id,new ReceivableContracts.Cancellation(title.version(),note));}
 }
}
