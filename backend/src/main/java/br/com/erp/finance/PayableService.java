package br.com.erp.finance;
import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.annotation.Isolation;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.finance.PayableContracts.*;

@Service
public class PayableService {
    private final JdbcTemplate db;private final BranchAccess access;private final Audit audit;private final ZoneId zone;private final TreasuryService treasury;
    public PayableService(JdbcTemplate db,BranchAccess access,Audit audit,TreasuryService treasury,@Value("${erp.finance-zone:America/Sao_Paulo}") String zone){this.db=db;this.access=access;this.audit=audit;this.zone=ZoneId.of(zone);this.treasury=treasury;}
    private LocalDate today(){return LocalDate.now(zone);}
    public Map<String,String> calendar(){return Map.of("date",today().toString(),"zone",zone.getId());}
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
    private String note(String value){String note=value.trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Justificativa deve conter pelo menos cinco caracteres.");return note;}
    private View row(java.sql.ResultSet rs,int i)throws java.sql.SQLException{return rowAt(rs,i,today());}
    private View rowAt(java.sql.ResultSet rs,int i,LocalDate day)throws java.sql.SQLException{
        BigDecimal amount=rs.getBigDecimal("amount"),paid=rs.getBigDecimal("paid_amount");String status=rs.getString("status");LocalDate due=rs.getObject("due_on",LocalDate.class);
        return new View(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),rs.getObject("supplier_id",UUID.class),rs.getString("supplier_name"),rs.getObject("purchase_receipt_id",UUID.class),rs.getString("reference"),rs.getString("document"),rs.getString("description"),rs.getObject("issued_on",LocalDate.class),due,amount,paid,status.equals("CANCELLED")?BigDecimal.ZERO.setScale(2):amount.subtract(paid).subtract(rs.getBigDecimal("credited_amount")),status,rs.getLong("version"),rs.getObject("created_by",UUID.class),rs.getObject("created_at",OffsetDateTime.class),rs.getString("cancellation_note"),List.of("OPEN","PARTIAL").contains(status)&&due.isBefore(day),rs.getBigDecimal("credited_amount"));
    }
    private View find(Actor actor,UUID id,boolean lock){var rows=db.query("SELECT * FROM payable WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),this::row,actor.tenantId(),id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Título não encontrado.");var title=rows.getFirst();access.requireBranch(actor,title.branchId());return title;}
    @Transactional(readOnly=true) public View get(Actor actor,UUID id){return find(actor,id,false);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor actor,String status,boolean overdue,int page,int size){return list(actor,status,overdue,page,size,null);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor actor,String status,boolean overdue,int page,int size,PayableSearch.Filter filter){
        if(filter!=null&&filter.branchId()!=null)access.requireBranch(actor,filter.branchId());
        LocalDate day=today();var selection=PayableSearch.select(actor,status,overdue,day,filter);String where=selection.where();var args=new ArrayList<Object>(selection.args());
        Long total=db.queryForObject("SELECT COUNT(*) FROM payable t"+where,Long.class,args.toArray());
        var aggregateArgs=new ArrayList<Object>();aggregateArgs.add(day);aggregateArgs.addAll(args);
        var totals=db.queryForObject("SELECT COALESCE(SUM(t.amount),0),COALESCE(SUM(t.paid_amount),0),COALESCE(SUM(CASE WHEN t.status<>'CANCELLED' THEN t.amount-t.paid_amount-t.credited_amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN t.status IN ('OPEN','PARTIAL') AND t.due_on<? THEN t.amount-t.paid_amount-t.credited_amount ELSE 0 END),0) FROM payable t"+where,(r,i)->new Totals(r.getBigDecimal(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getBigDecimal(4)),aggregateArgs.toArray());
        args.add(size);args.add((long)page*size);
        var items=db.query("SELECT t.* FROM payable t"+where+" ORDER BY t.due_on,t.id LIMIT ? OFFSET ?",(r,i)->rowAt(r,i,day),args.toArray());
        return Map.of("items",items,"total",total,"page",page,"size",size,"summary",totals,"today",day,"zone",zone.getId());
    }

    @Transactional public View create(Actor actor,Input input){
        db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());access.requireBranch(actor,input.branchId());
        String description=note(input.description()),reference=input.reference().toUpperCase(Locale.ROOT),document=input.document().trim();
        if(input.dueOn().isBefore(input.issuedOn()))throw error(HttpStatus.BAD_REQUEST,"Vencimento não pode anteceder emissão.");
        String hash=StockService.fingerprint("PAYABLE",input.branchId(),input.supplierId(),input.purchaseReceiptId(),reference,document,description,input.issuedOn(),input.dueOn(),input.amount());
        var existing=db.query("SELECT id,fingerprint FROM payable WHERE tenant_id=? AND request_id=?",(rs,i)->Map.entry(rs.getObject(1,UUID.class),rs.getString(2)),actor.tenantId(),input.requestId());
        if(!existing.isEmpty()){if(!existing.getFirst().getValue().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de título reutilizado com outros dados.");return get(actor,existing.getFirst().getKey());}
        if(db.queryForObject("SELECT COUNT(*) FROM branch WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,actor.tenantId(),input.branchId())==0)throw error(HttpStatus.NOT_FOUND,"Filial ativa não encontrada.");
        var names=db.query("SELECT name FROM supplier WHERE tenant_id=? AND id=?",(rs,i)->rs.getString(1),actor.tenantId(),input.supplierId());if(names.isEmpty())throw error(HttpStatus.NOT_FOUND,"Fornecedor não encontrado.");
        if(input.purchaseReceiptId()!=null && db.queryForObject("SELECT COUNT(*) FROM purchase_receipt r JOIN purchase_order o ON o.tenant_id=r.tenant_id AND o.id=r.order_id WHERE r.tenant_id=? AND r.id=? AND o.branch_id=? AND o.supplier_id=?",Long.class,actor.tenantId(),input.purchaseReceiptId(),input.branchId(),input.supplierId())==0)throw error(HttpStatus.NOT_FOUND,"Recebimento da filial/fornecedor não encontrado.");
        if(input.purchaseReceiptId()!=null&&db.queryForObject("SELECT COUNT(*) FROM purchase_receipt_finance WHERE tenant_id=? AND receipt_id=?",Long.class,actor.tenantId(),input.purchaseReceiptId())>0)throw error(HttpStatus.CONFLICT,"Recebimento já possui geração financeira. Não vincule outro título a essa origem.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO payable(id,tenant_id,request_id,fingerprint,branch_id,supplier_id,supplier_name,purchase_receipt_id,reference,document,description,issued_on,due_on,amount,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,'OPEN',?)",id,actor.tenantId(),input.requestId(),hash,input.branchId(),input.supplierId(),names.getFirst(),input.purchaseReceiptId(),reference,document,description,input.issuedOn(),input.dueOn(),input.amount(),actor.userId());
        audit.record(actor,"PAYABLE_CREATED",id);return get(actor,id);
    }
    private Payment payment(Actor actor,UUID id){
        var rows=db.query("SELECT * FROM payable_payment WHERE tenant_id=? AND id=?",(rs,i)->new Payment(rs.getObject("id",UUID.class),rs.getObject("payable_id",UUID.class),rs.getBigDecimal("amount"),rs.getObject("paid_on",LocalDate.class),Method.valueOf(rs.getString("method")),rs.getString("reference"),rs.getString("note"),rs.getString("status"),rs.getObject("created_by",UUID.class),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("reversal_request_id",UUID.class),rs.getString("reversal_note"),rs.getObject("reversed_by",UUID.class),rs.getObject("reversed_at",OffsetDateTime.class),rs.getObject("treasury_account_id",UUID.class),rs.getObject("treasury_operation_id",UUID.class),rs.getObject("treasury_reversal_operation_id",UUID.class)),actor.tenantId(),id);
        if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Pagamento não encontrado.");return rows.getFirst();
    }
    private void lockTenant(Actor actor){db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());}
    @Transactional public Payment pay(Actor actor,UUID id,PaymentInput input){
        lockTenant(actor);View title=find(actor,id,true);String note=note(input.note()),reference=input.reference().trim();
        if(input.treasuryAccountId()!=null)treasury.requirePaymentAccount(actor,input.treasuryAccountId());
        if(input.paidOn().isAfter(today()) || input.paidOn().isBefore(title.issuedOn()))throw error(HttpStatus.BAD_REQUEST,"Data do pagamento deve estar entre emissão e hoje.");
        String hash=input.treasuryAccountId()==null?StockService.fingerprint("PAYABLE_PAYMENT",id,input.amount(),input.paidOn(),input.method(),reference,note):StockService.fingerprint("PAYABLE_PAYMENT_TREASURY",id,input.amount(),input.paidOn(),input.method(),reference,note,input.treasuryAccountId());
        var hashes=db.query("SELECT fingerprint FROM payable_payment WHERE tenant_id=? AND id=?",(rs,i)->rs.getString(1),actor.tenantId(),input.paymentId());
        if(!hashes.isEmpty()){if(!hashes.getFirst().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de pagamento reutilizado com outros dados.");return payment(actor,input.paymentId());}
        if(!List.of("OPEN","PARTIAL").contains(title.status()))throw error(HttpStatus.CONFLICT,"Título encerrado não aceita pagamento.");
        if(input.amount().compareTo(title.outstanding())>0)throw error(HttpStatus.CONFLICT,"Pagamento excede saldo pendente.");
        UUID operation=input.treasuryAccountId()==null?null:treasury.recordPayment(actor,input.treasuryAccountId(),input.amount(),note,false);
        db.update("INSERT INTO payable_payment(id,tenant_id,payable_id,fingerprint,amount,paid_on,method,reference,note,status,created_by,treasury_account_id,treasury_operation_id) VALUES(?,?,?,?,?,?,?,?,?,'POSTED',?,?,?)",input.paymentId(),actor.tenantId(),id,hash,input.amount(),input.paidOn(),input.method().name(),reference,note,actor.userId(),input.treasuryAccountId(),operation);
        BigDecimal paid=title.paidAmount().add(input.amount());String status=settlement(actor,id,paid,title.amount());
        db.update("UPDATE payable SET paid_amount=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",paid,status,actor.tenantId(),id);
        audit.record(actor,"PAYABLE_PAYMENT_POSTED",input.paymentId());return payment(actor,input.paymentId());
    }
    @Transactional public Payment reverse(Actor actor,UUID id,UUID paymentId,Reversal input){
        lockTenant(actor);View title=find(actor,id,true);Payment payment=payment(actor,paymentId);String note=note(input.note());
        if(!payment.payableId().equals(id))throw error(HttpStatus.NOT_FOUND,"Pagamento não encontrado.");
        if(payment.treasuryAccountId()!=null)treasury.requirePaymentAccount(actor,payment.treasuryAccountId());
        if(payment.status().equals("REVERSED")){if(input.requestId().equals(payment.reversalRequestId()) && note.equals(payment.reversalNote()))return payment;throw error(HttpStatus.CONFLICT,"Pagamento já estornado.");}
        if(title.purchaseReceiptId()!=null&&db.queryForObject("SELECT COUNT(*) FROM supplier_return WHERE tenant_id=? AND receipt_id=? AND supplier_credit>0",Long.class,actor.tenantId(),title.purchaseReceiptId())>0)throw error(HttpStatus.CONFLICT,"Recebimento possui crédito a reclamar do fornecedor. Reconcilie a devolução antes de estornar pagamentos.");
        UUID operation=payment.treasuryAccountId()==null?null:treasury.recordPayment(actor,payment.treasuryAccountId(),payment.amount(),note,true);
        db.update("UPDATE payable_payment SET status='REVERSED',reversal_request_id=?,reversal_note=?,reversed_by=?,reversed_at=CURRENT_TIMESTAMP,treasury_reversal_operation_id=? WHERE tenant_id=? AND id=?",input.requestId(),note,actor.userId(),operation,actor.tenantId(),paymentId);
        BigDecimal paid=title.paidAmount().subtract(payment.amount());String status=settlement(actor,id,paid,title.amount());
        db.update("UPDATE payable SET paid_amount=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",paid,status,actor.tenantId(),id);
        audit.record(actor,"PAYABLE_PAYMENT_REVERSED",paymentId);return payment(actor,paymentId);
    }
    private String settlement(Actor a,UUID id,BigDecimal paid,BigDecimal amount){BigDecimal credit=db.queryForObject("SELECT credited_amount FROM payable WHERE tenant_id=? AND id=?",BigDecimal.class,a.tenantId(),id);return paid.add(credit).compareTo(amount)==0?(credit.signum()>0?"CREDITED":"PAID"):(paid.signum()>0?"PARTIAL":"OPEN");}
    @Transactional public View cancel(Actor actor,UUID id,Cancellation input){
        lockTenant(actor);View title=find(actor,id,true);String note=note(input.note());
        if(title.status().equals("CANCELLED") && note.equals(title.cancellationNote()))return title;
        if(title.version()!=input.version())throw error(HttpStatus.CONFLICT,"Título alterado por outro acesso. Atualize.");
        if(!title.status().equals("OPEN") || title.paidAmount().signum()!=0 || title.outstanding().compareTo(title.amount())!=0)throw error(HttpStatus.CONFLICT,"Título com pagamentos ou abatimentos não pode ser cancelado.");
        db.update("UPDATE payable SET status='CANCELLED',cancellation_note=?,version=version+1 WHERE tenant_id=? AND id=?",note,actor.tenantId(),id);audit.record(actor,"PAYABLE_CANCELLED",id);return get(actor,id);
    }
    @Transactional(readOnly=true) public Map<String,Object> payments(Actor actor,UUID id,int page,int size){
        find(actor,id,false);var ids=db.query("SELECT id FROM payable_payment WHERE tenant_id=? AND payable_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),id,size,(long)page*size);
        return Map.of("items",ids.stream().map(value->payment(actor,value)).toList(),"total",db.queryForObject("SELECT COUNT(*) FROM payable_payment WHERE tenant_id=? AND payable_id=?",Long.class,actor.tenantId(),id),"page",page,"size",size);
    }
}
