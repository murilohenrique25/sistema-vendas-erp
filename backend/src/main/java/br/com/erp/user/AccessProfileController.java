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

@RestController @RequestMapping("/api/v1") @Validated
public class AccessProfileController {
    private final AccessProfileService service;
    public AccessProfileController(AccessProfileService service) { this.service=service; }
    public record Input(@NotBlank @Size(max=160) String name,@NotNull @Size(max=100) List<@NotBlank @Size(max=80) String> permissions) {}
    public record Update(@NotBlank @Size(max=160) String name,@NotNull @Size(max=100) List<@NotBlank @Size(max=80) String> permissions,@NotNull @Min(0) Long version) {}
    public record Apply(@NotNull UUID profileId,@NotNull @Min(0) Long profileVersion,@NotNull @Min(0) Long userVersion) {}
    @GetMapping("/access-profiles") @PreAuthorize("hasAuthority('user:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.list(actor,page,size); }
    @PostMapping("/access-profiles") @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('user:write')")
    public AccessProfileService.View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody Input input) { return service.create(actor,input); }
    @PutMapping("/access-profiles/{id}") @PreAuthorize("hasAuthority('user:write')")
    public AccessProfileService.View update(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Update input) { return service.update(actor,id,input); }
    @PutMapping("/users/{id}/profile") @PreAuthorize("hasAuthority('user:write')")
    public UserService.View apply(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody Apply input) { return service.apply(actor,id,input); }
}
