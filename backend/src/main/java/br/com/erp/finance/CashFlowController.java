package br.com.erp.finance;
import br.com.erp.auth.Actor;
import java.time.LocalDate;
import java.util.*;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/v1/cash-flow")
@PreAuthorize("hasAuthority('cashflow:read') and hasAuthority('treasury:read') and hasAuthority('payable:read') and hasAuthority('receivable:read')")
public class CashFlowController {
 private final CashFlowService service;public CashFlowController(CashFlowService service){this.service=service;}
 @GetMapping("/calendar") public Map<String,String> calendar(){return service.calendar();}
 @GetMapping public CashFlowContracts.Report report(@AuthenticationPrincipal Actor actor,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate from,@RequestParam @DateTimeFormat(iso=DateTimeFormat.ISO.DATE) LocalDate to,@RequestParam(required=false) UUID branchId){return service.report(actor,from,to,branchId);}
}
