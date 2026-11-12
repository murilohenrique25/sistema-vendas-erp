package br.com.erp.finance;
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
import static br.com.erp.finance.PayableContracts.*;

@RestController @RequestMapping("/api/v1/payables") @Validated
public class PayableController {
    private final PayableService service;public PayableController(PayableService service){this.service=service;}
    @GetMapping("/calendar") @PreAuthorize("hasAuthority('payable:read')") public Map<String,String> calendar(){return service.calendar();}
    @GetMapping @PreAuthorize("hasAuthority('payable:read')") public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="") @Pattern(regexp="|OPEN|PARTIAL|PAID|CANCELLED") String status,@RequestParam(defaultValue="false") boolean overdue,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue="") @Size(max=200) String query,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dueFrom,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dueTo,
        @RequestParam(required=false) UUID branchId,@RequestParam(required=false) UUID supplierId,@RequestParam(required=false) UUID receiptId,@RequestParam(required=false) Boolean withReceipt){return service.list(actor,status,overdue,page,size,new PayableSearch.Filter(query,dueFrom,dueTo,branchId,supplierId,receiptId,withReceipt));}

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('payable:read')") public View get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id){return service.get(actor,id);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('payable:read') and hasAuthority('payable:write')") public View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input){return service.create(actor,input);}
    @PostMapping("/{id}/payments") @PreAuthorize("hasAuthority('payable:read') and hasAuthority('payable:pay')") public Payment pay(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody PaymentInput input){return service.pay(actor,id,input);}
    @GetMapping("/{id}/payments") @PreAuthorize("hasAuthority('payable:read')") public Map<String,Object> payments(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.payments(actor,id,page,size);}
    @PostMapping("/{id}/payments/{paymentId}/reverse") @PreAuthorize("hasAuthority('payable:read') and hasAuthority('payable:reverse')") public Payment reverse(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID paymentId,@Valid @RequestBody Reversal input){return service.reverse(actor,id,paymentId,input);}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('payable:read') and hasAuthority('payable:write')") public View cancel(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Cancellation input){return service.cancel(actor,id,input);}
}
