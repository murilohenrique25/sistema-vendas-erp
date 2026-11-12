package br.com.erp.pos;
import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import java.time.LocalDate;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static br.com.erp.pos.SaleContracts.*;
@RestController @RequestMapping("/api/v1/pos") @Validated
@PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read')")
public class SaleController {
 private final SaleService service;private final SaleCancellationService cancellation;public SaleController(SaleService service,SaleCancellationService cancellation){this.service=service;this.cancellation=cancellation;}
 @PostMapping("/sales/{id}/cancel") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('treasury:move') and hasAuthority('pos:cancel')") public Sale cancel(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody CancellationInput input){return cancellation.cancel(a,id,input);}
 @PutMapping("/terminals/{id}/warehouse") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:manage') and hasAuthority('inventory:read')") public CashContracts.Terminal bind(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody WarehouseBinding input){return service.bind(a,id,input);}
 @GetMapping("/sales") public Map<String,Object> list(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
  @RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,
  @RequestParam(defaultValue="") @Size(max=160) String query,@RequestParam(defaultValue="") @Pattern(regexp="|COMPLETED|CANCELLED|PARTIALLY_RETURNED|RETURNED") String commercialStatus,
  @RequestParam(required=false) Method method,@RequestParam(required=false) Boolean credit,@RequestParam(required=false) UUID branchId){return service.list(a,page,size,new SaleHistoryQuery.Filter(from,to,query,commercialStatus,method,credit,branchId));}

 @GetMapping("/sales/{id}") public Sale get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.get(a,id);}
 @PostMapping("/sales") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('treasury:move') and hasAuthority('pos:sell')") public Sale complete(@AuthenticationPrincipal Actor a,@Valid @RequestBody Input input){return service.complete(a,input);}
}
