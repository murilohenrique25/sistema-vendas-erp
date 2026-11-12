package br.com.erp.finance;
import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;
import static br.com.erp.finance.ReceivablePlanContracts.*;

@RestController @RequestMapping("/api/v1/receivables/plans")
@PreAuthorize("hasAuthority('receivable:read') and hasAuthority('receivable:write')")
public class ReceivablePlanController {
 private final ReceivablePlanService service;
 public ReceivablePlanController(ReceivablePlanService service){this.service=service;}
 @PostMapping("/preview") public Preview preview(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input){return service.preview(actor,input);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) public Plan create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input){return service.create(actor,input);}
 @GetMapping("/{id}") @PreAuthorize("hasAuthority('receivable:read')") public Plan get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id){return service.get(actor,id);}
}
