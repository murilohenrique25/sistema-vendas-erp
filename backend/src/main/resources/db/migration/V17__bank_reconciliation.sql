ALTER TABLE treasury_entry ADD CONSTRAINT uq_entry_account_identity UNIQUE(tenant_id,account_id,id);
ALTER TABLE treasury_entry ADD CONSTRAINT uq_entry_tenant_identity UNIQUE(tenant_id,id);
CREATE TABLE bank_import (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, account_id UUID NOT NULL, request_id UUID NOT NULL,
 fingerprint VARCHAR(64) NOT NULL, reference VARCHAR(160) NOT NULL, new_count INTEGER NOT NULL, reused_count INTEGER NOT NULL,
 created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,request_id),
 FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE bank_statement_line (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, account_id UUID NOT NULL, external_id VARCHAR(100) NOT NULL,
 fingerprint VARCHAR(64) NOT NULL, posted_on DATE NOT NULL, amount NUMERIC(19,2) NOT NULL CHECK(amount<>0),
 description VARCHAR(500) NOT NULL, status VARCHAR(15) NOT NULL DEFAULT 'UNMATCHED' CHECK(status IN ('UNMATCHED','MATCHED','IGNORED')),
 version BIGINT NOT NULL DEFAULT 0, matched_entry_id UUID, review_note VARCHAR(500), reviewed_by UUID,
 reviewed_at TIMESTAMP WITH TIME ZONE, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,account_id,external_id), UNIQUE(tenant_id,matched_entry_id),
 CHECK((status='UNMATCHED' AND matched_entry_id IS NULL AND review_note IS NULL AND reviewed_by IS NULL AND reviewed_at IS NULL) OR
 (status='MATCHED' AND matched_entry_id IS NOT NULL AND review_note IS NOT NULL AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL) OR
 (status='IGNORED' AND matched_entry_id IS NULL AND review_note IS NOT NULL AND reviewed_by IS NOT NULL AND reviewed_at IS NOT NULL)),
 FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,account_id,matched_entry_id) REFERENCES treasury_entry(tenant_id,account_id,id),
 FOREIGN KEY(tenant_id,reviewed_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE bank_import_line (
 tenant_id UUID NOT NULL, import_id UUID NOT NULL, line_id UUID NOT NULL,
 PRIMARY KEY(tenant_id,import_id,line_id),
 FOREIGN KEY(tenant_id,import_id) REFERENCES bank_import(tenant_id,id),
 FOREIGN KEY(tenant_id,line_id) REFERENCES bank_statement_line(tenant_id,id)
);
CREATE TABLE bank_statement_review (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, line_id UUID NOT NULL, request_id UUID NOT NULL,
 fingerprint VARCHAR(64) NOT NULL, action VARCHAR(10) NOT NULL CHECK(action IN ('MATCH','IGNORE','RESET')),
 entry_id UUID, note VARCHAR(500) NOT NULL, created_by UUID NOT NULL, line_version BIGINT NOT NULL CHECK(line_version>0),
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,line_id,line_version), FOREIGN KEY(tenant_id,line_id) REFERENCES bank_statement_line(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id), FOREIGN KEY(tenant_id,entry_id) REFERENCES treasury_entry(tenant_id,id)
);
CREATE INDEX ix_bank_line_account_status_date ON bank_statement_line(tenant_id,account_id,status,posted_on);
CREATE INDEX ix_bank_review_line ON bank_statement_review(tenant_id,line_id,created_at);
CREATE INDEX ix_bank_import_account ON bank_import(tenant_id,account_id,created_at);
