package br.com.erp.auth;

import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

@Component
public class BranchAccess {
    private final JdbcTemplate db;
    public BranchAccess(JdbcTemplate db) { this.db=db; }
    // Column expressions come exclusively from source code; two bind parameters: tenant/user.
    public static String predicate(String branchColumn) {
        return "EXISTS (SELECT 1 FROM app_user scope_user WHERE scope_user.tenant_id=? AND scope_user.id=? AND scope_user.active=TRUE AND (scope_user.all_branches=TRUE OR EXISTS (SELECT 1 FROM user_branch_access grant_row WHERE grant_row.tenant_id=scope_user.tenant_id AND grant_row.user_id=scope_user.id AND grant_row.branch_id="+branchColumn+")))";
    }
    public void requireBranch(Actor actor,UUID id) {
        Long count=db.queryForObject("SELECT COUNT(*) FROM branch b WHERE b.tenant_id=? AND b.id=? AND "+predicate("b.id"),Long.class,actor.tenantId(),id,actor.tenantId(),actor.userId());
        if(count==null || count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Filial não encontrada ou sem acesso.");
    }
    public void requireAll(Actor actor) {
        Long count=db.queryForObject("SELECT COUNT(*) FROM app_user WHERE tenant_id=? AND id=? AND active=TRUE AND all_branches=TRUE",Long.class,actor.tenantId(),actor.userId());
        if(count==null || count==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Esta administração exige acesso a todas as filiais.");
    }
    public void requireWarehouse(Actor actor,UUID id) {
        Long count=db.queryForObject("SELECT COUNT(*) FROM warehouse w WHERE w.tenant_id=? AND w.id=? AND "+predicate("w.branch_id"),Long.class,actor.tenantId(),id,actor.tenantId(),actor.userId());
        if(count==null || count==0) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Depósito não encontrado ou sem acesso.");
    }
}
