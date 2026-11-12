package br.com.erp;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
abstract class SaleFixture extends ApiTestSupport {
 String auth,account,terminal,warehouse,product,session;UUID branch;
 @BeforeEach void fixture()throws Exception{
  auth=token(company);branch=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,tenant);
  account=postApi("/api/v1/treasury/accounts",auth,Map.of("requestId",UUID.randomUUID(),"branchId",branch,"code","C","name","Caixa","kind","CASH"),201).get("id").asText();
  terminal=postApi("/api/v1/pos/terminals",auth,Map.of("requestId",UUID.randomUUID(),"branchId",branch,"accountId",account,"code","T","name","Terminal"),201).get("id").asText();
  warehouse=postApi("/api/v1/inventory/warehouses",auth,Map.of("branchId",branch,"code","W","name","Depósito"),201).get("id").asText();
  bind(warehouse,0,200);product=createProduct("P","10.25");stock(product,"10");
  session=postApi("/api/v1/pos/cash-sessions",auth,Map.of("requestId",UUID.randomUUID(),"terminalId",terminal,"openingAmount","0","note","Conferência de abertura"),201).get("id").asText();
 }
 String createProduct(String code,String price)throws Exception{return postApi("/api/v1/products",auth,Map.of("sku",code,"name","Produto "+code,"description","","unit","KG","salePrice",price,"barcodes",List.of()),201).get("id").asText();}
 void stock(String id,String quantity)throws Exception{postApi("/api/v1/inventory/movements",auth,Map.of("operationId",UUID.randomUUID(),"warehouseId",warehouse,"productId",id,"type","RECEIPT","quantity",quantity,"unitCost","3","reason","Entrada para testes"),200);}
 void bind(String warehouse,long version,int status)throws Exception{http.perform(put("/api/v1/pos/terminals/"+terminal+"/warehouse").header("Authorization","Bearer "+auth).contentType("application/json").content(json.writeValueAsString(Map.of("warehouseId",warehouse,"version",version)))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(status));}
 Map<String,Object> item(String id,String quantity,String price){return Map.of("productId",id,"quantity",quantity,"expectedUnitPrice",price);}
 Map<String,Object> input(UUID request,String quantity,String price,String tendered){return Map.of("requestId",request,"sessionId",session,"tendered",tendered,"items",List.of(item(product,quantity,price)));}
 JsonNode sell(Object body,int status)throws Exception{return postApi("/api/v1/pos/sales",auth,body,status);}
 void money(String expected,JsonNode actual){assertTrue(actual.isTextual());assertEquals(0,new BigDecimal(expected).compareTo(new BigDecimal(actual.asText())));}
 BigDecimal balance(){return db.queryForObject("SELECT balance FROM treasury_account WHERE tenant_id=? AND id=?",BigDecimal.class,tenant,UUID.fromString(account));}
 BigDecimal quantity(String id){return db.queryForObject("SELECT quantity FROM stock_balance WHERE tenant_id=? AND warehouse_id=? AND product_id=?",BigDecimal.class,tenant,UUID.fromString(warehouse),UUID.fromString(id));}
 long count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=?",Long.class,tenant);}
 List<Integer> race(Object one,Object two)throws Exception{try(var pool=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return status("/api/v1/pos/sales",one);});var b=pool.submit(()->{gate.await();return status("/api/v1/pos/sales",two);});gate.countDown();var result=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS)));Collections.sort(result);return result;}}
 int status(String path,Object body)throws Exception{return http.perform(post(path).header("Authorization","Bearer "+auth).contentType("application/json").content(json.writeValueAsString(body))).andReturn().getResponse().getStatus();}
}
