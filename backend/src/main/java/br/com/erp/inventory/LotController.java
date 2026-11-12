package br.com.erp.inventory;
import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;
import org.springframework.validation.annotation.Validated;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/v1/inventory/lots") @PreAuthorize("hasAuthority('inventory:read')")
public class LotController {
 private final LotService service;public LotController(LotService service){this.service=service;}
 public record Classify(@NotNull UUID requestId,@NotNull UUID warehouseId,@NotNull UUID productId,@NotBlank @Pattern(regexp="[A-Za-z0-9._/-]{1,80}") String code,LocalDate expiresOn,@NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity){}
 public record Lot(UUID id,UUID productId,String sku,String name,String code,LocalDate expiresOn,String quantity){}
 @PostMapping("/classify") @PreAuthorize("hasAuthority('inventory:read') and hasAuthority('inventory:write')") public Map<String,UUID> classify(@AuthenticationPrincipal Actor a,@Valid @RequestBody Classify input){return Map.of("lotId",service.classify(a,input));}
 @GetMapping public Map<String,Object> list(@AuthenticationPrincipal Actor a,@RequestParam UUID warehouseId,@RequestParam(required=false) UUID productId,@RequestParam(defaultValue="false") boolean expired,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(a,warehouseId,productId,expired,page,size);}
 @GetMapping("/movements") public Map<String,Object> movements(@AuthenticationPrincipal Actor a,@RequestParam UUID warehouseId,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.movements(a,warehouseId,page,size);}
}
