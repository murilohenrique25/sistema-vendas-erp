package br.com.erp.tenant;

import br.com.erp.auth.Actor;
import br.com.erp.shared.Audit;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ProvisioningService {
    private final JdbcTemplate db;
    private final PasswordEncoder encoder;
    private final Audit audit;
    public ProvisioningService(JdbcTemplate db,PasswordEncoder encoder,Audit audit) { this.db=db; this.encoder=encoder; this.audit=audit; }
    @Transactional public UUID create(String name,String slug,String adminName,String email,String password) {
        UUID tenant=UUID.randomUUID(),user=UUID.randomUUID(),branch=UUID.randomUUID();
        db.update("INSERT INTO tenant(id,name,slug) VALUES(?,?,?)",tenant,name,slug);
        db.update("INSERT INTO app_user(id,tenant_id,name,email,password_hash) VALUES(?,?,?,?,?)",user,tenant,adminName,email.toLowerCase(Locale.ROOT),encoder.encode(password));
        for (String permission: List.of("fiscal:read","fiscal:configure","fiscal:issue","fiscal:cancel","salesorder:read","salesorder:write","salesorder:approve","commercial:read","commercial:write","inventory:approve","receivable:credit","pos:credit","customer:credit","pos:discount","pos:discount-approve","pos:discount-policy","pos:suspend","pos:return","pos:cancel","pos:sell","pos:read","pos:manage","pos:operate","pos:close","pos:supervise","reconciliation:read","reconciliation:import","reconciliation:review","cashflow:read","customer:read","customer:write","receivable:read","receivable:write","receivable:receive","receivable:reverse","treasury:read","treasury:write","treasury:move","payable:read","payable:write","payable:pay","payable:reverse","purchase:read","purchase:write","purchase:receive","supplier:read","supplier:write","user:read","user:write","branch:read","branch:write","product:read","product:write","audit:read","inventory:read","inventory:write","inventory:cost","loss:read","loss:create","loss:approve"))
            db.update("INSERT INTO user_permission(tenant_id,user_id,permission) VALUES(?,?,?)",tenant,user,permission);
        db.update("INSERT INTO branch(id,tenant_id,code,name) VALUES(?,?,?,?)",branch,tenant,"MATRIZ","Matriz");
        audit.record(new Actor(tenant,user,adminName,null),"TENANT_PROVISIONED",tenant);
        return tenant;
    }
}


