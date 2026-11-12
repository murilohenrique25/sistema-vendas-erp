CREATE TABLE fiscal_establishment (
 tenant_id UUID NOT NULL,branch_id UUID NOT NULL,environment VARCHAR(20) NOT NULL CHECK(environment IN ('HOMOLOGATION','PRODUCTION')),
 provider VARCHAR(30) NOT NULL CHECK(provider='DIRECT_SEFAZ'),issuer_json TEXT NOT NULL,credential_ciphertext TEXT,version BIGINT NOT NULL DEFAULT 1,
 created_by UUID NOT NULL,updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 PRIMARY KEY(tenant_id,branch_id,environment),FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id)
);
CREATE TABLE fiscal_product_profile (
 tenant_id UUID NOT NULL,branch_id UUID NOT NULL,product_id UUID NOT NULL,ncm VARCHAR(8) NOT NULL,cest VARCHAR(7),cfop VARCHAR(4) NOT NULL,
 origin VARCHAR(1) NOT NULL,tax_fields_json TEXT NOT NULL,version BIGINT NOT NULL DEFAULT 1,
 PRIMARY KEY(tenant_id,branch_id,product_id),FOREIGN KEY(tenant_id,branch_id) REFERENCES branch(tenant_id,id),FOREIGN KEY(tenant_id,product_id) REFERENCES product(tenant_id,id)
);
