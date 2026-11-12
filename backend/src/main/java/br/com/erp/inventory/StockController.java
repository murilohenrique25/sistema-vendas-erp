package br.com.erp.inventory;

import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import static br.com.erp.inventory.StockContracts.*;

@RestController @RequestMapping("/api/v1/inventory") @Validated
public class StockController {
    private final StockService service;
    public StockController(StockService service) { this.service=service; }
    @PostMapping("/warehouses") @ResponseStatus(org.springframework.http.HttpStatus.CREATED) @PreAuthorize("hasAuthority('inventory:write')")
    public Warehouse createWarehouse(@AuthenticationPrincipal Actor actor,@Valid @RequestBody WarehouseInput input) { return service.createWarehouse(actor,input); }
    @GetMapping("/warehouses") @PreAuthorize("hasAuthority('inventory:read')")
    public Map<String,Object> warehouses(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.warehouses(actor,page,size); }
    @GetMapping("/warehouses/{id}/balances") @PreAuthorize("hasAuthority('inventory:read')")
    public Map<String,Object> balances(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="") @Size(max=200) String q,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.balances(actor,id,q,page,size); }
    @GetMapping("/warehouses/{id}/movements") @PreAuthorize("hasAuthority('inventory:read')")
    public Map<String,Object> movements(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.movements(actor,id,page,size); }
    @PostMapping("/movements") @PreAuthorize("hasAuthority('inventory:write')")
    public Operation move(@AuthenticationPrincipal Actor actor,@Valid @RequestBody MovementInput input) { return service.move(actor,input); }
    @PostMapping("/transfers") @PreAuthorize("hasAuthority('inventory:write')")
    public Operation transfer(@AuthenticationPrincipal Actor actor,@Valid @RequestBody TransferInput input) { return service.transfer(actor,input); }
}
