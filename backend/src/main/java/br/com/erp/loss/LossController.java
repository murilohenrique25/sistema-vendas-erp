package br.com.erp.loss;

import br.com.erp.auth.Actor;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@RestController @RequestMapping("/api/v1/losses") @Validated
public class LossController {
    private final LossService service;
    public LossController(LossService service) { this.service=service; }
    @PostMapping @PreAuthorize("hasAuthority('loss:create')")
    public LossService.View create(@AuthenticationPrincipal Actor actor,@Valid @RequestBody LossService.Input input) { return service.create(actor,input); }
    @GetMapping @PreAuthorize("hasAuthority('loss:read')")
    public Map<String,Object> list(@AuthenticationPrincipal Actor actor,@RequestParam(defaultValue="0") @Min(0) @Max(100000) int page,@RequestParam(defaultValue="20") @Min(1) @Max(100) int size) { return service.list(actor,page,size); }
    @GetMapping("/{id}") @PreAuthorize("hasAuthority('loss:read')")
    public LossService.View get(@AuthenticationPrincipal Actor actor,@PathVariable UUID id) { return service.get(actor,id); }
    @PostMapping("/{id}/approve") @PreAuthorize("hasAuthority('loss:approve')")
    public LossService.View approve(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody LossService.Approval input) { return service.approve(actor,id,input); }
    @PostMapping("/{id}/reject") @PreAuthorize("hasAuthority('loss:approve')")
    public LossService.View reject(@AuthenticationPrincipal Actor actor,@PathVariable UUID id,@Valid @RequestBody LossService.Rejection input) { return service.reject(actor,id,input); }
}
