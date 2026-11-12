package br.com.erp.pos;
import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import java.util.UUID;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/customers/{id}/credit-policy")
@PreAuthorize("hasAuthority('customer:read') and hasAuthority('customer:credit')")
public class CustomerCreditController {
 private final SaleCreditService service;public CustomerCreditController(SaleCreditService service){this.service=service;}
 @GetMapping public SaleCreditService.Policy get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.policy(a,id);}
 @PutMapping public SaleCreditService.Policy put(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody SaleCreditService.PolicyInput input){return service.configure(a,id,input);}
}
