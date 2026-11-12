CREATE TABLE product (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    sku VARCHAR(60) NOT NULL,
    name VARCHAR(200) NOT NULL,
    description VARCHAR(2000) NOT NULL,
    unit VARCHAR(10) NOT NULL,
    sale_price NUMERIC(19,2) NOT NULL CHECK (sale_price >= 0),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE (tenant_id, sku),
    UNIQUE (tenant_id, id)
);
CREATE INDEX ix_product_tenant_name ON product(tenant_id, name);
CREATE TABLE product_barcode (
    tenant_id UUID NOT NULL,
    product_id UUID NOT NULL,
    code VARCHAR(60) NOT NULL,
    canonical_code VARCHAR(60) NOT NULL,
    kind VARCHAR(10) NOT NULL CHECK (kind IN ('GTIN','INTERNAL')),
    PRIMARY KEY (tenant_id, canonical_code),
    FOREIGN KEY (tenant_id, product_id) REFERENCES product(tenant_id, id)
);
CREATE INDEX ix_barcode_product ON product_barcode(tenant_id, product_id);
