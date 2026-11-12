package br.com.erp.user;

import br.com.erp.auth.Actor;
import br.com.erp.auth.BranchAccess;
import br.com.erp.shared.Audit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@Service
public class AccessProfileService {
    private final JdbcTemplate db;
    private final UserService users;
    private final Audit audit;
    public AccessProfileService(JdbcTemplate db,UserService users,Audit audit) { this.db=db;this.users=users;this.audit=audit; }
    public record View(UUID id,String name,long version,List<String> permissions) {}
    private List<String> permissions(UUID tenant,UUID id) { return db.query("SELECT permission FROM access_profile_permission WHERE tenant_id=? AND profile_id=? ORDER BY permission",(rs,i)->rs.getString(1),tenant,id); }
    private View view(Actor actor,UUID id) {
        var rows=db.query("SELECT id,name,version FROM access_profile WHERE tenant_id=? AND id=?",(rs,i)->new View(rs.getObject(1,UUID.class),rs.getString(2),rs.getLong(3),permissions(actor.tenantId(),id)),actor.tenantId(),id);
        if(rows.isEmpty()) throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Perfil não encontrado.");
        return rows.getFirst();
    }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,int page,int size) {
        new BranchAccess(db).requireAll(actor);
        var ids=db.query("SELECT id FROM access_profile WHERE tenant_id=? ORDER BY name,id LIMIT ? OFFSET ?",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),size,(long)page*size);
        Long count=db.queryForObject("SELECT COUNT(*) FROM access_profile WHERE tenant_id=?",Long.class,actor.tenantId());
        return Map.of("items",ids.stream().map(id->view(actor,id)).toList(),"total",count,"page",page,"size",size);
    }
    private Set<String> validate(Actor actor,List<String> permissions) {
        Set<String> requested=new TreeSet<>(permissions);
        if(!UserService.PERMISSIONS.containsAll(requested)) throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Permissão desconhecida.");
        users.validateManagerPermissions(requested);
        var held=db.query("SELECT permission FROM user_permission WHERE tenant_id=? AND user_id=?",(rs,i)->rs.getString(1),actor.tenantId(),actor.userId());
        if(!held.containsAll(requested)) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Você só pode definir perfis dentro dos seus próprios privilégios.");
        return requested;
    }
    private void insertPermissions(Actor actor,UUID id,Set<String> permissions) {
        for(String permission:permissions) db.update("INSERT INTO access_profile_permission(tenant_id,profile_id,permission) VALUES(?,?,?)",actor.tenantId(),id,permission);
    }
    @Transactional public View create(Actor actor,AccessProfileController.Input input) {
        users.lockAndAuthorize(actor);
        var requested=validate(actor,input.permissions());UUID id=UUID.randomUUID();
        db.update("INSERT INTO access_profile(id,tenant_id,name) VALUES(?,?,?)",id,actor.tenantId(),input.name().trim());
        insertPermissions(actor,id,requested);audit.record(actor,"ACCESS_PROFILE_CREATED",id);
        return view(actor,id);
    }
    @Transactional public View update(Actor actor,UUID id,AccessProfileController.Update input) {
        users.lockAndAuthorize(actor);var profile=view(actor,id);
        if(profile.version()!=input.version()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Perfil alterado por outro acesso. Atualize a lista.");
        var requested=validate(actor,input.permissions());validate(actor,profile.permissions());
        String name=input.name().trim();
        if(name.equals(profile.name()) && requested.equals(new TreeSet<>(profile.permissions()))) return profile;
        db.update("UPDATE access_profile SET name=?,version=version+1 WHERE tenant_id=? AND id=?",name,actor.tenantId(),id);
        db.update("DELETE FROM access_profile_permission WHERE tenant_id=? AND profile_id=?",actor.tenantId(),id);
        insertPermissions(actor,id,requested);audit.record(actor,"ACCESS_PROFILE_UPDATED",id);
        return view(actor,id);
    }
    // Explicit snapshot: updating a reusable profile never silently changes existing users.
    @Transactional public UserService.View apply(Actor actor,UUID userId,AccessProfileController.Apply input) {
        users.lockAndAuthorize(actor);var profile=view(actor,input.profileId());
        if(profile.version()!=input.profileVersion()) throw new ResponseStatusException(HttpStatus.CONFLICT,"Perfil alterado por outro acesso. Atualize a lista.");
        return users.setPermissions(actor,userId,new UserController.Permissions(profile.permissions(),input.userVersion()));
    }
}
