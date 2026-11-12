package br.com.erp;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class UserTest extends ApiTestSupport {
    private int editPermissions(String token,String id,List<String> permissions,long version) throws Exception {
        return http.perform(put("/api/v1/users/"+id+"/permissions").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("permissions",permissions,"version",version)))).andReturn().getResponse().getStatus();
    }
    @Test void permissionEditChangesExistingSessionImmediatelyAndNoOpDoesNotAudit() throws Exception {
        String admin=token(company);
        String id=postApi("/api/v1/users",admin,input("operator@example.com",List.of("product:read")),201).get("id").asText();
        String operator=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),200).get("token").asText();
        getApi("/api/v1/products",operator,200);
        assertEquals(200,editPermissions(admin,id,List.of("branch:read"),0));
        getApi("/api/v1/products",operator,403);getApi("/api/v1/branches",operator,200);
        assertEquals(200,editPermissions(admin,id,List.of("branch:read","branch:read"),1));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_PERMISSIONS_CHANGED'",Integer.class,tenant));
        assertEquals(1,db.queryForObject("SELECT version FROM app_user WHERE id=?",Integer.class,UUID.fromString(id)));
        assertEquals(409,editPermissions(admin,id,List.of(),0));
    }
    @Test void permissionEditRejectsPrivilegeEscalationInvalidAndLastManagerRemoval() throws Exception {
        String admin=token(company);
        String self=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant).toString();
        assertEquals(409,editPermissions(admin,self,List.of("user:read"),0));
        assertEquals(400,editPermissions(admin,self,List.of("user:write"),0));
        String id=postApi("/api/v1/users",admin,input("operator@example.com",List.of()),201).get("id").asText();
        assertEquals(400,editPermissions(admin,id,List.of("superuser"),0));
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='inventory:cost'",tenant);
        assertEquals(403,editPermissions(admin,id,List.of("inventory:cost"),0));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_PERMISSIONS_CHANGED'",Integer.class,tenant));
    }
    @Test void permissionEditIsTenantScopedAndCannotEditHigherPrivilegeAccount() throws Exception {
        String admin=token(company);
        String otherCompany="other-"+UUID.randomUUID();
        UUID other=provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);
        String otherId=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,other).toString();
        assertEquals(404,editPermissions(admin,otherId,List.of(),0));
        String id=postApi("/api/v1/users",admin,input("operator@example.com",List.of("inventory:cost")),201).get("id").asText();
        UUID self=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=? AND email='admin@example.com'",UUID.class,tenant);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND user_id=? AND permission='inventory:cost'",tenant,self);
        assertEquals(403,editPermissions(admin,id,List.of(),0));
    }
    @Test void concurrentManagersCannotBothRemoveOwnManagementPermission() throws Exception {
        String firstToken=token(company);
        String firstId=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant).toString();
        String secondId=postApi("/api/v1/users",firstToken,input("manager@example.com",new ArrayList<>(br.com.erp.user.UserService.PERMISSIONS)),201).get("id").asText();
        String secondToken=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","manager@example.com","password",password),200).get("token").asText();
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{start.await();return editPermissions(firstToken,firstId,List.of("user:read"),0);});
            var second=pool.submit(()->{start.await();return editPermissions(secondToken,secondId,List.of("user:read"),0);});start.countDown();
            var results=new ArrayList<>(List.of(first.get(30,java.util.concurrent.TimeUnit.SECONDS),second.get(30,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(results);
            assertEquals(List.of(200,409),results);
        }
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM user_permission WHERE tenant_id=? AND permission='user:write'",Integer.class,tenant));
    }
    @Test void statusRequiresExplicitStateAndVersion() throws Exception {
        String admin=token(company);
        String id=postApi("/api/v1/users",admin,input("operator@example.com",List.of()),201).get("id").asText();
        for(String body:List.of("{}","{\"version\":0}","{\"active\":false}")) {
            http.perform(put("/api/v1/users/"+id+"/active").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertTrue(db.queryForObject("SELECT active FROM app_user WHERE id=?",Boolean.class,UUID.fromString(id)));
    }
    @Test void concurrentStatusChangesRejectStaleVersionAndAuditOnce() throws Exception {
        String admin=token(company);
        String id=postApi("/api/v1/users",admin,input("operator@example.com",List.of()),201).get("id").asText();
        var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> task=()->{start.await();return http.perform(put("/api/v1/users/"+id+"/active").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"active\":false,\"version\":0}")).andReturn().getResponse().getStatus();};
            var first=pool.submit(task);var second=pool.submit(task);start.countDown();
            var results=new ArrayList<>(List.of(first.get(30,java.util.concurrent.TimeUnit.SECONDS),second.get(30,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(results);
            assertEquals(List.of(200,409),results);
        }
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_DEACTIVATED'",Integer.class,tenant));
    }
    private Map<String,Object> input(String email,List<String> permissions) { return Map.of("name","Operador","email",email,"password",password,"permissions",permissions); }
    private void active(String token,String id,boolean active,long version,int status) throws Exception {
        http.perform(put("/api/v1/users/"+id+"/active").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("active",active,"version",version)))).andExpect(status().is(status));
    }
    @Test void createUsesHashAndScopedPermissionsAndNoSecretsInResponse() throws Exception {
        String admin=token(company);
        var created=postApi("/api/v1/users",admin,input("OPERATOR@example.com",List.of("product:read","product:read")),201);
        assertEquals("operator@example.com",created.get("email").asText());
        assertEquals(1,created.get("permissions").size()); assertEquals(6,created.size());
        assertNotEquals(password,db.queryForObject("SELECT password_hash FROM app_user WHERE id=?",String.class,UUID.fromString(created.get("id").asText())));
        String operator=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),200).get("token").asText();
        getApi("/api/v1/products",operator,200); getApi("/api/v1/users",operator,403);
        assertEquals(2,getApi("/api/v1/users",admin,200).get("total").asInt());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_CREATED'",Integer.class,tenant));
    }
    @Test void duplicateEmailAndInvalidPermissionRollBackWithoutAudit() throws Exception {
        String admin=token(company);
        postApi("/api/v1/users",admin,input("ADMIN@example.com",List.of()),409);
        postApi("/api/v1/users",admin,input("bad@example.com",List.of("superuser")),400);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM app_user WHERE tenant_id=?",Integer.class,tenant));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_CREATED'",Integer.class,tenant));
        http.perform(get("/api/v1/users?size=101").header("Authorization","Bearer "+admin)).andExpect(status().isBadRequest());
        http.perform(get("/api/v1/users")).andExpect(status().isUnauthorized());
    }
    @Test void cannotGrantPermissionsNotHeldAndRevocationTakesEffectImmediately() throws Exception {
        String admin=token(company);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='inventory:cost'",tenant);
        postApi("/api/v1/users",admin,input("bad@example.com",List.of("inventory:cost")),403);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='user:write'",tenant);
        postApi("/api/v1/users",admin,input("bad@example.com",List.of()),403);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM app_user WHERE tenant_id=?",Integer.class,tenant));
    }
    @Test void deactivateRevokesSessionsAndReactivationDoesNotRestoreThem() throws Exception {
        String admin=token(company);
        var user=postApi("/api/v1/users",admin,input("operator@example.com",List.of("product:read")),201);
        String id=user.get("id").asText();
        String operator=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),200).get("token").asText();
        active(admin,id,false,0,200);
        getApi("/api/v1/auth/me",operator,401);
        postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),401);
        active(admin,id,true,0,409);
        active(admin,id,true,1,200);
        getApi("/api/v1/auth/me",operator,401);
        postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),200);
        assertEquals(2,db.queryForObject("SELECT version FROM app_user WHERE id=?",Integer.class,UUID.fromString(id)));
    }
    @Test void cannotDeactivateSelfOrAccessOtherTenantUser() throws Exception {
        String admin=token(company);
        String self=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant).toString();
        active(admin,self,false,0,409);
        String otherCompany="other-"+UUID.randomUUID();
        UUID other=provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);
        String id=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,other).toString();
        active(admin,id,false,0,404);
        assertEquals(1,getApi("/api/v1/users",admin,200).get("total").asInt());
    }
    @Test void cannotReactivateUserWithHigherPrivileges() throws Exception {
        String admin=token(company);
        var user=postApi("/api/v1/users",admin,input("manager@example.com",List.of("inventory:cost")),201);
        String id=user.get("id").asText(); active(admin,id,false,0,200);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND user_id=? AND permission='inventory:cost'",tenant,db.queryForObject("SELECT id FROM app_user WHERE tenant_id=? AND email='admin@example.com'",UUID.class,tenant));
        active(admin,id,true,1,403);
    }
}
