package br.com.erp.pos;
import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static br.com.erp.pos.SaleReturnContracts.*;
@RestController @RequestMapping("/api/v1/pos/sales/{saleId}/returns") @Validated
@PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read')")
public class SaleReturnController {
 private final SaleReturnService service;public SaleReturnController(SaleReturnService service){this.service=service;}
 @GetMapping("/credit-receipts") public Map<String,Object> receipts(@AuthenticationPrincipal Actor a,@PathVariable UUID saleId,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.creditReceipts(a,saleId,page,size);}
 @GetMapping public Map<String,Object> list(@AuthenticationPrincipal Actor a,@PathVariable UUID saleId,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(a,saleId,page,size);}
 @GetMapping("/{id}") public Return get(@AuthenticationPrincipal Actor a,@PathVariable UUID saleId,@PathVariable UUID id){return service.get(a,saleId,id);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('treasury:move') and hasAuthority('pos:return')") public Return create(@AuthenticationPrincipal Actor a,@PathVariable UUID saleId,@Valid @RequestBody Input input){return service.create(a,saleId,input);}
}
