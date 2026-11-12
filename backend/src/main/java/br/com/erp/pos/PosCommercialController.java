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
import static br.com.erp.pos.PosCommercialContracts.*;
@RestController @RequestMapping("/api/v1/pos") @Validated
@PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read')")
public class PosCommercialController {
 private final DiscountService discounts;private final SuspensionService suspensions;
 public PosCommercialController(DiscountService discounts,SuspensionService suspensions){this.discounts=discounts;this.suspensions=suspensions;}
 @GetMapping("/discount-policies/{branch}") public Policy policy(@AuthenticationPrincipal Actor a,@PathVariable UUID branch){return discounts.policy(a,branch);}
 @PutMapping("/discount-policies/{branch}") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:discount-policy')") public Policy policy(@AuthenticationPrincipal Actor a,@PathVariable UUID branch,@Valid @RequestBody PolicyInput input){return discounts.policy(a,branch,input);}
 @GetMapping("/discount-requests") public Map<String,Object> approvals(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="") @Pattern(regexp="|PENDING|APPROVED|REJECTED|CONSUMED") String status,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return discounts.list(a,status,page,size);}
 @GetMapping("/discount-requests/{id}") public Approval approval(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return discounts.get(a,id);}
 @PostMapping("/discount-requests") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:discount')") public Approval request(@AuthenticationPrincipal Actor a,@Valid @RequestBody CartInput input){return discounts.request(a,input);}
 @PostMapping("/discount-requests/{id}/decision") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:discount-approve')") public Approval decision(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody DecisionInput input){return discounts.decide(a,id,input);}
 @GetMapping("/suspensions") public Map<String,Object> suspensions(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="") @Pattern(regexp="|SUSPENDED|COMPLETED|DISCARDED") String status,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return suspensions.list(a,status,page,size);}
 @GetMapping("/suspensions/{id}") public Suspension suspension(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return suspensions.get(a,id);}
 @PostMapping("/suspensions") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:suspend')") public Suspension create(@AuthenticationPrincipal Actor a,@Valid @RequestBody CartInput input){return suspensions.save(a,null,input);}
 @PutMapping("/suspensions/{id}") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:suspend')") public Suspension update(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody CartInput input){return suspensions.save(a,id,input);}
 @PostMapping("/suspensions/{id}/discard") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:suspend')") public Suspension discard(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody DiscardInput input){return suspensions.discard(a,id,input);}
}
