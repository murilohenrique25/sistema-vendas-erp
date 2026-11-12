package db.migration;
import java.math.*;
import java.util.*;
import org.flywaydb.core.api.migration.*;

public class V27__purchase_receipt_finance extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 1;}
 @Override public void migrate(Context context)throws Exception{
  var c=context.getConnection();
  try(var s=c.createStatement()){s.execute("ALTER TABLE purchase_receipt_line ADD COLUMN amount NUMERIC(19,2) NOT NULL DEFAULT 0 CHECK(amount>=0)");}
  // Old receipts have quantities/costs but no monetary allocation. Stable chronological backfill.
  var quantities=new HashMap<String,BigDecimal>();
  try(var read=c.prepareStatement("SELECT r.tenant_id,r.receipt_id,r.order_line_id,r.quantity,l.unit_cost FROM purchase_receipt_line r JOIN purchase_receipt h ON h.tenant_id=r.tenant_id AND h.id=r.receipt_id JOIN purchase_order_line l ON l.tenant_id=r.tenant_id AND l.order_id=r.order_id AND l.id=r.order_line_id ORDER BY h.created_at,h.id,r.order_line_id");var rows=read.executeQuery();var write=c.prepareStatement("UPDATE purchase_receipt_line SET amount=? WHERE tenant_id=? AND receipt_id=? AND order_line_id=?")){
   while(rows.next()){
    UUID tenant=rows.getObject(1,UUID.class),receipt=rows.getObject(2,UUID.class),line=rows.getObject(3,UUID.class);String key=tenant+":"+line;
    BigDecimal before=quantities.getOrDefault(key,BigDecimal.ZERO),after=before.add(rows.getBigDecimal(4)),cost=rows.getBigDecimal(5);
    BigDecimal amount=after.multiply(cost).setScale(2,RoundingMode.HALF_UP).subtract(before.multiply(cost).setScale(2,RoundingMode.HALF_UP));quantities.put(key,after);
    write.setBigDecimal(1,amount);write.setObject(2,tenant);write.setObject(3,receipt);write.setObject(4,line);write.executeUpdate();
   }
  }
  try(var s=c.createStatement()){
   s.execute("ALTER TABLE payable ADD UNIQUE(tenant_id,id,purchase_receipt_id)");
   s.execute("CREATE INDEX ix_payable_purchase_receipt ON payable(tenant_id,purchase_receipt_id,status)");
   s.execute("CREATE TABLE purchase_receipt_finance (tenant_id UUID NOT NULL,receipt_id UUID NOT NULL,payable_id UUID NOT NULL,request_id UUID NOT NULL,fingerprint VARCHAR(64) NOT NULL,created_by UUID NOT NULL,created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,PRIMARY KEY(tenant_id,receipt_id),UNIQUE(tenant_id,payable_id),UNIQUE(tenant_id,request_id),FOREIGN KEY(tenant_id,receipt_id) REFERENCES purchase_receipt(tenant_id,id),FOREIGN KEY(tenant_id,payable_id,receipt_id) REFERENCES payable(tenant_id,id,purchase_receipt_id),FOREIGN KEY(tenant_id,created_by) REFERENCES app_user(tenant_id,id))");
  }
 }
}
