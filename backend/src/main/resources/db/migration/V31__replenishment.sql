CREATE TABLE replenishment_policy (
 tenant_id UUID NOT NULL,warehouse_id UUID NOT NULL,product_id UUID NOT NULL,
 minimum_quantity NUMERIC(19,6) NOT NULL CHECK(minimum_quantity>=0),target_quantity NUMERIC(19,6) NOT NULL CHECK(target_quantity>=minimum_quantity),version BIGINT NOT NULL DEFAULT 0,
 PRIMARY KEY(tenant_id,warehouse_id,product_id),FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),FOREIGN KEY(tenant_id,product_id) REFERENCES product(tenant_id,id)
);
