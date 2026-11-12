package br.com.erp.auth;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
public class SessionFilter extends OncePerRequestFilter {
    private final JdbcTemplate db;
    public SessionFilter(JdbcTemplate db) { this.db = db; }
    @Override protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ") && header.length() <= 100) {
            String hash = TokenCodec.hash(header.substring(7));
            var actors = db.query("SELECT s.tenant_id,s.user_id,u.name FROM auth_session s JOIN app_user u ON u.id=s.user_id AND u.tenant_id=s.tenant_id WHERE s.token_hash=? AND s.expires_at>CURRENT_TIMESTAMP AND u.active=TRUE", (rs, i) -> new Actor(rs.getObject(1, UUID.class), rs.getObject(2, UUID.class), rs.getString(3), hash), hash);
            if (!actors.isEmpty()) {
                Actor actor = actors.getFirst();
                var permissions = db.query("SELECT permission FROM user_permission WHERE tenant_id=? AND user_id=?", (rs, i) -> new SimpleGrantedAuthority(rs.getString(1)), actor.tenantId(), actor.userId());
                SecurityContextHolder.getContext().setAuthentication(new UsernamePasswordAuthenticationToken(actor, null, permissions));
            }
        }
        chain.doFilter(request, response);
    }
}
