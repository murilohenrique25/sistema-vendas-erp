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
import static br.com.erp.finance.ReconciliationContracts.*;
@RestController @RequestMapping("/api/v1/reconciliation") @Validated
@PreAuthorize("hasAuthority('reconciliation:read') and hasAuthority('treasury:read')")
public class ReconciliationController {
 private final ReconciliationService service;public ReconciliationController(ReconciliationService s){service=s;}
 @GetMapping("/calendar") public Map<String,String> calendar(){return service.calendar();}
 @PostMapping("/imports") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('reconciliation:read') and hasAuthority('treasury:read') and hasAuthority('reconciliation:import')") public ImportView importLines(@AuthenticationPrincipal Actor a,@Valid @RequestBody Input input){return service.importLines(a,input);}
 @PostMapping("/imports/csv") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('reconciliation:read') and hasAuthority('treasury:read') and hasAuthority('reconciliation:import')") public ImportView importCsv(@AuthenticationPrincipal Actor a,@Valid @RequestBody CsvInput input){return service.importCsv(a,input);}
 @PostMapping("/imports/csv/preview") @PreAuthorize("hasAuthority('reconciliation:read') and hasAuthority('treasury:read') and hasAuthority('reconciliation:import')") public Map<String,Object> preview(@AuthenticationPrincipal Actor a,@Valid @RequestBody CsvInput input){return service.preview(a,input);}
 @GetMapping("/accounts/{id}/imports") public Map<String,Object> imports(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.imports(a,id,page,size);}
 @GetMapping("/accounts/{id}/summary") public Map<String,Object> summary(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.summary(a,id);}
 @GetMapping("/accounts/{id}/lines") public Map<String,Object> lines(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@RequestParam(defaultValue="") @Pattern(regexp="|UNMATCHED|MATCHED|IGNORED") String status,@RequestParam(required=false) UUID importId,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.lines(a,id,status,importId,page,size);}
 @GetMapping("/lines/{id}") public Line get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.get(a,id);}
 @GetMapping("/lines/{id}/candidates") public Map<String,Object> candidates(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.candidates(a,id,page,size);}
 @GetMapping("/lines/{id}/history") public Map<String,Object> history(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.history(a,id,page,size);}
 @PostMapping("/lines/{id}/{action:match|ignore|reset}") @PreAuthorize("hasAuthority('reconciliation:read') and hasAuthority('treasury:read') and hasAuthority('reconciliation:review')") public Line review(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@PathVariable String action,@Valid @RequestBody Review input){return service.review(a,id,action.toUpperCase(Locale.ROOT),input);}
}
