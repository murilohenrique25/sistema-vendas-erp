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
import static br.com.erp.pos.CashContracts.*;
@RestController @RequestMapping("/api/v1/pos") @Validated
@PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read')")
public class CashController {
 private final CashService service;public CashController(CashService s){service=s;}
 @GetMapping("/operator") public Map<String,UUID> operator(@AuthenticationPrincipal Actor a){return Map.of("userId",a.userId());}
 @GetMapping("/terminals") public Map<String,Object> terminals(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.terminals(a,page,size);}
 @GetMapping("/terminals/{id}") public Terminal terminal(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.terminal(a,id);}
 @PostMapping("/terminals") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:manage')") public Terminal create(@AuthenticationPrincipal Actor a,@Valid @RequestBody TerminalInput input){return service.createTerminal(a,input);}
 @PutMapping("/terminals/{id}/active") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:manage')") public Terminal active(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody Active input){return service.active(a,id,input);}
 @GetMapping("/cash-sessions") public Map<String,Object> sessions(@AuthenticationPrincipal Actor a,@RequestParam(defaultValue="") @Pattern(regexp="|OPEN|CLOSED") String status,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.sessions(a,status,page,size);}
 @GetMapping("/cash-sessions/{id}") public Session get(@AuthenticationPrincipal Actor a,@PathVariable UUID id){return service.get(a,id);}
 @PostMapping("/cash-sessions") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:operate')") public Session open(@AuthenticationPrincipal Actor a,@Valid @RequestBody Opening input){return service.open(a,input);}
 @GetMapping("/cash-sessions/{id}/movements") public Map<String,Object> movements(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){return service.movements(a,id,page,size);}
 @PostMapping("/cash-sessions/{id}/movements") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('treasury:move') and hasAuthority('pos:operate')") public Movement move(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody MovementInput input){return service.move(a,id,input);}
 @PostMapping("/cash-sessions/{id}/close") @PreAuthorize("hasAuthority('pos:read') and hasAuthority('treasury:read') and hasAuthority('pos:close')") public Session close(@AuthenticationPrincipal Actor a,@PathVariable UUID id,@Valid @RequestBody Closing input){return service.close(a,id,input);}
}
