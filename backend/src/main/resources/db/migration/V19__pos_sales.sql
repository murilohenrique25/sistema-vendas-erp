ALTER TABLE pos_terminal ADD COLUMN warehouse_id UUID;
ALTER TABLE pos_terminal ADD FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id);
ALTER TABLE treasury_operation DROP CONSTRAINT ck_treasury_origin;
ALTER TABLE treasury_operation ADD CONSTRAINT ck_treasury_origin CHECK(origin IN ('MANUAL','PAYABLE_PAYMENT','PAYABLE_REVERSAL','RECEIVABLE_RECEIPT','RECEIVABLE_REVERSAL','POS_SUPPLY','POS_WITHDRAW','POS_SALE'));
CREATE TABLE pos_sale (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 session_id UUID NOT NULL, branch_id UUID NOT NULL, warehouse_id UUID NOT NULL,
 total NUMERIC(19,2) NOT NULL CHECK(total>0), tendered NUMERIC(19,2) NOT NULL CHECK(tendered>=total),
 change_amount NUMERIC(19,2) NOT NULL CHECK(change_amount=tendered-total),
 treasury_operation_id UUID NOT NULL, created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,treasury_operation_id),
 FOREIGN KEY(tenant_id,session_id) REFERENCES pos_cash_session(tenant_id,id),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
 FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
 FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE pos_sale_item (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, sale_id UUID NOT NULL, product_id UUID NOT NULL,
 position INTEGER NOT NULL CHECK(position>=0), sku VARCHAR(60) NOT NULL, name VARCHAR(200) NOT NULL, unit VARCHAR(8) NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0), unit_price NUMERIC(19,2) NOT NULL CHECK(unit_price>=0),
 total NUMERIC(19,2) NOT NULL CHECK(total>=0), stock_operation_id UUID NOT NULL,
 UNIQUE(tenant_id,sale_id,product_id), UNIQUE(tenant_id,sale_id,position), UNIQUE(tenant_id,stock_operation_id),
 FOREIGN KEY(tenant_id,sale_id) REFERENCES pos_sale(tenant_id,id),
 FOREIGN KEY(tenant_id,product_id) REFERENCES product(tenant_id,id),
 FOREIGN KEY(tenant_id,stock_operation_id) REFERENCES stock_operation(tenant_id,id)
);
CREATE INDEX ix_pos_sale_branch ON pos_sale(tenant_id,branch_id,created_at);
