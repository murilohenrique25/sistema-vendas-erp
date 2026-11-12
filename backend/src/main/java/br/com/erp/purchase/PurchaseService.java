package br.com.erp.purchase;

import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import java.math.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.purchase.PurchaseContracts.*;

@Service
public class PurchaseService {
    private final JdbcTemplate db;private final StockService stock;private final BranchAccess access;private final Audit audit;
    public PurchaseService(JdbcTemplate db,StockService stock,BranchAccess access,Audit audit) { this.db=db;this.stock=stock;this.access=access;this.audit=audit; }
    private ResponseStatusException error(HttpStatus status,String message) { return new ResponseStatusException(status,message); }
    private Header row(java.sql.ResultSet rs,int i) throws java.sql.SQLException { return new Header(rs.getObject("id",UUID.class),rs.getObject("branch_id",UUID.class),rs.getObject("supplier_id",UUID.class),rs.getString("supplier_name"),rs.getString("notes"),rs.getString("status"),rs.getLong("version"),rs.getBigDecimal("total"),rs.getObject("created_by",UUID.class),rs.getObject("created_at",java.time.OffsetDateTime.class),rs.getString("cancellation_note")); }
    private Header header(Actor actor,UUID id,boolean lock) {
        var rows=db.query("SELECT * FROM purchase_order WHERE tenant_id=? AND id=?"+(lock?" FOR UPDATE":""),this::row,actor.tenantId(),id);
        if(rows.isEmpty()) throw error(HttpStatus.NOT_FOUND,"Pedido não encontrado.");var value=rows.getFirst();access.requireBranch(actor,value.branchId());return value;
    }
    private List<Line> lines(Actor actor,UUID id) { return db.query("SELECT * FROM purchase_order_line WHERE tenant_id=? AND order_id=? ORDER BY product_id",(rs,i)->new Line(rs.getObject("id",UUID.class),rs.getObject("product_id",UUID.class),rs.getString("sku"),rs.getString("name"),rs.getString("unit"),rs.getBigDecimal("quantity"),rs.getBigDecimal("received_quantity"),rs.getBigDecimal("unit_cost"),rs.getBigDecimal("total")),actor.tenantId(),id); }
    @Transactional(readOnly=true) public Order get(Actor actor,UUID id) { return new Order(header(actor,id,false),lines(actor,id)); }
    @Transactional(readOnly=true) public Map<String,Object> list(Actor actor,int page,int size) {
        String where=" WHERE o.tenant_id=? AND "+BranchAccess.predicate("o.branch_id");
        var items=db.query("SELECT o.* FROM purchase_order o"+where+" ORDER BY o.created_at DESC,o.id LIMIT ? OFFSET ?",this::row,actor.tenantId(),actor.tenantId(),actor.userId(),size,(long)page*size);
        return Map.of("items",items,"total",db.queryForObject("SELECT COUNT(*) FROM purchase_order o"+where,Long.class,actor.tenantId(),actor.tenantId(),actor.userId()),"page",page,"size",size);
    }
    @Transactional public Order create(Actor actor,Input input) {
        access.requireBranch(actor,input.branchId());
        // Serialize request-key creation only; receiving locks the order, then products in stable order.
        db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,actor.tenantId());
        var items=input.items().stream().sorted(Comparator.comparing(item->item.productId().toString())).toList();
        if(items.stream().map(Item::productId).distinct().count()!=items.size()) throw error(HttpStatus.BAD_REQUEST,"Produto repetido no pedido.");
        var fields=new ArrayList<Object>(List.of("PURCHASE_ORDER",input.branchId(),input.supplierId(),input.notes().trim()));
        for(var item:items) { fields.add(item.productId());fields.add(item.quantity());fields.add(item.unitCost()); }
        String hash=StockService.fingerprint(fields.toArray());
        var existing=db.query("SELECT id,fingerprint FROM purchase_order WHERE tenant_id=? AND request_id=?",(rs,i)->Map.entry(rs.getObject(1,UUID.class),rs.getString(2)),actor.tenantId(),input.requestId());
        if(!existing.isEmpty()) { if(!existing.getFirst().getValue().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de pedido já utilizado com outros dados.");return get(actor,existing.getFirst().getKey()); }
        if(db.queryForObject("SELECT COUNT(*) FROM branch WHERE tenant_id=? AND id=? AND active=TRUE",Long.class,actor.tenantId(),input.branchId())==0)throw error(HttpStatus.NOT_FOUND,"Filial ativa não encontrada.");
        var suppliers=db.query("SELECT name FROM supplier WHERE tenant_id=? AND id=? AND active=TRUE",(rs,i)->rs.getString(1),actor.tenantId(),input.supplierId());
        if(suppliers.isEmpty())throw error(HttpStatus.NOT_FOUND,"Fornecedor ativo não encontrado.");
        BigDecimal total=items.stream().map(item->item.quantity().multiply(item.unitCost()).setScale(2,RoundingMode.HALF_UP)).reduce(BigDecimal.ZERO,BigDecimal::add);
        if(total.compareTo(new BigDecimal("99999999999999999.99"))>0)throw error(HttpStatus.BAD_REQUEST,"Total do pedido excede o limite monetário.");
        UUID id=UUID.randomUUID();
        db.update("INSERT INTO purchase_order(id,tenant_id,request_id,fingerprint,branch_id,supplier_id,supplier_name,notes,total,status,created_by) VALUES(?,?,?,?,?,?,?,?,?,'OPEN',?)",id,actor.tenantId(),input.requestId(),hash,input.branchId(),input.supplierId(),suppliers.getFirst(),input.notes().trim(),total,actor.userId());
        for(var item:items) {
            var products=db.query("SELECT sku,name,unit FROM product WHERE tenant_id=? AND id=? AND active=TRUE",(rs,i)->List.of(rs.getString(1),rs.getString(2),rs.getString(3)),actor.tenantId(),item.productId());
            if(products.isEmpty())throw error(HttpStatus.NOT_FOUND,"Produto ativo não encontrado.");var product=products.getFirst();
            db.update("INSERT INTO purchase_order_line(id,tenant_id,order_id,product_id,sku,name,unit,quantity,unit_cost,total) VALUES(?,?,?,?,?,?,?,?,?,?)",UUID.randomUUID(),actor.tenantId(),id,item.productId(),product.get(0),product.get(1),product.get(2),item.quantity(),item.unitCost(),item.quantity().multiply(item.unitCost()).setScale(2,RoundingMode.HALF_UP));
        }
        audit.record(actor,"PURCHASE_ORDER_CREATED",id);return get(actor,id);
    }
    @Transactional public Receipt receive(Actor actor,UUID id,ReceiptInput input) {
        Header order=header(actor,id,true);access.requireWarehouse(actor,input.warehouseId());
        String note=input.note().trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Descreva o recebimento.");
        var entries=input.items().stream().sorted(Comparator.comparing(item->item.orderLineId().toString())).toList();
        if(entries.stream().map(ReceivedItem::orderLineId).distinct().count()!=entries.size())throw error(HttpStatus.BAD_REQUEST,"Item repetido no recebimento.");
        var fields=new ArrayList<Object>(List.of("PURCHASE_RECEIPT",id,input.warehouseId(),note));
        for(var item:entries){fields.add(item.orderLineId());fields.add(item.quantity());}String hash=StockService.fingerprint(fields.toArray());
        var existing=db.query("SELECT fingerprint FROM purchase_receipt WHERE tenant_id=? AND id=?",(rs,i)->rs.getString(1),actor.tenantId(),input.receiptId());
        if(!existing.isEmpty()){if(!existing.getFirst().equals(hash))throw error(HttpStatus.CONFLICT,"Identificador de recebimento já utilizado com outros dados.");return receipt(actor,input.receiptId());}
        if(!List.of("OPEN","PARTIAL").contains(order.status()))throw error(HttpStatus.CONFLICT,"Pedido encerrado não aceita recebimento.");
        if(db.queryForObject("SELECT COUNT(*) FROM warehouse WHERE tenant_id=? AND id=? AND branch_id=? AND active=TRUE",Long.class,actor.tenantId(),input.warehouseId(),order.branchId())==0)throw error(HttpStatus.BAD_REQUEST,"Depósito deve estar ativo e pertencer à filial do pedido.");
        Map<UUID,Line> ordered=new HashMap<>();for(var line:lines(actor,id))ordered.put(line.id(),line);
        for(var entry:entries){var line=ordered.get(entry.orderLineId());if(line==null)throw error(HttpStatus.NOT_FOUND,"Item não pertence ao pedido.");if(line.receivedQuantity().add(entry.quantity()).compareTo(line.quantity())>0)throw error(HttpStatus.CONFLICT,"Recebimento excede a quantidade pendente.");}
        db.update("INSERT INTO purchase_receipt(id,tenant_id,order_id,warehouse_id,fingerprint,note,created_by) VALUES(?,?,?,?,?,?,?)",input.receiptId(),actor.tenantId(),id,input.warehouseId(),hash,note,actor.userId());
        // Globally consistent product order prevents deadlocks across multi-item receipts.
        var sorted=entries.stream().sorted(Comparator.comparing(entry->ordered.get(entry.orderLineId()).productId().toString())).toList();
        for(var entry:sorted){var line=ordered.get(entry.orderLineId());
            UUID operation=UUID.nameUUIDFromBytes(("PURCHASE_RECEIPT|"+actor.tenantId()+"|"+input.receiptId()+"|"+line.id()).getBytes(StandardCharsets.UTF_8));
            stock.receivePurchase(actor,operation,input.receiptId(),line.id(),input.warehouseId(),line.productId(),line.unit(),entry.quantity(),line.unitCost(),"Recebimento pedido "+id);
            db.update("INSERT INTO purchase_receipt_line(tenant_id,order_id,receipt_id,order_line_id,operation_id,quantity,amount) VALUES(?,?,?,?,?,?,?)",actor.tenantId(),id,input.receiptId(),line.id(),operation,entry.quantity(),line.receivedQuantity().add(entry.quantity()).multiply(line.unitCost()).setScale(2,RoundingMode.HALF_UP).subtract(line.receivedQuantity().multiply(line.unitCost()).setScale(2,RoundingMode.HALF_UP)));
            db.update("UPDATE purchase_order_line SET received_quantity=received_quantity+? WHERE tenant_id=? AND order_id=? AND id=?",entry.quantity(),actor.tenantId(),id,line.id());
        }
        Long pending=db.queryForObject("SELECT COUNT(*) FROM purchase_order_line WHERE tenant_id=? AND order_id=? AND received_quantity<quantity",Long.class,actor.tenantId(),id);
        db.update("UPDATE purchase_order SET status=?,version=version+1 WHERE tenant_id=? AND id=?",pending==0?"RECEIVED":"PARTIAL",actor.tenantId(),id);
        audit.record(actor,"PURCHASE_RECEIVED",input.receiptId());return receipt(actor,input.receiptId());
    }
    @Transactional public Order cancel(Actor actor,UUID id,Cancellation input) {
        Header order=header(actor,id,true);String note=input.note().trim();if(note.length()<5)throw error(HttpStatus.BAD_REQUEST,"Justifique o cancelamento.");
        if(order.status().equals("CANCELLED") && note.equals(order.cancellationNote()))return get(actor,id);
        if(order.version()!=input.version())throw error(HttpStatus.CONFLICT,"Pedido alterado por outro acesso. Atualize.");
        if(!List.of("OPEN","PARTIAL").contains(order.status()))throw error(HttpStatus.CONFLICT,"Pedido encerrado não pode ser cancelado.");
        db.update("UPDATE purchase_order SET status='CANCELLED',cancellation_note=?,version=version+1 WHERE tenant_id=? AND id=?",note,actor.tenantId(),id);
        audit.record(actor,"PURCHASE_ORDER_CANCELLED",id);return get(actor,id);
    }
    private Receipt receipt(Actor actor,UUID id){
        var rows=db.query("SELECT * FROM purchase_receipt WHERE tenant_id=? AND id=?",(rs,i)->new Receipt(rs.getObject("id",UUID.class),rs.getObject("order_id",UUID.class),rs.getObject("warehouse_id",UUID.class),rs.getString("note"),rs.getObject("created_by",UUID.class),rs.getObject("created_at",java.time.OffsetDateTime.class),db.query("SELECT order_line_id,operation_id,quantity,amount FROM purchase_receipt_line WHERE tenant_id=? AND receipt_id=? ORDER BY order_line_id",(r,j)->new ReceiptLine(r.getObject(1,UUID.class),r.getObject(2,UUID.class),r.getBigDecimal(3),r.getBigDecimal(4)),actor.tenantId(),id)),actor.tenantId(),id);return rows.getFirst();
    }
    @Transactional(readOnly=true) public Map<String,Object> receipts(Actor actor,UUID id,int page,int size){
        header(actor,id,false);var ids=db.query("SELECT id FROM purchase_receipt WHERE tenant_id=? AND order_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",(rs,i)->rs.getObject(1,UUID.class),actor.tenantId(),id,size,(long)page*size);
        return Map.of("items",ids.stream().map(value->receipt(actor,value)).toList(),"total",db.queryForObject("SELECT COUNT(*) FROM purchase_receipt WHERE tenant_id=? AND order_id=?",Long.class,actor.tenantId(),id),"page",page,"size",size);
    }
}
