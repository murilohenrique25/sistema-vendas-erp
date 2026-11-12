package br.com.erp.inventory;

import br.com.erp.auth.Actor;
import br.com.erp.auth.BranchAccess;
import br.com.erp.auth.TokenCodec;
import br.com.erp.shared.Audit;
import java.math.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.inventory.StockContracts.*;

@Service
public class StockService {
    private static final BigDecimal MAX_QUANTITY=new BigDecimal("9999999999999.999999");
    private final JdbcTemplate db;
    private final Audit audit;
    private final BranchAccess access;
    private final LotService lots;
    public StockService(JdbcTemplate db,Audit audit,BranchAccess access,LotService lots) { this.db=db; this.audit=audit;this.access=access;this.lots=lots; }

    @Transactional public Warehouse createWarehouse(Actor actor,WarehouseInput input) {
        access.requireBranch(actor,input.branchId());
        if (db.queryForObject("SELECT COUNT(*) FROM branch WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,actor.tenantId(),input.branchId())==0) throw missing("Filial");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO warehouse(id,tenant_id,branch_id,code,name) VALUES(?,?,?,?,?)",id,actor.tenantId(),input.branchId(),input.code(),input.name().trim());
        audit.record(actor,"WAREHOUSE_CREATED",id);
        return new Warehouse(id,input.branchId(),input.code(),input.name().trim(),true);
    }
    @Transactional(readOnly=true) public Map<String,Object> warehouses(Actor actor,int page,int size) {
        String where=" WHERE w.tenant_id=? AND "+BranchAccess.predicate("w.branch_id");
        var items=db.query("SELECT w.id,w.branch_id,w.code,w.name,w.active FROM warehouse w"+where+" ORDER BY w.name,w.id LIMIT ? OFFSET ?",(rs,i)->new Warehouse(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getString(3),rs.getString(4),rs.getBoolean(5)),actor.tenantId(),actor.tenantId(),actor.userId(),size,(long)page*size);
        return page(items,db.queryForObject("SELECT COUNT(*) FROM warehouse w"+where,Long.class,actor.tenantId(),actor.tenantId(),actor.userId()),page,size);
    }
    @Transactional(readOnly=true) public Map<String,Object> balances(Actor actor,UUID warehouseId,String query,int page,int size) {
        warehouse(actor,warehouseId);
        boolean costs=canSeeCost(actor);
        var items=db.query("SELECT p.id,p.sku,p.name,p.unit,b.quantity,b.average_cost FROM stock_balance b JOIN product p ON p.tenant_id=b.tenant_id AND p.id=b.product_id WHERE b.tenant_id=? AND b.warehouse_id=? AND (POSITION(LOWER(?) IN LOWER(p.name))>0 OR POSITION(LOWER(?) IN LOWER(p.sku))>0) ORDER BY p.name,p.id LIMIT ? OFFSET ?",
            (rs,i)->new Balance(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getString(4),rs.getBigDecimal(5),costs ? rs.getBigDecimal(6) : null),actor.tenantId(),warehouseId,query,query,size,(long)page*size);
        long total=db.queryForObject("SELECT COUNT(*) FROM stock_balance b JOIN product p ON p.tenant_id=b.tenant_id AND p.id=b.product_id WHERE b.tenant_id=? AND b.warehouse_id=? AND (POSITION(LOWER(?) IN LOWER(p.name))>0 OR POSITION(LOWER(?) IN LOWER(p.sku))>0)",Long.class,actor.tenantId(),warehouseId,query,query);
        return page(items,total,page,size);
    }
    @Transactional public Operation move(Actor actor,MovementInput input) {
        boolean incoming=input.type()==Type.RECEIPT || input.type()==Type.ADJUSTMENT_IN;
        if (incoming && input.unitCost()==null) throw invalid("Informe o custo unitário da entrada.");
        if (!incoming && input.unitCost()!=null) throw invalid("O custo da saída é calculado pelo sistema.");
        String reason=reason(input.reason());
        warehouse(actor,input.warehouseId());
        String unit=lockProduct(actor,input.productId());
        String fingerprint=fingerprint("MOVEMENT",input.warehouseId(),input.productId(),input.type(),input.quantity(),input.unitCost(),reason);
        if (replay(actor,input.operationId(),fingerprint)) return operation(actor,input.operationId());
        begin(actor,input.operationId(),fingerprint,"MOVEMENT");
        change(actor,input.operationId(),input.warehouseId(),input.productId(),unit,input.type().name(),incoming ? input.quantity() : input.quantity().negate(),input.unitCost(),reason,null);
        audit.record(actor,"STOCK_MOVED",input.operationId());
        return operation(actor,input.operationId());
    }
    @Transactional public void receivePurchase(Actor actor,UUID operationId,UUID receiptId,UUID lineId,UUID warehouseId,UUID productId,String expectedUnit,BigDecimal quantity,BigDecimal cost,String reason) {
        warehouse(actor,warehouseId);String unit=lockProduct(actor,productId);
        if(!unit.equals(expectedUnit))throw conflict("Unidade do produto mudou desde o pedido.");
        String hash=fingerprint("PURCHASE_RECEIPT",receiptId,lineId,warehouseId,productId,quantity,cost,reason);
        if(replay(actor,operationId,hash))throw conflict("Operação de estoque já vinculada a um recebimento.");
        begin(actor,operationId,hash,"MOVEMENT");change(actor,operationId,warehouseId,productId,unit,"RECEIPT",quantity,cost,reason,null);
    }
    @Transactional public void returnSupplier(Actor actor,UUID operation,UUID returnId,UUID warehouseId,UUID productId,String expectedUnit,BigDecimal quantity,String note){
        warehouse(actor,warehouseId);var units=db.query("SELECT unit FROM product WHERE tenant_id=? AND id=? FOR UPDATE",(r,i)->r.getString(1),actor.tenantId(),productId);if(units.isEmpty()||!units.getFirst().equals(expectedUnit))throw conflict("Unidade difere do recebimento.");String unit=units.getFirst();
        String hash=fingerprint("SUPPLIER_RETURN",returnId,warehouseId,productId,quantity,note);if(replay(actor,operation,hash))throw conflict("Operação já utilizada.");
        begin(actor,operation,hash,"MOVEMENT");change(actor,operation,warehouseId,productId,unit,"ADJUSTMENT_OUT",quantity.negate(),null,note,null);
    }
    // SaleService owns the surrounding atomic sale/payment transaction.
    @Transactional public void consumeSale(Actor actor,UUID operationId,UUID saleId,UUID warehouseId,UUID productId,String expectedUnit,BigDecimal quantity) {
        warehouse(actor,warehouseId);String unit=lockProduct(actor,productId);
        if(!unit.equals(expectedUnit))throw conflict("Unidade do produto mudou durante a venda.");
        String hash=fingerprint("POS_SALE",saleId,warehouseId,productId,quantity);
        if(replay(actor,operationId,hash))throw conflict("Operação de estoque já vinculada a uma venda.");
        begin(actor,operationId,hash,"MOVEMENT");
        change(actor,operationId,warehouseId,productId,unit,"CONSUMPTION",quantity.negate(),null,"Venda "+saleId,null);
    }
    // Full cancellation restores the original sale cost, including inactive products.
    @Transactional public void returnSale(Actor actor,UUID operation,UUID saleId,UUID originalOperation,UUID warehouseId,UUID productId,String expectedUnit,BigDecimal quantity) {
        returnSale(actor,operation,saleId,originalOperation,warehouseId,productId,expectedUnit,quantity,"Cancelamento da venda "+saleId);
    }
    @Transactional public void returnSale(Actor actor,UUID operation,UUID saleId,UUID originalOperation,UUID warehouseId,UUID productId,String expectedUnit,BigDecimal quantity,String reason) {
        warehouse(actor,warehouseId);
        var units=db.query("SELECT unit FROM product WHERE tenant_id=? AND id=? FOR UPDATE",(r,i)->r.getString(1),actor.tenantId(),productId);
        if(units.isEmpty()||!units.getFirst().equals(expectedUnit))throw conflict("Unidade atual difere da venda. Regularize antes do cancelamento.");
        BigDecimal cost=db.queryForObject("SELECT unit_cost FROM stock_movement WHERE tenant_id=? AND operation_id=? AND warehouse_id=? AND product_id=? AND movement_type='CONSUMPTION'",BigDecimal.class,actor.tenantId(),originalOperation,warehouseId,productId);
        String hash=fingerprint("POS_SALE_RETURN",saleId,originalOperation,quantity);
        if(replay(actor,operation,hash))throw conflict("Retorno já vinculado à venda.");
        begin(actor,operation,hash,"MOVEMENT");change(actor,operation,warehouseId,productId,expectedUnit,"ADJUSTMENT_IN",quantity,cost,reason,null,originalOperation);
    }
    @Transactional public Operation transfer(Actor actor,TransferInput input) {
        if (input.sourceWarehouseId().equals(input.destinationWarehouseId())) throw invalid("Origem e destino devem ser diferentes.");
        warehouse(actor,input.sourceWarehouseId()); warehouse(actor,input.destinationWarehouseId());
        String unit=lockProduct(actor,input.productId()),reason=reason(input.reason());
        String hash=fingerprint("TRANSFER",input.sourceWarehouseId(),input.destinationWarehouseId(),input.productId(),input.quantity(),reason);
        if (replay(actor,input.operationId(),hash)) return operation(actor,input.operationId());
        begin(actor,input.operationId(),hash,"TRANSFER");
        BigDecimal cost=change(actor,input.operationId(),input.sourceWarehouseId(),input.productId(),unit,"TRANSFER_OUT",input.quantity().negate(),null,reason,null);
        change(actor,input.operationId(),input.destinationWarehouseId(),input.productId(),unit,"TRANSFER_IN",input.quantity(),cost,reason,null);
        audit.record(actor,"STOCK_TRANSFERRED",input.operationId());
        return operation(actor,input.operationId());
    }
    // LossService owns the approval transaction and the locked loss row.
    @Transactional public BigDecimal approveLoss(Actor actor,UUID operationId,UUID lossId,UUID warehouseId,UUID productId,BigDecimal quantity,String reason) {
        warehouse(actor,warehouseId);
        String unit=lockProduct(actor,productId);
        String hash=fingerprint("LOSS_APPROVAL",lossId,warehouseId,productId,quantity,reason);
        if (replay(actor,operationId,hash)) throw conflict("A perda já possui uma operação de aprovação.");
        begin(actor,operationId,hash,"LOSS_APPROVAL");
        return change(actor,operationId,warehouseId,productId,unit,"LOSS",quantity.negate(),null,reason,lossId);
    }
    @Transactional public Snapshot snapshotForLoss(Actor actor,UUID warehouseId,UUID productId) {
        warehouse(actor,warehouseId); lockProduct(actor,productId); return balance(actor,warehouseId,productId);
    }
    @Transactional(readOnly=true) public Map<String,Object> movements(Actor actor,UUID warehouseId,int page,int size) {
        warehouse(actor,warehouseId);
        boolean costs=canSeeCost(actor);
        var items=db.query(movementSql()+" WHERE m.tenant_id=? AND m.warehouse_id=? ORDER BY m.created_at DESC,m.id LIMIT ? OFFSET ?",(rs,i)->movement(rs,costs),actor.tenantId(),warehouseId,size,(long)page*size);
        return page(items,db.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE tenant_id=? AND warehouse_id=?",Long.class,actor.tenantId(),warehouseId),page,size);
    }
    @Transactional public UUID adjustInventory(Actor actor,UUID inventory,UUID warehouseId,UUID productId,String unit,BigDecimal expected,BigDecimal counted,long expectedMovements,BigDecimal suppliedCost){
        warehouse(actor,warehouseId);String actual=lockProduct(actor,productId);Snapshot current=balance(actor,warehouseId,productId);
        long movements=db.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE tenant_id=? AND warehouse_id=? AND product_id=?",Long.class,actor.tenantId(),warehouseId,productId);
        if(!actual.equals(unit)||current.quantity().compareTo(expected)!=0||movements!=expectedMovements)throw conflict("Estoque movimentado após abertura da contagem. Cancele e abra novo inventário.");
        BigDecimal delta=counted.subtract(expected);if(delta.signum()==0)return null;
        BigDecimal cost=current.averageCost();if(delta.signum()>0&&expected.signum()==0){if(suppliedCost==null)throw invalid("Informe custo para sobra de produto sem saldo anterior.");cost=suppliedCost;}
        UUID operation=UUID.randomUUID();begin(actor,operation,fingerprint("PHYSICAL_INVENTORY",inventory,productId,expected,counted),"MOVEMENT");change(actor,operation,warehouseId,productId,unit,delta.signum()>0?"ADJUSTMENT_IN":"ADJUSTMENT_OUT",delta,delta.signum()>0?cost:null,"Inventário "+inventory,null);return operation;
    }
    private BigDecimal change(Actor actor,UUID operationId,UUID warehouseId,UUID productId,String unit,String type,BigDecimal delta,BigDecimal incomingCost,String reason,UUID lossId) {
        return change(actor,operationId,warehouseId,productId,unit,type,delta,incomingCost,reason,lossId,null);
    }
    private BigDecimal change(Actor actor,UUID operationId,UUID warehouseId,UUID productId,String unit,String type,BigDecimal delta,BigDecimal incomingCost,String reason,UUID lossId,UUID sourceOperation) {
        Snapshot current=balance(actor,warehouseId,productId);
        BigDecimal next=current.quantity().add(delta);
        if (next.signum()<0) throw conflict("Estoque insuficiente para concluir a operação.");
        if (next.compareTo(MAX_QUANTITY)>0) throw conflict("Saldo excede o limite de quantidade.");
        BigDecimal applicableCost=delta.signum()>0 ? incomingCost : current.averageCost();
        BigDecimal average=delta.signum()>0 ? current.quantity().multiply(current.averageCost()).add(delta.multiply(applicableCost)).divide(next,6,RoundingMode.HALF_UP) : current.averageCost();
        if (next.signum()==0) average=BigDecimal.ZERO;
        int updated=db.update("UPDATE stock_balance SET quantity=?,average_cost=? WHERE tenant_id=? AND warehouse_id=? AND product_id=?",next,average,actor.tenantId(),warehouseId,productId);
        if (updated==0) db.update("INSERT INTO stock_balance(tenant_id,warehouse_id,product_id,quantity,average_cost) VALUES(?,?,?,?,?)",actor.tenantId(),warehouseId,productId,next,average);
        UUID movement=UUID.randomUUID();
        db.update("INSERT INTO stock_movement(id,tenant_id,operation_id,warehouse_id,product_id,loss_id,movement_type,quantity,balance_after,unit,unit_cost,reason) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",movement,actor.tenantId(),operationId,warehouseId,productId,lossId,type,delta,next,unit,applicableCost,reason);
        lots.apply(actor,operationId,movement,warehouseId,productId,delta,type,sourceOperation);
        return applicableCost;
    }
    private Snapshot balance(Actor actor,UUID warehouseId,UUID productId) {
        var values=db.query("SELECT quantity,average_cost FROM stock_balance WHERE tenant_id=? AND warehouse_id=? AND product_id=?",(rs,i)->new Snapshot(rs.getBigDecimal(1),rs.getBigDecimal(2)),actor.tenantId(),warehouseId,productId);
        return values.isEmpty() ? new Snapshot(BigDecimal.ZERO,BigDecimal.ZERO) : values.getFirst();
    }
    private String lockProduct(Actor actor,UUID productId) {
        var units=db.query("SELECT unit FROM product WHERE tenant_id=? AND id=? AND active=TRUE FOR UPDATE",(rs,i)->rs.getString(1),actor.tenantId(),productId);
        if (units.isEmpty()) throw missing("Produto ativo"); return units.getFirst();
    }
    private void warehouse(Actor actor,UUID id) {
        access.requireWarehouse(actor,id);
        if (db.queryForObject("SELECT COUNT(*) FROM warehouse WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,actor.tenantId(),id)==0) throw missing("Depósito ativo");
    }
    private boolean replay(Actor actor,UUID id,String hash) {
        var values=db.query("SELECT fingerprint FROM stock_operation WHERE tenant_id=? AND id=?",(rs,i)->rs.getString(1),actor.tenantId(),id);
        if (values.isEmpty()) return false;
        if (!values.getFirst().equals(hash)) throw conflict("Identificador da operação já utilizado com outros dados."); return true;
    }
    private void begin(Actor actor,UUID id,String hash,String kind) {
        db.update("INSERT INTO stock_operation(id,tenant_id,fingerprint,kind,actor_id) VALUES(?,?,?,?,?)",id,actor.tenantId(),hash,kind,actor.userId());
    }
    private Operation operation(Actor actor,UUID id) {
        boolean costs=canSeeCost(actor);
        return new Operation(id,db.query(movementSql()+" WHERE m.tenant_id=? AND m.operation_id=? ORDER BY m.warehouse_id",(rs,i)->movement(rs,costs),actor.tenantId(),id));
    }
    private String movementSql() { return "SELECT m.id,m.operation_id,m.warehouse_id,m.product_id,m.loss_id,m.movement_type,m.quantity,m.balance_after,m.unit,m.unit_cost,m.reason,o.actor_id,m.created_at FROM stock_movement m JOIN stock_operation o ON o.tenant_id=m.tenant_id AND o.id=m.operation_id"; }
    private Movement movement(java.sql.ResultSet rs,boolean costs) throws java.sql.SQLException {
        return new Movement(rs.getObject(1,UUID.class),rs.getObject(2,UUID.class),rs.getObject(3,UUID.class),rs.getObject(4,UUID.class),rs.getObject(5,UUID.class),rs.getString(6),rs.getBigDecimal(7),rs.getBigDecimal(8),rs.getString(9),costs ? rs.getBigDecimal(10) : null,rs.getString(11),rs.getObject(12,UUID.class),rs.getObject(13,java.time.OffsetDateTime.class));
    }
    public boolean canSeeCost(Actor actor) { return db.queryForObject("SELECT COUNT(*) FROM user_permission WHERE tenant_id=? AND user_id=? AND permission='inventory:cost'",Long.class,actor.tenantId(),actor.userId())>0; }
    public static String fingerprint(Object... values) {
        // Length-prefixed fields prevent delimiter collisions; scale differences are equivalent.
        var canonical=new StringBuilder();
        for (Object value:values) { String text=value instanceof BigDecimal decimal ? decimal.stripTrailingZeros().toPlainString() : Objects.toString(value,"<null>"); canonical.append(text.length()).append(':').append(text); }
        return TokenCodec.hash(canonical.toString());
    }
    private String reason(String value) { String reason=value.trim(); if (reason.length()<5) throw invalid("Justificativa deve conter pelo menos cinco caracteres."); return reason; }
    private Map<String,Object> page(List<?> items,long total,int page,int size) { return Map.of("items",items,"total",total,"page",page,"size",size); }
    private ResponseStatusException missing(String name) { return new ResponseStatusException(HttpStatus.NOT_FOUND,name+" não encontrado."); }
    private ResponseStatusException invalid(String text) { return new ResponseStatusException(HttpStatus.BAD_REQUEST,text); }
    private ResponseStatusException conflict(String text) { return new ResponseStatusException(HttpStatus.CONFLICT,text); }
}
