package br.com.erp;

import br.com.erp.tenant.ProvisioningService;
import java.util.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;
import org.springframework.http.MediaType;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
class FoundationTest extends ApiTestSupport {
    @Test void sessionsExposeNoTokensAndRevocationPreservesCurrentAndOtherUsers() throws Exception {
        String current=token(company),other=token(company);
        String otherCompany="other-"+UUID.randomUUID();
        provisioning.create("Outra empresa",otherCompany,"Admin","admin@example.com",password);
        String unrelated=token(otherCompany);
        var sessions=getApi("/api/v1/auth/sessions",current,200);
        assertEquals(2,sessions.size());
        assertEquals(1,java.util.stream.StreamSupport.stream(sessions.spliterator(),false).filter(s -> s.get("current").asBoolean()).count());
        for (var session:sessions) {
            assertEquals(3,session.size());
            assertTrue(session.has("createdAt")); assertTrue(session.has("expiresAt"));
        }
        http.perform(delete("/api/v1/auth/sessions/others").header("Authorization","Bearer "+current)).andExpect(status().isNoContent());
        getApi("/api/v1/auth/me",current,200);
        getApi("/api/v1/auth/me",other,401);
        getApi("/api/v1/auth/me",unrelated,200);
        assertEquals(1,getApi("/api/v1/auth/sessions",current,200).size());
        http.perform(delete("/api/v1/auth/sessions/others").header("Authorization","Bearer "+current)).andExpect(status().isNoContent());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='OTHER_SESSIONS_REVOKED'",Integer.class,tenant));
    }
    @Test void sessionsExcludeExpiredAndRevocationIsScopedToSameUser() throws Exception {
        String current=token(company);
        UUID user=UUID.randomUUID();
        String hash=db.queryForObject("SELECT password_hash FROM app_user WHERE tenant_id=?",String.class,tenant);
        db.update("INSERT INTO app_user(id,tenant_id,name,email,password_hash) VALUES(?,?,?,?,?)",user,tenant,"Outro usuário","other@example.com",hash);
        String otherUser=postApi("/api/v1/auth/login",null,Map.of("company",company,"email","other@example.com","password",password),200).get("token").asText();
        String expired=token(company);
        db.update("UPDATE auth_session SET expires_at=TIMESTAMP '2020-01-01 00:00:00' WHERE token_hash=?",br.com.erp.auth.TokenCodec.hash(expired));
        assertEquals(1,getApi("/api/v1/auth/sessions",current,200).size());
        http.perform(delete("/api/v1/auth/sessions/others").header("Authorization","Bearer "+current)).andExpect(status().isNoContent());
        getApi("/api/v1/auth/me",otherUser,200);
        http.perform(delete("/api/v1/auth/sessions/others")).andExpect(status().isUnauthorized());
    }
    @Test void anonymousCannotReadTenantData() throws Exception {
        http.perform(get("/api/v1/branches")).andExpect(status().isUnauthorized());
    }
    @Test void createListAuditAndLogout() throws Exception {
        String token=token(company);
        http.perform(post("/api/v1/branches").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"LOJA2\",\"name\":\"Loja Centro\"}")).andExpect(status().isCreated()).andExpect(jsonPath("name").value("Loja Centro"));
        http.perform(get("/api/v1/branches").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("total").value(2));
        http.perform(get("/api/v1/audit").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("total").value(3));
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM auth_session WHERE token_hash=?",Integer.class,token));
        http.perform(post("/api/v1/auth/logout").header("Authorization","Bearer "+token)).andExpect(status().isNoContent());
        http.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }
    @Test void tenantCannotReadOtherTenantBranchEvenWithForgedHeader() throws Exception {
        String other="other-"+UUID.randomUUID();
        UUID otherId=provisioning.create("Outra empresa",other,"Admin","admin@example.com",password);
        UUID branch=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,otherId);
        String token=token(company);
        http.perform(get("/api/v1/branches/"+branch).header("Authorization","Bearer "+token).header("X-Tenant-Id",otherId)).andExpect(status().isNotFound());
        http.perform(get("/api/v1/branches").header("Authorization","Bearer "+token).header("X-Tenant-Id",otherId)).andExpect(status().isOk()).andExpect(jsonPath("total").value(1));
    }
    @Test void permissionRevocationTakesEffectImmediately() throws Exception {
        String token=token(company);
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='branch:write'",tenant);
        http.perform(post("/api/v1/branches").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"X\",\"name\":\"Loja\"}")).andExpect(status().isForbidden());
    }
    @Test void uniquenessIsScopedToTenantAndInvalidPageIsRejected() throws Exception {
        String token=token(company);
        http.perform(post("/api/v1/branches").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"MATRIZ\",\"name\":\"Duplicada\"}")).andExpect(status().isConflict());
        http.perform(get("/api/v1/branches?size=500").header("Authorization","Bearer "+token)).andExpect(status().isBadRequest());
        http.perform(post("/api/v1/branches").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content("{\"code\":\"\",\"name\":\"\"}")).andExpect(status().isBadRequest());
    }
    @Test void provisioningRequiresServerSecret() throws Exception {
        http.perform(post("/api/v1/provisioning").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("companyName","Empresa","slug","new-company","adminName","Admin","email","new@example.com","password",password)))).andExpect(status().isForbidden());
    }
    @Test void failedLoginsPersistLockout() throws Exception {
        for (int i=0;i<5;i++) http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("company",company,"email","admin@example.com","password","wrong")))).andExpect(status().isUnauthorized());
        assertEquals(5,db.queryForObject("SELECT failed_attempts FROM app_user WHERE tenant_id=?",Integer.class,tenant));
        http.perform(post("/api/v1/auth/login").contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("company",company,"email","admin@example.com","password",password)))).andExpect(status().isUnauthorized());
    }
    @Test void passwordChangeRevokesAllSessions() throws Exception {
        String first=token(company),second=token(company);
        http.perform(post("/api/v1/auth/password").header("Authorization","Bearer "+first).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(Map.of("currentPassword",password,"newPassword","Changed-password-2026")))).andExpect(status().isNoContent());
        http.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+second)).andExpect(status().isUnauthorized());
    }
    @Test void expiredSessionIsRejected() throws Exception {
        String token=token(company);
        db.update("UPDATE auth_session SET expires_at=TIMESTAMP '2020-01-01 00:00:00' WHERE tenant_id=?",tenant);
        http.perform(get("/api/v1/auth/me").header("Authorization","Bearer "+token)).andExpect(status().isUnauthorized());
    }

    @Test void productIsPersistedSearchedAndFoundByEquivalentGtin() throws Exception {
        String token=token(company);
        http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(productBody("P1","4006381333931","12.50"))).andExpect(status().isCreated()).andExpect(jsonPath("salePrice").value("12.50"));
        http.perform(get("/api/v1/products/lookup?code=04006381333931").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("sku").value("P1"));
        http.perform(get("/api/v1/products?q=638133").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("total").value(1));
        http.perform(get("/api/v1/products?q=produto").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("total").value(1));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PRODUCT_CREATED'",Integer.class,tenant));
    }
    @Test void duplicateBarcodeRollsBackEntireProductAndAudit() throws Exception {
        String token=token(company);
        http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(productBody("P1","4006381333931","12.50"))).andExpect(status().isCreated());
        http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(productBody("P2","04006381333931","10.00"))).andExpect(status().isConflict());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM product WHERE tenant_id=?",Integer.class,tenant));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PRODUCT_CREATED'",Integer.class,tenant));
    }
    @Test void invalidBarcodeAndInvalidMoneyAreRejected() throws Exception {
        String token=token(company);
        for (String body:List.of(productBody("P1","4006381333932","12.50"),productBody("P2","4006381333931","12.501"),productBody("P3","4006381333931","-1.00"))) {
            http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isBadRequest());
        }
        assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM product WHERE tenant_id=?",Integer.class,tenant));
    }
    @Test void productBarcodeDoesNotLeakAcrossTenants() throws Exception {
        String token=token(company);
        String result=http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(productBody("P1","4006381333931","12.50"))).andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        String other="other-"+UUID.randomUUID();
        provisioning.create("Outra",other,"Admin","admin@example.com",password);
        String otherToken=token(other);
        http.perform(get("/api/v1/products/lookup?code=4006381333931").header("Authorization","Bearer "+otherToken)).andExpect(status().isNotFound());
        http.perform(get("/api/v1/products/"+json.readTree(result).get("id").asText()).header("Authorization","Bearer "+otherToken)).andExpect(status().isNotFound());
        http.perform(post("/api/v1/products").header("Authorization","Bearer "+otherToken).contentType(MediaType.APPLICATION_JSON).content(productBody("P1","4006381333931","12.50"))).andExpect(status().isCreated());
    }
    @Test void productWritesRequirePermissionAndInternalCodesAreSupported() throws Exception {
        String token=token(company);
String body=json.writeValueAsString(Map.of("sku","INT-1","name","Produto interno","description","","unit","KG","salePrice","3.25","barcodes",List.of(Map.of("code","BAL-001","kind","INTERNAL"),Map.of("code","X".repeat(60),"kind","INTERNAL"))));
        http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andExpect(status().isCreated());
        http.perform(get("/api/v1/products/lookup?code=BAL-001&kind=INTERNAL").header("Authorization","Bearer "+token)).andExpect(status().isOk()).andExpect(jsonPath("barcodes.length()").value(2));
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='product:write'",tenant);
        http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(productBody("P1","4006381333931","12.50"))).andExpect(status().isForbidden());
    }
    @Test void concurrentDuplicateProductCreatesOnlyOneRecord() throws Exception {
        String token=token(company);
        String body=productBody("P1","4006381333931","12.50");
        var gate=new java.util.concurrent.CountDownLatch(1);
        try (var executor=java.util.concurrent.Executors.newFixedThreadPool(2)) {
            java.util.concurrent.Callable<Integer> create=()-> { gate.await(); return http.perform(post("/api/v1/products").header("Authorization","Bearer "+token).contentType(MediaType.APPLICATION_JSON).content(body)).andReturn().getResponse().getStatus(); };
            var first=executor.submit(create); var second=executor.submit(create); gate.countDown();
            var statuses=new ArrayList<>(List.of(first.get(20,java.util.concurrent.TimeUnit.SECONDS),second.get(20,java.util.concurrent.TimeUnit.SECONDS))); Collections.sort(statuses);
            assertEquals(List.of(201,409),statuses);
        }
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM product WHERE tenant_id=?",Integer.class,tenant));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PRODUCT_CREATED'",Integer.class,tenant));
    }
}
