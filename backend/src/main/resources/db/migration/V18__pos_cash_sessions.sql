ALTER TABLE treasury_operation DROP CONSTRAINT ck_treasury_origin;
ALTER TABLE treasury_operation ADD CONSTRAINT ck_treasury_origin CHECK(origin IN ('MANUAL','PAYABLE_PAYMENT','PAYABLE_REVERSAL','RECEIVABLE_RECEIPT','RECEIVABLE_REVERSAL','POS_SUPPLY','POS_WITHDRAW'));
CREATE TABLE pos_terminal (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 branch_id UUID NOT NULL, account_id UUID NOT NULL, code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
 active BOOLEAN NOT NULL DEFAULT TRUE, version BIGINT NOT NULL DEFAULT 0,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,code), UNIQUE(tenant_id,account_id),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id), FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id)
);
CREATE TABLE pos_cash_session (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 terminal_id UUID NOT NULL, account_id UUID NOT NULL, branch_id UUID NOT NULL, operator_id UUID NOT NULL,
 opening_amount NUMERIC(19,2) NOT NULL CHECK(opening_amount>=0), opening_note VARCHAR(500) NOT NULL,
 status VARCHAR(10) NOT NULL DEFAULT 'OPEN' CHECK(status IN ('OPEN','CLOSED')), version BIGINT NOT NULL DEFAULT 0,
 opened_at TIMESTAMP WITH TIME ZONE NOT NULL, open_terminal_id UUID, open_operator_id UUID, open_account_id UUID,
 close_request_id UUID, close_fingerprint VARCHAR(64), counted_amount NUMERIC(19,2), expected_amount NUMERIC(19,2),
 difference NUMERIC(19,2), close_note VARCHAR(500), closed_by UUID, closed_at TIMESTAMP WITH TIME ZONE,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,close_request_id),
 UNIQUE(tenant_id,open_terminal_id), UNIQUE(tenant_id,open_operator_id), UNIQUE(tenant_id,open_account_id),
 CHECK((status='OPEN' AND open_terminal_id=terminal_id AND open_operator_id=operator_id AND open_account_id=account_id
 AND open_terminal_id IS NOT NULL AND open_operator_id IS NOT NULL AND open_account_id IS NOT NULL
 AND close_request_id IS NULL AND close_fingerprint IS NULL AND counted_amount IS NULL AND expected_amount IS NULL
 AND difference IS NULL AND close_note IS NULL AND closed_by IS NULL AND closed_at IS NULL) OR
 (status='CLOSED' AND open_terminal_id IS NULL AND open_operator_id IS NULL AND open_account_id IS NULL
 AND close_request_id IS NOT NULL AND close_fingerprint IS NOT NULL AND counted_amount IS NOT NULL AND counted_amount>=0
 AND expected_amount IS NOT NULL AND expected_amount>=0 AND difference=counted_amount-expected_amount
 AND difference IS NOT NULL AND close_note IS NOT NULL AND closed_by IS NOT NULL AND closed_at IS NOT NULL)),
 FOREIGN KEY(tenant_id,terminal_id) REFERENCES pos_terminal(tenant_id,id),
 FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id), FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
 FOREIGN KEY(tenant_id,operator_id) REFERENCES app_user(tenant_id,id), FOREIGN KEY(tenant_id,closed_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE pos_cash_movement (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 session_id UUID NOT NULL, kind VARCHAR(10) NOT NULL CHECK(kind IN ('SUPPLY','WITHDRAW')),
 other_account_id UUID, amount NUMERIC(19,2) NOT NULL CHECK(amount>0), note VARCHAR(500) NOT NULL,
 treasury_operation_id UUID NOT NULL, created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,treasury_operation_id),
 FOREIGN KEY(tenant_id,session_id) REFERENCES pos_cash_session(tenant_id,id),
 FOREIGN KEY(tenant_id,other_account_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE INDEX ix_pos_session_branch ON pos_cash_session(tenant_id,branch_id,status,opened_at);
CREATE INDEX ix_pos_movement_session ON pos_cash_movement(tenant_id,session_id,created_at);
