package br.com.erp.auth;

import java.time.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;
import br.com.erp.shared.Audit;

@Service
public class AuthService {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final Audit audit;
    private final String dummyHash;
    public AuthService(JdbcTemplate db, PasswordEncoder encoder, Audit audit) {
        this.db=db; this.encoder=encoder; this.audit=audit; this.dummyHash=encoder.encode(TokenCodec.issue());
    }
    record User(UUID id, UUID tenantId, String name, String password, boolean active, int failures, OffsetDateTime lockedUntil) {}
    public record LoginResult(String token, OffsetDateTime expiresAt, UUID tenantId, String name, List<String> permissions,boolean allBranches) {}

    // Failures must commit the lockout counters; do not roll back on rejected credentials.
    @Transactional(noRollbackFor=ResponseStatusException.class)
    public LoginResult login(String slug, String email, String password) {
        var users = db.query("SELECT u.id,u.tenant_id,u.name,u.password_hash,u.active,u.failed_attempts,u.locked_until FROM app_user u JOIN tenant t ON t.id=u.tenant_id WHERE t.slug=? AND u.email=? FOR UPDATE", (rs,i) -> new User(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getBoolean(5),rs.getInt(6),rs.getObject(7,OffsetDateTime.class)), slug.toLowerCase(Locale.ROOT),email.toLowerCase(Locale.ROOT));
        if (users.isEmpty()) { encoder.matches(password,dummyHash); throw denied(); }
        User user=users.getFirst();
        var now=OffsetDateTime.now(ZoneOffset.UTC);
        if (!user.active() || (user.lockedUntil()!=null && user.lockedUntil().isAfter(now))) throw denied();
        if (!encoder.matches(password,user.password())) {
            int failures=user.lockedUntil()!=null ? 1 : user.failures()+1;
            db.update("UPDATE app_user SET failed_attempts=?,locked_until=? WHERE id=?",failures,failures>=5 ? now.plusMinutes(15) : null,user.id());
            audit.record(new Actor(user.tenantId(),user.id(),user.name(),null),"LOGIN_REJECTED",user.id());
            throw denied();
        }
        db.update("UPDATE app_user SET failed_attempts=0,locked_until=NULL WHERE id=?",user.id());
        String token=TokenCodec.issue(); var expires=now.plusHours(8);
        db.update("INSERT INTO auth_session(token_hash,tenant_id,user_id,expires_at) VALUES(?,?,?,?)",TokenCodec.hash(token),user.tenantId(),user.id(),expires);
        audit.record(new Actor(user.tenantId(),user.id(),user.name(),null),"LOGIN_SUCCEEDED",user.id());
        return new LoginResult(token,expires,user.tenantId(),user.name(),permissions(user.tenantId(),user.id()),allBranches(user.tenantId(),user.id()));
    }
    public boolean allBranches(UUID tenantId,UUID userId) { return Boolean.TRUE.equals(db.queryForObject("SELECT all_branches FROM app_user WHERE tenant_id=? AND id=?",Boolean.class,tenantId,userId)); }
    public List<String> permissions(UUID tenantId,UUID userId) {
        return db.query("SELECT permission FROM user_permission WHERE tenant_id=? AND user_id=? ORDER BY permission",(rs,i)->rs.getString(1),tenantId,userId);
    }
    public record ActiveSession(OffsetDateTime createdAt, OffsetDateTime expiresAt, boolean current) {}
    public List<ActiveSession> sessions(Actor actor) {
        return db.query("SELECT created_at,expires_at,token_hash FROM auth_session WHERE tenant_id=? AND user_id=? AND expires_at>CURRENT_TIMESTAMP ORDER BY created_at DESC,token_hash", (rs,i) -> new ActiveSession(rs.getObject(1,OffsetDateTime.class),rs.getObject(2,OffsetDateTime.class),rs.getString(3).equals(actor.tokenHash())), actor.tenantId(),actor.userId());
    }
    @Transactional public void revokeOtherSessions(Actor actor) {
        // Serialize with login/password changes so concurrent logins have a defined order.
        db.queryForObject("SELECT id FROM app_user WHERE tenant_id=? AND id=? FOR UPDATE",UUID.class,actor.tenantId(),actor.userId());
        Integer valid=db.queryForObject("SELECT COUNT(*) FROM auth_session WHERE tenant_id=? AND user_id=? AND token_hash=? AND expires_at>CURRENT_TIMESTAMP",Integer.class,actor.tenantId(),actor.userId(),actor.tokenHash());
        if (valid==null || valid==0) throw denied();
        int removed=db.update("DELETE FROM auth_session WHERE tenant_id=? AND user_id=? AND token_hash<>?",actor.tenantId(),actor.userId(),actor.tokenHash());
        if (removed>0) audit.record(actor,"OTHER_SESSIONS_REVOKED",actor.userId());
    }
    @Transactional public void logout(Actor actor) {
        db.update("DELETE FROM auth_session WHERE token_hash=? AND tenant_id=?",actor.tokenHash(),actor.tenantId());
        audit.record(actor,"LOGOUT",actor.userId());
    }
    @Transactional public void changePassword(Actor actor,String current,String next) {
        String hash=db.queryForObject("SELECT password_hash FROM app_user WHERE tenant_id=? AND id=? FOR UPDATE",String.class,actor.tenantId(),actor.userId());
        if (!encoder.matches(current,hash)) throw denied();
        db.update("UPDATE app_user SET password_hash=? WHERE tenant_id=? AND id=?",encoder.encode(next),actor.tenantId(),actor.userId());
        db.update("DELETE FROM auth_session WHERE tenant_id=? AND user_id=?",actor.tenantId(),actor.userId());
        audit.record(actor,"PASSWORD_CHANGED",actor.userId());
    }
    private ResponseStatusException denied() { return new ResponseStatusException(HttpStatus.UNAUTHORIZED,"Credenciais inválidas ou acesso temporariamente bloqueado."); }
}
