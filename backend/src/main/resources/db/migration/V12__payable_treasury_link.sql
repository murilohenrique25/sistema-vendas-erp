ALTER TABLE treasury_operation ADD COLUMN origin VARCHAR(30) NOT NULL DEFAULT 'MANUAL' CHECK(origin IN ('MANUAL','PAYABLE_PAYMENT','PAYABLE_REVERSAL'));
ALTER TABLE payable_payment ADD COLUMN treasury_account_id UUID;
ALTER TABLE payable_payment ADD COLUMN treasury_operation_id UUID;
ALTER TABLE payable_payment ADD COLUMN treasury_reversal_operation_id UUID;
ALTER TABLE payable_payment ADD CONSTRAINT fk_payment_treasury_account FOREIGN KEY(tenant_id,treasury_account_id) REFERENCES treasury_account(tenant_id,id);
ALTER TABLE payable_payment ADD CONSTRAINT fk_payment_treasury_operation FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id);
ALTER TABLE payable_payment ADD CONSTRAINT fk_payment_treasury_reversal FOREIGN KEY(tenant_id,treasury_reversal_operation_id) REFERENCES treasury_operation(tenant_id,id);
ALTER TABLE payable_payment ADD CONSTRAINT uq_payment_treasury_operation UNIQUE(tenant_id,treasury_operation_id);
ALTER TABLE payable_payment ADD CONSTRAINT uq_payment_treasury_reversal UNIQUE(tenant_id,treasury_reversal_operation_id);
ALTER TABLE payable_payment ADD CONSTRAINT ck_payment_treasury_link CHECK(
 (treasury_account_id IS NULL AND treasury_operation_id IS NULL AND treasury_reversal_operation_id IS NULL)
 OR (treasury_account_id IS NOT NULL AND treasury_operation_id IS NOT NULL AND
 ((status='POSTED' AND treasury_reversal_operation_id IS NULL) OR (status='REVERSED' AND treasury_reversal_operation_id IS NOT NULL)))
);
