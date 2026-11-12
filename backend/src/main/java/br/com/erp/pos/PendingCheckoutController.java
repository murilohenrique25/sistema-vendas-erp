package br.com.erp.pos;

import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import br.com.erp.pos.SaleContracts.ItemInput;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @Validated @RequestMapping("/api/v1/pos/pending-checkouts")
@PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:sell')")
public class PendingCheckoutController {
 private final PendingCheckoutService service;
 public PendingCheckoutController(PendingCheckoutService service){this.service=service;}
 public record Create(@NotNull UUID requestId,@NotNull UUID sessionId,@NotEmpty @Size(max=100) List<@NotNull @Valid ItemInput> items){}
 public record Discard(@NotNull UUID requestId,@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note){}
 public record Complete(@NotNull @Min(0) Long version,@NotNull @Valid SaleContracts.Input sale){}
 public record View(UUID id,UUID sessionId,UUID branchId,UUID createdBy,OffsetDateTime createdAt,OffsetDateTime updatedAt,String status,long version,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total,List<CartPricing.Line> items,UUID saleId,String discardNote){}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public View create(@AuthenticationPrincipal Actor a,@Valid @RequestBody Create input){return service.create(a,input);}
 @GetMapping("/{id}") public View get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.get(a,id);}
 @GetMapping public Map<String,Object> list(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(a,page,size);}
 @PostMapping("/{id}/discard") public View discard(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody Discard input){return service.discard(a,id,input);}
 @PostMapping("/{id}/complete") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('treasury:move') and hasAuthority('pos:sell')") public SaleContracts.Sale complete(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody Complete input){return service.complete(a,id,input);}
}

@Service
class PendingCheckoutService {
 private final JdbcTemplate db;private final CartPricing pricing;private final CashService cash;private final BranchAccess access;private final SaleService sales;private final Audit audit;
 PendingCheckoutService(JdbcTemplate db,CartPricing pricing,CashService cash,BranchAccess access,SaleService sales,Audit audit){this.db=db;this.pricing=pricing;this.cash=cash;this.access=access;this.sales=sales;this.audit=audit;}
 private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
 private void lock(Actor a){db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());}
 private String fingerprint(PendingCheckoutController.Create input){var fields=new ArrayList<Object>();fields.add(input.sessionId());for(var i:input.items())Collections.addAll(fields,i.productId(),i.quantity(),i.expectedUnitPrice(),CartPricing.discount(i));return StockService.fingerprint(fields.toArray());}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public PendingCheckoutController.View get(Actor a,UUID id){
  var rows=db.query("SELECT * FROM pos_pending_checkout WHERE tenant_id=? AND id=?",(r,i)->new PendingCheckoutController.View(r.getObject("id",UUID.class),r.getObject("session_id",UUID.class),r.getObject("branch_id",UUID.class),r.getObject("created_by",UUID.class),r.getObject("created_at",OffsetDateTime.class),r.getObject("updated_at",OffsetDateTime.class),r.getString("status"),r.getLong("version"),r.getBigDecimal("total"),pricing.decode(r.getString("items_json")),r.getObject("sale_id",UUID.class),r.getString("discard_note")),a.tenantId(),id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Preparação não encontrada.");var v=rows.getFirst();access.requireBranch(a,v.branchId());if(!v.createdBy().equals(a.userId())&&!pricing.has(a,"pos:supervise"))throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Preparação não encontrada.");return v;
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public Map<String,Object> list(Actor a,int page,int size){
  String where=" WHERE p.tenant_id=? AND "+BranchAccess.predicate("p.branch_id")+(pricing.has(a,"pos:supervise")?"":" AND p.created_by=?");var args=new ArrayList<Object>(List.of(a.tenantId(),a.tenantId(),a.userId()));if(!pricing.has(a,"pos:supervise"))args.add(a.userId());long total=db.queryForObject("SELECT COUNT(*) FROM pos_pending_checkout p"+where,Long.class,args.toArray());args.add(size);args.add((long)page*size);var ids=db.query("SELECT p.id FROM pos_pending_checkout p"+where+" ORDER BY p.created_at DESC,p.id LIMIT ? OFFSET ?",(r,i)->r.getObject(1,UUID.class),args.toArray());return Map.of("items",ids.stream().map(id->get(a,id)).toList(),"total",total,"page",page,"size",size);
 }
 @Transactional public PendingCheckoutController.View create(Actor a,PendingCheckoutController.Create input){
  lock(a);String hash=fingerprint(input);var old=db.query("SELECT id,fingerprint FROM pos_pending_checkout WHERE tenant_id=? AND request_id=?",(r,i)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),a.tenantId(),input.requestId());if(!old.isEmpty()){if(!old.getFirst().getValue().equals(hash))throw conflict("Identificador reutilizado com outros dados.");return get(a,old.getFirst().getKey());}
  var s=cash.get(a,input.sessionId());pricing.owner(a,s);if(!s.status().equals("OPEN"))throw conflict("Preparação exige caixa aberto.");var t=cash.terminal(a,s.terminalId());if(!t.active()||t.warehouseId()==null)throw conflict("Configure um terminal ativo com depósito.");access.requireWarehouse(a,t.warehouseId());var quote=pricing.quote(a,s.branchId(),input.items());UUID id=UUID.randomUUID();var now=OffsetDateTime.now(ZoneOffset.UTC);
  db.update("INSERT INTO pos_pending_checkout(id,tenant_id,request_id,fingerprint,session_id,branch_id,created_by,created_at,updated_at,items_json,cart_hash,total,status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?, 'PENDING')",id,a.tenantId(),input.requestId(),hash,s.id(),s.branchId(),a.userId(),now,now,pricing.encode(quote.items()),quote.hash(),quote.total());audit.record(a,"POS_CHECKOUT_PREPARED",id);return get(a,id);
 }
 @Transactional public PendingCheckoutController.View discard(Actor a,UUID id,PendingCheckoutController.Discard input){
  lock(a);var v=get(a,id);String hash=StockService.fingerprint(id,input.version(),input.note().trim());if(v.status().equals("DISCARDED")){boolean replay=db.queryForObject("SELECT COUNT(*) FROM pos_pending_checkout WHERE tenant_id=? AND id=? AND discard_request_id=? AND discard_fingerprint=?",Long.class,a.tenantId(),id,input.requestId(),hash)==1;if(replay)return v;throw conflict("Preparação já descartada.");}
  if(!v.status().equals("PENDING")||v.version()!=input.version())throw conflict("Preparação alterada. Atualize.");if(db.queryForObject("SELECT COUNT(*) FROM pos_pending_checkout WHERE tenant_id=? AND discard_request_id=?",Long.class,a.tenantId(),input.requestId())>0)throw conflict("Identificador de descarte reutilizado.");
  db.update("UPDATE pos_pending_checkout SET status='DISCARDED',version=version+1,discard_request_id=?,discard_fingerprint=?,discard_note=?,updated_at=? WHERE tenant_id=? AND id=?",input.requestId(),hash,input.note().trim(),OffsetDateTime.now(ZoneOffset.UTC),a.tenantId(),id);audit.record(a,"POS_CHECKOUT_DISCARDED",id);return get(a,id);
 }
 @Transactional public SaleContracts.Sale complete(Actor a,UUID id,PendingCheckoutController.Complete input){
  lock(a);var v=get(a,id);var sale=input.sale();if(!sale.requestId().equals(id)||!sale.sessionId().equals(v.sessionId()))throw conflict("Use a identidade da preparação e seu caixa na conclusão.");if(sale.credit()!=null||sale.suspensionId()!=null||sale.suspensionVersion()!=null)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Preparação de pagamento não aceita crediário ou suspensão vinculada.");
  if(v.status().equals("COMPLETED")){var result=sales.complete(a,sale);if(!result.id().equals(v.saleId()))throw conflict("Venda vinculada divergente.");return result;}
  if(!v.status().equals("PENDING")||v.version()!=input.version())throw conflict("Preparação alterada. Atualize.");var quote=pricing.quote(a,v.branchId(),sale.items());String savedHash=db.queryForObject("SELECT cart_hash FROM pos_pending_checkout WHERE tenant_id=? AND id=?",String.class,a.tenantId(),id);if(!quote.hash().equals(savedHash))throw conflict("Carrinho mudou. Descarte a preparação e prepare novamente.");
  // Existing sale transaction rechecks price, permissions, discounts, stock and actual payment proof.
  var result=sales.complete(a,sale);db.update("UPDATE pos_pending_checkout SET status='COMPLETED',version=version+1,sale_id=?,updated_at=? WHERE tenant_id=? AND id=?",result.id(),OffsetDateTime.now(ZoneOffset.UTC),a.tenantId(),id);audit.record(a,"POS_CHECKOUT_COMPLETED",id);return result;
 }
}
