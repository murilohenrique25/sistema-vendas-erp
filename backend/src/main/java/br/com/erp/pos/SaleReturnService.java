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
import static br.com.erp.pos.SaleReturnContracts.*;
@Service
public class SaleReturnService {
 private final CommercialBenefits benefits;private final CreditReturnService credit;private final JdbcTemplate db;private final SaleService sales;private final CashService cash;private final TreasuryService treasury;private final StockService stock;private final Audit audit;
 public SaleReturnService(JdbcTemplate db,SaleService sales,CashService cash,TreasuryService treasury,StockService stock,Audit audit,CreditReturnService credit,CommercialBenefits benefits){this.benefits=benefits;this.credit=credit;this.db=db;this.sales=sales;this.cash=cash;this.treasury=treasury;this.stock=stock;this.audit=audit;}
 private ResponseStatusException error(HttpStatus s,String m){return new ResponseStatusException(s,m);}
 private boolean supervisor(Actor a){return db.queryForObject("SELECT COUNT(*) FROM user_permission WHERE tenant_id=? AND user_id=? AND permission='pos:supervise'",Long.class,a.tenantId(),a.userId())>0;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Return get(Actor a,UUID sale,UUID id){var origin=sales.get(a,sale);if(origin.credit()!=null)credit.readPermission(a);var rows=db.query("SELECT * FROM pos_sale_return WHERE tenant_id=? AND sale_id=? AND id=?",(r,i)->new Return(r.getObject("id",UUID.class),sale,r.getObject("session_id",UUID.class),r.getBigDecimal("total"),r.getString("note"),r.getObject("created_by",UUID.class),r.getObject("created_at",OffsetDateTime.class),List.of(),List.of(),List.of(),List.of()),a.tenantId(),sale,id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Devolução não encontrada.");var h=rows.getFirst();var items=db.query("SELECT * FROM pos_sale_return_item WHERE tenant_id=? AND return_id=? ORDER BY product_id",(r,i)->new Item(r.getObject("product_id",UUID.class),r.getBigDecimal("quantity"),r.getBigDecimal("amount"),r.getObject("stock_operation_id",UUID.class)),a.tenantId(),id);var refunds=db.query("SELECT * FROM pos_sale_return_refund WHERE tenant_id=? AND return_id=? ORDER BY payment_id",(r,i)->new Refund(r.getObject("payment_id",UUID.class),SaleContracts.Method.valueOf(r.getString("method")),r.getObject("account_id",UUID.class),r.getBigDecimal("amount"),r.getString("reference"),r.getObject("treasury_operation_id",UUID.class)),a.tenantId(),id);return new Return(h.id(),sale,h.sessionId(),h.total(),h.note(),h.createdBy(),h.createdAt(),items,refunds,credit.debts(a,id),credit.refunds(a,id));}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor a,UUID sale,int page,int size){var origin=sales.get(a,sale);if(origin.credit()!=null)credit.readPermission(a);var ids=db.query("SELECT id FROM pos_sale_return WHERE tenant_id=? AND sale_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",(r,i)->r.getObject(1,UUID.class),a.tenantId(),sale,size,(long)page*size);return Map.of("items",ids.stream().map(id->get(a,sale,id)).toList(),"total",db.queryForObject("SELECT COUNT(*) FROM pos_sale_return WHERE tenant_id=? AND sale_id=?",Long.class,a.tenantId(),sale),"page",page,"size",size);}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> creditReceipts(Actor a,UUID sale,int page,int size){return credit.sources(a,sales.get(a,sale),page,size);}
 @Transactional public Return create(Actor a,UUID saleId,Input input){
  db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());var sale=sales.get(a,saleId);if(sale.credit()!=null)credit.permissions(a);var session=cash.get(a,input.sessionId());
  if(!session.branchId().equals(sale.branchId()))throw error(HttpStatus.BAD_REQUEST,"Caixa da devolução deve pertencer à filial da venda.");
  if(!session.operatorId().equals(a.userId())&&!supervisor(a))throw error(HttpStatus.FORBIDDEN,"Devolução exige operador do caixa escolhido ou supervisor.");treasury.requirePaymentAccount(a,session.accountId());
  String note=input.note().trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Informe justificativa com cinco caracteres.");
  var fields=new ArrayList<Object>(List.of("POS_RETURN",saleId,input.sessionId(),input.version(),note,input.externalRefundsConfirmed()));var seenProducts=new HashSet<UUID>();var seenPayments=new HashSet<UUID>();
  for(var item:input.items()){if(!seenProducts.add(item.productId()))throw error(HttpStatus.BAD_REQUEST,"Agrupe as quantidades de cada produto.");fields.add(item.productId());fields.add(item.quantity());}fields.add("REFUNDS");
  for(var refund:input.refunds()){if(!seenPayments.add(refund.paymentId()))throw error(HttpStatus.BAD_REQUEST,"Agrupe cada pagamento de origem.");var payment=sale.payments().stream().filter(p->p.id().equals(refund.paymentId())).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Pagamento da venda não encontrado."));treasury.requirePaymentAccount(a,payment.accountId());fields.add(refund.paymentId());fields.add(refund.amount());fields.add(refund.reference()==null?"":refund.reference().trim());}
  if(sale.credit()!=null){fields.add("CREDIT_RETURN");fields.add(input.expectedDebtReduction());}
  var receiptInputs=input.receiptRefunds()==null?List.<ReceiptRefundInput>of():input.receiptRefunds();var seenReceipts=new HashSet<UUID>();
  if(!receiptInputs.isEmpty()){fields.add("RECEIPT_REFUNDS");for(var f:receiptInputs){if(!seenReceipts.add(f.receiptId()))throw error(HttpStatus.BAD_REQUEST,"Agrupe cada recebimento de origem.");credit.source(a,sale,f.receiptId());Collections.addAll(fields,f.receiptId(),f.amount(),f.method(),f.accountId(),f.reference()==null?"":f.reference().trim());}}
  String hash=StockService.fingerprint(fields.toArray());var old=db.query("SELECT id,sale_id,fingerprint FROM pos_sale_return WHERE tenant_id=? AND request_id=?",(r,i)->new Object[]{r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getString(3)},a.tenantId(),input.requestId());
  if(!old.isEmpty()){var row=old.getFirst();if(!saleId.equals(row[1])||!hash.equals(row[2]))throw error(HttpStatus.CONFLICT,"Identificador de devolução reutilizado com outros dados.");return get(a,saleId,(UUID)row[0]);}
  if(!sale.status().equals("COMPLETED")||sale.version()!=input.version())throw error(HttpStatus.CONFLICT,"Venda cancelada ou alterada. Atualize antes de devolver.");if(!session.status().equals("OPEN"))throw error(HttpStatus.CONFLICT,"Devolução exige caixa aberto.");
  var lines=new ArrayList<Item>();BigDecimal total=BigDecimal.ZERO;
  for(var item:input.items()){
   var line=sale.items().stream().filter(i->i.productId().equals(item.productId())).findFirst().orElseThrow(()->error(HttpStatus.NOT_FOUND,"Item da venda não encontrado."));BigDecimal cumulative=line.returnedQuantity().add(item.quantity());
   if(cumulative.compareTo(line.quantity())>0)throw error(HttpStatus.CONFLICT,"Quantidade excede o saldo ainda não devolvido.");
   BigDecimal amount=line.total().multiply(cumulative).divide(line.quantity(),2,RoundingMode.HALF_UP).subtract(line.returnedAmount());if(amount.signum()<0)throw error(HttpStatus.CONFLICT,"Saldo de devolução inconsistente.");total=total.add(amount);lines.add(new Item(line.productId(),item.quantity(),amount,UUID.randomUUID()));
  }
  BigDecimal debt=credit.debt(sale,total);
  if(sale.credit()!=null){if(input.expectedDebtReduction()==null)throw error(HttpStatus.BAD_REQUEST,"Confira e informe o abatimento esperado da dívida.");if(debt.compareTo(input.expectedDebtReduction())!=0)throw error(HttpStatus.CONFLICT,"Saldo da dívida mudou. Atualize e confira a divisão da devolução.");}
  else if(input.expectedDebtReduction()!=null&&input.expectedDebtReduction().signum()!=0||!receiptInputs.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Venda sem crediário não aceita abatimento ou recebimento de parcela.");
  var debtCredits=credit.allocate(a,sale,debt);
  var refunds=new ArrayList<Refund>();BigDecimal refundTotal=BigDecimal.ZERO;var references=new HashSet<String>();
  for(var refund:input.refunds()){
   var payment=sale.payments().stream().filter(p->p.id().equals(refund.paymentId())).findFirst().orElseThrow();
   if(refund.amount().compareTo(payment.amount().subtract(payment.refundedAmount()))>0)throw error(HttpStatus.CONFLICT,"Reembolso excede o saldo do pagamento original.");
   boolean isCash=payment.method()==SaleContracts.Method.CASH;UUID account=isCash?session.accountId():payment.accountId();String reference=refund.reference()==null?"":refund.reference().trim();
   if(isCash&&!reference.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Dinheiro não aceita comprovante bancário.");
   if(!isCash){if(!input.externalRefundsConfirmed()||reference.isEmpty())throw error(HttpStatus.BAD_REQUEST,"Confirme o reembolso externo e informe o comprovante.");String key=account+":"+payment.method()+":"+reference;if(!references.add(key)||credit.proofExists(a,account,payment.method(),reference))throw error(HttpStatus.CONFLICT,"Comprovante de reembolso já registrado.");}
   refunds.add(new Refund(payment.id(),payment.method(),account,refund.amount(),reference,UUID.randomUUID()));refundTotal=refundTotal.add(refund.amount());
  }
  var receiptRefunds=new ArrayList<ReceiptRefund>();
  for(var inputRefund:receiptInputs){var f=credit.prepare(a,sale,session,inputRefund,input.externalRefundsConfirmed());if(f.method()!=SaleContracts.Method.CASH&&!references.add(f.accountId()+":"+f.method()+":"+f.reference()))throw error(HttpStatus.CONFLICT,"Comprovante repetido na devolução.");receiptRefunds.add(f);refundTotal=refundTotal.add(f.amount());}
  if(total.compareTo(refundTotal.add(debt))!=0)throw error(HttpStatus.BAD_REQUEST,"Abatimento da dívida e reembolsos devem corresponder ao valor histórico devolvido.");
  UUID id=UUID.randomUUID();for(var line:lines.stream().sorted(Comparator.comparing(Item::productId)).toList()){var original=sale.items().stream().filter(i->i.productId().equals(line.productId())).findFirst().orElseThrow();stock.returnSale(a,line.stockOperationId(),saleId,original.stockOperationId(),sale.warehouseId(),line.productId(),original.unit(),line.quantity(),"Devolução "+id+" da venda "+saleId);}
  for(var refund:refunds){var movement=new TreasuryContracts.Movement(refund.treasuryOperationId(),refund.accountId(),null,refund.amount(),"Devolução "+id+" da venda "+saleId);if(refund.method()==SaleContracts.Method.CASH)treasury.recordDrawerMovement(a,session.accountId(),movement,"POS_SALE_RETURN");else treasury.recordSaleReturnBankMovement(a,movement);}
  for(var f:receiptRefunds){var movement=new TreasuryContracts.Movement(f.treasuryOperationId(),f.accountId(),null,f.amount(),"Reembolso de parcela na devolução "+id);if(f.method()==SaleContracts.Method.CASH)treasury.recordDrawerMovement(a,session.accountId(),movement,"POS_SALE_RETURN");else treasury.recordSaleReturnBankMovement(a,movement);}
  db.update("INSERT INTO pos_sale_return(id,tenant_id,request_id,fingerprint,sale_id,session_id,total,note,created_by,created_at) VALUES(?,?,?,?,?,?,?,?,?,?)",id,a.tenantId(),input.requestId(),hash,saleId,session.id(),total,note,a.userId(),OffsetDateTime.now(ZoneOffset.UTC));
  for(var line:lines)db.update("INSERT INTO pos_sale_return_item(id,tenant_id,return_id,sale_id,product_id,quantity,amount,stock_operation_id) VALUES(?,?,?,?,?,?,?,?)",UUID.randomUUID(),a.tenantId(),id,saleId,line.productId(),line.quantity(),line.amount(),line.stockOperationId());
  for(var refund:refunds)db.update("INSERT INTO pos_sale_return_refund(id,tenant_id,return_id,sale_id,payment_id,method,account_id,amount,reference,external_reference,treasury_operation_id) VALUES(?,?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),a.tenantId(),id,saleId,refund.paymentId(),refund.method().name(),refund.accountId(),refund.amount(),refund.reference(),refund.method()==SaleContracts.Method.CASH?null:refund.reference(),refund.treasuryOperationId());
  credit.persist(a,id,saleId,debtCredits,receiptRefunds);benefits.reverse(a,saleId,id,false);
  db.update("UPDATE pos_sale SET version=version+1 WHERE tenant_id=? AND id=?",a.tenantId(),saleId);db.update("UPDATE pos_cash_session SET version=version+1 WHERE tenant_id=? AND id=?",a.tenantId(),session.id());audit.record(a,"POS_SALE_RETURNED",id);return get(a,saleId,id);
 }
}
