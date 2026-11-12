package br.com.erp;

import com.fasterxml.jackson.databind.JsonNode;
import java.math.BigDecimal;
import java.util.*;
import java.util.concurrent.*;
import org.junit.jupiter.api.*;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import static org.junit.jupiter.api.Assertions.*;

class PurchaseTest extends ApiTestSupport {
    @Test void concurrentCreationWithSameKeyCreatesOneOrderAndAudit() throws Exception {
        var command=input(UUID.randomUUID(),List.of(item(product1,"2","10")));
        assertEquals(List.of(201,201),concurrent("/api/v1/purchase-orders",command,"/api/v1/purchase-orders",command));
        assertEquals(1,count("purchase_order"));assertEquals(1,count("purchase_order_line"));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PURCHASE_ORDER_CREATED'",Integer.class,tenant));
    }
    @Test void manualMovementCannotMasqueradeAsPurchaseReceiptOperation() throws Exception {
        var order=order(List.of(item(product1,"2","10")));UUID key=UUID.randomUUID();UUID line=line(order,product1);
        UUID operation=UUID.nameUUIDFromBytes(("PURCHASE_RECEIPT|"+tenant+"|"+key+"|"+line).getBytes(java.nio.charset.StandardCharsets.UTF_8));
        postApi("/api/v1/inventory/movements",auth,Map.of("operationId",operation,"warehouseId",warehouse,"productId",product1,"type","RECEIPT","quantity","1","unitCost","10","reason","Recebimento pedido "+id(order)),200);
        postApi(receivePath(order),auth,receipt(key,warehouse,List.of(received(line,"1"))),409);
        assertEquals(0,count("purchase_receipt"));assertEquals(0,count("purchase_receipt_line"));equal("1",balance(product1));
        assertEquals("OPEN",getApi("/api/v1/purchase-orders/"+id(order),auth,200).get("header").get("status").asText());
    }
    @Test void orderReceiptPermissionDoesNotRequireManualInventoryWrite() throws Exception {
        var order=order(List.of(item(product1,"2","10")));
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='inventory:write'",tenant);
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),200);equal("1",balance(product1));
        postApi("/api/v1/inventory/movements",auth,Map.of("operationId",UUID.randomUUID(),"warehouseId",warehouse,"productId",product1,"type","RECEIPT","quantity","1","unitCost","10","reason","Entrada manual negada"),403);
    }
    String auth;UUID branch,supplier,warehouse,product1,product2;
    @BeforeEach void purchaseFixture() throws Exception {
        auth=token(company);branch=db.queryForObject("SELECT id FROM branch WHERE tenant_id=?",UUID.class,tenant);
        warehouse=UUID.fromString(postApi("/api/v1/inventory/warehouses",auth,Map.of("branchId",branch,"code","PURCHASE","name","Compras"),201).get("id").asText());
        supplier=UUID.fromString(postApi("/api/v1/suppliers",auth,Map.of("code","SUP","name","Fornecedor","taxIdentifier","","contactName","","email","","phone","","address","","notes",""),201).get("id").asText());
        product1=product("P1");product2=product("P2");
    }
    UUID product(String sku) throws Exception {return UUID.fromString(postApi("/api/v1/products",auth,Map.of("sku",sku,"name","Produto "+sku,"description","","unit","UN","salePrice","20","barcodes",List.of()),201).get("id").asText());}
    Map<String,Object> item(UUID product,String quantity,String cost){return Map.of("productId",product,"quantity",quantity,"unitCost",cost);}
    Map<String,Object> input(UUID key,List<Map<String,Object>> items){return Map.of("requestId",key,"branchId",branch,"supplierId",supplier,"notes","Pedido teste","items",items);}
    JsonNode order(List<Map<String,Object>> items) throws Exception {return postApi("/api/v1/purchase-orders",auth,input(UUID.randomUUID(),items),201);}
    String id(JsonNode order){return order.get("header").get("id").asText();}
    UUID line(JsonNode order,UUID product){for(var line:order.get("lines"))if(line.get("productId").asText().equals(product.toString()))return UUID.fromString(line.get("id").asText());throw new AssertionError("Produto ausente");}
    Map<String,Object> received(UUID line,String quantity){return Map.of("orderLineId",line,"quantity",quantity);}
    Map<String,Object> receipt(UUID key,UUID warehouse,List<Map<String,Object>> items){return Map.of("receiptId",key,"warehouseId",warehouse,"note","Mercadoria conferida","items",items);}
    String receivePath(JsonNode order){return "/api/v1/purchase-orders/"+id(order)+"/receipts";}
    long count(String table){return db.queryForObject("SELECT COUNT(*) FROM "+table+" WHERE tenant_id=?",Long.class,tenant);}
    BigDecimal balance(UUID product){var values=db.query("SELECT quantity FROM stock_balance WHERE tenant_id=? AND warehouse_id=? AND product_id=?",(rs,i)->rs.getBigDecimal(1),tenant,warehouse,product);return values.isEmpty()?BigDecimal.ZERO:values.getFirst();}
    void equal(String expected,BigDecimal actual){assertEquals(0,new BigDecimal(expected).compareTo(actual));}
    List<Integer> concurrent(String path1,Object body1,String path2,Object body2) throws Exception {
        var gate=new CountDownLatch(1);try(var pool=Executors.newFixedThreadPool(2)){
            var first=pool.submit(()->{gate.await();return http.perform(post(path1).header("Authorization","Bearer "+auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body1))).andReturn().getResponse().getStatus();});
            var second=pool.submit(()->{gate.await();return http.perform(post(path2).header("Authorization","Bearer "+auth).contentType(MediaType.APPLICATION_JSON).content(json.writeValueAsString(body2))).andReturn().getResponse().getStatus();});gate.countDown();
            var results=new ArrayList<>(List.of(first.get(40,TimeUnit.SECONDS),second.get(40,TimeUnit.SECONDS)));Collections.sort(results);return results;
        }
    }
    @Test void creationSnapshotsPricesAndIdempotenceNormalizesScaleAndItemOrder() throws Exception {
        UUID key=UUID.randomUUID();var first=postApi("/api/v1/purchase-orders",auth,input(key,List.of(item(product1,"3","10"),item(product2,"2","20"))),201);
        var replay=postApi("/api/v1/purchase-orders",auth,input(key,List.of(item(product2,"2.000000","20.0"),item(product1,"3.0","10.000000"))),201);
        assertEquals(first,replay);assertEquals("70.00",first.get("header").get("total").asText());assertEquals(1,count("purchase_order"));assertEquals(2,count("purchase_order_line"));
        postApi("/api/v1/purchase-orders",auth,input(key,List.of(item(product1,"4","10"))),409);
        db.update("UPDATE supplier SET name='Nome alterado' WHERE id=?",supplier);db.update("UPDATE product SET name='Produto alterado',sale_price=999 WHERE id=?",product1);
        var stored=getApi("/api/v1/purchase-orders/"+id(first),auth,200);assertEquals("Fornecedor",stored.get("header").get("supplierName").asText());assertEquals("70.00",stored.get("header").get("total").asText());
        assertEquals(0,count("stock_movement"));
    }
    @Test void partialThenCompleteReceiptUpdatesWeightedCostLedgerAndHistory() throws Exception {
        postApi("/api/v1/inventory/movements",auth,Map.of("operationId",UUID.randomUUID(),"warehouseId",warehouse,"productId",product1,"type","RECEIPT","quantity","1","unitCost","6","reason","Saldo inicial teste"),200);
        var order=order(List.of(item(product1,"3","10"),item(product2,"2","20")));
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),200);
        assertEquals("PARTIAL",getApi("/api/v1/purchase-orders/"+id(order),auth,200).get("header").get("status").asText());equal("2",balance(product1));
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"2"),received(line(order,product2),"2"))),200);
        var complete=getApi("/api/v1/purchase-orders/"+id(order),auth,200);assertEquals("RECEIVED",complete.get("header").get("status").asText());equal("4",balance(product1));equal("2",balance(product2));
        equal("9",db.queryForObject("SELECT average_cost FROM stock_balance WHERE tenant_id=? AND warehouse_id=? AND product_id=?",BigDecimal.class,tenant,warehouse,product1));
        assertEquals(4,count("stock_movement"));assertEquals(2,getApi(receivePath(order),auth,200).get("total").asInt());
    }
    @Test void receiptReplayReturnsOriginalAndConflictingKeyCannotDuplicateStock() throws Exception {
        var order=order(List.of(item(product1,"2","10")));UUID key=UUID.randomUUID();
        var first=postApi(receivePath(order),auth,receipt(key,warehouse,List.of(received(line(order,product1),"2"))),200);
        var replay=postApi(receivePath(order),auth,receipt(key,warehouse,List.of(received(line(order,product1),"2.000000"))),200);
        assertEquals(first,replay);assertEquals(1,count("purchase_receipt"));assertEquals(1,count("stock_movement"));equal("2",balance(product1));
        postApi(receivePath(order),auth,receipt(key,warehouse,List.of(received(line(order,product1),"1"))),409);
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),409);
    }
    @Test void overReceiptAndFailureOnLaterProductRollBackEveryEffect() throws Exception {
        var order=order(List.of(item(product1,"2","10"),item(product2,"2","20")));
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"),received(line(order,product2),"3"))),409);
        UUID later=product1.toString().compareTo(product2.toString())>0?product1:product2;
        db.update("UPDATE product SET active=FALSE WHERE id=?",later);
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"),received(line(order,product2),"1"))),404);
        assertEquals(0,count("stock_movement"));assertEquals(0,count("stock_operation"));assertEquals(0,count("purchase_receipt"));assertEquals(0,count("purchase_receipt_line"));
        assertEquals("OPEN",getApi("/api/v1/purchase-orders/"+id(order),auth,200).get("header").get("status").asText());
    }
    @Test void cancellationPreservesReceivedGoodsAndReplayWhileClosingPendingQuantity() throws Exception {
        var order=order(List.of(item(product1,"3","10")));UUID key=UUID.randomUUID();var received=receipt(key,warehouse,List.of(received(line(order,product1),"1")));
        postApi(receivePath(order),auth,received,200);String path="/api/v1/purchase-orders/"+id(order)+"/cancel";
        postApi(path,auth,Map.of("version",0,"note","Saldo pendente cancelado"),409);
        postApi(path,auth,Map.of("version",1,"note","Saldo pendente cancelado"),200);
        postApi(path,auth,Map.of("version",1,"note","Saldo pendente cancelado"),200);equal("1",balance(product1));
        postApi(receivePath(order),auth,received,200);postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),409);
        assertEquals("CANCELLED",getApi("/api/v1/purchase-orders/"+id(order),auth,200).get("header").get("status").asText());
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PURCHASE_ORDER_CANCELLED'",Integer.class,tenant));
    }
    @Test void tenantBranchAndDestinationWarehouseAreEnforced() throws Exception {
        var order=order(List.of(item(product1,"2","10")));
        UUID second=UUID.fromString(postApi("/api/v1/branches",auth,Map.of("code","OTHER","name","Outra filial"),201).get("id").asText());
        UUID otherWarehouse=UUID.fromString(postApi("/api/v1/inventory/warehouses",auth,Map.of("branchId",second,"code","OTHER","name","Outro"),201).get("id").asText());
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),otherWarehouse,List.of(received(line(order,product1),"1"))),400);
        String otherCompany="other-"+UUID.randomUUID();provisioning.create("Outra",otherCompany,"Admin","admin@example.com",password);String other=token(otherCompany);
        getApi("/api/v1/purchase-orders/"+id(order),other,404);postApi(receivePath(order),other,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),404);
        UUID user=db.queryForObject("SELECT id FROM app_user WHERE tenant_id=?",UUID.class,tenant);db.update("UPDATE app_user SET all_branches=FALSE WHERE id=?",user);db.update("INSERT INTO user_branch_access(tenant_id,user_id,branch_id) VALUES(?,?,?)",tenant,user,second);
        assertEquals(0,getApi("/api/v1/purchase-orders",auth,200).get("total").asInt());getApi("/api/v1/purchase-orders/"+id(order),auth,404);getApi(receivePath(order),auth,404);
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),404);
        assertEquals(0,count("stock_movement"));
    }
    @Test void permissionsValidationAndInvalidReferenceNeverChangeStock() throws Exception {
        var order=order(List.of(item(product1,"2","10")));
        db.update("DELETE FROM user_permission WHERE tenant_id=? AND permission='purchase:receive'",tenant);
        postApi(receivePath(order),auth,receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),403);
        postApi("/api/v1/purchase-orders",auth,input(UUID.randomUUID(),List.of(item(product1,"1","10"),item(product1,"2","20"))),400);
        postApi("/api/v1/purchase-orders",auth,input(UUID.randomUUID(),List.of(item(product1,"0","10"))),400);
        postApi("/api/v1/purchase-orders",auth,input(UUID.randomUUID(),List.of(item(product1,"9999999999999","9999999999999"))),400);
        postApi("/api/v1/purchase-orders",auth,input(UUID.randomUUID(),List.of(item(product1,"1","10"),item(UUID.randomUUID(),"1","20"))),404);
        assertEquals(1,count("purchase_order"));assertEquals(0,count("stock_movement"));
        http.perform(get("/api/v1/purchase-orders")).andExpect(status().isUnauthorized());
    }
    @Test void concurrentRemainingQuantityHasOneWinner() throws Exception {
        var order=order(List.of(item(product1,"2","10")));var entries=List.of(received(line(order,product1),"2"));
        assertEquals(List.of(200,409),concurrent(receivePath(order),receipt(UUID.randomUUID(),warehouse,entries),receivePath(order),receipt(UUID.randomUUID(),warehouse,entries)));
        equal("2",balance(product1));assertEquals(1,count("purchase_receipt"));
    }
    @Test void concurrentRetryPostsOneEntryAndAudit() throws Exception {
        var order=order(List.of(item(product1,"2","10")));var command=receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"2")));
        assertEquals(List.of(200,200),concurrent(receivePath(order),command,receivePath(order),command));equal("2",balance(product1));assertEquals(1,count("stock_movement"));
        assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM audit_event WHERE tenant_id=? AND action='PURCHASE_RECEIVED'",Integer.class,tenant));
    }
    @Test void differentMultiProductOrdersAcquireProductLocksInStableOrder() throws Exception {
        var first=order(List.of(item(product1,"2","10"),item(product2,"2","20")));
        var second=order(List.of(item(product2,"2","20"),item(product1,"2","10")));
        var firstReceipt=receipt(UUID.randomUUID(),warehouse,List.of(received(line(first,product1),"1"),received(line(first,product2),"1")));
        var secondReceipt=receipt(UUID.randomUUID(),warehouse,List.of(received(line(second,product2),"1"),received(line(second,product1),"1")));
        assertEquals(List.of(200,200),concurrent(receivePath(first),firstReceipt,receivePath(second),secondReceipt));equal("2",balance(product1));equal("2",balance(product2));
    }
    @Test void cancellationRacingReceiptCannotLoseOrCreateUnrecordedGoods() throws Exception {
        var order=order(List.of(item(product1,"2","10")));
        assertEquals(List.of(200,409),concurrent(receivePath(order),receipt(UUID.randomUUID(),warehouse,List.of(received(line(order,product1),"1"))),"/api/v1/purchase-orders/"+id(order)+"/cancel",Map.of("version",0,"note","Cancelamento concorrente")));
        String state=getApi("/api/v1/purchase-orders/"+id(order),auth,200).get("header").get("status").asText();
        if(state.equals("CANCELLED")){equal("0",balance(product1));assertEquals(0,count("purchase_receipt"));}else{assertEquals("PARTIAL",state);equal("1",balance(product1));assertEquals(1,count("purchase_receipt"));}
    }
}
