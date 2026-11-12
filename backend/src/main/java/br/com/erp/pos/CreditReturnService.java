package br.com.erp.pos;
import br.com.erp.auth.*;
import br.com.erp.finance.*;
import com.fasterxml.jackson.annotation.*;
import java.math.BigDecimal;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.pos.SaleReturnContracts.*;

@Service
public class CreditReturnService {
 private final JdbcTemplate db;private final TreasuryService treasury;private final SaleCreditService credit;
 public CreditReturnService(JdbcTemplate db,TreasuryService treasury,SaleCreditService credit){this.db=db;this.treasury=treasury;this.credit=credit;}
 private ResponseStatusException error(HttpStatus s,String text){return new ResponseStatusException(s,text);}
 public record ReceiptSource(UUID id,UUID receivableId,String titleReference,String method,String reference,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal refundedAmount,UUID treasuryAccountId,String accountKind){@JsonProperty @JsonFormat(shape=JsonFormat.Shape.STRING) public BigDecimal refundable(){return amount.subtract(refundedAmount);}}
 private String sourceSql(){return "SELECT r.*,t.reference AS title_reference,a.kind AS account_kind,COALESCE((SELECT SUM(f.amount) FROM pos_sale_return_receipt_refund f WHERE f.tenant_id=r.tenant_id AND f.receipt_id=r.id),0) AS refunded_amount FROM receivable_receipt r JOIN receivable t ON t.tenant_id=r.tenant_id AND t.id=r.receivable_id JOIN receivable_installment i ON i.tenant_id=t.tenant_id AND i.receivable_id=t.id LEFT JOIN treasury_account a ON a.tenant_id=r.tenant_id AND a.id=r.treasury_account_id WHERE r.tenant_id=? AND i.plan_id=? AND r.status='POSTED'";}
 private ReceiptSource row(java.sql.ResultSet r,int i)throws java.sql.SQLException{return new ReceiptSource(r.getObject("id",UUID.class),r.getObject("receivable_id",UUID.class),r.getString("title_reference"),r.getString("method"),r.getString("reference"),r.getBigDecimal("amount"),r.getBigDecimal("refunded_amount"),r.getObject("treasury_account_id",UUID.class),r.getString("account_kind"));}
 public void permissions(Actor a){credit.require(a,"receivable:read");credit.require(a,"receivable:credit");}
 public void readPermission(Actor a){credit.require(a,"receivable:read");}
 public Map<String,Object> sources(Actor a,SaleContracts.Sale sale,int page,int size){
  credit.require(a,"receivable:read");if(sale.credit()==null)return Map.of("items",List.of(),"total",0,"page",page,"size",size);
  // A sale has at most 120 titles; paginate receipts in SQL, not after loading the result set.
  String sql=sourceSql();var items=db.query(sql+" ORDER BY r.created_at,r.id LIMIT ? OFFSET ?",this::row,a.tenantId(),sale.credit().planId(),size,(long)page*size);
  Long count=db.queryForObject("SELECT COUNT(*) FROM receivable_receipt r JOIN receivable_installment i ON i.tenant_id=r.tenant_id AND i.receivable_id=r.receivable_id WHERE r.tenant_id=? AND i.plan_id=? AND r.status='POSTED'",Long.class,a.tenantId(),sale.credit().planId());
  return Map.of("items",items,"total",count,"page",page,"size",size);
 }
 public ReceiptSource source(Actor a,SaleContracts.Sale sale,UUID id){
  if(sale.credit()==null)throw error(HttpStatus.BAD_REQUEST,"Venda sem crediário não aceita reembolso de parcela.");
  var rows=db.query(sourceSql()+" AND r.id=?",this::row,a.tenantId(),sale.credit().planId(),id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Recebimento da venda não encontrado.");return rows.getFirst();
 }
 public BigDecimal debt(SaleContracts.Sale sale,BigDecimal returned){return sale.credit()==null?BigDecimal.ZERO:returned.min(sale.credit().outstanding());}
 public boolean proofExists(Actor a,UUID account,SaleContracts.Method method,String reference){
  return db.queryForObject("SELECT (SELECT COUNT(*) FROM pos_sale_return_refund WHERE tenant_id=? AND account_id=? AND method=? AND external_reference=?)+(SELECT COUNT(*) FROM pos_sale_return_receipt_refund WHERE tenant_id=? AND account_id=? AND method=? AND external_reference=?)",Long.class,a.tenantId(),account,method.name(),reference,a.tenantId(),account,method.name(),reference)>0;
 }
 public ReceiptRefund prepare(Actor a,SaleContracts.Sale sale,CashContracts.Session session,ReceiptRefundInput input,boolean confirmed){
  var source=source(a,sale,input.receiptId());if(input.amount().compareTo(source.refundable())>0)throw error(HttpStatus.CONFLICT,"Reembolso excede o saldo do recebimento da parcela.");
  boolean cash=input.method()==SaleContracts.Method.CASH;UUID account=cash?session.accountId():input.accountId();String ref=input.reference()==null?"":input.reference().trim();
  if(source.treasuryAccountId()!=null){
   treasury.requirePaymentAccount(a,source.treasuryAccountId());
   if("BANK".equals(source.accountKind())&&(cash||!source.treasuryAccountId().equals(account)))throw error(HttpStatus.BAD_REQUEST,"Recebimento bancário deve ser reembolsado pela conta original.");
   if("CASH".equals(source.accountKind())&&!cash)throw error(HttpStatus.BAD_REQUEST,"Recebimento em conta de caixa deve ser reembolsado pela gaveta atual.");
  }
  if(cash){if(input.accountId()!=null&&!input.accountId().equals(account)||!ref.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Dinheiro usa o caixa atual e não aceita comprovante externo.");}
  else{
   if(account==null||ref.isEmpty()||!confirmed)throw error(HttpStatus.BAD_REQUEST,"Reembolso bancário exige conta, comprovante e confirmação externa.");
   var bank=treasury.get(a,account);if(bank.kind()!=TreasuryContracts.Kind.BANK||source.treasuryAccountId()==null&&!bank.branchId().equals(sale.branchId()))throw error(HttpStatus.BAD_REQUEST,"Selecione conta bancária autorizada para o reembolso.");treasury.requirePaymentAccount(a,account);
   if(proofExists(a,account,input.method(),ref))throw error(HttpStatus.CONFLICT,"Comprovante de reembolso já registrado.");
  }
  return new ReceiptRefund(source.id(),source.receivableId(),input.method(),account,input.amount(),ref,UUID.randomUUID());
 }
 public List<DebtCredit> allocate(Actor a,SaleContracts.Sale sale,BigDecimal reduction){
  var result=new ArrayList<DebtCredit>();if(reduction.signum()==0)return result;
  var titles=db.query("SELECT r.id,r.reference,r.amount-r.received_amount-r.credited_amount AS outstanding FROM receivable r JOIN receivable_installment i ON i.tenant_id=r.tenant_id AND i.receivable_id=r.id WHERE i.tenant_id=? AND i.plan_id=? AND r.status IN ('OPEN','PARTIAL') ORDER BY r.due_on DESC,i.position DESC",(r,n)->new DebtCredit(r.getObject("id",UUID.class),r.getString("reference"),r.getBigDecimal("outstanding")),a.tenantId(),sale.credit().planId());
  BigDecimal left=reduction;for(var title:titles){BigDecimal value=left.min(title.amount());if(value.signum()>0)result.add(new DebtCredit(title.receivableId(),title.reference(),value));left=left.subtract(value);if(left.signum()==0)break;}
  if(left.signum()!=0)throw error(HttpStatus.CONFLICT,"Saldo da dívida alterado. Atualize a venda.");return result;
 }
 // The title settlement check requires amounts and status to change in one statement.
 public void persist(Actor a,UUID returnId,UUID sale,List<DebtCredit> debts,List<ReceiptRefund> refunds){
  var changes=new LinkedHashMap<UUID,BigDecimal[]>();for(var d:debts)changes.computeIfAbsent(d.receivableId(),k->new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO})[0]=d.amount();
  for(var f:refunds){var values=changes.computeIfAbsent(f.receivableId(),k->new BigDecimal[]{BigDecimal.ZERO,BigDecimal.ZERO});values[1]=values[1].add(f.amount());}
  for(var e:changes.entrySet()){
   BigDecimal creditAmount=e.getValue()[0].add(e.getValue()[1]),refund=e.getValue()[1];
   var amounts=db.queryForObject("SELECT amount,received_amount,credited_amount FROM receivable WHERE tenant_id=? AND id=? FOR UPDATE",(r,n)->new BigDecimal[]{r.getBigDecimal(1),r.getBigDecimal(2),r.getBigDecimal(3)},a.tenantId(),e.getKey());
   BigDecimal paid=amounts[1].subtract(refund),credited=amounts[2].add(creditAmount);
   if(paid.signum()<0||paid.add(credited).compareTo(amounts[0])>0)throw error(HttpStatus.CONFLICT,"Saldo do título mudou. Atualize antes de devolver.");
   String status=paid.add(credited).compareTo(amounts[0])==0?"CREDITED":paid.signum()==0?"OPEN":"PARTIAL";
   db.update("UPDATE receivable SET credited_amount=?,received_amount=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",credited,paid,status,a.tenantId(),e.getKey());
  }
  for(var d:debts)db.update("INSERT INTO pos_sale_return_debt_credit(tenant_id,return_id,sale_id,receivable_id,amount) VALUES(?,?,?,?,?)",a.tenantId(),returnId,sale,d.receivableId(),d.amount());
  for(var f:refunds)db.update("INSERT INTO pos_sale_return_receipt_refund(id,tenant_id,return_id,sale_id,receipt_id,receivable_id,method,account_id,amount,reference,external_reference,treasury_operation_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),a.tenantId(),returnId,sale,f.receiptId(),f.receivableId(),f.method().name(),f.accountId(),f.amount(),f.reference(),f.method()==SaleContracts.Method.CASH?null:f.reference(),f.treasuryOperationId());
 }
 public List<DebtCredit> debts(Actor a,UUID id){return db.query("SELECT d.*,r.reference FROM pos_sale_return_debt_credit d JOIN receivable r ON r.tenant_id=d.tenant_id AND r.id=d.receivable_id WHERE d.tenant_id=? AND d.return_id=? ORDER BY r.due_on DESC,r.id",(r,n)->new DebtCredit(r.getObject("receivable_id",UUID.class),r.getString("reference"),r.getBigDecimal("amount")),a.tenantId(),id);}
 public List<ReceiptRefund> refunds(Actor a,UUID id){return db.query("SELECT * FROM pos_sale_return_receipt_refund WHERE tenant_id=? AND return_id=? ORDER BY receipt_id",(r,n)->new ReceiptRefund(r.getObject("receipt_id",UUID.class),r.getObject("receivable_id",UUID.class),SaleContracts.Method.valueOf(r.getString("method")),r.getObject("account_id",UUID.class),r.getBigDecimal("amount"),r.getString("reference"),r.getObject("treasury_operation_id",UUID.class)),a.tenantId(),id);}
}
