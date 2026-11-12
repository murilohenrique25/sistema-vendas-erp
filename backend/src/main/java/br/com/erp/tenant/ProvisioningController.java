package br.com.erp.tenant;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProvisioningController {
    private final ProvisioningService service;
    private final String key;
    public ProvisioningController(ProvisioningService service,@Value("${erp.provisioning-key}") String key) { this.service=service; this.key=key; }
    public record Request(@NotBlank @Size(max=160) String companyName,@Pattern(regexp="[a-z0-9-]{3,80}") @NotBlank String slug,@NotBlank @Size(max=160) String adminName,@Email @NotBlank @Size(max=254) String email,@Size(min=12,max=72) @NotBlank @Pattern(regexp="[\\x20-\\x7E]+") String password) {}
    @PostMapping("/api/v1/provisioning") @ResponseStatus(HttpStatus.CREATED)
    public Map<String,UUID> provision(@RequestHeader(value="X-Provisioning-Key",defaultValue="") String provided,@Valid @RequestBody Request request) {
        if (key.length()<32 || !MessageDigest.isEqual(key.getBytes(StandardCharsets.UTF_8),provided.getBytes(StandardCharsets.UTF_8))) throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Provisionamento não autorizado.");
        return Map.of("tenantId",service.create(request.companyName(),request.slug(),request.adminName(),request.email(),request.password()));
    }
}
