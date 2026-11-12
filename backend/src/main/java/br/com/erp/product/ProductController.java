package br.com.erp.product;

import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/v1/products") @Validated
public class ProductController {
    private final BranchPricing prices;private final br.com.erp.auth.BranchAccess access;private final ProductService service;private final ProductMaintenanceService maintenance;
    public ProductController(ProductService service,ProductMaintenanceService maintenance,BranchPricing prices,br.com.erp.auth.BranchAccess access) { this.service=service;this.maintenance=maintenance;this.prices=prices;this.access=access; }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('product:read') and hasAuthority('product:write')")
    public ProductService.View update(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody ProductMaintenanceService.Update input){return maintenance.update(actor,id,input);}
    @PutMapping("/{id}/active") @PreAuthorize("hasAuthority('product:read') and hasAuthority('product:write')")
    public ProductService.View active(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody ProductMaintenanceService.Active input){return maintenance.active(actor,id,input);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('product:write')")
    public ProductService.View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody ProductService.Input input) { return service.create(actor,input); }
    @GetMapping @PreAuthorize("hasAuthority('product:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="") @Size(max=200) String q,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,@RequestParam(required=false) Boolean active,@RequestParam(required=false) UUID branchId) {var result=service.list(actor,q,page,size,active);if(branchId==null)return result;access.requireBranch(actor,branchId);var items=((List<?>)result.get("items")).stream().map(v->priced(actor,branchId,(ProductService.View)v)).toList();return Map.of("items",items,"total",result.get("total"),"page",page,"size",size); }
    private ProductService.View priced(Actor a,UUID branch,ProductService.View p){return new ProductService.View(p.id(),p.sku(),p.name(),p.description(),p.unit(),prices.price(a,branch,p.id(),p.salePrice()),p.active(),p.version(),p.barcodes());}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('product:read')")
    public ProductService.View get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) { return service.get(actor,id); }
    @GetMapping("/lookup") @PreAuthorize("hasAuthority('product:read')")
    public ProductService.View lookup(@AuthenticationPrincipal Actor actor,@RequestParam @Pattern(regexp="[A-Za-z0-9._-]{1,60}") String code,@RequestParam(defaultValue="GTIN") Barcode.Kind kind) { return service.lookup(actor,code,kind); }
}
