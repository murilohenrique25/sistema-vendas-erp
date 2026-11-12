ALTER TABLE app_user ADD COLUMN all_branches BOOLEAN NOT NULL DEFAULT TRUE;
CREATE TABLE user_branch_access (
    tenant_id UUID NOT NULL,
    user_id UUID NOT NULL,
    branch_id UUID NOT NULL,
    PRIMARY KEY(user_id,branch_id),
    FOREIGN KEY(tenant_id,user_id) REFERENCES app_user(tenant_id,id),
    FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id)
);
