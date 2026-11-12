package db.migration;
import org.flywaydb.core.api.migration.BaseJavaMigration;
import org.flywaydb.core.api.migration.Context;
import java.util.ArrayList;
public class V14__treasury_receivable_origins extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 1;}
 @Override public void migrate(Context context)throws Exception{
  // V12 used an automatically named check. Locate its name on both PostgreSQL and H2.
  var names=new ArrayList<String>();
  try(var statement=context.getConnection().prepareStatement("SELECT t.constraint_name FROM information_schema.table_constraints t JOIN information_schema.check_constraints c ON c.constraint_schema=t.constraint_schema AND c.constraint_name=t.constraint_name WHERE LOWER(t.table_schema)=LOWER(CURRENT_SCHEMA) AND LOWER(t.table_name)='treasury_operation' AND t.constraint_type='CHECK' AND LOWER(c.check_clause) LIKE '%payable_payment%'" );var rows=statement.executeQuery()){
   while(rows.next())names.add(rows.getString(1));
  }
  if(names.size()!=1)throw new IllegalStateException("Expected exactly one treasury origin constraint.");
  String name=names.getFirst();if(!name.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected constraint identifier.");
  try(var statement=context.getConnection().createStatement()){
   statement.execute("ALTER TABLE treasury_operation DROP CONSTRAINT \""+name+"\"");
   statement.execute("ALTER TABLE treasury_operation ADD CONSTRAINT ck_treasury_origin CHECK(origin IN ('MANUAL','PAYABLE_PAYMENT','PAYABLE_REVERSAL','RECEIVABLE_RECEIPT','RECEIVABLE_REVERSAL'))");
  }
 }
}
