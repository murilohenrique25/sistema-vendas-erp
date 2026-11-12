package br.com.erp;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class SaleCreditTest extends SaleFixture {
 String customer;LocalDate today;
 @BeforeEach void customerFixture()throws Exception{
  customer=postApi("/api/v1/customers",auth,Map.of("code","CREDIT","name","Cliente crediário","taxIdentifier","","contactName","","email","","phone","","address","","notes",""),201).get("id").asText();
  today=LocalDate.parse(getApi("/api/v1/receivables/calendar",auth,200).get("date").asText());
 }
 String policyPath(){return "/api/v1/customers/"+customer+"/credit-policy";}
 JsonNode policy(long version,String limit,boolean blocked,int expected)throws Exception{
  var result=http.perform(put(policyPath()).header("Authorization","Bearer "+auth).contentType("application/json").content(json.writeValueAsString(Map.of("version",version,"creditLimit",limit,"blocked",blocked)))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().is(expected)).andReturn();return json.readTree(result.getResponse().getContentAsString());
 }
 Map<String,Object> credit(String amount){return new HashMap<>(Map.of("customerId",customer,"amount",amount,"firstDueOn",today.plusMonths(1).toString(),"installmentCount",2,"intervalMonths",1));}
 Map<String,Object> command(String amount){var body=new HashMap<String,Object>();body.put("requestId",UUID.randomUUID());body.put("sessionId",session);body.put("items",List.of(item(product,"1","10.25")));body.put("credit",credit(amount));return body;}
 String salePath(JsonNode sale){return "/api/v1/pos/sales/"+sale.get("id").asText();}
 JsonNode plan(JsonNode sale)throws Exception{return getApi("/api/v1/receivables/plans/"+sale.get("credit").get("planId").asText(),auth,200);}
 JsonNode receipt(JsonNode sale,String amount,UUID key)throws Exception{
  var first=plan(sale).get("installments").get(0);return postApi("/api/v1/receivables/"+first.get("id").asText()+"/receipts",auth,Map.of("receiptId",key,"amount",amount,"receivedOn",today.toString(),"method","PIX","reference","Conferido externamente","note","Registro de recebimento real"),200);
 }
 Map<String,Object> cancellation(){return Map.of("requestId",UUID.randomUUID(),"version",0,"note","Cancelamento integral de teste","externalRefundsConfirmed",true);}
 @Test void defaultIsBlockedAndPolicyRequiresVersionAndAllBranches()throws Exception{
  assertTrue(getApi(policyPath(),auth,200).get("blocked").asBoolean());sell(command("10.25"),409);assertEquals(0,count("pos_sale"));
  var created=policy(0,"100",false,200);assertEquals(created,policy(0,"100.00",false,200));policy(0,"200",false,409);
  db.update("UPDATE app_user SET all_branches=FALSE WHERE tenant_id=?",tenant);getApi(policyPath(),auth,403);policy(1,"100",true,403);
 }
 @Test void fullCreditCreatesExactInstallmentsAndStockWithoutCashOrPayments()throws Exception{
  policy(0,"100",false,200);var sale=sell(command("10.25"),201);assertEquals("UNPAID",sale.get("financialStatus").asText());money("0",sale.get("tendered"));money("0",sale.get("changeAmount"));
  assertEquals(0,balance().signum());assertEquals(0,count("treasury_operation"));assertEquals(0,count("pos_sale_payment"));assertEquals(0,new BigDecimal("9").compareTo(quantity(product)));
  var installments=plan(sale).get("installments");money("5.13",installments.get(0).get("amount"));money("5.12",installments.get(1).get("amount"));assertEquals(customer,sale.get("credit").get("customerId").asText());
 }
 @Test void entryAndCreditOnlyPostImmediateAmountAndChange()throws Exception{
  policy(0,"100",false,200);var body=command("6.25");body.put("tendered","5");var sale=sell(body,201);
  money("1",sale.get("changeAmount"));money("5",sale.get("tendered"));money("4",sale.get("payments").get(0).get("amount"));assertEquals("PARTIALLY_PAID",sale.get("financialStatus").asText());assertEquals(0,new BigDecimal("4").compareTo(balance()));money("6.25",sale.get("credit").get("outstanding"));
 }
 @Test void replayDoesNotCreateMoreDebtAndRejectsChangedTerms()throws Exception{
  policy(0,"100",false,200);var body=command("10.25");var sale=sell(body,201);assertEquals(sale,sell(body,201));assertEquals(1,count("receivable_plan"));assertEquals(2,count("receivable"));
  var modified=credit("10.25");modified.put("installmentCount",3);body.put("credit",modified);sell(body,409);assertEquals(2,count("receivable"));
 }
 @Test void receiptsAndReversalsUpdateFinancialStateWithoutRepostingSale()throws Exception{
  policy(0,"100",false,200);var body=command("10.25");var sale=sell(body,201);UUID firstKey=UUID.randomUUID();receipt(sale,"5.13",firstKey);
  assertEquals("PARTIALLY_PAID",sell(body,201).get("financialStatus").asText());var second=plan(sale).get("installments").get(1);
  postApi("/api/v1/receivables/"+second.get("id").asText()+"/receipts",auth,Map.of("receiptId",UUID.randomUUID(),"amount","5.12","receivedOn",today.toString(),"method","CASH","reference","","note","Recebimento manual registrado"),200);
  assertEquals("PAID",getApi(salePath(sale),auth,200).get("financialStatus").asText());assertEquals(0,count("pos_sale_payment"));assertEquals(0,count("treasury_operation"));
  var first=plan(sale).get("installments").get(0);postApi("/api/v1/receivables/"+first.get("id").asText()+"/receipts/"+firstKey+"/reverse",auth,Map.of("requestId",UUID.randomUUID(),"note","Correção do recebimento"),200);
  assertEquals("PARTIALLY_PAID",getApi(salePath(sale),auth,200).get("financialStatus").asText());
 }
 @Test void limitCountsManualDebtAcrossBranchesAndOverdueBlocks()throws Exception{
  policy(0,"15",false,200);var otherBranch=postApi("/api/v1/branches",auth,Map.of("code","OTHER","name","Outra filial"),201).get("id").asText();postApi("/api/v1/receivables",auth,Map.of("requestId",UUID.randomUUID(),"branchId",otherBranch,"customerId",customer,"reference","MANUAL","document","","description","Dívida anterior manual","issuedOn",today.toString(),"dueOn",today.plusMonths(1).toString(),"amount","5"),201);
  sell(command("10.25"),409);policy(1,"100",false,200);
  db.update("UPDATE receivable SET issued_on=?,due_on=? WHERE tenant_id=?",today.minusDays(2),today.minusDays(1),tenant);sell(command("10.25"),409);assertEquals(0,count("pos_sale"));
 }
 @Test void competingSalesCannotExceedLimit()throws Exception{
  policy(0,"15",false,200);assertEquals(List.of(201,409),race(command("10.25"),command("10.25")));assertEquals(1,count("pos_sale"));assertEquals(2,count("receivable"));assertEquals(0,new BigDecimal("9").compareTo(quantity(product)));
 }
 @Test void insufficientStockRollsBackPlanAndAllTitles()throws Exception{
  policy(0,"1000",false,200);var body=command("112.75");body.put("items",List.of(item(product,"11","10.25")));sell(body,409);assertEquals(0,count("receivable_plan"));assertEquals(0,count("receivable"));assertEquals(0,count("pos_sale"));
 }
 @Test void invalidPaymentOrTermsRollBackCreatedPlan()throws Exception{
  policy(0,"100",false,200);var body=command("6.25");body.put("tendered","3");sell(body,400);assertEquals(0,count("receivable_plan"));
  body=command("10.25");body.put("tendered","1");sell(body,400);
  body=command("10.25");var c=credit("10.25");c.put("firstDueOn",today.minusDays(1).toString());body.put("credit",c);sell(body,400);assertEquals(0,count("receivable"));
  sell(command("11"),400);assertEquals(0,count("pos_sale"));
 }
 @Test void inactiveForeignOrMissingCustomerCannotOriginateDebt()throws Exception{
  policy(0,"100",false,200);db.update("UPDATE customer SET active=FALSE WHERE tenant_id=? AND id=?",tenant,UUID.fromString(customer));sell(command("10.25"),404);
  var body=command("10.25");var c=credit("10.25");c.put("customerId",UUID.randomUUID());body.put("credit",c);sell(body,404);assertEquals(0,count("receivable"));
 }
 @Test void creditPermissionIsIndependentAndScopedOperatorCanUseExistingPolicy()throws Exception{
  policy(0,"100",false,200);db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='pos:credit'",tenant);sell(command("10.25"),403);
  db.update("INSERT INTO user_permission(tenant_id,user_id,permission) SELECT tenant_id,id,'pos:credit' FROM app_user WHERE tenant_id=?",tenant);
  UUID user=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant);db.update("UPDATE app_user SET all_branches=FALSE WHERE id=?",user);db.update("INSERT INTO user_branch_access(tenant_id,user_id,branch_id) VALUES(?,?,?)",tenant,user,branch);
  getApi(policyPath(),auth,403);sell(command("10.25"),201);
 }
 @Test void fullCancellationCancelsDebtRestoresStockAndReplaysSafely()throws Exception{
  policy(0,"100",false,200);var body=command("10.25");var sale=sell(body,201);var cancel=cancellation();var result=postApi(salePath(sale)+"/cancel",auth,cancel,200);
  assertEquals("CANCELLED",result.get("financialStatus").asText());assertEquals(result,postApi(salePath(sale)+"/cancel",auth,cancel,200));assertEquals("CANCELLED",sell(body,201).get("status").asText());
  for(var title:plan(sale).get("installments"))assertEquals("CANCELLED",title.get("status").asText());assertEquals(0,new BigDecimal("10").compareTo(quantity(product)));assertEquals(0,count("treasury_operation"));
 }
 @Test void receivedDebtBlocksCancellationAndManualTitleCancellationIsForbidden()throws Exception{
  policy(0,"100",false,200);var sale=sell(command("10.25"),201);var first=plan(sale).get("installments").get(0);
  postApi("/api/v1/receivables/"+first.get("id").asText()+"/cancel",auth,Map.of("version",0,"note","Tentativa de cancelar dívida"),409);
  receipt(sale,"1",UUID.randomUUID());postApi(salePath(sale)+"/cancel",auth,cancellation(),409);assertEquals(0,new BigDecimal("9").compareTo(quantity(product)));assertEquals("OPEN",plan(sale).get("installments").get(1).get("status").asText());
 }
 @Test void returnWithoutDebtConfirmationCannotChangeStockOrDebt()throws Exception{
  policy(0,"100",false,200);var sale=sell(command("10.25"),201);postApi(salePath(sale)+"/returns",auth,Map.of("requestId",UUID.randomUUID(),"sessionId",session,"version",0,"note","Devolução parcial de teste","items",List.of(Map.of("productId",product,"quantity","1")),"refunds",List.of(),"externalRefundsConfirmed",false),400);
  assertEquals(0,count("pos_sale_return"));money("10.25",getApi(salePath(sale),auth,200).get("credit").get("outstanding"));
 }
 @Test void failureOnSecondTitleRollsBackEarlierCancellation()throws Exception{
  policy(0,"100",false,200);var sale=sell(command("10.25"),201);var second=plan(sale).get("installments").get(1);
  postApi("/api/v1/receivables/"+second.get("id").asText()+"/receipts",auth,Map.of("receiptId",UUID.randomUUID(),"amount","1","receivedOn",today.toString(),"method","CASH","reference","","note","Recebimento de segunda parcela"),200);
  postApi(salePath(sale)+"/cancel",auth,cancellation(),409);assertEquals("OPEN",plan(sale).get("installments").get(0).get("status").asText());assertEquals("COMPLETED",getApi(salePath(sale),auth,200).get("status").asText());
 }
 @Test void spentEntryPreventsCancellationAndRollsBackDebtAndStock()throws Exception{
  policy(0,"100",false,200);var body=command("6.25");body.put("tendered","4");var sale=sell(body,201);
  postApi("/api/v1/pos/cash-sessions/"+session+"/movements",auth,Map.of("requestId",UUID.randomUUID(),"kind","WITHDRAW","amount","4","note","Retirada autorizada de teste"),200);
  postApi(salePath(sale)+"/cancel",auth,cancellation(),409);for(var title:plan(sale).get("installments"))assertEquals("OPEN",title.get("status").asText());assertEquals(0,new BigDecimal("9").compareTo(quantity(product)));assertEquals("COMPLETED",getApi(salePath(sale),auth,200).get("status").asText());
 }
 @Test void bankEntryAndCreditDoNotCreateCashAndCancellationReversesBothSides()throws Exception{
  policy(0,"100",false,200);var bank=postApi("/api/v1/treasury/accounts",auth,Map.of("requestId",UUID.randomUUID(),"branchId",branch,"code","BANK","name","Banco","kind","BANK"),201).get("id").asText();
  var body=command("6.25");body.put("payments",List.of(Map.of("method","PIX","accountId",bank,"amount","4","reference","PROOF-CREDIT","confirmed",true)));var sale=sell(body,201);assertEquals(0,balance().signum());
  assertEquals(0,new BigDecimal("4").compareTo(db.queryForObject("SELECT balance FROM treasury_account WHERE tenant_id=? AND id=?",BigDecimal.class,tenant,UUID.fromString(bank))));
  postApi(salePath(sale)+"/cancel",auth,cancellation(),200);assertEquals(0,db.queryForObject("SELECT balance FROM treasury_account WHERE tenant_id=? AND id=?",BigDecimal.class,tenant,UUID.fromString(bank)).signum());for(var title:plan(sale).get("installments"))assertEquals("CANCELLED",title.get("status").asText());
 }
}
