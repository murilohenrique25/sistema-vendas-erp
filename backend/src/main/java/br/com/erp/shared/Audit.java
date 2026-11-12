package br.com.erp.shared;

import br.com.erp.auth.Actor;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
public class Audit {
    private final JdbcTemplate db;
    public Audit(JdbcTemplate db) { this.db = db; }
    public void record(Actor actor, String action, UUID entityId) {
        db.update("INSERT INTO audit_event(id,tenant_id,actor_id,action,entity_id) VALUES (?,?,?,?,?)", UUID.randomUUID(), actor.tenantId(), actor.userId(), action, entityId);
    }
}
