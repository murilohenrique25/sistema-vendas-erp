package br.com.erp.loss;

import br.com.erp.auth.Actor;
import br.com.erp.auth.BranchAccess;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class LossService {
    public enum Reason { EXPIRY, DAMAGE, BREAKAGE, THEFT, MISPLACEMENT, DETERIORATION, OPERATIONAL_ERROR, TRANSPORT, STORAGE, TECHNICAL, OTHER }
    public record Input(@NotNull UUID requestId,@NotNull UUID warehouseId,@NotNull UUID productId,
        @NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity,
        @NotNull Reason reason,@NotBlank @Size(min=5,max=500) String details) {}
    public record Approval(@NotNull UUID operationId) {}
    public record Rejection(@NotBlank @Size(min=5,max=500) String note) {}
    public record View(UUID id,UUID warehouseId,UUID productId,String productName,String unit,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,Reason reason,String details,String status,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal estimatedTotal,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal approvedTotal,
        UUID createdBy,UUID reviewedBy,String reviewNote,OffsetDateTime createdAt,OffsetDateTime reviewedAt) {}
    private record Loss(UUID id,UUID warehouseId,UUID productId,BigDecimal quantity,String details,String status,String reviewNote) {}
    private final JdbcTemplate db;
    private final StockService stock;
    private final Audit audit;
    private final BranchAccess access;
    public LossService(JdbcTemplate db,StockService stock,Audit audit,BranchAccess access) { this.db=db; this.stock=stock; this.audit=audit;this.access=access; }

    @Transactional public View create(Actor actor,Input input) {
        String details=input.details().trim();
        if (details.length()<5) throw invalid("Descreva o motivo da perda com pelo menos cinco caracteres.");
        var snapshot=stock.snapshotForLoss(actor,input.warehouseId(),input.productId());
        String hash=StockService.fingerprint("LOSS_REPORT",input.warehouseId(),input.productId(),input.quantity(),input.reason(),details);
        var existing=db.query("SELECT id,fingerprint FROM stock_loss WHERE tenant_id=? AND request_id=?",(rs,i)->Map.entry(rs.getObject(1,UUID.class),rs.getString(2)),actor.tenantId(),input.requestId());
        if (!existing.isEmpty()) {
            if (!existing.getFirst().getValue().equals(hash)) throw conflict("Identificador do registro já utilizado com outros dados.");
            return get(actor,existing.getFirst().getKey());
        }
        if (snapshot.quantity().compareTo(input.quantity())<0) throw conflict("Quantidade da perda excede o saldo atual.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO stock_loss(id,tenant_id,request_id,fingerprint,warehouse_id,product_id,quantity,reason,details,estimated_unit_cost,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,?,'PENDING',?)",id,actor.tenantId(),input.requestId(),hash,input.warehouseId(),input.productId(),input.quantity(),input.reason().name(),details,snapshot.averageCost(),actor.userId());
        audit.record(actor,"LOSS_REPORTED",id);
        return get(actor,id);
    }
    @Transactional public View approve(Actor actor,UUID id,Approval input) {
        Loss loss=lock(actor,id);
        if (loss.status().equals("APPROVED")) return get(actor,id);
        if (!loss.status().equals("PENDING")) throw conflict("Uma perda rejeitada não pode ser aprovada.");
        BigDecimal cost=stock.approveLoss(actor,input.operationId(),id,loss.warehouseId(),loss.productId(),loss.quantity(),loss.details());
        db.update("UPDATE stock_loss SET status='APPROVED',approved_unit_cost=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",cost,actor.userId(),actor.tenantId(),id);
        audit.record(actor,"LOSS_APPROVED",id);
        return get(actor,id);
    }
    @Transactional public View reject(Actor actor,UUID id,Rejection input) {
        String note=input.note().trim();
        if (note.length()<5) throw invalid("Informe a justificativa da rejeição.");
        Loss loss=lock(actor,id);
        if (loss.status().equals("REJECTED") && note.equals(loss.reviewNote())) return get(actor,id);
        if (!loss.status().equals("PENDING")) throw conflict("Somente perdas pendentes podem ser rejeitadas.");
        db.update("UPDATE stock_loss SET status='REJECTED',review_note=?,reviewed_by=?,reviewed_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",note,actor.userId(),actor.tenantId(),id);
        audit.record(actor,"LOSS_REJECTED",id);
        return get(actor,id);
    }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,int page,int size) {
        boolean cost=stock.canSeeCost(actor);
        String where=" WHERE l.tenant_id=? AND "+BranchAccess.predicate("w.branch_id");
        var items=db.query(viewSql()+where+" ORDER BY l.created_at DESC,l.id LIMIT ? OFFSET ?",(rs,i)->view(rs,cost),actor.tenantId(),actor.tenantId(),actor.userId(),size,(long)page*size);
        return Map.of("items",items,"total",db.queryForObject("SELECT COUNT(*) FROM stock_loss l JOIN warehouse w ON w.tenant_id=l.tenant_id AND w.id=l.warehouse_id"+where,Long.class,actor.tenantId(),actor.tenantId(),actor.userId()),"page",page,"size",size);
    }
    @Transactional(readOnly=true) public View get(Actor actor,UUID id) {
        boolean cost=stock.canSeeCost(actor);
        var losses=db.query(viewSql()+" WHERE l.tenant_id=? AND l.id=? AND "+BranchAccess.predicate("w.branch_id"),(rs,i)->view(rs,cost),actor.tenantId(),id,actor.tenantId(),actor.userId());
        if (losses.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Perda não encontrada."); return losses.getFirst();
    }
    private Loss lock(Actor actor,UUID id) {
        var values=db.query("SELECT id,warehouse_id,product_id,quantity,details,status,review_note FROM stock_loss WHERE tenant_id=? AND id=? FOR UPDATE",(rs,i)->new Loss(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getBigDecimal(4),rs.getString(5),rs.getString(6),rs.getString(7)),actor.tenantId(),id);
        if (values.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Perda não encontrada.");
        access.requireWarehouse(actor,values.getFirst().warehouseId());return values.getFirst();
    }
    private String viewSql() { return "SELECT l.id,l.warehouse_id,l.product_id,p.name,p.unit,l.quantity,l.reason,l.details,l.status,l.estimated_unit_cost,l.approved_unit_cost,l.created_by,l.reviewed_by,l.review_note,l.created_at,l.reviewed_at FROM stock_loss l JOIN product p ON p.tenant_id=l.tenant_id AND p.id=l.product_id JOIN warehouse w ON w.tenant_id=l.tenant_id AND w.id=l.warehouse_id"; }
    private View view(java.sql.ResultSet rs,boolean costs) throws java.sql.SQLException {
        BigDecimal quantity=rs.getBigDecimal(6),estimated=costs ? rs.getBigDecimal(10).multiply(quantity).setScale(2,RoundingMode.HALF_UP) : null,approved=rs.getBigDecimal(11);
        return new View(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getString(4),rs.getString(5),quantity,Reason.valueOf(rs.getString(7)),rs.getString(8),rs.getString(9),estimated,costs && approved!=null ? approved.multiply(quantity).setScale(2,RoundingMode.HALF_UP) : null,rs.getObject(12,UUID.class),rs.getObject(13,UUID.class),rs.getString(14),rs.getObject(15,OffsetDateTime.class),rs.getObject(16,OffsetDateTime.class));
    }
    private ResponseStatusException conflict(String text) { return new ResponseStatusException(HttpStatus.CONFLICT,text); }
    private ResponseStatusException invalid(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,text); }
}
