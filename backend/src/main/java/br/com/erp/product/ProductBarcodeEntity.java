package br.com.erp.product;

import jakarta.persistence.*;
import java.io.Serializable;
import java.util.*;

@Entity @Table(name="product_barcode") @IdClass(ProductBarcodeEntity.Key.class)
class ProductBarcodeEntity {
    @Id @Column(name="tenant_id") UUID tenantId;
    @Id @Column(name="canonical_code",length=70) String canonicalCode;
    @Column(name="product_id") UUID productId;
    String code;
    String kind;
    protected ProductBarcodeEntity() {}
    public static class Key implements Serializable {
        public UUID tenantId;
        public String canonicalCode;
        public Key() {}
        @Override public boolean equals(Object other) { return other instanceof Key key && Objects.equals(tenantId,key.tenantId) && Objects.equals(canonicalCode,key.canonicalCode); }
        @Override public int hashCode() { return Objects.hash(tenantId,canonicalCode); }
    }
}
