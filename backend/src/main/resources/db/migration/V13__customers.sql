CREATE TABLE customer (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    code VARCHAR(40) NOT NULL,
    name VARCHAR(200) NOT NULL,
    tax_identifier VARCHAR(40) NOT NULL,
    contact_name VARCHAR(160) NOT NULL,
    email VARCHAR(254) NOT NULL,
    phone VARCHAR(40) NOT NULL,
    address VARCHAR(500) NOT NULL,
    notes VARCHAR(2000) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id,id),
    UNIQUE(tenant_id,code)
);
CREATE INDEX ix_customer_tenant_name ON customer(tenant_id,name);
