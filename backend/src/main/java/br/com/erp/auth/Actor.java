package br.com.erp.auth;

import java.util.UUID;

public record Actor(UUID tenantId, UUID userId, String name, String tokenHash) {}
