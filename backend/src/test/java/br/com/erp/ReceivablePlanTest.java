package br.com.erp;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;

class ReceivablePlanTest extends ReceivableTest {
 private static final String ENDPOINT="/api/v1/receivables/plans";
 Map<String,Object> plan(String reference,String amount,int count,String due){
  var result=input(UUID.randomUUID(),reference,amount);result.remove("dueOn");result.put("firstDueOn",due);result.put("installmentCount",count);result.put("intervalMonths",1);return result;
 }
 @Test void previewDistributesRemainderAndAnchorsMonthEndWithoutWrites()throws Exception{
  var body=plan("plan","100",3,"2020-01-31");var preview=postApi(ENDPOINT+"/preview",auth,body,200);
  assertEquals(List.of("33.34","33.33","33.33"),values(preview,"amount"));assertEquals(List.of("2020-01-31","2020-02-29","2020-03-31"),values(preview,"dueOn"));
  assertEquals(List.of("PLAN.001","PLAN.002","PLAN.003"),values(preview,"reference"));assertEquals(0,count("receivable"));assertEquals(0,count("receivable_plan"));
 }
 List<String> values(JsonNode result,String key){var values=new ArrayList<String>();result.get("installments").forEach(row->values.add(row.get(key).asText()));return values;}
 @Test void creationIsAtomicExactAndReturnsCurrentStateOnRetry()throws Exception{
  var body=plan("contract","100",3,"2020-01-31");var created=postApi(ENDPOINT,auth,body,201);assertEquals(3,count("receivable"));assertEquals(1,count("receivable_plan"));
  assertEquals(0,new BigDecimal("100").compareTo(db.queryForObject("SELECT SUM(amount) FROM receivable WHERE tenant_id=?",BigDecimal.class,tenant)));
  var first=created.get("installments").get(0);receive(first,UUID.randomUUID(),"10");body.put("amount","100.00");body.put("reference","CONTRACT");
  var replay=postApi(ENDPOINT,auth,body,201);assertEquals(created.get("id"),replay.get("id"));assertEquals("PARTIAL",replay.get("installments").get(0).get("status").asText());assertEquals(3,count("receivable"));
  assertEquals(replay,getApi(ENDPOINT+"/"+created.get("id").asText(),auth,200));
 }
 @Test void collisionOnLastInstallmentRollsBackHeaderEarlierTitlesAndAudit()throws Exception{
  title("COLLISION.003");postApi(ENDPOINT,auth,plan("collision","100",3,"2020-02-01"),409);
  assertEquals(1,count("receivable"));assertEquals(0,count("receivable_plan"));assertEquals(0,count("receivable_installment"));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='RECEIVABLE_PLAN_CREATED'",Integer.class,tenant));
 }
 @Test void changedPayloadAndDuplicateReferenceAreRejected()throws Exception{
  var body=plan("contract","1",3,"2020-02-01");postApi(ENDPOINT,auth,body,201);body.put("intervalMonths",2);postApi(ENDPOINT,auth,body,409);
  postApi(ENDPOINT,auth,plan("contract","1",2,"2020-02-01"),409);assertEquals(3,count("receivable"));
 }
 @Test void concurrentRetriesCreateOnlyOnePlan()throws Exception{
  var body=plan("parallel","100",3,"2020-02-01");assertEquals(List.of(201,201),concurrent(ENDPOINT,body,ENDPOINT,body));assertEquals(1,count("receivable_plan"));assertEquals(3,count("receivable"));
 }
 @Test void branchScopeAndTenantProtectPreviewCreationAndRead()throws Exception{
  var body=plan("scope","100",3,"2020-02-01");var created=postApi(ENDPOINT,auth,body,201);
  String otherCompany="other-"+UUID.randomUUID();provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);String other=token(otherCompany);
  getApi(ENDPOINT+"/"+created.get("id").asText(),other,404);postApi(ENDPOINT,other,body,404);
  db.update("UPDATE app_user SET all_branches=FALSE WHERE tenant_id=?",tenant);
  getApi(ENDPOINT+"/"+created.get("id").asText(),auth,404);postApi(ENDPOINT+"/preview",auth,body,404);postApi(ENDPOINT,auth,body,404);assertEquals(3,count("receivable"));
 }
 @Test void invalidTermsAndInsufficientCentsCannotCreateTitles()throws Exception{
  postApi(ENDPOINT,auth,plan("tiny","0.02",3,"2020-02-01"),400);
  postApi(ENDPOINT,auth,plan("date","100",3,"2019-12-31"),400);
  postApi(ENDPOINT,auth,plan("count","100",121,"2020-02-01"),400);
  var body=plan("interval","100",3,"2020-02-01");body.put("intervalMonths",0);postApi(ENDPOINT,auth,body,400);
  body=plan("precision","1.001",3,"2020-02-01");postApi(ENDPOINT,auth,body,400);
  body=plan("far","100",3,"9999-12-31");postApi(ENDPOINT,auth,body,400);assertEquals(0,count("receivable"));
 }
 @Test void inactiveOrForeignCustomerCannotCreatePlan()throws Exception{
  var body=plan("customer","100",3,"2020-02-01");body.put("customerId",UUID.randomUUID());postApi(ENDPOINT,auth,body,404);
  body.put("customerId",customer);db.update("UPDATE customer SET active=FALSE WHERE id=?",customer);postApi(ENDPOINT,auth,body,404);assertEquals(0,count("receivable_plan"));
 }
 @Test void readOnlyPermissionCanReadExistingPlanButCannotPreviewOrCreate()throws Exception{
  var body=plan("permissions","100",3,"2020-02-01");var created=postApi(ENDPOINT,auth,body,201);
  db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='receivable:write'",tenant);
  getApi(ENDPOINT+"/"+created.get("id").asText(),auth,200);postApi(ENDPOINT,auth,body,403);postApi(ENDPOINT+"/preview",auth,body,403);postApi(ENDPOINT,null,body,401);
 }
 @Test void cancelledInstallmentIsNotRecreatedByRetry()throws Exception{
  var body=plan("cancelled","100",2,"2020-02-01");var created=postApi(ENDPOINT,auth,body,201);var first=created.get("installments").get(0);
  postApi(path(first)+"/cancel",auth,Map.of("version",0,"note","Documento cancelado"),200);
  assertEquals("CANCELLED",postApi(ENDPOINT,auth,body,201).get("installments").get(0).get("status").asText());assertEquals(2,count("receivable"));
 }
}
