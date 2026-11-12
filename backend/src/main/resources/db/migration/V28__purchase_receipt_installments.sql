CREATE TABLE purchase_receipt_installment (
 tenant_id UUID NOT NULL, receipt_id UUID NOT NULL, position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 120),
 payable_id UUID NOT NULL,
 PRIMARY KEY(tenant_id,receipt_id,position), UNIQUE(tenant_id,payable_id),
 FOREIGN KEY(tenant_id,receipt_id) REFERENCES purchase_receipt_finance(tenant_id,receipt_id),
 FOREIGN KEY(tenant_id,payable_id,receipt_id) REFERENCES payable(tenant_id,id,purchase_receipt_id)
);
INSERT INTO purchase_receipt_installment(tenant_id,receipt_id,position,payable_id)
 SELECT tenant_id,receipt_id,1,payable_id FROM purchase_receipt_finance;
