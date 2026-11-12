package br.com.erp.user;

import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.HttpStatus;

@RestController @RequestMapping("/api/v1/users") @Validated
public class UserController {
    private final UserService service;
    public UserController(UserService service) { this.service=service; }
    public record Input(@NotBlank @Size(max=160) String name,@NotBlank @Email @Size(max=254) String email,
        @NotBlank @Size(min=12,max=72) @Pattern(regexp="[\\x20-\\x7E]+") String password,
        @NotNull @Size(max=100) List<@NotBlank @Size(max=80) String> permissions) {}
    public record Status(@NotNull Boolean active,@NotNull @Min(0) Long version) {}
    public record Permissions(@NotNull @Size(max=100) List<@NotBlank @Size(max=80) String> permissions,@NotNull @Min(0) Long version) {}
    public record Branches(@NotNull Boolean allBranches,@NotNull @Size(max=100) List<@NotNull UUID> branchIds,@NotNull @Min(0) Long version) {}
    @GetMapping("/{id}/branches") @PreAuthorize("hasAuthority('user:read')")
    public UserService.BranchScope branches(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) { return service.branchScope(actor,id); }
    @PutMapping("/{id}/branches") @PreAuthorize("hasAuthority('user:write')")
    public UserService.BranchScope branches(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Branches input) { return service.setBranchScope(actor,id,input); }
    @GetMapping @PreAuthorize("hasAuthority('user:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.list(actor,page,size); }
    @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('user:write')")
    public UserService.View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input) { return service.create(actor,input); }
    @PutMapping("/{id}/active") @PreAuthorize("hasAuthority('user:write')")
    public UserService.View status(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Status input) { return service.setActive(actor,id,input); }
    @PutMapping("/{id}/permissions") @PreAuthorize("hasAuthority('user:write')")
    public UserService.View permissions(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Permissions input) { return service.setPermissions(actor,id,input); }
}
