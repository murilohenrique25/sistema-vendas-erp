ALTER TABLE treasury_operation DROP CONSTRAINT ck_treasury_origin;
ALTER TABLE treasury_operation ADD CONSTRAINT ck_treasury_origin CHECK(origin IN ('MANUAL','PAYABLE_PAYMENT','PAYABLE_REVERSAL','RECEIVABLE_RECEIPT','RECEIVABLE_REVERSAL','POS_SUPPLY','POS_WITHDRAW','POS_SALE','POS_SALE_REVERSAL','POS_SALE_RETURN'));
ALTER TABLE pos_sale_payment ADD UNIQUE(tenant_id,id,sale_id);
CREATE TABLE pos_sale_return (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 sale_id UUID NOT NULL, session_id UUID NOT NULL, total NUMERIC(19,2) NOT NULL CHECK(total>=0),
 note VARCHAR(500) NOT NULL, created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,id,sale_id), UNIQUE(tenant_id,request_id),
 FOREIGN KEY(tenant_id,sale_id) REFERENCES pos_sale(tenant_id,id),
 FOREIGN KEY(tenant_id,session_id) REFERENCES pos_cash_session(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE pos_sale_return_item (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, return_id UUID NOT NULL, sale_id UUID NOT NULL, product_id UUID NOT NULL,
 quantity NUMERIC(19,6) NOT NULL CHECK(quantity>0), amount NUMERIC(19,2) NOT NULL CHECK(amount>=0), stock_operation_id UUID NOT NULL,
 UNIQUE(tenant_id,return_id,product_id), UNIQUE(tenant_id,stock_operation_id),
 FOREIGN KEY(tenant_id,return_id,sale_id) REFERENCES pos_sale_return(tenant_id,id,sale_id),
 FOREIGN KEY(tenant_id,sale_id,product_id) REFERENCES pos_sale_item(tenant_id,sale_id,product_id),
 FOREIGN KEY(tenant_id,stock_operation_id) REFERENCES stock_operation(tenant_id,id)
);
CREATE TABLE pos_sale_return_refund (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, return_id UUID NOT NULL, sale_id UUID NOT NULL, payment_id UUID NOT NULL,
 method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','PIX','BANK_TRANSFER')), account_id UUID NOT NULL,
 amount NUMERIC(19,2) NOT NULL CHECK(amount>0), reference VARCHAR(160) NOT NULL, external_reference VARCHAR(160),
 treasury_operation_id UUID NOT NULL,
 UNIQUE(tenant_id,return_id,payment_id), UNIQUE(tenant_id,treasury_operation_id),
 UNIQUE(tenant_id,account_id,method,external_reference),
 CHECK((method='CASH' AND reference='' AND external_reference IS NULL) OR (method<>'CASH' AND reference<>'' AND external_reference IS NOT NULL AND external_reference=reference)),
 FOREIGN KEY(tenant_id,return_id,sale_id) REFERENCES pos_sale_return(tenant_id,id,sale_id),
 FOREIGN KEY(tenant_id,payment_id,sale_id) REFERENCES pos_sale_payment(tenant_id,id,sale_id),
 FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id)
);
CREATE INDEX ix_sale_return_sale ON pos_sale_return(tenant_id,sale_id,created_at);
CREATE INDEX ix_sale_return_item ON pos_sale_return_item(tenant_id,sale_id,product_id);
CREATE INDEX ix_sale_return_payment ON pos_sale_return_refund(tenant_id,payment_id);
