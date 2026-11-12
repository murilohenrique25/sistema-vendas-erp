package br.com.erp;

import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class BranchScopeTest extends ApiTestSupport {
    String admin,operator,user;UUID first,second,warehouse1,warehouse2,product;
    @BeforeEach void setupScope() throws Exception {
        admin=token(company);first=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,tenant);
        second=UUID.fromString(postApi("/api/v1/branches",admin,Map.of("code","SECOND","name","Segunda filial"),201).get("id").asText());
        warehouse1=warehouse(first,"W1");warehouse2=warehouse(second,"W2");
        product=UUID.fromString(postApi("/api/v1/products",admin,Map.of("sku","SCOPE","name","Produto","description","","unit","UN","salePrice","10","barcodes",List.of()),201).get("id").asText());
        var permissions=new ArrayList<>(br.com.erp.user.UserService.PERMISSIONS);permissions.remove("user:write");
        user=postApi("/api/v1/users",admin,Map.of("name","Operador","email","operator@example.com","password",password,"permissions",permissions),201).get("id").asText();
        operator=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","operator@example.com","password",password),200).get("token").asText();
    }
    UUID warehouse(UUID branch,String code) throws Exception { return UUID.fromString(postApi("/api/v1/inventory/warehouses",admin,Map.of("branchId",branch,"code",code,"name",code),201).get("id").asText()); }
    int scope(boolean all,List<UUID> ids,long version) throws Exception { return http.perform(put("/api/v1/users/"+user+"/branches").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("allBranches",all,"branchIds",ids,"version",version)))).andReturn().getResponse().getStatus(); }
    Map<String,Object> receipt(UUID warehouse) { return Map.of("operationId",UUID.randomUUID(),"warehouseId",warehouse,"productId",product,"type","RECEIPT","quantity","5","unitCost","2","reason","Entrada para teste"); }
    @Test void scopeFiltersPagesAndDirectAccessAndUpdatesExistingSession() throws Exception {
        assertEquals(200,scope(false,List.of(first),0));
        assertEquals(1,getApi("/api/v1/branches",operator,200).get("total").asInt());
        assertEquals(1,getApi("/api/v1/inventory/warehouses",operator,200).get("total").asInt());
        getApi("/api/v1/branches/"+second,operator,404);getApi("/api/v1/branches/"+first,operator,200);
        getApi("/api/v1/inventory/warehouses/"+warehouse2+"/balances",operator,404);
        getApi("/api/v1/inventory/warehouses/"+warehouse2+"/movements",operator,404);
        assertFalse(getApi("/api/v1/auth/me",operator,200).get("allBranches").asBoolean());
        assertEquals(200,scope(false,List.of(second),1));
        getApi("/api/v1/branches/"+first,operator,404);getApi("/api/v1/branches/"+second,operator,200);
        assertEquals(409,scope(true,List.of(),1));
    }
    @Test void unauthorizedMovementTransferAndWarehouseCreationRollBack() throws Exception {
        postApi("/api/v1/inventory/movements",admin,receipt(warehouse1),200);
        assertEquals(200,scope(false,List.of(first),0));
        postApi("/api/v1/inventory/movements",operator,receipt(warehouse2),404);
        postApi("/api/v1/inventory/warehouses",operator,Map.of("branchId",second,"code","DENIED","name","Negado"),404);
        postApi("/api/v1/inventory/transfers",operator,Map.of("operationId",UUID.randomUUID(),"sourceWarehouseId",warehouse1,"destinationWarehouseId",warehouse2,"productId",product,"quantity","1","reason","Transferência teste"),404);
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE tenant_id=?",Integer.class,tenant));
        assertEquals(200,scope(false,List.of(first,second),1));
        postApi("/api/v1/inventory/transfers",operator,Map.of("operationId",UUID.randomUUID(),"sourceWarehouseId",warehouse1,"destinationWarehouseId",warehouse2,"productId",product,"quantity","1","reason","Transferência teste"),200);
    }
    @Test void lossesAreFilteredAndNeitherRejectionNorApprovalBypassScope() throws Exception {
        postApi("/api/v1/inventory/movements",admin,receipt(warehouse2),200);
        String loss=postApi("/api/v1/losses",admin,Map.of("requestId",UUID.randomUUID(),"warehouseId",warehouse2,"productId",product,"quantity","1","reason","DAMAGE","details","Avaria de teste"),200).get("id").asText();
        assertEquals(200,scope(false,List.of(first),0));
        assertEquals(0,getApi("/api/v1/losses",operator,200).get("total").asInt());
        getApi("/api/v1/losses/"+loss,operator,404);
        postApi("/api/v1/losses/"+loss+"/reject",operator,Map.of("note","Rejeição teste"),404);
        postApi("/api/v1/losses/"+loss+"/approve",operator,Map.of("operationId",UUID.randomUUID()),404);
        assertEquals("PENDING",db.queryForObject("SELECT status FROM stock_loss WHERE id=?",String.class,UUID.fromString(loss)));
        assertEquals(200,scope(false,List.of(second),1));
        postApi("/api/v1/losses/"+loss+"/approve",operator,Map.of("operationId",UUID.randomUUID()),200);
    }
    @Test void globalAdministrationAndManagerGrantsCannotBypassScope() throws Exception {
        assertEquals(200,scope(false,List.of(first),0));
        getApi("/api/v1/users",operator,403);getApi("/api/v1/access-profiles",operator,403);getApi("/api/v1/audit",operator,403);
        postApi("/api/v1/branches",operator,Map.of("code","DENIED","name","Negada"),403);
        http.perform(put("/api/v1/users/"+user+"/permissions").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content("{\"permissions\":[\"user:read\",\"user:write\"],\"version\":1}")).andExpect(status().isBadRequest());
        String self=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=? AND email='admin@example.com'",UUID.class,tenant).toString();
        http.perform(put("/api/v1/users/"+self+"/branches").header("Authorization","Bearer "+admin).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("allBranches",false,"branchIds",List.of(first),"version",0)))).andExpect(status().isBadRequest());
    }
    @Test void emptyScopeNoOpAndCrossTenantBranchValidation() throws Exception {
        assertEquals(200,scope(false,List.of(),0));
        assertEquals(0,getApi("/api/v1/branches",operator,200).get("total").asInt());
        assertEquals(0,getApi("/api/v1/inventory/warehouses",operator,200).get("total").asInt());
        assertEquals(200,scope(false,List.of(),1));
        UUID other=provisioning.create("Outra","other-"+UUID.randomUUID(),"Admin","admin@example.com",password);
        UUID otherBranch=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,other);
        assertEquals(404,scope(false,List.of(otherBranch),1));
        assertEquals(400,scope(true,List.of(first),1));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='USER_BRANCH_SCOPE_CHANGED'",Integer.class,tenant));
        getApi("/api/v1/products",operator,200);
    }
}
