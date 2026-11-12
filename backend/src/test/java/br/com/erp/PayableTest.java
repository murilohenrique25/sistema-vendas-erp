package br.com.erp;
import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PayableTest extends ApiTestSupport {
    @Test void concurrentPaymentKeyAcrossTitlesCannotPayBoth()throws Exception{
        var first=title("FIRST");var second=title("SECOND");UUID key=UUID.randomUUID();
        assertEquals(List.of(200,409),concurrent(path(first)+"/payments",payment(key,"100"),path(second)+"/payments",payment(key,"100")));
        amount("100",json.valueToTree(db.queryForObject("SELECT SUM(paid_amount) FROM payable WHERE tenant_id=?",BigDecimal.class,tenant)));assertEquals(1,count("payable_payment"));
    }
    @Test void paymentInAnotherBranchCannotBeProbedOrReversedViaAccessibleTitle()throws Exception{
        var accessible=title("ACCESS");UUID otherBranch=UUID.fromString(postApi("/api/v1/branches",auth,Map.of("code","OTHER","name","Outra"),201).get("id").asText());
        var otherInput=input(UUID.randomUUID(),"OTHER","100");otherInput.put("branchId",otherBranch);var other=postApi("/api/v1/payables",auth,otherInput,201);UUID key=UUID.randomUUID();pay(other,key,"50");
        UUID user=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant);db.update("UPDATE app_user SET all_branches=FALSE WHERE id=?",user);db.update("INSERT INTO user_branch_access(tenant_id,user_id,branch_id) VALUES(?,?,?)",tenant,user,branch);
        var hidden=postApi(path(accessible)+"/payments/"+key+"/reverse",auth,reverse(UUID.randomUUID()),404);
        var missing=postApi(path(accessible)+"/payments/"+UUID.randomUUID()+"/reverse",auth,reverse(UUID.randomUUID()),404);
        assertEquals(missing.get("detail").asText(),hidden.get("detail").asText());amount("50",dbAmount(other));
    }
    String auth;UUID branch,supplier;
    @BeforeEach void financeFixture()throws Exception{
        auth=token(company);branch=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,tenant);
        supplier=UUID.fromString(postApi("/api/v1/suppliers",auth,Map.of("code","SUP","name","Fornecedor","taxIdentifier","","contactName","","email","","phone","","address","","notes",""),201).get("id").asText());
    }
    Map<String,Object> input(UUID key,String reference,String amount){var result=new HashMap<String,Object>();result.put("requestId",key);result.put("branchId",branch);result.put("supplierId",supplier);result.put("purchaseReceiptId",null);result.put("reference",reference);result.put("document","Documento teste");result.put("description","Obrigação declarada de teste");result.put("issuedOn","2020-01-01");result.put("dueOn","2020-02-01");result.put("amount",amount);return result;}
    JsonNode title(String reference)throws Exception{return postApi("/api/v1/payables",auth,input(UUID.randomUUID(),reference,"100.00"),201);}
    String path(JsonNode title){return "/api/v1/payables/"+title.get("id").asText();}
    Map<String,Object> payment(UUID key,String amount){return Map.of("paymentId",key,"amount",amount,"paidOn","2020-02-01","method","BANK_TRANSFER","reference","Comprovante teste","note","Pagamento realizado informado");}
    JsonNode pay(JsonNode title,UUID key,String amount)throws Exception{return postApi(path(title)+"/payments",auth,payment(key,amount),200);}
    Map<String,Object> reverse(UUID key){return Map.of("requestId",key,"note","Correção do registro de pagamento");}
    long count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=?",Long.class,tenant);}
    void amount(String expected,JsonNode value){assertEquals(0,new BigDecimal(expected).compareTo(new BigDecimal(value.asText())));}
    List<Integer> concurrent(String firstPath,Object firstBody,String secondPath,Object secondBody)throws Exception{
        var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){
            var first=pool.submit(()->{gate.await();return http.perform(post(firstPath).header("Authorization","Bearer "+auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(firstBody))).andReturn().getResponse().getStatus();});
            var second=pool.submit(()->{gate.await();return http.perform(post(secondPath).header("Authorization","Bearer "+auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(secondBody))).andReturn().getResponse().getStatus();});gate.countDown();var result=new ArrayList<>(List.of(first.get(40,TimeUnit.SECONDS),second.get(40,TimeUnit.SECONDS)));Collections.sort(result);return result;
        }
    }
    @Test void createIdempotenceUniquenessSnapshotAndOverdueFilters()throws Exception{
        UUID key=UUID.randomUUID();var first=postApi("/api/v1/payables",auth,input(key,"bill-1","100"),201);var replay=postApi("/api/v1/payables",auth,input(key,"BILL-1","100.00"),201);
        assertEquals(first,replay);assertEquals("BILL-1",first.get("reference").asText());assertTrue(first.get("overdue").asBoolean());
        postApi("/api/v1/payables",auth,input(key,"BILL-1","101.00"),409);postApi("/api/v1/payables",auth,input(UUID.randomUUID(),"bill-1","100.00"),409);
        db.update("UPDATE supplier SET name='Atualizado',active=FALSE WHERE id=?",supplier);assertEquals("Fornecedor",getApi(path(first),auth,200).get("supplierName").asText());title("BILL-2");
        String today=getApi("/api/v1/payables/calendar",auth,200).get("date").asText();var dueToday=input(UUID.randomUUID(),"TODAY","50");dueToday.put("dueOn",today);postApi("/api/v1/payables",auth,dueToday,201);
        assertEquals(2,getApi("/api/v1/payables?overdue=true",auth,200).get("total").asInt());assertEquals(3,count("payable"));
    }
    @Test void partialAndFullPaymentCannotExceedOutstandingAndReplayIsSafe()throws Exception{
        var title=title("BILL");UUID firstKey=UUID.randomUUID();var first=pay(title,firstKey,"30.00");var partial=getApi(path(title),auth,200);assertEquals("PARTIAL",partial.get("status").asText());amount("70",partial.get("outstanding"));
        postApi(path(title)+"/payments",auth,payment(UUID.randomUUID(),"71"),409);pay(title,UUID.randomUUID(),"70");
        var settled=getApi(path(title),auth,200);assertEquals("PAID",settled.get("status").asText());amount("0",settled.get("outstanding"));assertFalse(settled.get("overdue").asBoolean());
        assertEquals(first,pay(title,firstKey,"30.0"));postApi(path(title)+"/payments",auth,payment(firstKey,"31"),409);postApi(path(title)+"/payments",auth,payment(UUID.randomUUID(),"0.01"),409);assertEquals(2,count("payable_payment"));
        assertEquals(2,getApi(path(title)+"/payments",auth,200).get("total").asInt());
    }
    @Test void reversalPreservesHistoryAndDoesNotRepostOnOriginalPaymentRetry()throws Exception{
        var title=title("BILL");UUID paymentKey=UUID.randomUUID();pay(title,paymentKey,"100");UUID reversal=UUID.randomUUID();String endpoint=path(title)+"/payments/"+paymentKey+"/reverse";
        var reversed=postApi(endpoint,auth,reverse(reversal),200);assertEquals("REVERSED",reversed.get("status").asText());assertEquals(reversed,postApi(endpoint,auth,reverse(reversal),200));
        assertEquals("REVERSED",pay(title,paymentKey,"100.00").get("status").asText());var open=getApi(path(title),auth,200);assertEquals("OPEN",open.get("status").asText());amount("0",open.get("paidAmount"));amount("100",open.get("outstanding"));
        postApi(endpoint,auth,reverse(UUID.randomUUID()),409);assertEquals(1,count("payable_payment"));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PAYABLE_PAYMENT_REVERSED'",Integer.class,tenant));
    }
    @Test void reusedReversalKeyRollsBackWithoutChangingSecondPayment()throws Exception{
        var title=title("BILL");UUID first=UUID.randomUUID(),second=UUID.randomUUID(),reversal=UUID.randomUUID();pay(title,first,"40");pay(title,second,"60");
        postApi(path(title)+"/payments/"+first+"/reverse",auth,reverse(reversal),200);postApi(path(title)+"/payments/"+second+"/reverse",auth,reverse(reversal),409);
        amount("60",getApi(path(title),auth,200).get("paidAmount"));assertEquals("POSTED",db.queryForObject("SELECT status FROM payable_payment WHERE tenant_id=? AND id=?",String.class,tenant,second));
    }
    @Test void cancellationRequiresZeroPaidAndKeepsReversedRecords()throws Exception{
        var title=title("BILL");UUID key=UUID.randomUUID();pay(title,key,"20");String endpoint=path(title)+"/cancel";
        postApi(endpoint,auth,Map.of("version",1,"note","Documento cancelado"),409);postApi(path(title)+"/payments/"+key+"/reverse",auth,reverse(UUID.randomUUID()),200);
        postApi(endpoint,auth,Map.of("version",0,"note","Documento cancelado"),409);postApi(endpoint,auth,Map.of("version",2,"note","Documento cancelado"),200);postApi(endpoint,auth,Map.of("version",2,"note","Documento cancelado"),200);
        var cancelled=getApi(path(title),auth,200);assertEquals("CANCELLED",cancelled.get("status").asText());amount("0",cancelled.get("outstanding"));amount("100",cancelled.get("amount"));assertFalse(cancelled.get("overdue").asBoolean());
        postApi(path(title)+"/payments",auth,payment(UUID.randomUUID(),"1"),409);assertEquals(1,getApi(path(title)+"/payments",auth,200).get("total").asInt());
    }
    @Test void tenantScopeAndPermissionsProtectMoneyAndHistory()throws Exception{
        var title=title("BILL");UUID payment=UUID.randomUUID();pay(title,payment,"20");
        String otherCompany="other-"+UUID.randomUUID();provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);String other=token(otherCompany);
        getApi(path(title),other,404);getApi(path(title)+"/payments",other,404);postApi(path(title)+"/payments",other,payment(UUID.randomUUID(),"1"),404);
        UUID second=UUID.fromString(postApi("/api/v1/branches",auth,Map.of("code","OTHER","name","Outra"),201).get("id").asText());UUID user=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant);db.update("UPDATE app_user SET all_branches=FALSE WHERE id=?",user);db.update("INSERT INTO user_branch_access(tenant_id,user_id,branch_id) VALUES(?,?,?)",tenant,user,second);
        assertEquals(0,getApi("/api/v1/payables?overdue=true",auth,200).get("total").asInt());getApi(path(title),auth,404);postApi(path(title)+"/payments/"+payment+"/reverse",auth,reverse(UUID.randomUUID()),404);postApi(path(title)+"/cancel",auth,Map.of("version",1,"note","Negado por filial"),404);amount("20",dbAmount(title));
    }
    JsonNode dbAmount(JsonNode title){return json.valueToTree(db.queryForObject("SELECT paid_amount FROM payable WHERE id=?",BigDecimal.class,UUID.fromString(title.get("id").asText())));}
    @Test void granularPermissionsAndValidationRejectInvalidPayments()throws Exception{
        var title=title("BILL");db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='payable:pay'",tenant);postApi(path(title)+"/payments",auth,payment(UUID.randomUUID(),"1"),403);
        db.update("INSERT INTO user_permission(tenant_id,user_id,permission) SELECT tenant_id,id,'payable:pay' FROM app_user WHERE tenant_id=?",tenant);
        postApi(path(title)+"/payments",auth,payment(UUID.randomUUID(),"0"),400);var future=new HashMap<String,Object>(payment(UUID.randomUUID(),"1"));future.put("paidOn",LocalDate.parse(getApi("/api/v1/payables/calendar",auth,200).get("date").asText()).plusDays(1).toString());postApi(path(title)+"/payments",auth,future,400);
        var badDates=input(UUID.randomUUID(),"BAD","1");badDates.put("dueOn","2019-01-01");postApi("/api/v1/payables",auth,badDates,400);
        postApi("/api/v1/payables",auth,input(UUID.randomUUID(),"BAD","1.001"),400);http.perform(get("/api/v1/payables?status=UNKNOWN").header("Authorization","Bearer "+auth)).andExpect(status().isBadRequest());http.perform(get("/api/v1/payables")).andExpect(status().isUnauthorized());assertEquals(0,count("payable_payment"));
    }
    @Test void optionalReceiptLinkMustMatchSupplierAndBranch()throws Exception{
        UUID warehouse=UUID.fromString(postApi("/api/v1/inventory/warehouses",auth,Map.of("branchId",branch,"code","W","name","Depósito"),201).get("id").asText());UUID product=UUID.fromString(postApi("/api/v1/products",auth,Map.of("sku","P","name","Produto","description","","unit","UN","salePrice","10","barcodes",List.of()),201).get("id").asText());
        var order=postApi("/api/v1/purchase-orders",auth,Map.of("requestId",UUID.randomUUID(),"branchId",branch,"supplierId",supplier,"notes","","items",List.of(Map.of("productId",product,"quantity","1","unitCost","10"))),201);
        UUID receiptId=UUID.randomUUID();postApi("/api/v1/purchase-orders/"+order.get("header").get("id").asText()+"/receipts",auth,Map.of("receiptId",receiptId,"warehouseId",warehouse,"note","Conferência de teste","items",List.of(Map.of("orderLineId",order.get("lines").get(0).get("id").asText(),"quantity","1"))),200);
        var linked=input(UUID.randomUUID(),"LINK","12.00");linked.put("purchaseReceiptId",receiptId);var title=postApi("/api/v1/payables",auth,linked,201);assertEquals(receiptId.toString(),title.get("purchaseReceiptId").asText());amount("12",title.get("amount"));
        UUID second=UUID.fromString(postApi("/api/v1/branches",auth,Map.of("code","OTHER","name","Outra"),201).get("id").asText());var bad=input(UUID.randomUUID(),"BAD","12");bad.put("branchId",second);bad.put("purchaseReceiptId",receiptId);postApi("/api/v1/payables",auth,bad,404);assertEquals(1,count("payable"));
    }
    @Test void concurrentCreationAndPaymentRetriesHaveSingleEffects()throws Exception{
        var command=input(UUID.randomUUID(),"BILL","100");assertEquals(List.of(201,201),concurrent("/api/v1/payables",command,"/api/v1/payables",command));assertEquals(1,count("payable"));var title=getApi("/api/v1/payables",auth,200).get("items").get(0);var pay=payment(UUID.randomUUID(),"100");
        assertEquals(List.of(200,200),concurrent(path(title)+"/payments",pay,path(title)+"/payments",pay));assertEquals(1,count("payable_payment"));amount("100",getApi(path(title),auth,200).get("paidAmount"));
    }
    @Test void concurrentPaymentsCannotExceedRemainingBalance()throws Exception{
        var title=title("BILL");pay(title,UUID.randomUUID(),"40");assertEquals(List.of(200,409),concurrent(path(title)+"/payments",payment(UUID.randomUUID(),"60"),path(title)+"/payments",payment(UUID.randomUUID(),"60")));amount("100",getApi(path(title),auth,200).get("paidAmount"));assertEquals(2,count("payable_payment"));
    }
    @Test void concurrentReversalAndPaymentProduceConsistentBalance()throws Exception{
        var title=title("BILL");UUID key=UUID.randomUUID();pay(title,key,"40");assertEquals(List.of(200,200),concurrent(path(title)+"/payments/"+key+"/reverse",reverse(UUID.randomUUID()),path(title)+"/payments",payment(UUID.randomUUID(),"60")));amount("60",getApi(path(title),auth,200).get("paidAmount"));assertEquals("PARTIAL",getApi(path(title),auth,200).get("status").asText());
    }
    @Test void paymentRacingCancellationHasOneWinner()throws Exception{
        var title=title("BILL");assertEquals(List.of(200,409),concurrent(path(title)+"/payments",payment(UUID.randomUUID(),"100"),path(title)+"/cancel",Map.of("version",0,"note","Cancelamento concorrente")));var result=getApi(path(title),auth,200);
        if(result.get("status").asText().equals("PAID")){amount("100",result.get("paidAmount"));assertEquals(1,count("payable_payment"));}else{assertEquals("CANCELLED",result.get("status").asText());amount("0",result.get("paidAmount"));assertEquals(0,count("payable_payment"));}
    }
}
