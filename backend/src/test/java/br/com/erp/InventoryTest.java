package br.com.erp;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import org.springframework.http.MediaType;

class InventoryTest extends ApiTestSupport {
    String auth;
    UUID product,warehouse,branch;
    @BeforeEach void inventory() throws Exception {
        auth=token(company);
        product=UUID.fromString(postApi("/api/v1/products",auth,Map.of("sku","P1","name","Produto de estoque","description","","unit","KG","salePrice","20.00","barcodes",List.of()),201).get("id").asText());
        branch=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,tenant);
        warehouse=createWarehouse("PRINCIPAL");
    }
    UUID createWarehouse(String code) throws Exception { return UUID.fromString(postApi("/api/v1/inventory/warehouses",auth,Map.of("branchId",branch,"code",code,"name","Depósito "+code),201).get("id").asText()); }
    Map<String,Object> movement(UUID operation,UUID location,String type,String quantity,String cost) {
        var values=new HashMap<String,Object>(); values.put("operationId",operation); values.put("warehouseId",location); values.put("productId",product); values.put("type",type); values.put("quantity",quantity); values.put("unitCost",cost); values.put("reason","Operação de teste"); return values;
    }
    JsonNode receipt(String quantity,String cost) throws Exception { return postApi("/api/v1/inventory/movements",auth,movement(UUID.randomUUID(),warehouse,"RECEIPT",quantity,cost),200); }
    BigDecimal balance(UUID location) { var values=db.query("SELECT quantity FROM stock_balance WHERE tenant_id=? AND warehouse_id=? AND product_id=?",(rs,i)->rs.getBigDecimal(1),tenant,location,product); return values.isEmpty() ? BigDecimal.ZERO : values.getFirst(); }
    long count(String table) { return db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=?",Long.class,tenant); }
    JsonNode loss(String quantity) throws Exception { return postApi("/api/v1/losses",auth,Map.of("requestId",UUID.randomUUID(),"warehouseId",warehouse,"productId",product,"quantity",quantity,"reason","DAMAGE","details","Avaria identificada na conferência"),200); }
    void equal(String expected,BigDecimal actual) { assertEquals(0,new BigDecimal(expected).compareTo(actual)); }
    List<Integer> concurrent(String path,Object body1,Object body2) throws Exception {
        var gate=new CountDownLatch(1);
        try (var executor=Executors.newFixedThreadPool(2)) {
            Callable<Integer> first=()-> { gate.await(); return http.perform(post(path).header("Authorization","Bearer "+auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body1))).andReturn().getResponse().getStatus(); };
            Callable<Integer> second=()-> { gate.await(); return http.perform(post(path).header("Authorization","Bearer "+auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body2))).andReturn().getResponse().getStatus(); };
            var a=executor.submit(first); var b=executor.submit(second); gate.countDown();
            var statuses=new ArrayList<>(List.of(a.get(30,TimeUnit.SECONDS),b.get(30,TimeUnit.SECONDS))); Collections.sort(statuses); return statuses;
        }
    }
    @Test void weightedAverageAndLedgerArePersisted() throws Exception {
        receipt("3","10"); receipt("1","14");
        equal("4",balance(warehouse));
        var balances=getApi("/api/v1/inventory/warehouses/"+warehouse+"/balances",auth,200);
        assertEquals(1,balances.get("total").asInt()); equal("11",new BigDecimal(balances.get("items").get(0).get("averageCost").asText()));
        var history=getApi("/api/v1/inventory/warehouses/"+warehouse+"/movements",auth,200); assertEquals(2,history.get("total").asInt());
        assertFalse(history.get("items").get(0).get("actorId").isNull());
    }
    @Test void retryReturnsOriginalResponseAndScaleEquivalentPayloadIsAccepted() throws Exception {
        UUID id=UUID.randomUUID();
        var first=postApi("/api/v1/inventory/movements",auth,movement(id,warehouse,"RECEIPT","2","3"),200);
        var retry=postApi("/api/v1/inventory/movements",auth,movement(id,warehouse,"RECEIPT","2.000000","3.0"),200);
        assertEquals(first,retry); assertEquals(1,count("stock_movement")); equal("2",balance(warehouse));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='STOCK_MOVED'",Long.class,tenant));
        postApi("/api/v1/inventory/movements",auth,movement(id,warehouse,"RECEIPT","3","3"),409); equal("2",balance(warehouse));
    }
    @Test void concurrentLastUnitAllowsOneWithdrawalAndRollsBackLoser() throws Exception {
        receipt("1","8");
        assertEquals(List.of(200,409),concurrent("/api/v1/inventory/movements",movement(UUID.randomUUID(),warehouse,"ADJUSTMENT_OUT","1",null),movement(UUID.randomUUID(),warehouse,"ADJUSTMENT_OUT","1",null)));
        equal("0",balance(warehouse)); assertEquals(2,count("stock_movement")); assertEquals(2,count("stock_operation"));
    }
    @Test void concurrentRetryCreatesOneReceipt() throws Exception {
        var command=movement(UUID.randomUUID(),warehouse,"RECEIPT","5","8");
        assertEquals(List.of(200,200),concurrent("/api/v1/inventory/movements",command,command));
        equal("5",balance(warehouse)); assertEquals(1,count("stock_movement"));
    }
    @Test void transfersAreAtomicIdempotentAndPreserveCost() throws Exception {
        UUID destination=createWarehouse("DESTINO"); receipt("4","8");
        postApi("/api/v1/inventory/movements",auth,movement(UUID.randomUUID(),destination,"RECEIPT","2","4"),200);
        var transfer=Map.of("operationId",UUID.randomUUID(),"sourceWarehouseId",warehouse,"destinationWarehouseId",destination,"productId",product,"quantity","2","reason","Transferência entre depósitos");
        var first=postApi("/api/v1/inventory/transfers",auth,transfer,200);
        assertEquals(2,first.get("movements").size()); assertEquals(first,postApi("/api/v1/inventory/transfers",auth,transfer,200));
        equal("2",balance(warehouse)); equal("4",balance(destination));
        equal("6",db.queryForObject("SELECT average_cost FROM stock_balance WHERE tenant_id=? AND warehouse_id=? AND product_id=?",BigDecimal.class,tenant,destination,product));
        assertEquals(4,count("stock_movement"));
    }
    @Test void insufficientTransferLeavesBothWarehousesAndOperationHistoryUnchanged() throws Exception {
        UUID destination=createWarehouse("DESTINO"); receipt("1","8");
        postApi("/api/v1/inventory/transfers",auth,Map.of("operationId",UUID.randomUUID(),"sourceWarehouseId",warehouse,"destinationWarehouseId",destination,"productId",product,"quantity","2","reason","Transferência sem saldo"),409);
        equal("1",balance(warehouse)); equal("0",balance(destination)); assertEquals(1,count("stock_operation")); assertEquals(1,count("stock_movement"));
    }
    @Test void lossApprovalIsTheOnlyMomentOfWithdrawalAndIsSafeToRetry() throws Exception {
        receipt("3","10"); var loss=loss("2"); String id=loss.get("id").asText();
        assertEquals("PENDING",loss.get("status").asText()); equal("3",balance(warehouse)); assertEquals("20.00",loss.get("estimatedTotal").asText());
        var approval=postApi("/api/v1/losses/"+id+"/approve",auth,Map.of("operationId",UUID.randomUUID()),200);
        assertEquals("APPROVED",approval.get("status").asText()); equal("1",balance(warehouse));
        assertEquals(approval,postApi("/api/v1/losses/"+id+"/approve",auth,Map.of("operationId",UUID.randomUUID()),200));
        assertEquals(2,count("stock_movement")); assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='LOSS_APPROVED'",Long.class,tenant));
    }
    @Test void concurrentLossApprovalWithdrawsOnce() throws Exception {
        receipt("3","10"); String id=loss("2").get("id").asText();
        assertEquals(List.of(200,200),concurrent("/api/v1/losses/"+id+"/approve",Map.of("operationId",UUID.randomUUID()),Map.of("operationId",UUID.randomUUID())));
        equal("1",balance(warehouse)); assertEquals(2,count("stock_movement"));
    }
    @Test void rejectionDoesNotWithdrawAndPreventsLaterApproval() throws Exception {
        receipt("3","10"); String id=loss("2").get("id").asText();
        var command=Map.of("note","Mercadoria reavaliada e liberada");
        var rejection=postApi("/api/v1/losses/"+id+"/reject",auth,command,200);
        assertEquals(rejection,postApi("/api/v1/losses/"+id+"/reject",auth,command,200));
        postApi("/api/v1/losses/"+id+"/approve",auth,Map.of("operationId",UUID.randomUUID()),409);
        equal("3",balance(warehouse)); assertEquals(1,count("stock_movement"));
    }
    @Test void overlappingLossReportsRevalidateStockOnApproval() throws Exception {
        receipt("3","10"); String first=loss("2").get("id").asText(),second=loss("2").get("id").asText();
        postApi("/api/v1/losses/"+first+"/approve",auth,Map.of("operationId",UUID.randomUUID()),200);
        postApi("/api/v1/losses/"+second+"/approve",auth,Map.of("operationId",UUID.randomUUID()),409);
        assertEquals("PENDING",getApi("/api/v1/losses/"+second,auth,200).get("status").asText()); equal("1",balance(warehouse)); assertEquals(2,count("stock_operation"));
    }
    @Test void lossCostUsesCurrentAverageAndPreservesOriginalEstimate() throws Exception {
        receipt("1","10"); String id=loss("1").get("id").asText(); receipt("1","30");
        var approved=postApi("/api/v1/losses/"+id+"/approve",auth,Map.of("operationId",UUID.randomUUID()),200);
        assertEquals("10.00",approved.get("estimatedTotal").asText()); assertEquals("20.00",approved.get("approvedTotal").asText());
    }
    @Test void lossesAreIdempotentByRequestId() throws Exception {
        receipt("3","10"); var body=new HashMap<String,Object>(Map.of("requestId",UUID.randomUUID(),"warehouseId",warehouse,"productId",product,"quantity","2","reason","DAMAGE","details","Avaria conferida na entrega"));
        var first=postApi("/api/v1/losses",auth,body,200); assertEquals(first,postApi("/api/v1/losses",auth,body,200));
        body.put("quantity","1"); postApi("/api/v1/losses",auth,body,409); assertEquals(1,count("stock_loss"));
    }
    @Test void tenantCannotReadOrMoveForeignWarehouse() throws Exception {
        String other="other-"+UUID.randomUUID(); provisioning.create("Outra empresa",other,"Admin","admin@example.com",password); String otherToken=token(other);
        getApi("/api/v1/inventory/warehouses/"+warehouse+"/balances",otherToken,404);
        postApi("/api/v1/inventory/movements",otherToken,movement(UUID.randomUUID(),warehouse,"RECEIPT","1","8"),404);
        postApi("/api/v1/inventory/warehouses",otherToken,Map.of("branchId",branch,"code","FOREIGN","name","Não permitido"),404);
        assertEquals(0,count("stock_movement"));
    }
    @Test void costsAreRedactedImmediatelyAfterPermissionRevocation() throws Exception {
        receipt("3","10"); var loss=loss("1");
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='inventory:cost'",tenant);
        assertTrue(getApi("/api/v1/inventory/warehouses/"+warehouse+"/balances",auth,200).get("items").get(0).get("averageCost").isNull());
        assertTrue(getApi("/api/v1/inventory/warehouses/"+warehouse+"/movements",auth,200).get("items").get(0).get("unitCost").isNull());
        assertTrue(getApi("/api/v1/losses/"+loss.get("id").asText(),auth,200).get("estimatedTotal").isNull());
    }
    @Test void writesAndApprovalRequireSeparatePermissions() throws Exception {
        receipt("3","10"); String id=loss("1").get("id").asText();
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission IN ('inventory:write','loss:approve')",tenant);
        postApi("/api/v1/inventory/movements",auth,movement(UUID.randomUUID(),warehouse,"RECEIPT","1","8"),403);
        postApi("/api/v1/losses/"+id+"/approve",auth,Map.of("operationId",UUID.randomUUID()),403); equal("3",balance(warehouse));
    }
    @Test void invalidValuesAndBalanceOverflowAreRejectedWithoutSideEffects() throws Exception {
        for (var command:List.of(movement(UUID.randomUUID(),warehouse,"RECEIPT","0","1"),movement(UUID.randomUUID(),warehouse,"RECEIPT","1",null),movement(UUID.randomUUID(),warehouse,"RECEIPT","1.0000001","1"),movement(UUID.randomUUID(),warehouse,"ADJUSTMENT_OUT","1","5"))) postApi("/api/v1/inventory/movements",auth,command,400);
        assertEquals(0,count("stock_operation")); receipt("9999999999999.999999","1");
        postApi("/api/v1/inventory/movements",auth,movement(UUID.randomUUID(),warehouse,"RECEIPT","0.000001","1"),409);
        assertEquals(1,count("stock_movement"));
    }
}
