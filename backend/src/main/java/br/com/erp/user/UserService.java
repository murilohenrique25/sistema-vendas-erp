package br.com.erp.user;

import br.com.erp.auth.Actor;
import br.com.erp.auth.BranchAccess;
import br.com.erp.shared.Audit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class UserService {
    public static final Set<String> PERMISSIONS=Set.of("fiscal:read","fiscal:configure","fiscal:issue","fiscal:cancel","salesorder:read","salesorder:write","salesorder:approve","commercial:read","commercial:write","inventory:approve","receivable:credit","pos:credit","customer:credit","pos:discount","pos:discount-approve","pos:discount-policy","pos:suspend","pos:return","pos:cancel","pos:sell","pos:read","pos:manage","pos:operate","pos:close","pos:supervise","reconciliation:read","reconciliation:import","reconciliation:review","cashflow:read","customer:read","customer:write","receivable:read","receivable:write","receivable:receive","receivable:reverse","treasury:read","treasury:write","treasury:move","payable:read","payable:write","payable:pay","payable:reverse","purchase:read","purchase:write","purchase:receive","supplier:read","supplier:write","user:read","user:write","branch:read","branch:write","product:read","product:write","audit:read","inventory:read","inventory:write","inventory:cost","loss:read","loss:create","loss:approve");
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final Audit audit;
    public UserService(JdbcTemplate db,PasswordEncoder encoder,Audit audit) { this.db=db; this.encoder=encoder; this.audit=audit; }
    public record View(UUID id,String name,String email,boolean active,long version,List<String> permissions) {}
    private View view(UUID tenant,UUID id) {
        var rows=db.query("SELECT id,name,email,active,version FROM app_user WHERE tenant_id=? AND id=?",(rs,i)->new View(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getBoolean(4),rs.getLong(5),permissions(tenant,id)),tenant,id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Usuário não encontrado.");
        return rows.getFirst();
    }
    private List<String> permissions(UUID tenant,UUID id) { return db.query("SELECT permission FROM user_permission WHERE tenant_id=? AND user_id=? ORDER BY permission",(rs,i)->rs.getString(1),tenant,id); }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,int page,int size) {
        new BranchAccess(db).requireAll(actor);
        var ids=db.query("SELECT id FROM app_user WHERE tenant_id=? ORDER BY name,id LIMIT ? OFFSET ?",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),size,(long)page*size);
        Long count=db.queryForObject("SELECT COUNT(*) FROM app_user WHERE tenant_id=?",Long.class,actor.tenantId());
        return Map.of("items",ids.stream().map(id->view(actor.tenantId(),id)).toList(),"total",count,"page",page,"size",size);
    }
    // One tenant lock serializes account administration, including last-manager checks.
    void lockAndAuthorize(Actor actor) {
        db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());
        Integer allowed=db.queryForObject("SELECT COUNT(*) FROM app_user u JOIN user_permission p ON p.tenant_id=u.tenant_id AND p.user_id=u.id WHERE u.tenant_id=? AND u.id=? AND u.active=TRUE AND u.all_branches=TRUE AND p.permission='user:write'",Integer.class,actor.tenantId(),actor.userId());
        if(allowed==null || allowed==0) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sem permissão para administrar usuários.");
        Integer valid=db.queryForObject("SELECT COUNT(*) FROM auth_session WHERE tenant_id=? AND user_id=? AND token_hash=? AND expires_at>CURRENT_TIMESTAMP",Integer.class,actor.tenantId(),actor.userId(),actor.tokenHash());
        if(valid==null || valid==0) throw new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Sessão encerrada.");
    }
    @Transactional public View create(Actor actor,UserController.Input input) {
        lockAndAuthorize(actor);
        Set<String> requested=new TreeSet<>(input.permissions());
        if(!PERMISSIONS.containsAll(requested)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Permissão desconhecida.");
        validateManagerPermissions(requested);
        if(!permissions(actor.tenantId(),actor.userId()).containsAll(requested)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Você só pode conceder permissões que possui.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO app_user(id,tenant_id,name,email,password_hash) VALUES(?,?,?,?,?)",id,actor.tenantId(),input.name().trim(),input.email().trim().toLowerCase(Locale.ROOT),encoder.encode(input.password()));
        for(String permission:requested) db.update("INSERT INTO user_permission(tenant_id,user_id,permission) VALUES(?,?,?)",actor.tenantId(),id,permission);
        audit.record(actor,"USER_CREATED",id);
        return view(actor.tenantId(),id);
    }
    void validateManagerPermissions(Set<String> requested) {
        if(requested.contains("user:write") && !requested.contains("user:read")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Administrar usuários exige também consultar usuários.");
    }
    @Transactional public View setPermissions(Actor actor,UUID id,UserController.Permissions input) {
        lockAndAuthorize(actor);
        var user=view(actor.tenantId(),id);
        if(user.version()!=input.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Usuário alterado por outro acesso. Atualize a lista.");
        Set<String> requested=new TreeSet<>(input.permissions());
        if(!PERMISSIONS.containsAll(requested)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Permissão desconhecida.");
        validateManagerPermissions(requested);
        var held=permissions(actor.tenantId(),actor.userId());
        if(requested.contains("user:write") && !db.queryForObject("SELECT all_branches FROM app_user WHERE tenant_id=? AND id=?",Boolean.class,actor.tenantId(),id)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Gestores de usuários precisam de acesso a todas as filiais.");
        if(!held.containsAll(requested) || !held.containsAll(user.permissions())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Você só pode editar contas e conceder permissões dentro dos seus próprios privilégios.");
        if(requested.equals(new TreeSet<>(user.permissions()))) return user;
        if(user.active() && user.permissions().contains("user:write") && !requested.contains("user:write")) {
            Integer managers=db.queryForObject("SELECT COUNT(*) FROM app_user u JOIN user_permission p ON p.tenant_id=u.tenant_id AND p.user_id=u.id WHERE u.tenant_id=? AND u.active=TRUE AND p.permission='user:write'",Integer.class,actor.tenantId());
            if(managers==null || managers<=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"A empresa precisa de um gestor de usuários ativo.");
        }
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND user_id=?",actor.tenantId(),id);
        for(String permission:requested) db.update("INSERT INTO user_permission(tenant_id,user_id,permission) VALUES(?,?,?)",actor.tenantId(),id,permission);
        db.update("UPDATE app_user SET version=version+1 WHERE tenant_id=? AND id=?",actor.tenantId(),id);
        audit.record(actor,"USER_PERMISSIONS_CHANGED",id);
        return view(actor.tenantId(),id);
    }
    public record BranchScope(boolean allBranches,List<UUID> branchIds,long version) {}
    @Transactional(readOnly=true) public BranchScope branchScope(Actor actor,UUID id) {
        new BranchAccess(db).requireAll(actor);var user=view(actor.tenantId(),id);
        boolean all=db.queryForObject("SELECT all_branches FROM app_user WHERE tenant_id=? AND id=?",Boolean.class,actor.tenantId(),id);
        var ids=db.query("SELECT branch_id FROM user_branch_access WHERE tenant_id=? AND user_id=? ORDER BY branch_id",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),id);
        return new BranchScope(all,ids,user.version());
    }
    @Transactional public BranchScope setBranchScope(Actor actor,UUID id,UserController.Branches input) {
        lockAndAuthorize(actor);var user=view(actor.tenantId(),id);
        if(user.version()!=input.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Usuário alterado por outro acesso. Atualize a lista.");
        if(!permissions(actor.tenantId(),actor.userId()).containsAll(user.permissions())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Conta acima dos seus privilégios.");
        if(input.allBranches() && !input.branchIds().isEmpty()) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Acesso global não usa uma lista de filiais.");
        if(!input.allBranches() && user.permissions().contains("user:write")) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Gestores de usuários precisam de acesso a todas as filiais.");
        Set<UUID> ids=new TreeSet<>(input.branchIds());
        for(UUID branch:ids) new BranchAccess(db).requireBranch(actor,branch);
        var current=branchScope(actor,id);
        if(current.allBranches()==input.allBranches() && new TreeSet<>(current.branchIds()).equals(ids)) return current;
        db.update("DELETE FROM user_branch_access WHERE tenant_id=? AND user_id=?",actor.tenantId(),id);
        for(UUID branch:ids) db.update("INSERT INTO user_branch_access(tenant_id,user_id,branch_id) VALUES(?,?,?)",actor.tenantId(),id,branch);
        db.update("UPDATE app_user SET all_branches=?,version=version+1 WHERE tenant_id=? AND id=?",input.allBranches(),actor.tenantId(),id);
        audit.record(actor,"USER_BRANCH_SCOPE_CHANGED",id);return branchScope(actor,id);
    }
    @Transactional public View setActive(Actor actor,UUID id,UserController.Status input) {
        lockAndAuthorize(actor);
        var user=view(actor.tenantId(),id);
        if(user.version()!=input.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Usuário alterado por outro acesso. Atualize a lista.");
        if(user.active()==input.active()) return user;
        if(input.active() && !permissions(actor.tenantId(),actor.userId()).containsAll(user.permissions())) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Você não pode reativar uma conta com permissões que não possui.");
        if(!input.active()) {
            if(id.equals(actor.userId())) throw new ResponseStatusException(HttpStatus.CONFLICT,"Você não pode desativar sua própria conta.");
            if(user.permissions().contains("user:write")) {
                Integer managers=db.queryForObject("SELECT COUNT(*) FROM app_user u JOIN user_permission p ON p.tenant_id=u.tenant_id AND p.user_id=u.id WHERE u.tenant_id=? AND u.active=TRUE AND p.permission='user:write'",Integer.class,actor.tenantId());
                if(managers==null || managers<=1) throw new ResponseStatusException(HttpStatus.CONFLICT,"A empresa precisa de um gestor de usuários ativo.");
            }
        }
        db.update("UPDATE app_user SET active=?,version=version+1 WHERE tenant_id=? AND id=?",input.active(),actor.tenantId(),id);
        if(!input.active()) db.update("DELETE FROM auth_session WHERE tenant_id=? AND user_id=?",actor.tenantId(),id);
        audit.record(actor,input.active()?"USER_ACTIVATED":"USER_DEACTIVATED",id);
        return view(actor.tenantId(),id);
    }
}


