package br.com.erp.finance;
import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.finance.TreasuryContracts.*;
@Service
public class TreasuryService {
 private final JdbcTemplate db;private final BranchAccess access;private final Audit audit;
 public TreasuryService(JdbcTemplate db,BranchAccess access,Audit audit){this.db=db;this.access=access;this.audit=audit;}
 private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
 private Account row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new Account(r.getObject("id",UUID.class),r.getObject("branch_id",UUID.class),r.getString("code"),r.getString("name"),Kind.valueOf(r.getString("kind")),r.getBigDecimal("balance"));}
 private Account find(Actor actor,UUID id){var rows=db.query("SELECT * FROM treasury_account WHERE tenant_id=? AND id=?",this::row,actor.tenantId(),id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Conta não encontrada.");access.requireBranch(actor,rows.getFirst().branchId());return rows.getFirst();}
 @Transactional(readOnly=true) public Account get(Actor a,UUID id){return find(a,id);}
 @Transactional(readOnly=true) public Map<String,Object> list(Actor a,int page,int size){String where=" WHERE t.tenant_id=? AND "+BranchAccess.predicate("t.branch_id");return Map.of("items",db.query("SELECT t.* FROM treasury_account t"+where+" ORDER BY t.code,t.id LIMIT ? OFFSET ?",this::row,a.tenantId(),a.tenantId(),a.userId(),size,(long)page*size),"total",db.queryForObject("SELECT COUNT(*) FROM treasury_account t"+where,Long.class,a.tenantId(),a.tenantId(),a.userId()),"page",page,"size",size);}
 @Transactional public Account create(Actor a,Input input){
  lockTenant(a);access.requireBranch(a,input.branchId());String code=input.code().toUpperCase(Locale.ROOT),name=input.name().trim();if(name.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Informe o nome da conta.");
  String hash=StockService.fingerprint("TREASURY_ACCOUNT",input.branchId(),code,name,input.kind());
  var old=db.query("SELECT id,fingerprint FROM treasury_account WHERE tenant_id=? AND request_id=?",(r,i)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),a.tenantId(),input.requestId());
  if(!old.isEmpty()){if(!old.getFirst().getValue().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador reutilizado com outros dados.");return find(a,old.getFirst().getKey());}
  if(db.queryForObject("SELECT COUNT(*) FROM branch WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,a.tenantId(),input.branchId())==0)throw error(HttpStatus.NOT_FOUND,"Filial ativa não encontrada.");
  UUID id=UUID.randomUUID();db.update("INSERT INTO treasury_account(id,tenant_id,request_id,fingerprint,branch_id,code,name,kind,created_by) VALUES(?,?,?,?,?,?,?,?,?)",id,a.tenantId(),input.requestId(),hash,input.branchId(),code,name,input.kind().name(),a.userId());audit.record(a,"TREASURY_ACCOUNT_CREATED",id);return find(a,id);
 }
 // Serialize operations within the tenant, including UUID reuse across unrelated accounts.
 // This also gives opposite-direction transfers one consistent lock order.
 private void lockTenant(Actor a){db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());}
 private void requireUnreserved(Actor a,UUID account,UUID allowedDrawer){if(account!=null&&!account.equals(allowedDrawer)&&db.queryForObject("SELECT COUNT(*) FROM pos_cash_session WHERE tenant_id=? AND open_account_id=?",Long.class,a.tenantId(),account)>0)throw error(HttpStatus.CONFLICT,"Conta reservada a uma sessão de caixa aberta. Registre movimentos pelo PDV.");}
 public void requirePaymentAccount(Actor a,UUID id){
  if(db.queryForObject("SELECT COUNT(*) FROM user_permission p JOIN app_user u ON u.tenant_id=p.tenant_id AND u.id=p.user_id WHERE p.tenant_id=? AND p.user_id=? AND u.active=TRUE AND p.permission IN ('treasury:read','treasury:move')",Long.class,a.tenantId(),a.userId())!=2)throw error(HttpStatus.FORBIDDEN,"Movimento financeiro vinculado exige leitura e movimentação da tesouraria.");
  find(a,id);
 }
 @Transactional public UUID recordPayment(Actor a,UUID accountId,BigDecimal amount,String note,boolean reversal){return financialEntry(a,accountId,amount,note,reversal,reversal?"PAYABLE_REVERSAL":"PAYABLE_PAYMENT");}
 @Transactional public UUID recordReceipt(Actor a,UUID accountId,BigDecimal amount,String note,boolean reversal){return financialEntry(a,accountId,amount,note,!reversal,reversal?"RECEIVABLE_REVERSAL":"RECEIVABLE_RECEIPT");}
 private UUID financialEntry(Actor a,UUID accountId,BigDecimal amount,String note,boolean credit,String origin){
  lockTenant(a);requirePaymentAccount(a,accountId);requireUnreserved(a,accountId,null);Account account=find(a,accountId);BigDecimal delta=credit?amount:amount.negate(),balance=account.balance().add(delta);
  if(balance.signum()<0)throw error(HttpStatus.CONFLICT,"Saldo insuficiente na conta do movimento financeiro.");
  if(balance.compareTo(new BigDecimal("99999999999999999.99"))>0)throw error(HttpStatus.CONFLICT,"Saldo excede limite monetário.");
  UUID id=UUID.randomUUID();
  db.update("INSERT INTO treasury_operation(id,tenant_id,fingerprint,source_id,destination_id,amount,note,created_by,origin,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",id,a.tenantId(),StockService.fingerprint(origin,accountId,amount,note),credit?null:accountId,credit?accountId:null,amount,note,a.userId(),origin,OffsetDateTime.now(java.time.ZoneOffset.UTC));
  entry(a,id,account,delta);audit.record(a,origin,id);return id;
 }
 private Operation operation(Actor a,UUID id){return db.queryForObject("SELECT * FROM treasury_operation WHERE tenant_id=? AND id=?",(r,i)->new Operation(r.getObject("id",UUID.class),r.getObject("source_id",UUID.class),r.getObject("destination_id",UUID.class),r.getBigDecimal("amount"),r.getString("note"),r.getObject("created_by",UUID.class),r.getObject("created_at",OffsetDateTime.class)),a.tenantId(),id);}
 @Transactional public Operation move(Actor a,Movement input){return postMovement(a,input,null,"MANUAL");}
 // Called only after CashService or SaleService validates session ownership and state under the tenant lock.
 @Transactional public Operation recordDrawerMovement(Actor a,UUID drawer,Movement input,String origin){
  lockTenant(a);requirePaymentAccount(a,drawer);
  if(origin.equals("POS_SALE")&&(input.sourceId()!=null||!drawer.equals(input.destinationId())))throw error(HttpStatus.BAD_REQUEST,"Venda deve creditar exclusivamente o caixa.");
  if(List.of("POS_SALE_REVERSAL","POS_SALE_RETURN").contains(origin)&&(!drawer.equals(input.sourceId())||input.destinationId()!=null))throw error(HttpStatus.BAD_REQUEST,"Estorno deve debitar exclusivamente o caixa.");
  if(!List.of("POS_SUPPLY","POS_WITHDRAW","POS_SALE","POS_SALE_REVERSAL","POS_SALE_RETURN").contains(origin)||!drawer.equals(input.sourceId())&&!drawer.equals(input.destinationId()))throw error(HttpStatus.BAD_REQUEST,"Movimento de caixa inválido.");
  return postMovement(a,input,drawer,origin);
 }
 @Transactional public Operation recordSaleBankMovement(Actor a,Movement input,boolean reversal){
  lockTenant(a);UUID account=reversal?input.sourceId():input.destinationId();requirePaymentAccount(a,account);
  if(find(a,account).kind()!=Kind.BANK||(reversal?input.destinationId()!=null:input.sourceId()!=null))throw error(HttpStatus.BAD_REQUEST,"Movimento bancário de venda inválido.");
  return postMovement(a,input,null,reversal?"POS_SALE_REVERSAL":"POS_SALE");
 }
 @Transactional public Operation recordSaleReturnBankMovement(Actor a,Movement input){
  lockTenant(a);requirePaymentAccount(a,input.sourceId());if(input.destinationId()!=null||find(a,input.sourceId()).kind()!=Kind.BANK)throw error(HttpStatus.BAD_REQUEST,"Reembolso bancário de devolução inválido.");return postMovement(a,input,null,"POS_SALE_RETURN");
 }
 private Operation postMovement(Actor a,Movement input,UUID allowedDrawer,String origin){
  if(input.sourceId()==null && input.destinationId()==null || Objects.equals(input.sourceId(),input.destinationId()))throw error(HttpStatus.BAD_REQUEST,"Informe contas distintas para débito e/ou crédito.");
  String note=input.note().trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Justificativa deve conter pelo menos cinco caracteres.");
  lockTenant(a);Account source=input.sourceId()==null?null:find(a,input.sourceId()),destination=input.destinationId()==null?null:find(a,input.destinationId());
  String hash=origin.equals("MANUAL")?StockService.fingerprint("TREASURY_MOVEMENT",input.sourceId(),input.destinationId(),input.amount(),note):StockService.fingerprint(origin,input.sourceId(),input.destinationId(),input.amount(),note);
  var old=db.query("SELECT fingerprint FROM treasury_operation WHERE tenant_id=? AND id=?",(r,i)->r.getString(1),a.tenantId(),input.requestId());
  if(!old.isEmpty()){if(!old.getFirst().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador reutilizado com outros dados.");return operation(a,input.requestId());}
  requireUnreserved(a,input.sourceId(),allowedDrawer);requireUnreserved(a,input.destinationId(),allowedDrawer);
  if(source!=null && source.balance().compareTo(input.amount())<0)throw error(HttpStatus.CONFLICT,"Saldo insuficiente.");
  if(destination!=null && destination.balance().add(input.amount()).compareTo(new BigDecimal("99999999999999999.99"))>0)throw error(HttpStatus.CONFLICT,"Saldo excede limite monetário.");
  db.update("INSERT INTO treasury_operation(id,tenant_id,fingerprint,source_id,destination_id,amount,note,created_by,created_at,origin) VALUES(?,?,?,?,?,?,?,?,?,?)",input.requestId(),a.tenantId(),hash,input.sourceId(),input.destinationId(),input.amount(),note,a.userId(),OffsetDateTime.now(java.time.ZoneOffset.UTC),origin);
  if(source!=null)entry(a,input.requestId(),source,input.amount().negate());if(destination!=null)entry(a,input.requestId(),destination,input.amount());audit.record(a,"TREASURY_MOVEMENT_POSTED",input.requestId());return operation(a,input.requestId());
 }
 private void entry(Actor a,UUID operation,Account account,BigDecimal delta){BigDecimal balance=account.balance().add(delta);db.update("UPDATE treasury_account SET balance=? WHERE tenant_id=? AND id=?",balance,a.tenantId(),account.id());db.update("INSERT INTO treasury_entry(id,tenant_id,operation_id,account_id,delta,balance_after,sequence) VALUES(?,?,?,?,?,?,?)",UUID.randomUUID(),a.tenantId(),operation,account.id(),delta,balance,db.queryForObject("SELECT COUNT(*)+1 FROM treasury_entry WHERE tenant_id=? AND account_id=?",Long.class,a.tenantId(),account.id()));}
 @Transactional(readOnly=true) public Map<String,Object> entries(Actor a,UUID id,int page,int size){find(a,id);var items=db.query("SELECT e.id,e.operation_id,e.delta,e.balance_after,o.note,o.created_by,o.created_at,o.origin FROM treasury_entry e JOIN treasury_operation o ON o.tenant_id=e.tenant_id AND o.id=e.operation_id WHERE e.tenant_id=? AND e.account_id=? ORDER BY e.sequence DESC LIMIT ? OFFSET ?",(r,i)->new Entry(r.getObject("id",UUID.class),r.getObject("operation_id",UUID.class),r.getBigDecimal("delta"),r.getBigDecimal("balance_after"),r.getString("note"),r.getObject("created_by",UUID.class),r.getObject("created_at",OffsetDateTime.class),r.getString("origin")),a.tenantId(),id,size,(long)page*size);return Map.of("items",items,"total",db.queryForObject("SELECT COUNT(*) FROM treasury_entry WHERE tenant_id=? AND account_id=?",Long.class,a.tenantId(),id),"page",page,"size",size);}
}
