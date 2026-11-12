package br.com.erp.pos;
import br.com.erp.auth.*;
import br.com.erp.finance.TreasuryService;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.pos.SaleContracts.*;
@Service
public class SaleCancellationService {
 private final CommercialBenefits benefits;private final SaleCreditService credit;private final JdbcTemplate db;private final SaleService sales;private final CashService cash;private final SalePayments payments;private final TreasuryService treasury;private final StockService stock;private final Audit audit;
 public SaleCancellationService(JdbcTemplate db,SaleService sales,CashService cash,SalePayments payments,TreasuryService treasury,StockService stock,Audit audit,SaleCreditService credit,CommercialBenefits benefits){this.benefits=benefits;this.credit=credit;this.db=db;this.sales=sales;this.cash=cash;this.payments=payments;this.treasury=treasury;this.stock=stock;this.audit=audit;}
 private ResponseStatusException error(HttpStatus status,String m){return new ResponseStatusException(status,m);}
 @Transactional public Sale cancel(Actor a,UUID id,CancellationInput input){
  db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());var sale=sales.get(a,id);var session=cash.get(a,sale.sessionId());
  if(!session.operatorId().equals(a.userId())&&db.queryForObject("SELECT COUNT(*) FROM user_permission WHERE tenant_id=? AND user_id=? AND permission='pos:supervise'",Long.class,a.tenantId(),a.userId())==0)throw error(HttpStatus.FORBIDDEN,"Cancelamento exige operador do caixa ou supervisor.");
  for(var p:sale.payments())treasury.requirePaymentAccount(a,p.accountId());String note=input.note().trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Justificativa deve conter cinco caracteres.");
  String hash=StockService.fingerprint("POS_SALE_CANCEL",id,input.version(),note,input.externalRefundsConfirmed());
  var old=db.query("SELECT id,cancel_fingerprint FROM pos_sale WHERE tenant_id=? AND cancel_request_id=?",(r,i)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),a.tenantId(),input.requestId());
  if(!old.isEmpty()){if(!old.getFirst().getKey().equals(id)||!old.getFirst().getValue().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de cancelamento reutilizado com outros dados.");return sale;}
  if(!sale.status().equals("COMPLETED")||sale.version()!=input.version())throw error(HttpStatus.CONFLICT,"Venda já cancelada ou versão alterada.");
  if(db.queryForObject("SELECT COUNT(*) FROM pos_sale_return WHERE tenant_id=? AND sale_id=?",Long.class,a.tenantId(),id)>0)throw error(HttpStatus.CONFLICT,"Venda já possui devoluções. Devolva apenas os itens restantes.");
  if(!session.status().equals("OPEN"))throw error(HttpStatus.CONFLICT,"Cancelamento exige a sessão original aberta.");
  if(sale.payments().stream().anyMatch(p->p.method()!=Method.CASH)&&!input.externalRefundsConfirmed())throw error(HttpStatus.BAD_REQUEST,"Confirme que os reembolsos Pix/transferência foram realizados externamente.");
  if(sale.credit()!=null)credit.cancel(a,sale.credit().planId(),note);
  for(var item:sale.items().stream().sorted(Comparator.comparing(Item::productId)).toList()){UUID operation=UUID.randomUUID();stock.returnSale(a,operation,id,item.stockOperationId(),sale.warehouseId(),item.productId(),item.unit(),item.quantity());db.update("UPDATE pos_sale_item SET return_operation_id=? WHERE tenant_id=? AND sale_id=? AND product_id=?",operation,a.tenantId(),id,item.productId());}
  payments.refund(a,sale,session);
  db.update("UPDATE pos_sale SET status='CANCELLED',version=version+1,cancel_request_id=?,cancel_fingerprint=?,cancel_note=?,cancelled_by=?,cancelled_at=? WHERE tenant_id=? AND id=?",input.requestId(),hash,note,a.userId(),OffsetDateTime.now(ZoneOffset.UTC),a.tenantId(),id);db.update("UPDATE pos_cash_session SET version=version+1 WHERE tenant_id=? AND id=?",a.tenantId(),session.id());audit.record(a,"POS_SALE_CANCELLED",id);benefits.reverse(a,id,input.requestId(),true);return sales.get(a,id);
 }
}
