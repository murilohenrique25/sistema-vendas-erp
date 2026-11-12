package br.com.erp.inventory;

import br.com.erp.auth.*;
import br.com.erp.shared.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @Validated @RequestMapping("/api/v1/inventory/counts") @PreAuthorize("hasAuthority('inventory:read')")
public class PhysicalInventoryController {
 private final PhysicalInventoryService service;
 public PhysicalInventoryController(PhysicalInventoryService service){this.service=service;}
 public record Open(@NotNull UUID requestId,@NotNull UUID warehouseId,@NotBlank @Size(min=5,max=500) String note,@NotEmpty @Size(max=100) List<@NotNull UUID> productIds){}
 public record CountLine(@NotNull UUID productId,@NotNull @DecimalMin("0") @Digits(integer=13,fraction=6) BigDecimal quantity,@DecimalMin("0") @Digits(integer=13,fraction=6) BigDecimal unitCost){}
 public record Count(@NotNull UUID requestId,@NotNull @Min(0) Long version,@NotEmpty @Size(max=100) List<@NotNull @Valid CountLine> lines){}
 public record Action(@NotNull UUID requestId,@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note){}
 public record Line(UUID productId,String sku,String name,String unit,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal expectedQuantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal countedQuantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal difference,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unitCost,UUID operationId){}
 public record View(UUID id,UUID warehouseId,UUID branchId,String status,long version,String note,UUID createdBy,OffsetDateTime createdAt,String reviewNote,List<Line> lines){}
 @GetMapping public Map<String,Object> list(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(a,page,size);}
 @GetMapping("/{id}") public View get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.get(a,id);}
 @PostMapping @PreAuthorize("hasAuthority('inventory:read') and hasAuthority('inventory:write')") public View open(@AuthenticationPrincipal Actor a,@Valid @RequestBody Open input){return service.open(a,input);}
 @PutMapping("/{id}/counts") @PreAuthorize("hasAuthority('inventory:read') and hasAuthority('inventory:write')") public View count(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody Count input){return service.count(a,id,input);}
 @PostMapping("/{id}/{action:submit|approve|cancel}") @PreAuthorize("hasAuthority('inventory:read') and hasAuthority('inventory:write')") public View action(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@PathVariable String action,@Valid @RequestBody Action input){return service.action(a,id,action,input);}
}

@Service
class PhysicalInventoryService {
 private final JdbcTemplate db;private final BranchAccess access;private final WorkflowSupport flow;private final StockService stock;private final Audit audit;
 PhysicalInventoryService(JdbcTemplate db,BranchAccess access,WorkflowSupport flow,StockService stock,Audit audit){this.db=db;this.access=access;this.flow=flow;this.stock=stock;this.audit=audit;}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public PhysicalInventoryController.View get(Actor a,UUID id){
  var headers=db.query("SELECT * FROM physical_inventory WHERE tenant_id=? AND id=?",(r,i)->new PhysicalInventoryController.View(id,r.getObject("warehouse_id",UUID.class),r.getObject("branch_id",UUID.class),r.getString("status"),r.getLong("version"),r.getString("note"),r.getObject("created_by",UUID.class),r.getObject("created_at",OffsetDateTime.class),r.getString("review_note"),List.of()),a.tenantId(),id);
  if(headers.isEmpty())throw flow.missing("Inventário não encontrado.");var h=headers.getFirst();access.requireBranch(a,h.branchId());
  boolean costs=stock.canSeeCost(a);var lines=db.query("SELECT * FROM physical_inventory_line WHERE tenant_id=? AND inventory_id=? ORDER BY product_id",(r,i)->{var counted=r.getBigDecimal("counted_quantity");var expected=r.getBigDecimal("expected_quantity");return new PhysicalInventoryController.Line(r.getObject("product_id",UUID.class),r.getString("sku"),r.getString("name"),r.getString("unit"),expected,counted,counted==null?null:counted.subtract(expected),costs?r.getBigDecimal("unit_cost"):null,r.getObject("operation_id",UUID.class));},a.tenantId(),id);
  return new PhysicalInventoryController.View(id,h.warehouseId(),h.branchId(),h.status(),h.version(),h.note(),h.createdBy(),h.createdAt(),h.reviewNote(),lines);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor a,int page,int size){String where=" WHERE h.tenant_id=? AND "+BranchAccess.predicate("h.branch_id");var ids=db.query("SELECT h.id FROM physical_inventory h"+where+" ORDER BY h.created_at DESC,h.id LIMIT ? OFFSET ?",(r,i)->r.getObject(1,UUID.class),a.tenantId(),a.tenantId(),a.userId(),size,(long)page*size);return Map.of("items",ids.stream().map(id->get(a,id)).toList(),"total",db.queryForObject("SELECT COUNT(*) FROM physical_inventory h"+where,Long.class,a.tenantId(),a.tenantId(),a.userId()),"page",page,"size",size);}
 @Transactional public PhysicalInventoryController.View open(Actor a,PhysicalInventoryController.Open input){flow.lock(a);if(input.note().trim().length()<5)throw flow.bad("Justificativa deve ter cinco caracteres.");access.requireWarehouse(a,input.warehouseId());var ids=input.productIds().stream().sorted(Comparator.comparing(UUID::toString)).toList();if(new HashSet<>(ids).size()!=ids.size())throw flow.bad("Produtos repetidos.");String hash=flow.hash("COUNT_OPEN",input.warehouseId(),input.note().trim(),ids);UUID replay=flow.replay(a,input.requestId(),"COUNT_OPEN",hash);if(replay!=null)return get(a,replay);UUID id=UUID.randomUUID();var branches=db.query("SELECT branch_id FROM warehouse WHERE tenant_id=? AND id=? AND active=TRUE",(r,i)->r.getObject(1,UUID.class),a.tenantId(),input.warehouseId());if(branches.isEmpty())throw flow.missing("Depósito ativo não encontrado.");UUID branch=branches.getFirst();
  db.update("INSERT INTO physical_inventory(id,tenant_id,warehouse_id,branch_id,status,note,created_by) VALUES(?,?,?,?,'COUNTING',?,?)",id,a.tenantId(),input.warehouseId(),branch,input.note().trim(),a.userId());
  for(UUID product:ids){var snapshot=stock.snapshotForLoss(a,input.warehouseId(),product);var p=db.queryForMap("SELECT sku,name,unit FROM product WHERE tenant_id=? AND id=?",a.tenantId(),product);long movements=db.queryForObject("SELECT COUNT(*) FROM stock_movement WHERE tenant_id=? AND warehouse_id=? AND product_id=?",Long.class,a.tenantId(),input.warehouseId(),product);db.update("INSERT INTO physical_inventory_line(tenant_id,inventory_id,product_id,sku,name,unit,expected_quantity,movement_count) VALUES(?,?,?,?,?,?,?,?)",a.tenantId(),id,product,p.get("sku"),p.get("name"),p.get("unit"),snapshot.quantity(),movements);}
  flow.record(a,input.requestId(),"COUNT_OPEN",hash,id);audit.record(a,"INVENTORY_OPENED",id);return get(a,id);
 }
 @Transactional public PhysicalInventoryController.View count(Actor a,UUID id,PhysicalInventoryController.Count input){flow.lock(a);var h=get(a,id);var fields=new ArrayList<Object>(List.of("COUNT",id,input.version()));for(var l:input.lines().stream().sorted(Comparator.comparing(PhysicalInventoryController.CountLine::productId)).toList())Collections.addAll(fields,l.productId(),l.quantity(),l.unitCost());String hash=flow.hash(fields.toArray());UUID replay=flow.replay(a,input.requestId(),"COUNT",hash);if(replay!=null)return get(a,replay);if(!h.status().equals("COUNTING")||h.version()!=input.version())throw flow.conflict("Inventário alterado ou fechado para contagem.");var seen=new HashSet<UUID>();for(var l:input.lines()){if(!seen.add(l.productId()))throw flow.bad("Produto repetido.");if(l.unitCost()!=null)flow.require(a,"inventory:cost");int changed=db.update("UPDATE physical_inventory_line SET counted_quantity=?,unit_cost=CASE WHEN ? THEN ? ELSE unit_cost END WHERE tenant_id=? AND inventory_id=? AND product_id=?",l.quantity(),stock.canSeeCost(a),l.unitCost(),a.tenantId(),id,l.productId());if(changed!=1)throw flow.bad("Produto não pertence ao inventário.");}db.update("UPDATE physical_inventory SET version=version+1 WHERE tenant_id=? AND id=?",a.tenantId(),id);flow.record(a,input.requestId(),"COUNT",hash,id);audit.record(a,"INVENTORY_COUNTED",id);return get(a,id);}
 @Transactional public PhysicalInventoryController.View action(Actor a,UUID id,String action,PhysicalInventoryController.Action input){flow.lock(a);if(input.note().trim().length()<5)throw flow.bad("Justificativa deve ter cinco caracteres.");var h=get(a,id);if(action.equals("approve"))flow.require(a,"inventory:approve");String hash=flow.hash("COUNT_ACTION",id,action,input.version(),input.note().trim());UUID replay=flow.replay(a,input.requestId(),"COUNT_ACTION",hash);if(replay!=null)return get(a,replay);if(h.version()!=input.version())throw flow.conflict("Inventário alterado. Atualize.");String next;
  switch(action){
   case "submit" -> {if(!h.status().equals("COUNTING")||h.lines().stream().anyMatch(l->l.countedQuantity()==null))throw flow.conflict("Conte todos os itens antes de enviar.");next="SUBMITTED";}
   case "cancel" -> {if(!List.of("COUNTING","SUBMITTED").contains(h.status()))throw flow.conflict("Inventário encerrado.");next="CANCELLED";}
   case "approve" -> {if(!h.status().equals("SUBMITTED"))throw flow.conflict("Envie a contagem antes de aprovar.");if(h.createdBy().equals(a.userId()))throw flow.conflict("Outro responsável deve aprovar o inventário.");for(var l:h.lines()){long movements=db.queryForObject("SELECT movement_count FROM physical_inventory_line WHERE tenant_id=? AND inventory_id=? AND product_id=?",Long.class,a.tenantId(),id,l.productId());BigDecimal cost=db.queryForObject("SELECT unit_cost FROM physical_inventory_line WHERE tenant_id=? AND inventory_id=? AND product_id=?",BigDecimal.class,a.tenantId(),id,l.productId());UUID operation=stock.adjustInventory(a,id,h.warehouseId(),l.productId(),l.unit(),l.expectedQuantity(),l.countedQuantity(),movements,cost);if(operation!=null)db.update("UPDATE physical_inventory_line SET operation_id=? WHERE tenant_id=? AND inventory_id=? AND product_id=?",operation,a.tenantId(),id,l.productId());}next="APPROVED";}
   default -> throw flow.bad("Ação inválida.");
  }
  db.update("UPDATE physical_inventory SET status=?,version=version+1,reviewed_by=?,review_note=?,reviewed_at=CURRENT_TIMESTAMP WHERE tenant_id=? AND id=?",next,a.userId(),input.note().trim(),a.tenantId(),id);flow.record(a,input.requestId(),"COUNT_ACTION",hash,id);audit.record(a,"INVENTORY_"+next,id);return get(a,id);
 }
}
