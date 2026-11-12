CREATE TABLE workflow_command (
 tenant_id UUID NOT NULL, request_id UUID NOT NULL, kind VARCHAR(60) NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 entity_id UUID NOT NULL, actor_id UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,request_id), FOREIGN KEY(tenant_id,actor_id) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE physical_inventory (
 id UUID PRIMARY KEY,tenant_id UUID NOT NULL,warehouse_id UUID NOT NULL,branch_id UUID NOT NULL,
 status VARCHAR(20) NOT NULL CHECK(status IN ('COUNTING','SUBMITTED','APPROVED','CANCELLED')),version BIGINT NOT NULL DEFAULT 0,
 note VARCHAR(500) NOT NULL,created_by UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 reviewed_by UUID,review_note VARCHAR(500),reviewed_at TIMESTAMP WITH TIME ZONE,
 UNIQUE(tenant_id,id),FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id),FOREIGN KEY(tenant_id,reviewed_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE physical_inventory_line (
 tenant_id UUID NOT NULL,inventory_id UUID NOT NULL,product_id UUID NOT NULL,sku VARCHAR(60) NOT NULL,name VARCHAR(200) NOT NULL,unit VARCHAR(10) NOT NULL,
 expected_quantity NUMERIC(19,6) NOT NULL CHECK(expected_quantity>=0),movement_count BIGINT NOT NULL,
 counted_quantity NUMERIC(19,6) CHECK(counted_quantity>=0),unit_cost NUMERIC(19,6) CHECK(unit_cost>=0),operation_id UUID,
 PRIMARY KEY(tenant_id,inventory_id,product_id),FOREIGN KEY(tenant_id,inventory_id) REFERENCES physical_inventory(tenant_id,id),
 FOREIGN KEY(tenant_id,product_id) REFERENCES product(tenant_id,id),FOREIGN KEY(tenant_id,operation_id) REFERENCES stock_operation(tenant_id,id)
);
CREATE INDEX ix_physical_inventory_branch ON physical_inventory(tenant_id,branch_id,created_at);
