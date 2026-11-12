package br.com.erp;

import br.com.erp.tenant.ProvisioningService;
import com.fasterxml.jackson.databind.*;
import java.util.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest @AutoConfigureMockMvc @ActiveProfiles("test")
abstract class ApiTestSupport {
    @Autowired MockMvc http;
    @Autowired ProvisioningService provisioning;
    @Autowired JdbcTemplate db;
    @Autowired ObjectMapper json;
    String company;
    UUID tenant;
    final String password="Secure-password-2026";
    @BeforeEach void company() {
        company="test-"+UUID.randomUUID();
        tenant=provisioning.create("Empresa de teste",company,"Administrador","admin@example.com",password);
    }
    String token(String slug) throws Exception {
        return postApi("/api/v1/auth/login",null,Map.of("company",slug,"email","admin@example.com","password",password),200).get("token").asText();
    }
    String productBody(String sku,String code,String price) throws Exception {
        return json.writeValueAsString(Map.of("sku",sku,"name","Produto "+sku,"description","Cadastro interno","unit","UN","salePrice",price,"barcodes",List.of(Map.of("code",code,"kind","GTIN"))));
    }
    JsonNode postApi(String path,String token,Object body,int status) throws Exception {
        var request=post(path).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body));
        if (token!=null) request.header("Authorization","Bearer "+token);
        var result=http.perform(request).andExpect(status().is(status)).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
    JsonNode getApi(String path,String token,int status) throws Exception {
        var result=http.perform(get(path).header("Authorization","Bearer "+token)).andExpect(status().is(status)).andReturn();
        return json.readTree(result.getResponse().getContentAsString());
    }
}
