CREATE TABLE receivable (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL,
    request_id UUID NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    branch_id UUID NOT NULL,
    customer_id UUID NOT NULL,
    customer_name VARCHAR(200) NOT NULL,
    reference VARCHAR(60) NOT NULL,
    document VARCHAR(160) NOT NULL,
    description VARCHAR(500) NOT NULL,
    issued_on DATE NOT NULL,
    due_on DATE NOT NULL CHECK(due_on>=issued_on),
    amount NUMERIC(19,2) NOT NULL CHECK(amount>0),
    received_amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK(received_amount>=0 AND received_amount<=amount),
    status VARCHAR(15) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    cancellation_note VARCHAR(500),
    UNIQUE(tenant_id,id),
    UNIQUE(tenant_id,request_id),
    UNIQUE(tenant_id,reference),
    CHECK((status='OPEN' AND received_amount=0) OR (status='PARTIAL' AND received_amount>0 AND received_amount<amount) OR (status='RECEIVED' AND received_amount=amount) OR (status='CANCELLED' AND received_amount=0)),
    FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
    FOREIGN KEY(tenant_id,customer_id) REFERENCES customer(tenant_id,id),
    FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX ix_receivable_tenant_branch_due ON receivable(tenant_id,branch_id,due_on);
CREATE TABLE receivable_receipt (
    id UUID NOT NULL,
    tenant_id UUID NOT NULL,
    receivable_id UUID NOT NULL,
    fingerprint VARCHAR(64) NOT NULL,
    amount NUMERIC(19,2) NOT NULL CHECK(amount>0),
    received_on DATE NOT NULL,
    method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','BANK_TRANSFER','PIX','OTHER')),
    reference VARCHAR(160) NOT NULL,
    note VARCHAR(500) NOT NULL,
    status VARCHAR(15) NOT NULL CHECK(status IN ('POSTED','REVERSED')),
    created_by UUID NOT NULL,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    reversal_request_id UUID,
    reversal_note VARCHAR(500),
    reversed_by UUID,
    reversed_at TIMESTAMP WITH TIME ZONE,
    PRIMARY KEY(tenant_id,id),
    UNIQUE(tenant_id,reversal_request_id),
    CHECK((status='POSTED' AND reversal_request_id IS NULL AND reversal_note IS NULL AND reversed_by IS NULL AND reversed_at IS NULL) OR (status='REVERSED' AND reversal_request_id IS NOT NULL AND reversal_note IS NOT NULL AND reversed_by IS NOT NULL AND reversed_at IS NOT NULL)),
    FOREIGN KEY(tenant_id,receivable_id) REFERENCES receivable(tenant_id,id),
    FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id),
    FOREIGN KEY(tenant_id,reversed_by) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX ix_receipt_receivable ON receivable_receipt(tenant_id,receivable_id,created_at);

ALTER TABLE receivable_receipt ADD COLUMN treasury_account_id UUID;
ALTER TABLE receivable_receipt ADD COLUMN treasury_operation_id UUID;
ALTER TABLE receivable_receipt ADD COLUMN treasury_reversal_operation_id UUID;
ALTER TABLE receivable_receipt ADD CONSTRAINT fk_receipt_treasury_account FOREIGN KEY(tenant_id,treasury_account_id) REFERENCES treasury_account(tenant_id,id);
ALTER TABLE receivable_receipt ADD CONSTRAINT fk_receipt_treasury_operation FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id);
ALTER TABLE receivable_receipt ADD CONSTRAINT fk_receipt_treasury_reversal FOREIGN KEY(tenant_id,treasury_reversal_operation_id) REFERENCES treasury_operation(tenant_id,id);
ALTER TABLE receivable_receipt ADD CONSTRAINT uq_receipt_treasury_operation UNIQUE(tenant_id,treasury_operation_id);
ALTER TABLE receivable_receipt ADD CONSTRAINT uq_receipt_treasury_reversal UNIQUE(tenant_id,treasury_reversal_operation_id);
ALTER TABLE receivable_receipt ADD CONSTRAINT ck_receipt_treasury_link CHECK(
 (treasury_account_id IS NULL AND treasury_operation_id IS NULL AND treasury_reversal_operation_id IS NULL)
 OR (treasury_account_id IS NOT NULL AND treasury_operation_id IS NOT NULL AND
 ((status='POSTED' AND treasury_reversal_operation_id IS NULL) OR (status='REVERSED' AND treasury_reversal_operation_id IS NOT NULL)))
);
