ALTER TABLE pos_sale_item ADD COLUMN gross_total NUMERIC(19,2) NOT NULL DEFAULT 0;
ALTER TABLE pos_sale_item ADD COLUMN discount_amount NUMERIC(19,2) NOT NULL DEFAULT 0;
UPDATE pos_sale_item SET gross_total=total;
ALTER TABLE pos_sale_item ADD CHECK(gross_total>=0 AND discount_amount>=0 AND discount_amount<=gross_total AND total=gross_total-discount_amount);
CREATE TABLE pos_discount_policy (
 tenant_id UUID NOT NULL, branch_id UUID NOT NULL, operator_percent NUMERIC(5,2) NOT NULL CHECK(operator_percent>=0 AND operator_percent<=100),
 version BIGINT NOT NULL CHECK(version>0), updated_by UUID NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 PRIMARY KEY(tenant_id,branch_id), FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id), FOREIGN KEY(tenant_id,updated_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE pos_discount_request (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 branch_id UUID NOT NULL, requested_by UUID NOT NULL, cart_hash VARCHAR(64) NOT NULL, items_json TEXT NOT NULL,
 gross_total NUMERIC(19,2) NOT NULL CHECK(gross_total>0), discount_total NUMERIC(19,2) NOT NULL CHECK(discount_total>0 AND discount_total<gross_total),
 note VARCHAR(500) NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 status VARCHAR(12) NOT NULL DEFAULT 'PENDING' CHECK(status IN ('PENDING','APPROVED','REJECTED','CONSUMED')), version BIGINT NOT NULL DEFAULT 0,
 policy_version BIGINT NOT NULL, decision_request_id UUID, decision_fingerprint VARCHAR(64), decision_note VARCHAR(500), decided_by UUID, decided_at TIMESTAMP WITH TIME ZONE, expires_at TIMESTAMP WITH TIME ZONE, sale_id UUID,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,decision_request_id), UNIQUE(tenant_id,sale_id),
 CHECK((status='PENDING' AND decided_by IS NULL AND decided_at IS NULL AND expires_at IS NULL AND sale_id IS NULL AND decision_request_id IS NULL AND decision_fingerprint IS NULL AND decision_note IS NULL) OR
 (status IN ('APPROVED','REJECTED','CONSUMED') AND decided_by IS NOT NULL AND decided_at IS NOT NULL AND decision_request_id IS NOT NULL AND decision_fingerprint IS NOT NULL AND decision_note IS NOT NULL AND requested_by<>decided_by AND
 ((status='REJECTED' AND expires_at IS NULL AND sale_id IS NULL) OR (status='APPROVED' AND expires_at IS NOT NULL AND sale_id IS NULL) OR (status='CONSUMED' AND expires_at IS NOT NULL AND sale_id IS NOT NULL)))),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id), FOREIGN KEY(tenant_id,requested_by) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY(tenant_id,decided_by) REFERENCES app_user(tenant_id,id), FOREIGN KEY(tenant_id,sale_id) REFERENCES pos_sale(tenant_id,id)
);
CREATE INDEX ix_discount_requests ON pos_discount_request(tenant_id,branch_id,status,created_at);
CREATE TABLE pos_suspension (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, branch_id UUID NOT NULL, original_session_id UUID NOT NULL, last_session_id UUID NOT NULL,
 created_by UUID NOT NULL, updated_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL, updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 note VARCHAR(500) NOT NULL, items_json TEXT NOT NULL, cart_hash VARCHAR(64) NOT NULL,
 gross_total NUMERIC(19,2) NOT NULL CHECK(gross_total>=0), discount_total NUMERIC(19,2) NOT NULL CHECK(discount_total>=0 AND discount_total<=gross_total), total NUMERIC(19,2) NOT NULL CHECK(total=gross_total-discount_total AND total>0),
 status VARCHAR(12) NOT NULL DEFAULT 'SUSPENDED' CHECK(status IN ('SUSPENDED','COMPLETED','DISCARDED')), version BIGINT NOT NULL DEFAULT 0,
 sale_id UUID, discard_note VARCHAR(500),
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,sale_id),
 CHECK((status='SUSPENDED' AND sale_id IS NULL AND discard_note IS NULL) OR (status='COMPLETED' AND sale_id IS NOT NULL AND discard_note IS NULL) OR (status='DISCARDED' AND sale_id IS NULL AND discard_note IS NOT NULL)),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id), FOREIGN KEY(tenant_id,original_session_id) REFERENCES pos_cash_session(tenant_id,id),
 FOREIGN KEY(tenant_id,last_session_id) REFERENCES pos_cash_session(tenant_id,id), FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY(tenant_id,updated_by) REFERENCES app_user(tenant_id,id), FOREIGN KEY(tenant_id,sale_id) REFERENCES pos_sale(tenant_id,id)
);
CREATE INDEX ix_pos_suspension_branch ON pos_suspension(tenant_id,branch_id,status,updated_at);
CREATE TABLE pos_suspension_command (
 tenant_id UUID NOT NULL, request_id UUID NOT NULL, suspension_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 kind VARCHAR(10) NOT NULL CHECK(kind IN ('CREATE','UPDATE','DISCARD')), PRIMARY KEY(tenant_id,request_id),
 FOREIGN KEY(tenant_id,suspension_id) REFERENCES pos_suspension(tenant_id,id)
);
ALTER TABLE pos_sale ADD COLUMN discount_approval_id UUID;
ALTER TABLE pos_sale ADD COLUMN suspension_id UUID;
ALTER TABLE pos_sale ADD UNIQUE(tenant_id,discount_approval_id);
ALTER TABLE pos_sale ADD UNIQUE(tenant_id,suspension_id);
ALTER TABLE pos_sale ADD FOREIGN KEY(tenant_id,discount_approval_id) REFERENCES pos_discount_request(tenant_id,id);
ALTER TABLE pos_sale ADD FOREIGN KEY(tenant_id,suspension_id) REFERENCES pos_suspension(tenant_id,id);
