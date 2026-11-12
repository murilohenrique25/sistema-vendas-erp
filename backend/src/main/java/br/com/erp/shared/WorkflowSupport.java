package br.com.erp.shared;

import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;

/** Used inside the caller's transaction. Scope must be checked before replaying a command. */
@Component
public class WorkflowSupport {
 private final JdbcTemplate db;
 public WorkflowSupport(JdbcTemplate db){this.db=db;}
 public void lock(Actor a){db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());}
 public void require(Actor a,String permission){if(db.queryForObject("SELECT COUNT(*) FROM user_permission p JOIN app_user u ON u.tenant_id=p.tenant_id AND u.id=p.user_id WHERE p.tenant_id=? AND p.user_id=? AND p.permission=? AND u.active=TRUE",Long.class,a.tenantId(),a.userId(),permission)!=1)throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sem permissão: "+permission);}
 public UUID replay(Actor a,UUID request,String kind,String hash){var rows=db.query("SELECT entity_id,kind,fingerprint FROM workflow_command WHERE tenant_id=? AND request_id=?",(r,i)->List.of(r.getObject(1,UUID.class),r.getString(2),r.getString(3)),a.tenantId(),request);if(rows.isEmpty())return null;var row=rows.getFirst();if(!row.get(1).equals(kind)||!row.get(2).equals(hash))throw conflict("Identificador reutilizado com outros dados.");return (UUID)row.getFirst();}
 public void record(Actor a,UUID request,String kind,String hash,UUID entity){db.update("INSERT INTO workflow_command(tenant_id,request_id,kind,fingerprint,entity_id,actor_id) VALUES(?,?,?,?,?,?)",a.tenantId(),request,kind,hash,entity,a.userId());}
 public String hash(Object... values){return StockService.fingerprint(values);}
 public ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 public ResponseStatusException bad(String text){return new ResponseStatusException(HttpStatus.BAD_REQUEST,text);}
 public ResponseStatusException missing(String text){return new ResponseStatusException(HttpStatus.NOT_FOUND,text);}
}
