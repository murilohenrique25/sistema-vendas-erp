CREATE TABLE warehouse (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    code VARCHAR(30) NOT NULL,
    name VARCHAR(160) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    UNIQUE (tenant_id,id),
    UNIQUE (tenant_id,branch_id,code),
    FOREIGN KEY (tenant_id,branch_id) REFERENCES branch(tenant_id,id)
);
CREATE INDEX ix_warehouse_branch ON warehouse(tenant_id,branch_id);
CREATE TABLE stock_balance (
    tenant_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19,6) NOT NULL CHECK (quantity >= 0),
    average_cost NUMERIC(19,6) NOT NULL CHECK (average_cost >= 0),
    PRIMARY KEY (tenant_id,warehouse_id,product_id),
    FOREIGN KEY (tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
    FOREIGN KEY (tenant_id,product_id) REFERENCES product(tenant_id,id)
);
CREATE TABLE stock_loss (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    quantity NUMERIC(19,6) NOT NULL CHECK (quantity > 0),
    reason VARCHAR(30) NOT NULL,
    details VARCHAR(500) NOT NULL,
    estimated_unit_cost NUMERIC(19,6) NOT NULL CHECK (estimated_unit_cost >= 0),
    approved_unit_cost NUMERIC(19,6),
    status VARCHAR(15) NOT NULL CHECK (status IN ('PENDING','APPROVED','REJECTED')),
    created_by UUID NOT NULL,
    reviewed_by UUID,
    review_note VARCHAR(500),
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reviewed_at TIMESTAMP WITH TIME ZONE,
    UNIQUE (tenant_id,id),
    UNIQUE (tenant_id,request_id),
    FOREIGN KEY (tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
    FOREIGN KEY (tenant_id,product_id) REFERENCES product(tenant_id,id),
    FOREIGN KEY (tenant_id,created_by) REFERENCES app_user(tenant_id,id),
    FOREIGN KEY (tenant_id,reviewed_by) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX ix_loss_tenant_date ON stock_loss(tenant_id,created_at);
CREATE TABLE stock_operation (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    fingerprint VARCHAR(64) NOT NULL,
    kind VARCHAR(20) NOT NULL CHECK (kind IN ('MOVEMENT','TRANSFER','LOSS_APPROVAL')),
    actor_id UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (tenant_id,id),
    FOREIGN KEY (tenant_id,actor_id) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE stock_movement (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    operation_id UUID NOT NULL,
    warehouse_id UUID NOT NULL,
    product_id UUID NOT NULL,
    loss_id UUID,
    movement_type VARCHAR(30) NOT NULL,
    quantity NUMERIC(19,6) NOT NULL CHECK (quantity <> 0),
    balance_after NUMERIC(19,6) NOT NULL CHECK (balance_after >= 0),
    unit VARCHAR(10) NOT NULL,
    unit_cost NUMERIC(19,6) NOT NULL CHECK (unit_cost >= 0),
    reason VARCHAR(500) NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id,operation_id) REFERENCES stock_operation(tenant_id,id),
    FOREIGN KEY (tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
    FOREIGN KEY (tenant_id,product_id) REFERENCES product(tenant_id,id),
    FOREIGN KEY (tenant_id,loss_id) REFERENCES stock_loss(tenant_id,id),
    UNIQUE (tenant_id,operation_id,warehouse_id)
);
CREATE INDEX ix_movement_warehouse_date ON stock_movement(tenant_id,warehouse_id,created_at);
