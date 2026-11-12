package br.com.erp.branch;

import jakarta.persistence.*;
import java.util.UUID;

@Entity @Table(name="branch")
public class Branch {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false) private UUID tenantId;
    private String code;
    private String name;
    private boolean active;
    @Version private long version;
    protected Branch() {}
    public Branch(UUID tenantId,String code,String name) { this.id=UUID.randomUUID(); this.tenantId=tenantId; this.code=code; this.name=name; this.active=true; }
    public UUID getId() { return id; }
    public String getCode() { return code; }
    public String getName() { return name; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
