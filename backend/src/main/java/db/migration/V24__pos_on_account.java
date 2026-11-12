package db.migration;
import org.flywaydb.core.api.migration.*;
import java.util.ArrayList;
public class V24__pos_on_account extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 1;}
 @Override public void migrate(Context context)throws Exception{
  var connection=context.getConnection();var names=new ArrayList<String>();
  // PostgreSQL also exposes column NOT NULL checks here; only the two monetary checks reference total.
  try(var statement=connection.prepareStatement("SELECT t.constraint_name FROM information_schema.table_constraints t JOIN information_schema.check_constraints c ON c.constraint_schema=t.constraint_schema AND c.constraint_name=t.constraint_name WHERE LOWER(t.table_schema)=LOWER(CURRENT_SCHEMA) AND LOWER(t.table_name)='pos_sale' AND t.constraint_type='CHECK' AND LOWER(c.check_clause) LIKE '%tendered%' AND LOWER(c.check_clause) LIKE '%total%'" );var rows=statement.executeQuery()){while(rows.next())names.add(rows.getString(1));}
  if(names.size()!=2)throw new IllegalStateException("Expected two original sale tender checks.");
  try(var s=connection.createStatement()){
   for(String name:names){if(!name.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected constraint identifier.");s.execute("ALTER TABLE pos_sale DROP CONSTRAINT \""+name+"\"");}
   s.execute("ALTER TABLE pos_sale ALTER COLUMN treasury_operation_id DROP NOT NULL");
   s.execute("ALTER TABLE pos_sale ADD COLUMN credit_amount NUMERIC(19,2) NOT NULL DEFAULT 0");
   s.execute("ALTER TABLE pos_sale ADD COLUMN credit_plan_id UUID");
   s.execute("ALTER TABLE pos_sale ADD COLUMN credit_customer_id UUID");
   s.execute("ALTER TABLE pos_sale ADD CONSTRAINT ck_sale_tender CHECK(tendered>=total-credit_amount AND change_amount=tendered-(total-credit_amount))");
   s.execute("ALTER TABLE pos_sale ADD CONSTRAINT ck_sale_credit CHECK((credit_amount=0 AND credit_plan_id IS NULL AND credit_customer_id IS NULL AND treasury_operation_id IS NOT NULL) OR (credit_amount>0 AND credit_amount<=total AND credit_plan_id IS NOT NULL AND credit_customer_id IS NOT NULL AND ((credit_amount=total AND treasury_operation_id IS NULL) OR (credit_amount<total AND treasury_operation_id IS NOT NULL))))");
   s.execute("ALTER TABLE pos_sale ADD UNIQUE(tenant_id,credit_plan_id)");
   s.execute("ALTER TABLE pos_sale ADD FOREIGN KEY(tenant_id,credit_plan_id) REFERENCES receivable_plan(tenant_id,id)");
   s.execute("ALTER TABLE pos_sale ADD FOREIGN KEY(tenant_id,credit_customer_id) REFERENCES customer(tenant_id,id)");
   s.execute("CREATE TABLE customer_credit_policy (tenant_id UUID NOT NULL,customer_id UUID NOT NULL,credit_limit NUMERIC(19,2) NOT NULL CHECK(credit_limit>=0),blocked BOOLEAN NOT NULL,version BIGINT NOT NULL,PRIMARY KEY(tenant_id,customer_id),FOREIGN KEY(tenant_id,customer_id) REFERENCES customer(tenant_id,id))");
  }
 }
}
