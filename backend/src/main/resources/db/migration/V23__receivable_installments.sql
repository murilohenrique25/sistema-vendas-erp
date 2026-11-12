CREATE TABLE receivable_plan (
 id UUID PRIMARY KEY,
 tenant_id UUID NOT NULL REFERENCES tenant(id),
 request_id UUID NOT NULL,
 fingerprint VARCHAR(64) NOT NULL,
 branch_id UUID NOT NULL,
 customer_id UUID NOT NULL,
 reference VARCHAR(50) NOT NULL,
 amount NUMERIC(19,2) NOT NULL CHECK(amount>0),
 installment_count INTEGER NOT NULL CHECK(installment_count BETWEEN 1 AND 120),
 first_due_on DATE NOT NULL,
 interval_months INTEGER NOT NULL CHECK(interval_months BETWEEN 1 AND 12),
 created_by UUID NOT NULL,
 created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 UNIQUE(tenant_id,id),
 UNIQUE(tenant_id,request_id),
 UNIQUE(tenant_id,reference),
 FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),
 FOREIGN KEY(tenant_id,customer_id) REFERENCES customer(tenant_id,id),
 FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE receivable_installment (
 tenant_id UUID NOT NULL,
 plan_id UUID NOT NULL,
 position INTEGER NOT NULL CHECK(position BETWEEN 1 AND 120),
 receivable_id UUID NOT NULL,
 PRIMARY KEY(tenant_id,plan_id,position),
 UNIQUE(tenant_id,receivable_id),
 FOREIGN KEY(tenant_id,plan_id) REFERENCES receivable_plan(tenant_id,id),
 FOREIGN KEY(tenant_id,receivable_id) REFERENCES receivable(tenant_id,id)
);
CREATE INDEX ix_receivable_plan_branch ON receivable_plan(tenant_id,branch_id,created_at);
