CREATE TABLE pos_pending_checkout (
 id UUID NOT NULL PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL,
 fingerprint VARCHAR(64) NOT NULL, session_id UUID NOT NULL, branch_id UUID NOT NULL,
 created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL,
 items_json TEXT NOT NULL, cart_hash VARCHAR(64) NOT NULL,
 total NUMERIC(19,2) NOT NULL CHECK(total>0),
 status VARCHAR(20) NOT NULL CHECK(status IN ('PENDING','COMPLETED','DISCARDED')),
 version BIGINT NOT NULL DEFAULT 0 CHECK(version>=0), sale_id UUID,
 discard_request_id UUID, discard_fingerprint VARCHAR(64), discard_note VARCHAR(500),
 updated_at TIMESTAMP WITH TIME ZONE NOT NULL,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,request_id), UNIQUE(tenant_id,sale_id),
 UNIQUE(tenant_id,discard_request_id),
 FOREIGN KEY(tenant_id,session_id) REFERENCES pos_cash_session(tenant_id,id),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id),
 FOREIGN KEY(tenant_id,sale_id) REFERENCES pos_sale(tenant_id,id),
 CHECK((status='COMPLETED' AND sale_id IS NOT NULL) OR (status<>'COMPLETED' AND sale_id IS NULL)),
 CHECK((status='DISCARDED' AND discard_request_id IS NOT NULL AND discard_fingerprint IS NOT NULL AND discard_note IS NOT NULL) OR (status<>'DISCARDED' AND discard_request_id IS NULL AND discard_fingerprint IS NULL AND discard_note IS NULL))
);
CREATE INDEX ix_pending_checkout_branch ON pos_pending_checkout(tenant_id,branch_id,status,created_at,id);
