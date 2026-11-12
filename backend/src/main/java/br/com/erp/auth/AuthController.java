package br.com.erp.auth;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {
    private final AuthService service;
    public AuthController(AuthService service) { this.service=service; }
    public record Login(@NotBlank @Pattern(regexp="[a-z0-9-]{3,80}") String company, @NotBlank @Email @Size(max=254) String email, @NotBlank @Size(max=72) @Pattern(regexp="[\\x20-\\x7E]+") String password) {}
    public record PasswordChange(@NotBlank @Size(max=72) String currentPassword, @NotBlank @Size(min=12,max=72) @Pattern(regexp="[\\x20-\\x7E]+") String newPassword) {}
    @PostMapping("/login") public AuthService.LoginResult login(@Valid @RequestBody Login request) { return service.login(request.company(),request.email(),request.password()); }
    @PostMapping("/logout") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void logout(@AuthenticationPrincipal Actor actor) { service.logout(actor); }
    @GetMapping("/me") public Map<String,Object> me(@AuthenticationPrincipal Actor actor) { return Map.of("tenantId",actor.tenantId(),"name",actor.name(),"permissions",service.permissions(actor.tenantId(),actor.userId()),"allBranches",service.allBranches(actor.tenantId(),actor.userId())); }
    @PostMapping("/password") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void password(@AuthenticationPrincipal Actor actor,@Valid @RequestBody PasswordChange request) { service.changePassword(actor,request.currentPassword(),request.newPassword()); }
    @GetMapping("/sessions") public List<AuthService.ActiveSession> sessions(@AuthenticationPrincipal Actor actor) { return service.sessions(actor); }
    @DeleteMapping("/sessions/others") @ResponseStatus(org.springframework.http.HttpStatus.NO_CONTENT)
    public void revokeOthers(@AuthenticationPrincipal Actor actor) { service.revokeOtherSessions(actor); }
}
