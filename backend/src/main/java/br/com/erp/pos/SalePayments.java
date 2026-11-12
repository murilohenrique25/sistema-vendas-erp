package br.com.erp.pos;
import br.com.erp.auth.*;
import br.com.erp.finance.*;
import java.math.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.pos.SaleContracts.*;
@Service
public class SalePayments {
 private final JdbcTemplate db;private final TreasuryService treasury;
 public SalePayments(JdbcTemplate db,TreasuryService treasury){this.db=db;this.treasury=treasury;}
 private ResponseStatusException invalid(String m){return new ResponseStatusException(HttpStatus.BAD_REQUEST,m);}
 public void requireAccounts(Actor a,Input input){if(input.payments()!=null)for(var p:input.payments())if(p.accountId()!=null)treasury.requirePaymentAccount(a,p.accountId());}
 public List<Payment> prepare(Actor a,Input input,CashContracts.Session session,BigDecimal total){
  var allocation=PaymentAllocation.calculate(total,input.tendered(),input.payments(),session.accountId());
  if(!allocation.balanced())throw invalid("A soma dos pagamentos confirmados deve ser igual ao total da venda.");
  var result=new ArrayList<Payment>();
  for(var p:allocation.lines()){
   if(p.method()!=Method.CASH){
    var account=treasury.get(a,p.accountId());
    if(account.kind()!=TreasuryContracts.Kind.BANK||!account.branchId().equals(session.branchId()))throw invalid("Pagamento bancário exige conta BANK da filial do caixa.");
    if(db.queryForObject("SELECT COUNT(*) FROM pos_sale_payment WHERE tenant_id=? AND account_id=? AND method=? AND external_reference=?",Long.class,a.tenantId(),p.accountId(),p.method().name(),p.reference())>0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Comprovante já registrado nesta conta e meio de pagamento.");
   }
   result.add(new Payment(UUID.randomUUID(),p.method(),p.accountId(),p.amount(),p.tendered(),p.change(),p.reference(),"CONFIRMED",UUID.randomUUID(),null,BigDecimal.ZERO));
  }
  return result;
 }
 public void post(Actor a,UUID sale,CashContracts.Session session,List<Payment> payments){for(var p:payments){var m=new TreasuryContracts.Movement(p.treasuryOperationId(),null,p.accountId(),p.amount(),"Venda "+sale);if(p.method()==Method.CASH)treasury.recordDrawerMovement(a,session.accountId(),m,"POS_SALE");else treasury.recordSaleBankMovement(a,m,false);}}
 public void persist(Actor a,UUID sale,List<Payment> payments){int position=0;for(var p:payments)db.update("INSERT INTO pos_sale_payment(id,tenant_id,sale_id,position,method,account_id,amount,tendered,change_amount,reference,external_reference,treasury_operation_id) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",p.id(),a.tenantId(),sale,position++,p.method().name(),p.accountId(),p.amount(),p.tendered(),p.changeAmount(),p.reference(),p.method()==Method.CASH?null:p.reference(),p.treasuryOperationId());}
 public List<Payment> read(Actor a,UUID sale){return db.query("SELECT p.*,CASE WHEN p.status='REFUNDED' THEN p.amount ELSE COALESCE((SELECT SUM(r.amount) FROM pos_sale_return_refund r WHERE r.tenant_id=p.tenant_id AND r.payment_id=p.id),0) END AS refunded_amount FROM pos_sale_payment p WHERE p.tenant_id=? AND p.sale_id=? ORDER BY p.position",(r,i)->new Payment(r.getObject("id",UUID.class),Method.valueOf(r.getString("method")),r.getObject("account_id",UUID.class),r.getBigDecimal("amount"),r.getBigDecimal("tendered"),r.getBigDecimal("change_amount"),r.getString("reference"),r.getString("status"),r.getObject("treasury_operation_id",UUID.class),r.getObject("reversal_operation_id",UUID.class),r.getBigDecimal("refunded_amount")),a.tenantId(),sale);}
 public void refund(Actor a,Sale sale,CashContracts.Session session){for(var p:sale.payments()){UUID operation=UUID.randomUUID();var m=new TreasuryContracts.Movement(operation,p.accountId(),null,p.amount(),"Cancelamento da venda "+sale.id());if(p.method()==Method.CASH)treasury.recordDrawerMovement(a,session.accountId(),m,"POS_SALE_REVERSAL");else treasury.recordSaleBankMovement(a,m,true);db.update("UPDATE pos_sale_payment SET status='REFUNDED',reversal_operation_id=? WHERE tenant_id=? AND id=?",operation,a.tenantId(),p.id());}}
}
