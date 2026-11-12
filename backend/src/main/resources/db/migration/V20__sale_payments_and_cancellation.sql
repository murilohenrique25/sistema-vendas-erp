ALTER TABLE treasury_operation DROP CONSTRAINT ck_treasury_origin;
ALTER TABLE treasury_operation ADD CONSTRAINT ck_treasury_origin CHECK(origin IN ('MANUAL','PAYABLE_PAYMENT','PAYABLE_REVERSAL','RECEIVABLE_RECEIPT','RECEIVABLE_REVERSAL','POS_SUPPLY','POS_WITHDRAW','POS_SALE','POS_SALE_REVERSAL'));
ALTER TABLE pos_sale ADD COLUMN status VARCHAR(12) NOT NULL DEFAULT 'COMPLETED' CHECK(status IN ('COMPLETED','CANCELLED'));
ALTER TABLE pos_sale ADD COLUMN version BIGINT NOT NULL DEFAULT 0;
ALTER TABLE pos_sale ADD COLUMN cancel_request_id UUID;
ALTER TABLE pos_sale ADD COLUMN cancel_fingerprint VARCHAR(64);
ALTER TABLE pos_sale ADD COLUMN cancel_note VARCHAR(500);
ALTER TABLE pos_sale ADD COLUMN cancelled_by UUID;
ALTER TABLE pos_sale ADD COLUMN cancelled_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE pos_sale ADD UNIQUE(tenant_id,cancel_request_id);
ALTER TABLE pos_sale ADD FOREIGN KEY(tenant_id,cancelled_by) REFERENCES app_user(tenant_id,id);
ALTER TABLE pos_sale ADD CHECK((status='COMPLETED' AND cancel_request_id IS NULL AND cancel_fingerprint IS NULL AND cancel_note IS NULL AND cancelled_by IS NULL AND cancelled_at IS NULL) OR (status='CANCELLED' AND cancel_request_id IS NOT NULL AND cancel_fingerprint IS NOT NULL AND cancel_note IS NOT NULL AND cancelled_by IS NOT NULL AND cancelled_at IS NOT NULL));
ALTER TABLE pos_sale_item ADD COLUMN return_operation_id UUID;
ALTER TABLE pos_sale_item ADD FOREIGN KEY(tenant_id,return_operation_id) REFERENCES stock_operation(tenant_id,id);
ALTER TABLE pos_sale_item ADD UNIQUE(tenant_id,return_operation_id);
CREATE TABLE pos_sale_payment (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, sale_id UUID NOT NULL, position INTEGER NOT NULL,
 method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','PIX','BANK_TRANSFER')), account_id UUID NOT NULL,
 amount NUMERIC(19,2) NOT NULL CHECK(amount>0), tendered NUMERIC(19,2) NOT NULL CHECK(tendered>=amount),
 change_amount NUMERIC(19,2) NOT NULL CHECK(change_amount=tendered-amount), reference VARCHAR(160) NOT NULL, external_reference VARCHAR(160),
 status VARCHAR(12) NOT NULL DEFAULT 'CONFIRMED' CHECK(status IN ('CONFIRMED','REFUNDED')),
 treasury_operation_id UUID NOT NULL, reversal_operation_id UUID,
 UNIQUE(tenant_id,sale_id,position), UNIQUE(tenant_id,treasury_operation_id), UNIQUE(tenant_id,reversal_operation_id),
 UNIQUE(tenant_id,account_id,method,external_reference),
 CHECK((method='CASH' AND external_reference IS NULL) OR (method<>'CASH' AND external_reference IS NOT NULL AND external_reference=reference)),
 CHECK((method='CASH' AND reference='') OR (method<>'CASH' AND tendered=amount AND change_amount=0 AND reference<>'')),
 CHECK((status='CONFIRMED' AND reversal_operation_id IS NULL) OR (status='REFUNDED' AND reversal_operation_id IS NOT NULL)),
 FOREIGN KEY(tenant_id,sale_id) REFERENCES pos_sale(tenant_id,id),
 FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id),
 FOREIGN KEY(tenant_id,reversal_operation_id) REFERENCES treasury_operation(tenant_id,id)
);
INSERT INTO pos_sale_payment(id,tenant_id,sale_id,position,method,account_id,amount,tendered,change_amount,reference,treasury_operation_id)
 SELECT s.id,s.tenant_id,s.id,0,'CASH',c.account_id,s.total,s.tendered,s.change_amount,'',s.treasury_operation_id FROM pos_sale s JOIN pos_cash_session c ON c.tenant_id=s.tenant_id AND c.id=s.session_id;
