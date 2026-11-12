CREATE TABLE treasury_account (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, request_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 branch_id UUID NOT NULL, code VARCHAR(40) NOT NULL, name VARCHAR(160) NOT NULL,
 kind VARCHAR(10) NOT NULL CHECK(kind IN ('CASH','BANK')), balance NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK(balance>=0),
 created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(tenant_id,id), UNIQUE(tenant_id,code), UNIQUE(tenant_id,request_id),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE treasury_operation (
 id UUID NOT NULL, tenant_id UUID NOT NULL, fingerprint VARCHAR(64) NOT NULL,
 source_id UUID, destination_id UUID, amount NUMERIC(19,2) NOT NULL CHECK(amount>0), note VARCHAR(500) NOT NULL,
 created_by UUID NOT NULL, created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,id),
 CHECK(source_id IS NOT NULL OR destination_id IS NOT NULL),
 CHECK(source_id IS NULL OR destination_id IS NULL OR source_id<>destination_id),
 FOREIGN KEY(tenant_id,source_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,destination_id) REFERENCES treasury_account(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE treasury_entry (
 id UUID PRIMARY KEY, tenant_id UUID NOT NULL, operation_id UUID NOT NULL, account_id UUID NOT NULL,
 sequence BIGINT NOT NULL CHECK(sequence>0), delta NUMERIC(19,2) NOT NULL CHECK(delta<>0), balance_after NUMERIC(19,2) NOT NULL CHECK(balance_after>=0),
 UNIQUE(tenant_id,account_id,sequence),
 UNIQUE(tenant_id,operation_id,account_id),
 FOREIGN KEY(tenant_id,operation_id) REFERENCES treasury_operation(tenant_id,id),
 FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id)
);
CREATE INDEX ix_treasury_account_branch ON treasury_account(tenant_id,branch_id);
CREATE INDEX ix_treasury_entry_account ON treasury_entry(tenant_id,account_id);
