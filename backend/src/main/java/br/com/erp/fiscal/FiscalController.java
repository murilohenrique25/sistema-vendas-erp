package br.com.erp.fiscal;
import br.com.erp.auth.*;
import br.com.erp.shared.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
@RestController @Validated @RequestMapping("/api/v1/fiscal") @PreAuthorize("hasAuthority('fiscal:read')")
public class FiscalController {
 private final FiscalConfiguration config;private final JdbcTemplate db;private final BranchAccess access;
 public FiscalController(FiscalConfiguration config,JdbcTemplate db,BranchAccess access){this.config=config;this.db=db;this.access=access;}
 @GetMapping("/configuration") public Map<String,Object> get(@AuthenticationPrincipal Actor a,@RequestParam UUID branchId,@RequestParam FiscalContext.Environment environment){return config.get(a,branchId,environment);}
 @PutMapping("/configuration") @PreAuthorize("hasAuthority('fiscal:read') and hasAuthority('fiscal:configure')") public Map<String,Object> save(@AuthenticationPrincipal Actor a,@Valid @RequestBody FiscalConfiguration.Save input){return config.save(a,input);}
 @PutMapping("/certificate") @PreAuthorize("hasAuthority('fiscal:read') and hasAuthority('fiscal:configure')") public Map<String,Object> certificate(@AuthenticationPrincipal Actor a,@Valid @RequestBody FiscalConfiguration.Credential input){return config.certificate(a,input);}
 public record Status(@NotNull UUID branchId,@NotNull FiscalContext.Environment environment){}
 @PostMapping("/service-status") @PreAuthorize("hasAuthority('fiscal:read') and hasAuthority('fiscal:configure')") public Map<String,Object> status(@AuthenticationPrincipal Actor a,@Valid @RequestBody Status input){access.requireBranch(a,input.branchId());UUID id=UUID.randomUUID();String state,message,code="";try{String xml="<consStatServ xmlns='"+SefazXml.NFE+"' versao='4.00'><tpAmb>"+input.environment().code()+"</tpAmb><cUF>52</cUF><xServ>STATUS</xServ></consStatServ>";var response=SefazXml.parse(config.transport(a,input.branchId(),input.environment()).exchange(input.environment(),SefazEndpoints.Service.STATUS,xml));var root=SefazXml.unique(response,SefazXml.NFE,"retConsStatServ");code=SefazXml.field(root,"cStat");message=SefazXml.field(root,"xMotivo");if(!input.environment().code().equals(SefazXml.field(root,"tpAmb"))||!"52".equals(SefazXml.field(root,"cUF")))throw new IllegalArgumentException("Resposta de ambiente ou UF divergente.");state=code.equals("107")?"AVAILABLE":"UNAVAILABLE";}catch(org.springframework.web.server.ResponseStatusException e){throw e;}catch(Exception e){if(e instanceof InterruptedException)Thread.currentThread().interrupt();state="UNKNOWN";message="Não foi possível confirmar a disponibilidade da SEFAZ. Confira certificado e conectividade.";}if(message.length()>500)message=message.substring(0,500);db.update("INSERT INTO fiscal_service_attempt(id,tenant_id,branch_id,environment,service,status,message,created_by) VALUES(?,?,?,?, 'STATUS',?,?,?)",id,a.tenantId(),input.branchId(),input.environment().name(),state,message,a.userId());return Map.of("id",id,"status",state,"code",code,"message",message);}
 @GetMapping("/attempts") public Map<String,Object> attempts(@AuthenticationPrincipal Actor a,@RequestParam UUID branchId,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size){access.requireBranch(a,branchId);return Map.of("items",db.queryForList("SELECT id,environment,service,status,message,created_at FROM fiscal_service_attempt WHERE tenant_id=? AND branch_id=? ORDER BY created_at DESC,id LIMIT ? OFFSET ?",a.tenantId(),branchId,size,(long)page*size),"total",db.queryForObject("SELECT COUNT(*) FROM fiscal_service_attempt WHERE tenant_id=? AND branch_id=?",Long.class,a.tenantId(),branchId),"page",page,"size",size);}
}
