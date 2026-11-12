package db.migration;
import org.flywaydb.core.api.migration.*;
import java.util.ArrayList;
public class V32__supplier_return_credits extends BaseJavaMigration {
 @Override public Integer getChecksum(){return 1;}
 @Override public void migrate(Context context)throws Exception{
  var c=context.getConnection();var names=new ArrayList<String>();
  try(var q=c.prepareStatement("SELECT t.constraint_name FROM information_schema.table_constraints t JOIN information_schema.check_constraints c ON c.constraint_schema=t.constraint_schema AND c.constraint_name=t.constraint_name WHERE LOWER(t.table_schema)=LOWER(CURRENT_SCHEMA) AND LOWER(t.table_name)='payable' AND t.constraint_type='CHECK' AND LOWER(c.check_clause) LIKE '%status%' AND LOWER(c.check_clause) LIKE '%paid_amount%'" );var r=q.executeQuery()){while(r.next())names.add(r.getString(1));}
  if(names.size()!=1)throw new IllegalStateException("Expected one payable settlement check.");
  try(var s=c.createStatement()){
   String name=names.getFirst();if(!name.matches("[A-Za-z0-9_]+"))throw new IllegalStateException("Unexpected constraint identifier.");s.execute("ALTER TABLE payable DROP CONSTRAINT \""+name+"\"");
   s.execute("ALTER TABLE payable ADD COLUMN credited_amount NUMERIC(19,2) NOT NULL DEFAULT 0");
   s.execute("ALTER TABLE payable ADD CONSTRAINT ck_payable_credits CHECK(credited_amount>=0 AND paid_amount+credited_amount<=amount)");
   s.execute("ALTER TABLE payable ADD CONSTRAINT ck_payable_settlement CHECK((status='OPEN' AND paid_amount=0 AND credited_amount<amount) OR (status='PARTIAL' AND paid_amount>0 AND paid_amount+credited_amount<amount) OR (status='PAID' AND paid_amount=amount AND credited_amount=0) OR (status='CREDITED' AND credited_amount>0 AND paid_amount+credited_amount=amount) OR (status='CANCELLED' AND paid_amount=0 AND credited_amount=0))");
  }
 }
}
