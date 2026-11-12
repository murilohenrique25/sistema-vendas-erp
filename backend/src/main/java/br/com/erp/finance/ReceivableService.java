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
import static br.com.erp.finance.ReceivableContracts.*;

@Service
public class ReceivableService {
    private final JdbcTemplate db;private final BranchAccess access;private final Audit audit;private final ZoneId zone;private final TreasuryService treasury;
    public ReceivableService(JdbcTemplate db,BranchAccess access,Audit audit,TreasuryService treasury,@Value("${erp.finance-zone:America/Sao_Paulo}") String zone){this.db=db;this.access=access;this.audit=audit;this.zone=ZoneId.of(zone);this.treasury=treasury;}
    private LocalDate today(){return LocalDate.now(zone);}
    public Map<String,String> calendar(){return Map.of("date",today().toString(),"zone",zone.getId());}
    private ResponseStatusException error(HttpStatus status,String message){return new ResponseStatusException(status,message);}
    private String note(String value){String note=value.trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Justificativa deve conter pelo menos cinco caracteres.");return note;}
    private View row(java.sql.ResultSet rs,int i)throws java.sql.SQLException{return rowAt(rs,i,today());}
    private View rowAt(java.sql.ResultSet rs,int i,LocalDate day)throws java.sql.SQLException{
        BigDecimal amount=rs.getBigDecimal("amount"),paid=rs.getBigDecimal("received_amount");String status=rs.getString("status");LocalDate due=rs.getObject("due_on",LocalDate.class);
        return new View(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),rs.getObject("customer_id",UUID.class),rs.getString("customer_name"),rs.getString("reference"),rs.getString("document"),rs.getString("description"),rs.getObject("issued_on",LocalDate.class),due,amount,paid,status.equals("CANCELLED")?BigDecimal.ZERO.setScale(2):amount.subtract(paid).subtract(rs.getBigDecimal("credited_amount")),status,rs.getLong("version"),rs.getObject("created_by",UUID.class),rs.getObject("created_at",OffsetDateTime.class),rs.getString("cancellation_note"),List.of("OPEN","PARTIAL").contains(status)&&due.isBefore(day),rs.getBigDecimal("credited_amount"));
    }
    private View find(Actor actor,UUID id,boolean lock){var rows=db.query("SELECT * FROM receivable WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),this::row,actor.tenantId(),id);if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Título não encontrado.");var title=rows.getFirst();access.requireBranch(actor,title.branchId());return title;}
    @Transactional(readOnly=true) public View get(Actor actor,UUID id){return find(actor,id,false);}
    @Transactional(readOnly=true) public Map<String,Object> adjustments(Actor actor,UUID id,int page,int size){
        find(actor,id,false);
        String union="SELECT d.return_id,d.sale_id,'DEBT_CREDIT' AS kind,d.amount,h.created_at,h.note,CAST(NULL AS UUID) AS receipt_id,CAST(NULL AS UUID) AS treasury_operation_id FROM pos_sale_return_debt_credit d JOIN pos_sale_return h ON h.tenant_id=d.tenant_id AND h.id=d.return_id WHERE d.tenant_id=? AND d.receivable_id=? UNION ALL SELECT f.return_id,f.sale_id,'RECEIPT_REFUND' AS kind,f.amount,h.created_at,h.note,f.receipt_id,f.treasury_operation_id FROM pos_sale_return_receipt_refund f JOIN pos_sale_return h ON h.tenant_id=f.tenant_id AND h.id=f.return_id WHERE f.tenant_id=? AND f.receivable_id=?";
        var items=db.query("SELECT * FROM ("+union+") a ORDER BY created_at,return_id,kind,receipt_id LIMIT ? OFFSET ?",(r,n)->new Adjustment(r.getObject("return_id",UUID.class),r.getObject("sale_id",UUID.class),r.getString("kind"),r.getBigDecimal("amount"),r.getObject("created_at",OffsetDateTime.class),r.getString("note"),r.getObject("receipt_id",UUID.class),r.getObject("treasury_operation_id",UUID.class)),actor.tenantId(),id,actor.tenantId(),id,size,(long)page*size);
        return Map.of("items",items,"total",db.queryForObject("SELECT COUNT(*) FROM ("+union+") a",Long.class,actor.tenantId(),id,actor.tenantId(),id),"page",page,"size",size);
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Origin origin(Actor actor,UUID id){
        find(actor,id,false);
        var rows=db.query("SELECT i.plan_id,i.position,p.installment_count,s.id AS sale_id,p.reference FROM receivable_installment i JOIN receivable_plan p ON p.tenant_id=i.tenant_id AND p.id=i.plan_id LEFT JOIN pos_sale s ON s.tenant_id=i.tenant_id AND s.credit_plan_id=i.plan_id WHERE i.tenant_id=? AND i.receivable_id=?",(r,n)->new Origin(r.getObject(1,UUID.class),r.getInt(2),r.getInt(3),r.getObject(4,UUID.class),r.getString(5)),actor.tenantId(),id);
        return rows.isEmpty()?new Origin(null,null,null,null,null):rows.getFirst();
    }
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor actor,String status,boolean overdue,int page,int size){return list(actor,status,overdue,page,size,null);}
    @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor actor,String status,boolean overdue,int page,int size,ReceivableSearch.Filter filter){
        if(filter!=null&&filter.branchId()!=null)access.requireBranch(actor,filter.branchId());
        LocalDate day=today();var selection=ReceivableSearch.select(actor,status,overdue,day,filter);String where=selection.where();var args=new ArrayList<Object>(selection.args());
        Long total=db.queryForObject("SELECT COUNT(*) FROM receivable t"+where,Long.class,args.toArray());
        var aggregateArgs=new ArrayList<Object>();aggregateArgs.add(day);aggregateArgs.addAll(args);
        var totals=db.queryForObject("SELECT COALESCE(SUM(t.amount),0),COALESCE(SUM(t.received_amount),0),COALESCE(SUM(t.credited_amount),0),COALESCE(SUM(CASE WHEN t.status<>'CANCELLED' THEN t.amount-t.received_amount-t.credited_amount ELSE 0 END),0),COALESCE(SUM(CASE WHEN t.status IN ('OPEN','PARTIAL') AND t.due_on<? THEN t.amount-t.received_amount-t.credited_amount ELSE 0 END),0) FROM receivable t"+where,(r,i)->new Totals(r.getBigDecimal(1),r.getBigDecimal(2),r.getBigDecimal(3),r.getBigDecimal(4),r.getBigDecimal(5)),aggregateArgs.toArray());
        args.add(size);args.add((long)page*size);
        var items=db.query("SELECT t.* FROM receivable t"+where+" ORDER BY t.due_on,t.id LIMIT ? OFFSET ?",(r,i)->rowAt(r,i,day),args.toArray());
        return Map.of("items",items,"total",total,"page",page,"size",size,"summary",totals,"today",day,"zone",zone.getId());
    }

    @Transactional public View create(Actor actor,Input input){
        db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());access.requireBranch(actor,input.branchId());
        String description=note(input.description()),reference=input.reference().toUpperCase(Locale.ROOT),document=input.document().trim();
        if(input.dueOn().isBefore(input.issuedOn()))throw error(HttpStatus.BAD_REQUEST,"Vencimento não pode anteceder emissão.");
        String hash=StockService.fingerprint("RECEIVABLE",input.branchId(),input.customerId(),reference,document,description,input.issuedOn(),input.dueOn(),input.amount());
        var existing=db.query("SELECT id,fingerprint FROM receivable WHERE tenant_id=? AND request_id=?",(rs,i)->Map.entry(rs.getObject(1,UUID.class),rs.getString(2)),actor.tenantId(),input.requestId());
        if(!existing.isEmpty()){if(!existing.getFirst().getValue().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de título reutilizado com outros dados.");return get(actor,existing.getFirst().getKey());}
        if(db.queryForObject("SELECT COUNT(*) FROM branch WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,actor.tenantId(),input.branchId())==0)throw error(HttpStatus.NOT_FOUND,"Filial ativa não encontrada.");
        var names=db.query("SELECT name FROM customer WHERE tenant_id=? AND id=?",(rs,i)->rs.getString(1),actor.tenantId(),input.customerId());if(names.isEmpty())throw error(HttpStatus.NOT_FOUND,"Cliente não encontrado.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO receivable(id,tenant_id,request_id,fingerprint,branch_id,customer_id,customer_name,reference,document,description,issued_on,due_on,amount,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,'OPEN',?)",id,actor.tenantId(),input.requestId(),hash,input.branchId(),input.customerId(),names.getFirst(),reference,document,description,input.issuedOn(),input.dueOn(),input.amount(),actor.userId());
        audit.record(actor,"RECEIVABLE_CREATED",id);return get(actor,id);
    }
    private Receipt receipt(Actor actor,UUID id){
        var rows=db.query("SELECT p.*,COALESCE((SELECT SUM(f.amount) FROM pos_sale_return_receipt_refund f WHERE f.tenant_id=p.tenant_id AND f.receipt_id=p.id),0) AS refunded_amount FROM receivable_receipt p WHERE p.tenant_id=? AND p.id=?",(rs,i)->new Receipt(rs.getObject("id",UUID.class),rs.getObject("receivable_id",UUID.class),rs.getBigDecimal("amount"),rs.getObject("received_on",LocalDate.class),Method.valueOf(rs.getString("method")),rs.getString("reference"),rs.getString("note"),rs.getString("status"),rs.getObject("created_by",UUID.class),rs.getObject("created_at",OffsetDateTime.class),rs.getObject("reversal_request_id",UUID.class),rs.getString("reversal_note"),rs.getObject("reversed_by",UUID.class),rs.getObject("reversed_at",OffsetDateTime.class),rs.getObject("treasury_account_id",UUID.class),rs.getObject("treasury_operation_id",UUID.class),rs.getObject("treasury_reversal_operation_id",UUID.class),rs.getBigDecimal("refunded_amount")),actor.tenantId(),id);
        if(rows.isEmpty())throw error(HttpStatus.NOT_FOUND,"Recebimento não encontrado.");return rows.getFirst();
    }
    private void lockTenant(Actor actor){db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());}
    @Transactional public Receipt receive(Actor actor,UUID id,ReceiptInput input){
        lockTenant(actor);View title=find(actor,id,true);String note=note(input.note()),reference=input.reference().trim();
        if(input.treasuryAccountId()!=null)treasury.requirePaymentAccount(actor,input.treasuryAccountId());
        if(input.receivedOn().isAfter(today()) || input.receivedOn().isBefore(title.issuedOn()))throw error(HttpStatus.BAD_REQUEST,"Data do recebimento deve estar entre emissão e hoje.");
        String hash=input.treasuryAccountId()==null?StockService.fingerprint("RECEIVABLE_RECEIPT",id,input.amount(),input.receivedOn(),input.method(),reference,note):StockService.fingerprint("RECEIVABLE_RECEIPT_TREASURY",id,input.amount(),input.receivedOn(),input.method(),reference,note,input.treasuryAccountId());
        var hashes=db.query("SELECT fingerprint FROM receivable_receipt WHERE tenant_id=? AND id=?",(rs,i)->rs.getString(1),actor.tenantId(),input.receiptId());
        if(!hashes.isEmpty()){if(!hashes.getFirst().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de recebimento reutilizado com outros dados.");return receipt(actor,input.receiptId());}
        if(!List.of("OPEN","PARTIAL").contains(title.status()))throw error(HttpStatus.CONFLICT,"Título encerrado não aceita recebimento.");
        if(input.amount().compareTo(title.outstanding())>0)throw error(HttpStatus.CONFLICT,"Recebimento excede saldo pendente.");
        UUID operation=input.treasuryAccountId()==null?null:treasury.recordReceipt(actor,input.treasuryAccountId(),input.amount(),note,false);
        db.update("INSERT INTO receivable_receipt(id,tenant_id,receivable_id,fingerprint,amount,received_on,method,reference,note,status,created_by,treasury_account_id,treasury_operation_id) VALUES(?,?,?,?,?,?,?,?,?,'POSTED',?,?,?)",input.receiptId(),actor.tenantId(),id,hash,input.amount(),input.receivedOn(),input.method().name(),reference,note,actor.userId(),input.treasuryAccountId(),operation);
        BigDecimal paid=title.receivedAmount().add(input.amount());String status=paid.add(title.creditedAmount()).compareTo(title.amount())==0?(title.creditedAmount().signum()>0?"CREDITED":"RECEIVED"):"PARTIAL";
        db.update("UPDATE receivable SET received_amount=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",paid,status,actor.tenantId(),id);
        audit.record(actor,"RECEIVABLE_RECEIPT_POSTED",input.receiptId());return receipt(actor,input.receiptId());
    }
    @Transactional public Receipt reverse(Actor actor,UUID id,UUID receiptId,Reversal input){
        lockTenant(actor);View title=find(actor,id,true);Receipt receipt=receipt(actor,receiptId);String note=note(input.note());
        if(!receipt.receivableId().equals(id))throw error(HttpStatus.NOT_FOUND,"Recebimento não encontrado.");
        if(receipt.treasuryAccountId()!=null)treasury.requirePaymentAccount(actor,receipt.treasuryAccountId());
        if(receipt.status().equals("REVERSED")){if(input.requestId().equals(receipt.reversalRequestId()) && note.equals(receipt.reversalNote()))return receipt;throw error(HttpStatus.CONFLICT,"Recebimento já estornado.");}
        if(receipt.refundedAmount().signum()>0)throw error(HttpStatus.CONFLICT,"Recebimento já reembolsado em devolução não aceita estorno integral.");
        UUID operation=receipt.treasuryAccountId()==null?null:treasury.recordReceipt(actor,receipt.treasuryAccountId(),receipt.amount(),note,true);
        db.update("UPDATE receivable_receipt SET status='REVERSED',reversal_request_id=?,reversal_note=?,reversed_by=?,reversed_at=CURRENT_TIMESTAMP,treasury_reversal_operation_id=? WHERE tenant_id=? AND id=?",input.requestId(),note,actor.userId(),operation,actor.tenantId(),receiptId);
        BigDecimal paid=title.receivedAmount().subtract(receipt.amount());String status=paid.signum()==0?"OPEN":paid.add(title.creditedAmount()).compareTo(title.amount())==0?(title.creditedAmount().signum()>0?"CREDITED":"RECEIVED"):"PARTIAL";
        db.update("UPDATE receivable SET received_amount=?,status=?,version=version+1 WHERE tenant_id=? AND id=?",paid,status,actor.tenantId(),id);
        audit.record(actor,"RECEIVABLE_RECEIPT_REVERSED",receiptId);return receipt(actor,receiptId);
    }
    @Transactional public View cancel(Actor actor,UUID id,Cancellation input){return cancelInternal(actor,id,input,false);}
    @Transactional public View cancelFromSale(Actor actor,UUID id,Cancellation input){return cancelInternal(actor,id,input,true);}
    private View cancelInternal(Actor actor,UUID id,Cancellation input,boolean fromSale){
        lockTenant(actor);
        View title=find(actor,id,true);String note=note(input.note());
        if(!fromSale && db.queryForObject("SELECT COUNT(*) FROM receivable_installment i JOIN pos_sale s ON s.tenant_id=i.tenant_id AND s.credit_plan_id=i.plan_id WHERE i.tenant_id=? AND i.receivable_id=?",Long.class,actor.tenantId(),id)>0)throw error(HttpStatus.CONFLICT,"Título vinculado a venda: use o cancelamento da venda.");
        if(title.status().equals("CANCELLED") && note.equals(title.cancellationNote()))return title;
        if(title.version()!=input.version())throw error(HttpStatus.CONFLICT,"Título alterado por outro acesso. Atualize.");
        if(!title.status().equals("OPEN") || title.receivedAmount().signum()!=0 || title.creditedAmount().signum()!=0)throw error(HttpStatus.CONFLICT,"Estorne recebimentos antes de cancelar o título.");
        db.update("UPDATE receivable SET status='CANCELLED',cancellation_note=?,version=version+1 WHERE tenant_id=? AND id=?",note,actor.tenantId(),id);audit.record(actor,"RECEIVABLE_CANCELLED",id);return get(actor,id);
    }
    @Transactional(readOnly=true) public Map<String,Object> receipts(Actor actor,UUID id,int page,int size){
        find(actor,id,false);var ids=db.query("SELECT id FROM receivable_receipt WHERE tenant_id=? AND receivable_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),id,size,(long)page*size);
        return Map.of("items",ids.stream().map(value->receipt(actor,value)).toList(),"total",db.queryForObject("SELECT COUNT(*) FROM receivable_receipt WHERE tenant_id=? AND receivable_id=?",Long.class,actor.tenantId(),id),"page",page,"size",size);
    }
}
