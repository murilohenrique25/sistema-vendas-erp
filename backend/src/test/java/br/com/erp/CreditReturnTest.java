package br.com.erp;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

class CreditReturnTest extends SaleFixture {
 String customer;LocalDate today;JsonNode sale;
 @BeforeEach void loan()throws Exception{
  customer=postApi("/api/v1/customers",auth,Map.of("code","C","name","Cliente","taxIdentifier","","contactName","","email","","phone","","address","","notes",""),201).get("id").asText();today=LocalDate.parse(getApi("/api/v1/receivables/calendar",auth,200).get("date").asText());
  http.perform(put("/api/v1/customers/"+customer+"/credit-policy").header("Authorization","Bearer "+auth).contentType("application/json").content(json.writeValueAsString(Map.of("version",0,"creditLimit","1000","blocked",false)))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());sale=loanSale("10.25",null);
 }
 JsonNode loanSale(String credit,String entry)throws Exception{var input=new HashMap<String,Object>();input.put("requestId",UUID.randomUUID());input.put("sessionId",session);input.put("items",List.of(item(product,"1","10.25")));input.put("credit",Map.of("customerId",customer,"amount",credit,"firstDueOn",today.plusMonths(1).toString(),"installmentCount",2,"intervalMonths",1));if(entry!=null)input.put("tendered",entry);return sell(input,201);}
 String path(){return "/api/v1/pos/sales/"+sale.get("id").asText();}
 JsonNode current()throws Exception{return getApi(path(),auth,200);}
 JsonNode plan()throws Exception{return getApi("/api/v1/receivables/plans/"+sale.get("credit").get("planId").asText(),auth,200);}
 JsonNode title(int index)throws Exception{return plan().get("installments").get(index);}
 Map<String,Object> request(String quantity,String debt){return request(quantity,debt,currentVersion());}
 long currentVersion(){return db.queryForObject("SELECT version FROM pos_sale WHERE tenant_id=? AND id=?",Long.class,tenant,UUID.fromString(sale.get("id").asText()));}
 Map<String,Object> request(String quantity,String debt,long version){var result=new HashMap<String,Object>();result.put("requestId",UUID.randomUUID());result.put("sessionId",session);result.put("version",version);result.put("note","Devolução de mercadoria recebida");result.put("externalRefundsConfirmed",true);result.put("items",List.of(Map.of("productId",product,"quantity",quantity)));result.put("refunds",List.of());result.put("expectedDebtReduction",debt);return result;}
 JsonNode returned(Map<String,Object> input,int status)throws Exception{return postApi(path()+"/returns",auth,input,status);}
 JsonNode receive(int index,String amount,String bank)throws Exception{var input=new HashMap<String,Object>();input.put("receiptId",UUID.randomUUID());input.put("amount",amount);input.put("receivedOn",today.toString());input.put("method",bank==null?"CASH":"PIX");input.put("reference","Registro de recebimento");input.put("note","Recebimento de parcela confirmado");if(bank!=null)input.put("treasuryAccountId",bank);return postApi("/api/v1/receivables/"+title(index).get("id").asText()+"/receipts",auth,input,200);}
 String bank(String code)throws Exception{return postApi("/api/v1/treasury/accounts",auth,Map.of("requestId",UUID.randomUUID(),"branchId",branch,"code",code,"name","Banco "+code,"kind","BANK"),201).get("id").asText();}
 void supply(String value)throws Exception{postApi("/api/v1/pos/cash-sessions/"+session+"/movements",auth,Map.of("requestId",UUID.randomUUID(),"kind","SUPPLY","amount",value,"note","Conferência de suprimento"),200);}
 Map<String,Object> refund(JsonNode receipt,String value,String method,String bank,String reference){var r=new HashMap<String,Object>();r.put("receiptId",receipt.get("id").asText());r.put("amount",value);r.put("method",method);r.put("reference",reference);if(bank!=null)r.put("accountId",bank);return r;}
 @Test void unpaidPartialReturnReducesLatestDebtFirstWithoutMoneyAndKeepsOriginalAmounts()throws Exception{
  var result=returned(request("0.5","5.13"),201);money("5.13",result.get("debtReduction"));assertEquals(2,result.get("debtCredits").size());assertEquals(0,result.get("refunds").size());
  money("5.13",title(0).get("amount"));money("0.01",title(0).get("creditedAmount"));money("5.12",title(0).get("outstanding"));assertEquals("CREDITED",title(1).get("status").asText());money("5.12",current().get("credit").get("outstanding"));assertEquals("UNPAID",current().get("financialStatus").asText());assertEquals(0,count("treasury_operation"));
 }
 @Test void fullReturnClosesAllDebtWithoutPretendingToRefundCash()throws Exception{
  returned(request("1","10.25"),201);assertEquals("RETURNED",current().get("commercialStatus").asText());assertEquals("SETTLED_BY_RETURN",current().get("financialStatus").asText());money("0",current().get("refundedAmount"));for(var t:plan().get("installments"))assertEquals("CREDITED",t.get("status").asText());assertEquals(0,new BigDecimal("10").compareTo(quantity(product)));
 }
 @Test void cumulativeRoundingAndZeroCentReturnPreserveTotals()throws Exception{
  var tiny=returned(request("0.000001","0"),201);money("0",tiny.get("total"));assertEquals(0,tiny.get("debtCredits").size());returned(request("0.999999","10.25"),201);money("10.25",current().get("credit").get("creditedAmount"));
 }
 @Test void returnCanUseNewShiftAndDoesNotChangeOriginalClosedDrawer()throws Exception{
  var original=getApi("/api/v1/pos/cash-sessions/"+session,auth,200);postApi("/api/v1/pos/cash-sessions/"+session+"/close",auth,Map.of("requestId",UUID.randomUUID(),"version",original.get("version").asLong(),"countedAmount","0","note","Fechamento conferido de teste"),200);
  var closed=getApi("/api/v1/pos/cash-sessions/"+session,auth,200);session=postApi("/api/v1/pos/cash-sessions",auth,Map.of("requestId",UUID.randomUUID(),"terminalId",terminal,"openingAmount","0","note","Novo turno conferido"),201).get("id").asText();returned(request("1","10.25"),201);assertEquals(closed,getApi("/api/v1/pos/cash-sessions/"+sale.get("sessionId").asText(),auth,200));
 }
 @Test void originalEntryIsRefundedOnlyAfterAllOutstandingDebtIsReduced()throws Exception{
  sale=loanSale("6.25","4");var command=request("1","6.25");command.put("refunds",List.of(Map.of("paymentId",sale.get("payments").get(0).get("id").asText(),"amount","4","reference","")));var result=returned(command,201);money("6.25",result.get("debtReduction"));money("4",current().get("refundedAmount"));assertEquals(0,balance().signum());
 }
 @Test void paidCashReceiptRefundReducesNetReceivedAndPreservesReceiptHistory()throws Exception{
  var receipt=receive(0,"5.13",null);supply("5.13");var command=request("1","5.12");command.put("receiptRefunds",List.of(refund(receipt,"5.13","CASH",null,"")));var result=returned(command,201);
  assertEquals(1,result.get("receiptRefunds").size());money("5.13",current().get("refundedAmount"));money("0",title(0).get("receivedAmount"));money("5.13",title(0).get("creditedAmount"));assertEquals(0,balance().signum());
  var history=getApi("/api/v1/receivables/"+title(0).get("id").asText()+"/receipts",auth,200).get("items").get(0);assertEquals("POSTED",history.get("status").asText());money("5.13",history.get("amount"));money("5.13",history.get("refundedAmount"));
  var adjustment=getApi("/api/v1/receivables/"+title(0).get("id").asText()+"/adjustments",auth,200).get("items").get(0);assertEquals("RECEIPT_REFUND",adjustment.get("kind").asText());money("5.13",adjustment.get("amount"));assertEquals(result.get("id"),adjustment.get("returnId"));
  postApi("/api/v1/receivables/"+title(0).get("id").asText()+"/receipts/"+receipt.get("id").asText()+"/reverse",auth,Map.of("requestId",UUID.randomUUID(),"note","Tentativa de estorno duplicado"),409);
 }
 @Test void paidBankReceiptMustRefundOriginalBankAndRequiresProof()throws Exception{
  String bank=bank("B");var receipt=receive(0,"5.13",bank);var command=request("1","5.12");command.put("receiptRefunds",List.of(refund(receipt,"5.13","CASH",null,"")));returned(command,400);
  command.put("receiptRefunds",List.of(refund(receipt,"5.13","PIX",bank,"")));returned(command,400);
  command.put("receiptRefunds",List.of(refund(receipt,"5.13","PIX",bank("OTHER"),"REFUND")));returned(command,400);
  command.put("receiptRefunds",List.of(refund(receipt,"5.13","PIX",bank,"REFUND")));command.put("externalRefundsConfirmed",false);returned(command,400);command.put("externalRefundsConfirmed",true);
  command.put("receiptRefunds",List.of(refund(receipt,"5.13","PIX",bank,"REFUND")));returned(command,201);assertEquals(0,db.queryForObject("SELECT balance FROM treasury_account WHERE tenant_id=? AND id=?",BigDecimal.class,tenant,UUID.fromString(bank)).signum());
 }
 @Test void repeatedPartialRefundUsesOnlyRemainingReceiptAmount()throws Exception{
  var receipt=receive(0,"5.13",null);supply("5.13");var first=request("0.5","5.12");first.put("receiptRefunds",List.of(refund(receipt,"0.01","CASH",null,"")));returned(first,201);money("5.12",title(0).get("receivedAmount"));
  var last=request("0.5","0");last.put("receiptRefunds",List.of(refund(receipt,"5.13","CASH",null,"")));returned(last,409);last.put("receiptRefunds",List.of(refund(receipt,"5.12","CASH",null,"")));returned(last,201);money("5.13",current().get("refundedAmount"));assertEquals(0,balance().signum());
 }
 @Test void staleDebtConfirmationAfterReceiptCannotChangeStockOrLedger()throws Exception{
  var command=request("1","10.25");receive(0,"1",null);returned(command,409);assertEquals(0,count("pos_sale_return"));assertEquals(0,new BigDecimal("9").compareTo(quantity(product)));money("0",title(0).get("creditedAmount"));
 }
 @Test void insufficientRefundFundsRollBackAllStockAndDebtChanges()throws Exception{
  var receipt=receive(0,"5.13",null);var command=request("1","5.12");command.put("receiptRefunds",List.of(refund(receipt,"5.13","CASH",null,"")));returned(command,409);assertEquals(0,count("pos_sale_return"));money("5.13",title(0).get("receivedAmount"));money("0",title(1).get("creditedAmount"));assertEquals(0,new BigDecimal("9").compareTo(quantity(product)));
 }
 @Test void receiptFromAnotherSaleIsNotARefundSource()throws Exception{
  var first=sale;sale=loanSale("10.25",null);var receipt=receive(0,"1",null);sale=first;var command=request("1","10.25");command.put("receiptRefunds",List.of(refund(receipt,"1","CASH",null,"")));returned(command,404);assertEquals(0,count("pos_sale_return"));
 }
 @Test void idempotentRetryAfterNewReceiptDoesNotReapplyDebtCredit()throws Exception{
  var command=request("0.5","5.13");var first=returned(command,201);receive(0,"1",null);assertEquals(first,returned(command,201));assertEquals(1,count("pos_sale_return"));money("5.13",current().get("credit").get("creditedAmount"));
  command.put("expectedDebtReduction","5.12");returned(command,409);
 }
 @Test void permissionAndBranchScopeProtectFinancialWriteoff()throws Exception{
  db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='receivable:credit'",tenant);returned(request("1","10.25"),403);db.update("INSERT INTO user_permission(tenant_id,user_id,permission) SELECT tenant_id,id,'receivable:credit' FROM app_user WHERE tenant_id=?",tenant);
  db.update("UPDATE app_user SET all_branches=FALSE WHERE tenant_id=?",tenant);returned(request("1","10.25"),404);assertEquals(0,count("pos_sale_return"));
 }
 @Test void receivedAmountCannotExceedTheNewOutstandingAndReversalKeepsCredits()throws Exception{
  returned(request("0.5","5.13"),201);var t=title(0);postApi("/api/v1/receivables/"+t.get("id").asText()+"/receipts",auth,Map.of("receiptId",UUID.randomUUID(),"amount","5.13","receivedOn",today.toString(),"method","CASH","reference","","note","Valor maior que dívida atual"),409);
  var r=receive(0,"5.12",null);assertEquals("CREDITED",title(0).get("status").asText());postApi("/api/v1/receivables/"+t.get("id").asText()+"/receipts/"+r.get("id").asText()+"/reverse",auth,Map.of("requestId",UUID.randomUUID(),"note","Correção de registro não reembolsado"),200);money("5.12",title(0).get("outstanding"));money("0.01",title(0).get("creditedAmount"));
 }
 @Test void cashFlowAndCreditLimitUseOnlyDebtAfterAdjustments()throws Exception{
  returned(request("0.5","5.13"),201);var report=getApi("/api/v1/cash-flow?from="+today+"&to="+today.plusMonths(2),auth,200);money("5.12",report.get("summary").get("pendingReceivables"));
  http.perform(put("/api/v1/customers/"+customer+"/credit-policy").header("Authorization","Bearer "+auth).contentType("application/json").content(json.writeValueAsString(Map.of("version",1,"creditLimit","15.37","blocked",false)))).andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.status().isOk());loanSale("10.25",null);
 }
 @Test void repaymentSourcesAreScopedAndReflectRefundedBalance()throws Exception{
  var receipt=receive(0,"5.13",null);var sources=getApi(path()+"/returns/credit-receipts?size=1",auth,200);assertEquals(1,sources.get("total").asInt());money("5.13",sources.get("items").get(0).get("refundable"));
  supply("0.01");var command=request("0.5","5.12");command.put("receiptRefunds",List.of(refund(receipt,"0.01","CASH",null,"")));returned(command,201);money("5.12",getApi(path()+"/returns/credit-receipts",auth,200).get("items").get(0).get("refundable"));
 }
 List<Integer> raceReturns(Object one,Object two)throws Exception{
  String endpoint=path()+"/returns";try(var pool=Executors.newFixedThreadPool(2)){var gate=new CountDownLatch(1);var a=pool.submit(()->{gate.await();return status(endpoint,one);});var b=pool.submit(()->{gate.await();return status(endpoint,two);});gate.countDown();var result=new ArrayList<>(List.of(a.get(40,TimeUnit.SECONDS),b.get(40,TimeUnit.SECONDS)));Collections.sort(result);return result;}
 }
 @Test void concurrentRetryCannotDuplicateStockOrDebtCredit()throws Exception{
  var command=request("0.5","5.13");assertEquals(List.of(201,201),raceReturns(command,command));assertEquals(1,count("pos_sale_return"));money("5.13",current().get("credit").get("creditedAmount"));assertEquals(0,new BigDecimal("9.5").compareTo(quantity(product)));
 }
 @Test void competingVersionsCannotReturnSameQuantityTwice()throws Exception{
  assertEquals(List.of(201,409),raceReturns(request("0.5","5.13"),request("0.5","5.13")));assertEquals(1,count("pos_sale_return"));money("5.13",current().get("credit").get("creditedAmount"));
 }
 @Test void bankProofCannotBeReusedBetweenInitialPaymentAndReceiptRefunds()throws Exception{
  String bank=bank("PROOFS");var regular=sell(Map.of("requestId",UUID.randomUUID(),"sessionId",session,"items",List.of(item(product,"1","10.25")),"payments",List.of(Map.of("method","PIX","accountId",bank,"amount","10.25","reference","PAYMENT","confirmed",true))),201);
  var legacy=request("1","0");legacy.remove("expectedDebtReduction");legacy.put("refunds",List.of(Map.of("paymentId",regular.get("payments").get(0).get("id").asText(),"amount","10.25","reference","DUPLICATE")));postApi("/api/v1/pos/sales/"+regular.get("id").asText()+"/returns",auth,legacy,201);
  var receipt=receive(0,"5.13",bank);var command=request("1","5.12");command.put("receiptRefunds",List.of(refund(receipt,"5.13","PIX",bank,"DUPLICATE")));returned(command,409);money("0",title(0).get("creditedAmount"));
 }
 @Test void paymentAndReceiptMayShareUuidWithoutBeingTheSameFinancialSource()throws Exception{
  sale=loanSale("6.25","4");String shared=sale.get("payments").get(0).get("id").asText();
  var receipt=postApi("/api/v1/receivables/"+title(0).get("id").asText()+"/receipts",auth,Map.of("receiptId",shared,"amount","3.13","receivedOn",today.toString(),"method","CASH","reference","","note","Recebimento com chave própria"),200);supply("3.13");
  var command=request("1","3.12");command.put("refunds",List.of(Map.of("paymentId",shared,"amount","4","reference","")));command.put("receiptRefunds",List.of(refund(receipt,"3.13","CASH",null,"")));var result=returned(command,201);
  assertEquals(result.get("refunds").get(0).get("paymentId"),result.get("receiptRefunds").get(0).get("receiptId"));assertEquals(0,balance().signum());assertEquals("SETTLED_BY_RETURN",current().get("financialStatus").asText());
 }
}
