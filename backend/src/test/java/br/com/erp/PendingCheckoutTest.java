package br.com.erp;
import java.util.*;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class PendingCheckoutTest extends SaleFixture {
 String base="/api/v1/pos/pending-checkouts";
 Map<String,Object> prepare(UUID id){return Map.of("requestId",id,"sessionId",session,"items",List.of(item(product,"1","10.25")));}
 Map<String,Object> completion(String id){return Map.of("version",0,"sale",input(UUID.fromString(id),"1","10.25","11"));}
 @Test void preparationIsNotMoneyOrStockAndReplays()throws Exception{
  var body=prepare(UUID.randomUUID());var v=postApi(base,auth,body,201);assertEquals("PENDING",v.path("status").asText());money("10.25",v.path("total"));assertEquals(v.path("id"),postApi(base,auth,body,201).path("id"));assertEquals(0,balance().signum());assertEquals(0,quantity(product).compareTo(new java.math.BigDecimal("10")));assertEquals(0,count("pos_sale"));assertEquals(1,count("pos_pending_checkout"));
  assertEquals(0,db.queryForObject("SELECT version FROM pos_cash_session WHERE id=?",Long.class,UUID.fromString(session)));
 }
 @Test void changedPreparationRequestConflicts()throws Exception{
  UUID key=UUID.randomUUID();postApi(base,auth,prepare(key),201);postApi(base,auth,Map.of("requestId",key,"sessionId",session,"items",List.of(item(product,"2","10.25"))),409);
 }
 @Test void completionPostsExactlyOnce()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();var body=completion(id);var sale=postApi(base+"/"+id+"/complete",auth,body,200);assertEquals(sale.path("id"),postApi(base+"/"+id+"/complete",auth,body,200).path("id"));assertEquals(1,count("pos_sale"));assertEquals(0,balance().compareTo(new java.math.BigDecimal("10.25")));assertEquals(0,quantity(product).compareTo(new java.math.BigDecimal("9")));assertEquals("COMPLETED",db.queryForObject("SELECT status FROM pos_pending_checkout WHERE id=?",String.class,UUID.fromString(id)));
 }
 @Test void failedPaymentLeavesPreparationPendingAndNoSale()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();postApi(base+"/"+id+"/complete",auth,Map.of("version",0,"sale",input(UUID.fromString(id),"1","10.25","1")),400);assertEquals(0,count("pos_sale"));assertEquals(0,balance().signum());assertEquals("PENDING",db.queryForObject("SELECT status FROM pos_pending_checkout WHERE id=?",String.class,UUID.fromString(id)));postApi(base+"/"+id+"/complete",auth,completion(id),200);
 }
 @Test void cartAndVersionMustMatch()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();postApi(base+"/"+id+"/complete",auth,Map.of("version",1,"sale",input(UUID.fromString(id),"1","10.25","11")),409);postApi(base+"/"+id+"/complete",auth,Map.of("version",0,"sale",input(UUID.fromString(id),"2","10.25","21")),409);assertEquals(0,count("pos_sale"));
 }
 @Test void currentPriceMustStillMatch()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();db.update("UPDATE product SET sale_price=12 WHERE tenant_id=? AND id=?",tenant,UUID.fromString(product));postApi(base+"/"+id+"/complete",auth,completion(id),409);assertEquals(0,count("pos_sale"));
 }
 @Test void discardReplaysButPreventsCompletion()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();var discard=Map.of("requestId",UUID.randomUUID(),"version",0,"note","Cliente desistiu");var result=postApi(base+"/"+id+"/discard",auth,discard,200);assertEquals("DISCARDED",result.path("status").asText());assertEquals(result,postApi(base+"/"+id+"/discard",auth,discard,200));postApi(base+"/"+id+"/complete",auth,completion(id),409);assertEquals(0,count("pos_sale"));
 }
 @Test void listAndAnonymousAccess()throws Exception{
  postApi(base,auth,prepare(UUID.randomUUID()),201);http.perform(get(base).header("Authorization","Bearer "+auth)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk()).andExpect(jsonPath("$.total").value(1));http.perform(get(base)).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());http.perform(post(base).contentType("application/json").content(json.writeValueAsString(prepare(UUID.randomUUID())))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isUnauthorized());
 }
 @Test void invalidItemsAreRejected()throws Exception{
  postApi(base,auth,Map.of("requestId",UUID.randomUUID(),"sessionId",session,"items",List.of()),400);postApi(base,auth,Map.of("requestId",UUID.randomUUID(),"sessionId",session,"items",List.of(item(product,"0","10.25"))),400);assertEquals(0,count("pos_pending_checkout"));
 }
 @Test void tenantBranchAndRevokedPermissionAreEnforced()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();String other="other-"+UUID.randomUUID();provisioning.create("Outra",other,"Admin","admin@example.com",password);getApi(base+"/"+id,token(other),404);db.update("UPDATE app_user SET all_branches=FALSE WHERE tenant_id=?",tenant);getApi(base+"/"+id,auth,404);assertEquals(0,getApi(base,auth,200).path("total").asInt());db.update("UPDATE app_user SET all_branches=TRUE WHERE tenant_id=?",tenant);db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='pos:sell'",tenant);getApi(base+"/"+id,auth,403);postApi(base+"/"+id+"/complete",auth,completion(id),403);
 }
 @Test void insufficientStockRollsBackAndCompletedReplaySurvivesClose()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();db.update("UPDATE stock_balance SET quantity=0 WHERE tenant_id=? AND product_id=?",tenant,UUID.fromString(product));postApi(base+"/"+id+"/complete",auth,completion(id),409);assertEquals(0,balance().signum());assertEquals(0,count("pos_sale"));db.update("UPDATE stock_balance SET quantity=10 WHERE tenant_id=? AND product_id=?",tenant,UUID.fromString(product));var sale=postApi(base+"/"+id+"/complete",auth,completion(id),200);postApi("/api/v1/pos/cash-sessions/"+session+"/close",auth,Map.of("requestId",UUID.randomUUID(),"version",1,"countedAmount","10.25","note","Fechamento para teste"),200);db.update("UPDATE product SET active=FALSE WHERE tenant_id=? AND id=?",tenant,UUID.fromString(product));assertEquals(sale,postApi(base+"/"+id+"/complete",auth,completion(id),200));
 }
 @Test void simultaneousCompletionAndDiscardHaveOneWinner()throws Exception{
  String id=postApi(base,auth,prepare(UUID.randomUUID()),201).path("id").asText();try(var pool=java.util.concurrent.Executors.newFixedThreadPool(2)){var gate=new java.util.concurrent.CountDownLatch(1);var one=pool.submit(()->{gate.await();return status(base+"/"+id+"/complete",completion(id));});var two=pool.submit(()->{gate.await();return status(base+"/"+id+"/discard",Map.of("requestId",UUID.randomUUID(),"version",0,"note","Desistência concorrente"));});gate.countDown();var result=new ArrayList<>(List.of(one.get(30,java.util.concurrent.TimeUnit.SECONDS),two.get(30,java.util.concurrent.TimeUnit.SECONDS)));Collections.sort(result);assertEquals(List.of(200,409),result);var v=getApi(base+"/"+id,auth,200);assertEquals(v.path("status").asText().equals("COMPLETED")?1:0,count("pos_sale"));}
 }
}
