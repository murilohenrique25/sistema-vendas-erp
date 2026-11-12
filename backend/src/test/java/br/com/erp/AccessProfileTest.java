package br.com.erp;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class AccessProfileTest extends ApiTestSupport {
    private String create(String token,String name,List<String> permissions) throws Exception { return postApi("/api/v1/access-profiles",token,Map.of("name",name,"permissions",permissions),201).get("id").asText(); }
    private int update(String token,String id,String name,List<String> permissions,long version) throws Exception { return putStatus("/api/v1/access-profiles/"+id,token,Map.of("name",name,"permissions",permissions,"version",version)); }
    private int apply(String token,String user,String profile,long profileVersion,long userVersion) throws Exception { return putStatus("/api/v1/users/"+user+"/profile",token,Map.of("profileId",profile,"profileVersion",profileVersion,"userVersion",userVersion)); }
    private int putStatus(String path,String token,Object body) throws Exception { return http.perform(put(path).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body))).andReturn().getResponse().getStatus(); }
    @Test void createUpdateNoOpVersionAndDuplicateRollback() throws Exception {
        String token=token(company),id=create(token,"Consulta",List.of("product:read","product:read"));
        var profile=getApi("/api/v1/access-profiles",token,200).get("items").get(0);
        assertEquals(1,profile.get("permissions").size());assertEquals(4,profile.size());
        assertEquals(200,update(token,id,"Consulta",List.of("product:read"),0));
        assertEquals(200,update(token,id,"Filiais",List.of("branch:read"),0));
        assertEquals(409,update(token,id,"Antigo",List.of(),0));
        postApi("/api/v1/access-profiles",token,Map.of("name","Filiais","permissions",List.of()),409);
        assertEquals(1,getApi("/api/v1/access-profiles",token,200).get("total").asInt());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='ACCESS_PROFILE_UPDATED'",Integer.class,tenant));
    }
    @Test void applicationUsesExplicitSnapshotAndRejectsStaleProfileAndUser() throws Exception {
        String token=token(company),profile=create(token,"Operador",List.of("product:read"));
        String user=postApi("/api/v1/users",token,Map.of("name","Operador","email","operator@example.com","password",password,"permissions",List.of()),201).get("id").asText();
        String operator=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),200).get("token").asText();
        assertEquals(200,apply(token,user,profile,0,0));getApi("/api/v1/products",operator,200);
        assertEquals(200,update(token,profile,"Operador",List.of("branch:read"),0));
        getApi("/api/v1/products",operator,200);getApi("/api/v1/branches",operator,403);
        assertEquals(409,apply(token,user,profile,0,1));assertEquals(409,apply(token,user,profile,1,0));
        assertEquals(200,apply(token,user,profile,1,1));getApi("/api/v1/products",operator,403);getApi("/api/v1/branches",operator,200);
        assertEquals(200,apply(token,user,profile,1,2));
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_PERMISSIONS_CHANGED'",Integer.class,tenant));
    }
    @Test void profilesRespectTenantPermissionsAndLastManagerRule() throws Exception {
        String token=token(company),profile=create(token,"Consulta",List.of("product:read"));
        String self=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant).toString();
        assertEquals(409,apply(token,self,profile,0,0));
        String otherCompany="other-"+UUID.randomUUID();provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);
        String otherToken=token(otherCompany);
        assertEquals(0,getApi("/api/v1/access-profiles",otherToken,200).get("total").asInt());
        assertEquals(404,update(otherToken,profile,"Inválido",List.of(),0));
        assertEquals(404,apply(otherToken,self,profile,0,0));
        postApi("/api/v1/access-profiles",token,Map.of("name","Inválido","permissions",List.of("user:write")),400);
        postApi("/api/v1/access-profiles",token,Map.of("name","Inválido","permissions",List.of("superuser")),400);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='product:read'",tenant);
        assertEquals(403,update(token,profile,"Inválido",List.of(),0));
        postApi("/api/v1/access-profiles",token,Map.of("name","Inválido","permissions",List.of("product:read")),403);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='user:write'",tenant);
        assertEquals(403,apply(token,self,profile,0,0));
    }
    @Test void concurrentProfileUpdatesHaveOneWinnerAndOneAudit() throws Exception {
        String token=token(company),profile=create(token,"Consulta",List.of("product:read"));
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{start.await();return update(token,profile,"Filiais",List.of("branch:read"),0);});
            var second=pool.submit(()->{start.await();return update(token,profile,"Auditoria",List.of("audit:read"),0);});start.countDown();
            var results=new ArrayList<>(List.of(first.get(30,java.util.concurrent.TimeUnit.SECONDS),second.get(30,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);
        }
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='ACCESS_PROFILE_UPDATED'",Integer.class,tenant));
    }
    @Test void invalidPayloadPaginationAndAnonymousAccessAreRejected() throws Exception {
        String token=token(company);
        http.perform(get("/api/v1/access-profiles")).andExpect(status().isUnauthorized());
        http.perform(get("/api/v1/access-profiles?size=101").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        postApi("/api/v1/access-profiles",token,Map.of("name"," ","permissions",List.of()),400);
        assertEquals(400,putStatus("/api/v1/users/"+UUID.randomUUID()+"/profile",token,Map.of()));
    }
}
