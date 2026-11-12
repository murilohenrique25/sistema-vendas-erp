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
import static br.com.erp.purchase.PurchaseContracts.*;

@RestController @RequestMapping("/api/v1/purchase-orders") @Validated
public class PurchaseController {
    private final PurchaseService service;
    public PurchaseController(PurchaseService service){this.service=service;}
    @GetMapping @PreAuthorize("hasAuthority('purchase:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(actor,page,size);}
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('purchase:read')") public Order get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id){return service.get(actor,id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('purchase:read') and hasAuthority('purchase:write')") public Order create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input){return service.create(actor,input);}
    @PostMapping("/{id}/receipts") @PreAuthorize("hasAuthority('purchase:read') and hasAuthority('purchase:receive')") public Receipt receive(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody ReceiptInput input){return service.receive(actor,id,input);}
    @GetMapping("/{id}/receipts") @PreAuthorize("hasAuthority('purchase:read')") public Map<String,Object> receipts(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.receipts(actor,id,page,size);}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('purchase:read') and hasAuthority('purchase:write')") public Order cancel(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Cancellation input){return service.cancel(actor,id,input);}
}
