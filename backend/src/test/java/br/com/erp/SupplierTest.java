package br.com.erp;

import java.util.*;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class SupplierTest extends ApiTestSupport {
    private Map<String,Object> input(String code,String name) { return Map.of("code",code,"name",name,"taxIdentifier","SUPPLIER-TEST","contactName","Contato","email","CONTACT@example.com","phone","123","address","Endereço de teste","notes","Observação de teste"); }
    private String create(String token,String code,String name) throws Exception { return postApi("/api/v1/suppliers",token,input(code,name),201).get("id").asText(); }
    private int update(String token,String id,Map<String,Object> data,boolean active,long version) throws Exception { return http.perform(put("/api/v1/suppliers/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("data",data,"active",active,"version",version)))).andReturn().getResponse().getStatus(); }
    @Test void createNormalizesCodeEmailAndSearchesStoredData() throws Exception {
        String token=token(company),id=create(token,"s-01","Fornecedor Centro");
        var supplier=getApi("/api/v1/suppliers/"+id,token,200);
        assertEquals("S-01",supplier.get("code").asText());assertEquals("contact@example.com",supplier.get("email").asText());assertTrue(supplier.get("active").asBoolean());
        for(String query:List.of("centro","s-01","SUPPLIER-TEST")) assertEquals(1,getApi("/api/v1/suppliers?q="+query,token,200).get("total").asInt());
        assertEquals(0,getApi("/api/v1/suppliers?q=ausente",token,200).get("total").asInt());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='SUPPLIER_CREATED'",Integer.class,tenant));
    }
    @Test void uniquenessAndUpdatesAreScopedToTenantWithAtomicRollback() throws Exception {
        String token=token(company),first=create(token,"SUP1","Primeiro"),second=create(token,"SUP2","Segundo");
        postApi("/api/v1/suppliers",token,input("sup1","Duplicado"),409);
        assertEquals(409,update(token,second,input("sup1","Duplicado"),false,0));
        var original=getApi("/api/v1/suppliers/"+second,token,200);assertEquals("Segundo",original.get("name").asText());assertTrue(original.get("active").asBoolean());assertEquals(0,original.get("version").asInt());
        String otherCompany="other-"+UUID.randomUUID();provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);String other=token(otherCompany);
        create(other,"SUP1","Outra empresa");getApi("/api/v1/suppliers/"+first,other,404);
        assertEquals(404,update(other,first,input("SUP1","Outra"),true,0));
        assertEquals(1,getApi("/api/v1/suppliers",other,200).get("total").asInt());
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='SUPPLIER_UPDATED'",Integer.class,tenant));
    }
    @Test void updateDeactivateReactivateAndNoOpRespectVersion() throws Exception {
        String token=token(company),id=create(token,"SUP1","Original");
        assertEquals(200,update(token,id,input("sup1","Original"),true,0));
        assertEquals(200,update(token,id,input("SUP1","Atualizado"),false,0));
        var inactive=getApi("/api/v1/suppliers/"+id,token,200);assertFalse(inactive.get("active").asBoolean());assertEquals(1,inactive.get("version").asInt());
        assertEquals(409,update(token,id,input("SUP1","Antigo"),true,0));
        assertEquals(200,update(token,id,input("SUP1","Atualizado"),true,1));
        assertTrue(getApi("/api/v1/suppliers/"+id,token,200).get("active").asBoolean());
        assertEquals(2,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='SUPPLIER_UPDATED'",Integer.class,tenant));
    }
    @Test void readAndWritePermissionsAreIndependentAndRevocationImmediate() throws Exception {
        String token=token(company),id=create(token,"SUP1","Original");
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='supplier:write'",tenant);
        getApi("/api/v1/suppliers",token,200);postApi("/api/v1/suppliers",token,input("SUP2","Negado"),403);
        assertEquals(403,update(token,id,input("SUP1","Negado"),false,0));
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='supplier:read'",tenant);
        getApi("/api/v1/suppliers",token,403);getApi("/api/v1/suppliers/"+id,token,403);
        http.perform(get("/api/v1/suppliers")).andExpect(status().isUnauthorized());
    }
    @Test void invalidFieldsMissingVersionAndNestedPayloadAreRejected() throws Exception {
        String token=token(company),id=create(token,"SUP1","Original");
        var bad=new HashMap<String,Object>(input("SUP2","Inválido"));bad.put("email","not-an-email");
        postApi("/api/v1/suppliers",token,bad,400);
        assertEquals(400,update(token,id,bad,true,0));
        http.perform(put("/api/v1/suppliers/"+id).header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("data",input("SUP1","Original"),"active",false)))).andExpect(status().isBadRequest());
        http.perform(get("/api/v1/suppliers?size=101").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        assertEquals(1,getApi("/api/v1/suppliers",token,200).get("total").asInt());
    }
    @Test void concurrentEditsHaveOneWinnerAndOneAudit() throws Exception {
        String token=token(company),id=create(token,"SUP1","Original");var start=new java.util.concurrent.CountDownLatch(1);
        try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first=pool.submit(()->{start.await();return update(token,id,input("SUP1","Primeiro"),false,0);});
            var second=pool.submit(()->{start.await();return update(token,id,input("SUP1","Segundo"),true,0);});start.countDown();
            var results=new ArrayList<>(List.of(first.get(30,java.util.concurrent.TimeUnit.SECONDS),second.get(30,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(results);assertEquals(List.of(200,409),results);
        }
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='SUPPLIER_UPDATED'",Integer.class,tenant));
    }
}
