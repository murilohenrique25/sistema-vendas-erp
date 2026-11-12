CREATE TABLE access_profile (
    id UUID PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id),
    name VARCHAR(160) NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(tenant_id,id),
    UNIQUE(tenant_id,name)
);
CREATE TABLE access_profile_permission (
    tenant_id UUID NOT NULL,
    profile_id UUID NOT NULL,
    permission VARCHAR(80) NOT NULL,
    PRIMARY KEY(profile_id,permission),
    FOREIGN KEY(tenant_id,profile_id) REFERENCES access_profile(tenant_id,id)
);
