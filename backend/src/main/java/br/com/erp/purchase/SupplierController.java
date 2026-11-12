package br.com.erp.purchase;

import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/v1/suppliers") @Validated
public class SupplierController {
    private final SupplierService service;
    public SupplierController(SupplierService service) { this.service=service; }
    @GetMapping @PreAuthorize("hasAuthority('supplier:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="") @Size(max=200) String q,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.list(actor,q,page,size); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('supplier:read')")
    public SupplierService.View get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) { return service.get(actor,id); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('supplier:write')")
    public SupplierService.View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody SupplierService.Input input) { return service.create(actor,input); }
    @PutMapping("/{id}") @PreAuthorize("hasAuthority('supplier:write')")
    public SupplierService.View update(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody SupplierService.Update input) { return service.update(actor,id,input); }
}
