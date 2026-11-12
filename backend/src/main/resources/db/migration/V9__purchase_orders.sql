CREATE TABLE purchase_order (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    branch_id UUID NOT NULL,
    supplier_id UUID NOT NULL,
    supplier_name VARCHAR(200) NOT NULL,
    notes VARCHAR(500) NOT NULL,
    total NUMERIC(19,2) NOT NULL CHECK(total>=0),
    status VARCHAR(15) NOT NULL CHECK(status IN ('OPEN','PARTIAL','RECEIVED','CANCELLED')),
    version BIGINT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancellation_note VARCHAR(500),
    UNIQUE(tenant_id,id),
    UNIQUE(tenant_id,request_id),
    FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
    FOREIGN KEY(tenant_id,supplier_id) REFERENCES supplier(tenant_id,id),
    FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX ix_purchase_order_tenant_branch ON purchase_order(tenant_id,branch_id,created_at);
CREATE TABLE purchase_order_line (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    product_id UUID NOT NULL,
    sku VARCHAR(60) NOT NULL,
    name VARCHAR(200) NOT NULL,
    unit VARCHAR(10) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0),
    received_quantity NUMERIC(19,6) NOT NULL DEFAULT 0 CHECK(received_quantity>=0 AND received_quantity<=quantity),
    unit_cost NUMERIC(19,6) NOT NULL CHECK(unit_cost>=0),
    total NUMERIC(19,2) NOT NULL CHECK(total>=0),
    UNIQUE(tenant_id,order_id,id),
    UNIQUE(tenant_id,order_id,product_id),
    FOREIGN KEY(tenant_id,order_id) REFERENCES purchase_order(tenant_id,id),
    FOREIGN KEY(tenant_id,product_id) REFERENCES product(tenant_id,id)
);
CREATE TABLE purchase_receipt (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    note VARCHAR(500) NOT NULL,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY(tenant_id,id),
    UNIQUE(tenant_id,order_id,id),
    FOREIGN KEY(tenant_id,order_id) REFERENCES purchase_order(tenant_id,id),
    FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
    FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE purchase_receipt_line (
    tenant_id UUID NOT NULL,
    order_id UUID NOT NULL,
    receipt_id UUID NOT NULL,
    order_line_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0),
    PRIMARY KEY(tenant_id,receipt_id,order_line_id),
    UNIQUE(tenant_id,operation_id),
    FOREIGN KEY(tenant_id,order_id,receipt_id) REFERENCES purchase_receipt(tenant_id,order_id,id),
    FOREIGN KEY(tenant_id,order_id,order_line_id) REFERENCES purchase_order_line(tenant_id,order_id,id),
    FOREIGN KEY(tenant_id,operation_id) REFERENCES stock_operation(tenant_id,id)
);
