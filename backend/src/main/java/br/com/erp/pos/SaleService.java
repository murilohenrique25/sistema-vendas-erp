package br.com.erp.pos;
import br.com.erp.auth.*;
import br.com.erp.finance.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.pos.SaleContracts.*;
@Service
public class SaleService {
 private final CommercialBenefits benefits;private final SaleCreditService credit;private final CartPricing pricing;private final DiscountService discounts;private final SuspensionService suspensions;private final SalePayments payments;private final JdbcTemplate db;private final CashService cash;private final StockService stock;private final TreasuryService treasury;private final BranchAccess access;private final Audit audit;
 private static final BigDecimal MAX=new BigDecimal("99999999999999999.99");
 public SaleService(JdbcTemplate db,CashService cash,StockService stock,TreasuryService treasury,BranchAccess access,Audit audit,SalePayments payments,CartPricing pricing,DiscountService discounts,SuspensionService suspensions,SaleCreditService credit,CommercialBenefits benefits){this.benefits=benefits;this.credit=credit;this.pricing=pricing;this.discounts=discounts;this.suspensions=suspensions;this.payments=payments;this.db=db;this.cash=cash;this.stock=stock;this.treasury=treasury;this.access=access;this.audit=audit;}
 private ResponseStatusException error(HttpStatus s,String message){return new ResponseStatusException(s,message);}
 private void lock(Actor a){db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());}
 @Transactional public CashContracts.Terminal bind(Actor a,UUID id,WarehouseBinding input){
  lock(a);var t=cash.terminal(a,id);access.requireWarehouse(a,input.warehouseId());
  if(db.queryForObject("SELECT COUNT(*) FROM warehouse WHERE tenant_id=? AND id=? AND branch_id=? AND active=TRUE",Long.class,a.tenantId(),input.warehouseId(),t.branchId())!=1)throw error(HttpStatus.BAD_REQUEST,"Depósito ativo deve pertencer à filial do terminal.");
  if(t.version()!=input.version())throw error(HttpStatus.CONFLICT,"Terminal alterado. Atualize.");
  if(db.queryForObject("SELECT COUNT(*) FROM pos_cash_session WHERE tenant_id=? AND open_terminal_id=?",Long.class,a.tenantId(),id)>0)throw error(HttpStatus.CONFLICT,"Feche o caixa antes de alterar o depósito.");
  db.update("UPDATE pos_terminal SET warehouse_id=?,version=version+1 WHERE tenant_id=? AND id=?",input.warehouseId(),a.tenantId(),id);audit.record(a,"POS_TERMINAL_WAREHOUSE",id);return cash.terminal(a,id);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Sale get(Actor a,UUID id){
  var rows=db.query("SELECT * FROM pos_sale WHERE tenant_id=? AND id=?",(r,i)->new Sale(r.getObject("id",UUID.class),r.getObject("session_id",UUID.class),r.getObject("branch_id",UUID.class),r.getObject("warehouse_id",UUID.class),r.getBigDecimal("total"),r.getBigDecimal("tendered"),r.getBigDecimal("change_amount"),r.getObject("treasury_operation_id",UUID.class),r.getObject("created_by",UUID.class),r.getObject("created_at",OffsetDateTime.class),List.of(),r.getString("status"),r.getLong("version"),r.getString("cancel_note"),r.getObject("cancelled_by",UUID.class),r.getObject("cancelled_at",OffsetDateTime.class),List.of(),r.getObject("discount_approval_id",UUID.class),r.getObject("suspension_id",UUID.class),credit.readSale(a,r.getObject("credit_plan_id",UUID.class))),a.tenantId(),id);
  if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Venda não encontrada.");var s=rows.getFirst();access.requireBranch(a,s.branchId());
  var items=db.query("SELECT i.*,COALESCE((SELECT SUM(r.quantity) FROM pos_sale_return_item r WHERE r.tenant_id=i.tenant_id AND r.sale_id=i.sale_id AND r.product_id=i.product_id),0) AS returned_quantity,COALESCE((SELECT SUM(r.amount) FROM pos_sale_return_item r WHERE r.tenant_id=i.tenant_id AND r.sale_id=i.sale_id AND r.product_id=i.product_id),0) AS returned_amount FROM pos_sale_item i WHERE i.tenant_id=? AND i.sale_id=? ORDER BY i.position",(r,i)->new Item(r.getObject("product_id",UUID.class),r.getString("sku"),r.getString("name"),r.getString("unit"),r.getBigDecimal("quantity"),r.getBigDecimal("unit_price"),r.getBigDecimal("total"),r.getObject("stock_operation_id",UUID.class),s.status().equals("CANCELLED")?r.getBigDecimal("quantity"):r.getBigDecimal("returned_quantity"),s.status().equals("CANCELLED")?r.getBigDecimal("total"):r.getBigDecimal("returned_amount"),r.getBigDecimal("gross_total"),r.getBigDecimal("discount_amount")),a.tenantId(),id);
  return new Sale(s.id(),s.sessionId(),s.branchId(),s.warehouseId(),s.total(),s.tendered(),s.changeAmount(),s.treasuryOperationId(),s.createdBy(),s.createdAt(),items,s.status(),s.version(),s.cancelNote(),s.cancelledBy(),s.cancelledAt(),payments.read(a,id),s.discountApprovalId(),s.suspensionId(),s.credit());
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor a,int page,int size){return list(a,page,size,null);}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor a,int page,int size,SaleHistoryQuery.Filter filter){
  if(filter!=null&&filter.branchId()!=null)access.requireBranch(a,filter.branchId());
  var selection=SaleHistoryQuery.select(a,filter);var args=new ArrayList<Object>(selection.args());
  Long total=db.queryForObject("SELECT COUNT(*) FROM pos_sale s"+selection.where(),Long.class,args.toArray());
  args.add(size);args.add((long)page*size);
  var ids=db.query("SELECT s.id FROM pos_sale s"+selection.where()+" ORDER BY s.created_at DESC,s.id LIMIT ? OFFSET ?",(r,i)->r.getObject(1,UUID.class),args.toArray());
  return Map.of("items",ids.stream().map(id->get(a,id)).toList(),"total",total,"page",page,"size",size);
 }

 private record Product(UUID id,String sku,String name,String unit,BigDecimal price){}
 @Transactional public Sale complete(Actor a,Input input){
  lock(a);var session=cash.get(a,input.sessionId());
  if(!session.operatorId().equals(a.userId())&&db.queryForObject("SELECT COUNT(*) FROM user_permission WHERE tenant_id=? AND user_id=? AND permission='pos:supervise'",Long.class,a.tenantId(),a.userId())==0)throw error(HttpStatus.FORBIDDEN,"Venda exige operador do caixa ou supervisor.");
  treasury.requirePaymentAccount(a,session.accountId());payments.requireAccounts(a,input);
  var fields=new ArrayList<Object>(Arrays.asList("POS_SALE",input.sessionId(),input.tendered()));var seen=new HashSet<UUID>();
  for(var i:input.items()){if(!seen.add(i.productId()))throw error(HttpStatus.BAD_REQUEST,"Agrupe quantidades do mesmo produto.");fields.add(i.productId());fields.add(i.quantity());fields.add(i.expectedUnitPrice());}
  if(input.payments()!=null){fields.add("PAYMENTS");for(var p:input.payments()){fields.add(p.method());fields.add(p.accountId());fields.add(p.amount());fields.add(p.tendered());fields.add(p.reference()==null?"":p.reference().trim());fields.add(p.confirmed());}}
  if(input.items().stream().anyMatch(i->CartPricing.discount(i).signum()>0)){pricing.require(a,"pos:discount");fields.add("DISCOUNTS");for(var i:input.items())fields.add(CartPricing.discount(i));}
  if(input.discountApprovalId()!=null)Collections.addAll(fields,"APPROVAL",input.discountApprovalId());
  if((input.suspensionId()==null)!=(input.suspensionVersion()==null))throw error(HttpStatus.BAD_REQUEST,"Informe identidade e versão da suspensão juntas.");
  if(input.suspensionId()!=null){pricing.require(a,"pos:suspend");suspensions.get(a,input.suspensionId());Collections.addAll(fields,"SUSPENSION",input.suspensionId(),input.suspensionVersion());}
  if(input.credit()!=null){credit.permissions(a);var c=input.credit();Collections.addAll(fields,"CREDIT",c.customerId(),c.amount(),c.firstDueOn(),c.installmentCount(),c.intervalMonths());}
  if(input.salespersonId()!=null||input.loyaltyCustomerId()!=null)Collections.addAll(fields,"COMMERCIAL",input.salespersonId(),input.loyaltyCustomerId());
  String hash=StockService.fingerprint(fields.toArray());
  var old=db.query("SELECT id,fingerprint FROM pos_sale WHERE tenant_id=? AND request_id=?",(r,i)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),a.tenantId(),input.requestId());
  if(!old.isEmpty()){if(!old.getFirst().getValue().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de venda reutilizado com outros dados.");return get(a,old.getFirst().getKey());}
  if(!session.status().equals("OPEN"))throw error(HttpStatus.CONFLICT,"Venda exige caixa aberto.");var terminal=cash.terminal(a,session.terminalId());UUID warehouse=terminal.warehouseId();
  if(!terminal.active()||warehouse==null)throw error(HttpStatus.CONFLICT,"Configure o depósito do terminal antes da venda.");access.requireWarehouse(a,warehouse);
  if(db.queryForObject("SELECT COUNT(*) FROM warehouse w JOIN branch b ON b.tenant_id=w.tenant_id AND b.id=w.branch_id WHERE w.tenant_id=? AND w.id=? AND w.branch_id=? AND w.active=TRUE AND b.active=TRUE",Long.class,a.tenantId(),warehouse,session.branchId())!=1)throw error(HttpStatus.CONFLICT,"Filial ou depósito inativo.");
  var quote=pricing.quote(a,session.branchId(),input.items());discounts.authorize(a,session.branchId(),quote,input.discountApprovalId());suspensions.authorizeCheckout(a,input.suspensionId(),input.suspensionVersion(),session.branchId(),quote);
  BigDecimal total=quote.total();var items=quote.items().stream().map(i->new Item(i.productId(),i.sku(),i.name(),i.unit(),i.quantity(),i.unitPrice(),i.total(),UUID.randomUUID(),BigDecimal.ZERO,BigDecimal.ZERO,i.grossTotal(),i.discountAmount())).toList();
  UUID id=UUID.randomUUID();BigDecimal creditAmount=input.credit()==null?BigDecimal.ZERO:input.credit().amount();
  UUID plan=input.credit()==null?null:credit.originate(a,id,session.branchId(),input.credit(),total);
  BigDecimal immediate=total.subtract(creditAmount);List<Payment> paymentLines;
  if(immediate.signum()==0){if(input.payments()!=null||input.tendered()!=null)throw error(HttpStatus.BAD_REQUEST,"Venda totalmente a prazo não recebe pagamento inicial.");paymentLines=List.of();}
  else paymentLines=payments.prepare(a,input,session,immediate);
  BigDecimal tendered=paymentLines.stream().map(Payment::tendered).reduce(BigDecimal.ZERO,BigDecimal::add);
  for(var i:items)stock.consumeSale(a,i.stockOperationId(),id,warehouse,i.productId(),i.unit(),i.quantity());
  payments.post(a,id,session,paymentLines);UUID operation=paymentLines.isEmpty()?null:paymentLines.getFirst().treasuryOperationId();
  db.update("INSERT INTO pos_sale(id,tenant_id,request_id,fingerprint,session_id,branch_id,warehouse_id,total,tendered,change_amount,treasury_operation_id,created_by,created_at,discount_approval_id,suspension_id,credit_amount,credit_plan_id,credit_customer_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",id,a.tenantId(),input.requestId(),hash,session.id(),session.branchId(),warehouse,total,tendered,tendered.subtract(immediate),operation,a.userId(),OffsetDateTime.now(ZoneOffset.UTC),input.discountApprovalId(),input.suspensionId(),creditAmount,plan,input.credit()==null?null:input.credit().customerId());
  payments.persist(a,id,paymentLines);int position=0;for(var i:items)db.update("INSERT INTO pos_sale_item(id,tenant_id,sale_id,product_id,position,sku,name,unit,quantity,unit_price,total,stock_operation_id,gross_total,discount_amount) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),a.tenantId(),id,i.productId(),position++,i.sku(),i.name(),i.unit(),i.quantity(),i.unitPrice(),i.total(),i.stockOperationId(),i.grossTotal(),i.discountAmount());
  benefits.originate(a,id,session.branchId(),total,input);
  discounts.consume(a,input.discountApprovalId(),id);suspensions.complete(a,input.suspensionId(),session.id(),id);
  db.update("UPDATE pos_cash_session SET version=version+1 WHERE tenant_id=? AND id=?",a.tenantId(),session.id());audit.record(a,"POS_SALE_COMPLETED",id);return get(a,id);
 }
}
