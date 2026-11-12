package br.com.erp.audit;

import br.com.erp.auth.Actor;
import br.com.erp.auth.BranchAccess;
import jakarta.validation.constraints.*;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/audit") @Validated
public class AuditController {
    private final JdbcTemplate db;
    public AuditController(JdbcTemplate db) { this.db=db; }
    public record Event(UUID id,UUID actorId,String action,UUID entityId,OffsetDateTime occurredAt) {}
    @GetMapping @PreAuthorize("hasAuthority('audit:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        new BranchAccess(db).requireAll(actor);
        var items=db.query("SELECT id,actor_id,action,entity_id,occurred_at FROM audit_event WHERE tenant_id=? ORDER BY occurred_at DESC,id LIMIT ? OFFSET ?",(rs,i)->new Event(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getObject(4,UUID.class),rs.getObject(5,OffsetDateTime.class)),actor.tenantId(),size,(long)page*size);
        return Map.of("items",items,"total",db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=?",Long.class,actor.tenantId()),"page",page,"size",size);
    }
}
