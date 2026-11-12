package db.migration;
import org.flywaydb.core.api.migration.*;
import java.util.ArrayList;
public class V25__credit_sale_returns extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 1;}
 @Override public void migrate(Context context)throws Exception{
  var c=context.getConnection();var names=new ArrayList<String>();
  try(var q=c.prepareStatement("SELECT t.constraint_name FROM information_schema.table_constraints t JOIN information_schema.check_constraints c ON c.constraint_schema=t.constraint_schema AND c.constraint_name=t.constraint_name WHERE LOWER(t.table_schema)=LOWER(CURRENT_SCHEMA) AND LOWER(t.table_name)='receivable' AND t.constraint_type='CHECK' AND LOWER(c.check_clause) LIKE '%status%' AND LOWER(c.check_clause) LIKE '%received_amount%'" );var r=q.executeQuery()){while(r.next())names.add(r.getString(1));}
  if(names.size()!=1)throw new IllegalStateException("Expected one receivable settlement check.");
  try(var s=c.createStatement()){
   String name=names.getFirst();if(!name.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected constraint identifier.");s.execute("ALTER TABLE receivable DROP CONSTRAINT \""+name+"\"");
   s.execute("ALTER TABLE receivable ADD COLUMN credited_amount NUMERIC(19,2) NOT NULL DEFAULT 0");
   s.execute("ALTER TABLE receivable ADD CONSTRAINT ck_receivable_credits CHECK(credited_amount>=0 AND received_amount+credited_amount<=amount)");
   s.execute("ALTER TABLE receivable ADD CONSTRAINT ck_receivable_settlement CHECK((status='OPEN' AND received_amount=0 AND credited_amount<amount) OR (status='PARTIAL' AND received_amount>0 AND received_amount+credited_amount<amount) OR (status='RECEIVED' AND received_amount=amount AND credited_amount=0) OR (status='CREDITED' AND credited_amount>0 AND received_amount+credited_amount=amount) OR (status='CANCELLED' AND received_amount=0 AND credited_amount=0))");
   s.execute("CREATE TABLE pos_sale_return_debt_credit (tenant_id UUID NOT NULL,return_id UUID NOT NULL,sale_id UUID NOT NULL,receivable_id UUID NOT NULL,amount NUMERIC(19,2) NOT NULL CHECK(amount>0),PRIMARY KEY(tenant_id,return_id,receivable_id),FOREIGN KEY(tenant_id,return_id,sale_id) REFERENCES pos_sale_return(tenant_id,id,sale_id),FOREIGN KEY(tenant_id,receivable_id) REFERENCES receivable(tenant_id,id))");
   s.execute("CREATE TABLE pos_sale_return_receipt_refund (id UUID PRIMARY KEY,tenant_id UUID NOT NULL,return_id UUID NOT NULL,sale_id UUID NOT NULL,receipt_id UUID NOT NULL,receivable_id UUID NOT NULL,method VARCHAR(20) NOT NULL CHECK(method IN ('CASH','PIX','BANK_TRANSFER')),account_id UUID NOT NULL,amount NUMERIC(19,2) NOT NULL CHECK(amount>0),reference VARCHAR(160) NOT NULL,external_reference VARCHAR(160),treasury_operation_id UUID NOT NULL,UNIQUE(tenant_id,return_id,receipt_id),UNIQUE(tenant_id,treasury_operation_id),UNIQUE(tenant_id,account_id,method,external_reference),CHECK((method='CASH' AND reference='' AND external_reference IS NULL) OR (method<>'CASH' AND reference<>'' AND external_reference IS NOT NULL AND external_reference=reference)),FOREIGN KEY(tenant_id,return_id,sale_id) REFERENCES pos_sale_return(tenant_id,id,sale_id),FOREIGN KEY(tenant_id,receipt_id) REFERENCES receivable_receipt(tenant_id,id),FOREIGN KEY(tenant_id,receivable_id) REFERENCES receivable(tenant_id,id),FOREIGN KEY(tenant_id,account_id) REFERENCES treasury_account(tenant_id,id),FOREIGN KEY(tenant_id,treasury_operation_id) REFERENCES treasury_operation(tenant_id,id))");
   s.execute("CREATE INDEX ix_credit_return_receipt ON pos_sale_return_receipt_refund(tenant_id,receipt_id)");
   s.execute("CREATE INDEX ix_credit_return_title ON pos_sale_return_debt_credit(tenant_id,receivable_id)");
   s.execute("CREATE INDEX ix_receivable_customer_debt ON receivable(tenant_id,customer_id,status,due_on)");
  }
 }
}
