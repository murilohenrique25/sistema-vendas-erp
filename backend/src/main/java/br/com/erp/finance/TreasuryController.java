package br.com.erp.finance;
import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static br.com.erp.finance.TreasuryContracts.*;
@RestController @RequestMapping("/api/v1/treasury") @Validated
public class TreasuryController {
 private final TreasuryService service;public TreasuryController(TreasuryService service){this.service=service;}
 @GetMapping("/accounts") @PreAuthorize("hasAuthority('treasury:read')") public Map<String,Object> list(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.list(a,page,size);}
 @GetMapping("/accounts/{id}") @PreAuthorize("hasAuthority('treasury:read')") public Account get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.get(a,id);}
 @PostMapping("/accounts") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('treasury:read') and hasAuthority('treasury:write')") public Account create(@AuthenticationPrincipal Actor a,@Valid @RequestBody Input input){return service.create(a,input);}
 @PostMapping("/movements") @PreAuthorize("hasAuthority('treasury:read') and hasAuthority('treasury:move')") public Operation move(@AuthenticationPrincipal Actor a,@Valid @RequestBody Movement input){return service.move(a,input);}
 @GetMapping("/accounts/{id}/entries") @PreAuthorize("hasAuthority('treasury:read')") public Map<String,Object> entries(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.entries(a,id,page,size);}
}
