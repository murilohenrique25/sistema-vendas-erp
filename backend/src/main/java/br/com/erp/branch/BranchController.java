package br.com.erp.branch;

import br.com.erp.auth.Actor;
import br.com.erp.auth.BranchAccess;
import org.springframework.jdbc.core.JdbcTemplate;
import br.com.erp.shared.Audit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.data.domain.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/v1/branches") @Validated
public class BranchController {
    private final BranchRepository repository;
    private final Audit audit;
    private final JdbcTemplate db;
    private final BranchAccess access;
    public BranchController(BranchRepository repository,Audit audit,JdbcTemplate db,BranchAccess access) { this.repository=repository; this.audit=audit;this.db=db;this.access=access; }
    public record Input(@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,30}") String code,@NotBlank @Size(max=160) String name) {}
    public record View(UUID id,String code,String name,boolean active,long version) {
        static View of(Branch b) { return new View(b.getId(),b.getCode(),b.getName(),b.isActive(),b.getVersion()); }
    }
    @GetMapping @PreAuthorize("hasAuthority('branch:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) {
        String where=" WHERE b.tenant_id=? AND "+BranchAccess.predicate("b.id");
        var items=db.query("SELECT b.id,b.code,b.name,b.active,b.version FROM branch b"+where+" ORDER BY b.code,b.id LIMIT ? OFFSET ?",(rs,i)->new View(rs.getObject(1,UUID.class),rs.getString(2),rs.getString(3),rs.getBoolean(4),rs.getLong(5)),actor.tenantId(),actor.tenantId(),actor.userId(),size,(long)page*size);
        return Map.of("items",items,"total",db.queryForObject("SELECT COUNT(*) FROM branch b"+where,Long.class,actor.tenantId(),actor.tenantId(),actor.userId()),"page",page,"size",size);
    }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('branch:read')")
    public View get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) {
        access.requireBranch(actor,id);
        return View.of(repository.findByTenantIdAndId(actor.tenantId(),id).orElseThrow(()->new ResponseStatusException(HttpStatus.NOT_FOUND,"Filial não encontrada.")));
    }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('branch:write')") @Transactional
    public View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input request) {
        access.requireAll(actor);
        Branch branch=repository.save(new Branch(actor.tenantId(),request.code(),request.name().trim()));
        audit.record(actor,"BRANCH_CREATED",branch.getId()); return View.of(branch);
    }
}
