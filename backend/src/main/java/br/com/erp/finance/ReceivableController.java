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
import static br.com.erp.finance.ReceivableContracts.*;

@RestController @RequestMapping("/api/v1/receivables") @Validated
public class ReceivableController {
    private final ReceivableService service;public ReceivableController(ReceivableService service){this.service=service;}
    @GetMapping("/calendar") @PreAuthorize("hasAuthority('receivable:read')") public Map<String,String> calendar(){return service.calendar();}
    @GetMapping @PreAuthorize("hasAuthority('receivable:read')") public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="") @Pattern(regexp="|OPEN|PARTIAL|RECEIVED|CREDITED|CANCELLED") String status,@RequestParam(defaultValue="false") boolean overdue,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size,
        @RequestParam(defaultValue="") @Size(max=200) String query,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dueFrom,@RequestParam(required=false) @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate dueTo,
        @RequestParam(required=false) UUID branchId,@RequestParam(required=false) UUID customerId,@RequestParam(required=false) UUID planId,@RequestParam(required=false) UUID saleId,@RequestParam(defaultValue="") @Pattern(regexp="|SINGLE|PLAN|POS_CREDIT") String origin){return service.list(actor,status,overdue,page,size,new ReceivableSearch.Filter(query,dueFrom,dueTo,branchId,customerId,planId,saleId,origin));}
    @GetMapping("/{id}/origin") @PreAuthorize("hasAuthority('receivable:read')") public Origin origin(@AuthenticationPrincipal Actor actor,@PathVariable UUID id){return service.origin(actor,id);}

    @GetMapping("/{id}") @PreAuthorize("hasAuthority('receivable:read')") public View get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id){return service.get(actor,id);}
    @GetMapping("/{id}/adjustments") @PreAuthorize("hasAuthority('receivable:read')") public Map<String,Object> adjustments(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.adjustments(actor,id,page,size);}
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('receivable:read') and hasAuthority('receivable:write')") public View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input){return service.create(actor,input);}
    @PostMapping("/{id}/receipts") @PreAuthorize("hasAuthority('receivable:read') and hasAuthority('receivable:receive')") public Receipt receive(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody ReceiptInput input){return service.receive(actor,id,input);}
    @GetMapping("/{id}/receipts") @PreAuthorize("hasAuthority('receivable:read')") public Map<String,Object> receipts(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.receipts(actor,id,page,size);}
    @PostMapping("/{id}/receipts/{receiptId}/reverse") @PreAuthorize("hasAuthority('receivable:read') and hasAuthority('receivable:reverse')") public Receipt reverse(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@PathVariable UUID receiptId,@Valid @RequestBody Reversal input){return service.reverse(actor,id,receiptId,input);}
    @PostMapping("/{id}/cancel") @PreAuthorize("hasAuthority('receivable:read') and hasAuthority('receivable:write')") public View cancel(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Cancellation input){return service.cancel(actor,id,input);}
}
