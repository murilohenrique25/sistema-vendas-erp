ALTER TABLE stock_movement ADD UNIQUE(tenant_id,id);
CREATE TABLE stock_lot (
 id UUID PRIMARY KEY,tenant_id UUID NOT NULL,product_id UUID NOT NULL,code VARCHAR(80) NOT NULL,expires_on DATE,
 UNIQUE(tenant_id,id),UNIQUE(tenant_id,product_id,code),FOREIGN KEY(tenant_id,product_id) REFERENCES product(tenant_id,id)
);
CREATE TABLE stock_lot_balance (
 tenant_id UUID NOT NULL,lot_id UUID NOT NULL,warehouse_id UUID NOT NULL,quantity NUMERIC(19,6) NOT NULL CHECK(quantity>=0),
 PRIMARY KEY(tenant_id,lot_id,warehouse_id),FOREIGN KEY(tenant_id,lot_id) REFERENCES stock_lot(tenant_id,id),FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id)
);
CREATE TABLE stock_lot_movement (
 id UUID PRIMARY KEY,tenant_id UUID NOT NULL,lot_id UUID NOT NULL,warehouse_id UUID NOT NULL,operation_id UUID NOT NULL,
 stock_movement_id UUID,source_operation_id UUID,quantity NUMERIC(19,6) NOT NULL CHECK(quantity<>0),kind VARCHAR(30) NOT NULL,
 actor_id UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
 FOREIGN KEY(tenant_id,lot_id) REFERENCES stock_lot(tenant_id,id),FOREIGN KEY(tenant_id,warehouse_id) REFERENCES warehouse(tenant_id,id),
 FOREIGN KEY(tenant_id,stock_movement_id) REFERENCES stock_movement(tenant_id,id),FOREIGN KEY(tenant_id,actor_id) REFERENCES app_user(tenant_id,id)
);
INSERT INTO stock_lot(id,tenant_id,product_id,code) SELECT p.id,p.tenant_id,p.id,'@UNASSIGNED' FROM product p WHERE EXISTS(SELECT 1 FROM stock_balance b WHERE b.tenant_id=p.tenant_id AND b.product_id=p.id AND b.quantity>0);
INSERT INTO stock_lot_balance(tenant_id,lot_id,warehouse_id,quantity) SELECT tenant_id,product_id,warehouse_id,quantity FROM stock_balance WHERE quantity>0;
CREATE INDEX ix_lot_product_expiry ON stock_lot(tenant_id,product_id,expires_on);
CREATE INDEX ix_lot_operation ON stock_lot_movement(tenant_id,operation_id);
CREATE INDEX ix_lot_source ON stock_lot_movement(tenant_id,source_operation_id,lot_id);
