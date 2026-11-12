package br.com.erp.product;

import jakarta.persistence.*;
import java.math.BigDecimal;
import java.util.UUID;

@Entity @Table(name="product")
public class Product {
    @Id private UUID id;
    @Column(name="tenant_id",nullable=false) private UUID tenantId;
    private String sku;
    private String name;
    private String description;
    private String unit;
    @Column(name="sale_price",precision=19,scale=2) private BigDecimal salePrice;
    private boolean active;
    @Version private long version;
    protected Product() {}
    public Product(UUID tenantId,String sku,String name,String description,String unit,BigDecimal price) {
        this.id=UUID.randomUUID(); this.tenantId=tenantId; this.sku=sku; this.name=name; this.description=description; this.unit=unit; this.salePrice=price; this.active=true;
    }
    public UUID getId() { return id; }
    public String getSku() { return sku; }
    public String getName() { return name; }
    public String getDescription() { return description; }
    public String getUnit() { return unit; }
    public BigDecimal getSalePrice() { return salePrice; }
    public boolean isActive() { return active; }
    public long getVersion() { return version; }
}
