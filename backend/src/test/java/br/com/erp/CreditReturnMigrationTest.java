package br.com.erp;
import java.util.UUID;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class CreditReturnMigrationTest {
 String url(){return "jdbc:h2:mem:credit-upgrade-"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";}String user(){return "sa";}String password(){return "";}
 @Test void upgradePreservesExistingPartialTitleAndReceiptWithoutGrantOrRefund(){
  String url=url();Flyway.configure().dataSource(url,user(),password()).target("24").load().migrate();var db=new JdbcTemplate(new DriverManagerDataSource(url,user(),password()));
  UUID t=UUID.randomUUID(),u=UUID.randomUUID(),b=UUID.randomUUID(),c=UUID.randomUUID(),title=UUID.randomUUID(),receipt=UUID.randomUUID();
  db.update("INSERT INTO tenant(id,name,slug) VALUES(?,'Legado',?)",t,"credit-legacy-"+t);
  db.update("INSERT INTO app_user(id,tenant_id,name,email,password_hash) VALUES(?,?,'Admin','admin@example.com','fixture')",u,t);
  db.update("INSERT INTO branch(id,tenant_id,code,name) VALUES(?,?,'B','Filial')",b,t);
  db.update("INSERT INTO customer(id,tenant_id,code,name,tax_identifier,contact_name,email,phone,address,notes) VALUES(?,?,'C','Cliente','','','','','','')",c,t);
  db.update("INSERT INTO receivable(id,tenant_id,request_id,fingerprint,branch_id,customer_id,customer_name,reference,document,description,issued_on,due_on,amount,received_amount,status,created_by) VALUES(?,?,?,'hash',?,?,'Cliente','OLD','','Título original',DATE '2020-01-01',DATE '2020-02-01',10,3,'PARTIAL',?)",title,t,UUID.randomUUID(),b,c,u);
  db.update("INSERT INTO receivable_receipt(id,tenant_id,receivable_id,fingerprint,amount,received_on,method,reference,note,status,created_by) VALUES(?,?,?,'hash',3,DATE '2020-01-01','CASH','','Recebimento antigo','POSTED',?)",receipt,t,title,u);
  Flyway.configure().dataSource(url,user(),password()).load().migrate();
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM receivable WHERE id=? AND amount=10 AND received_amount=3 AND credited_amount=0 AND status='PARTIAL' AND version=0",Integer.class,title));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM receivable_receipt WHERE id=? AND amount=3 AND status='POSTED'",Integer.class,receipt));
  assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM pos_sale_return_debt_credit",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM pos_sale_return_receipt_refund",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM user_permission",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM treasury_operation",Integer.class));
  assertEquals("NO",db.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE LOWER(table_schema)=LOWER(CURRENT_SCHEMA) AND LOWER(table_name)='receivable' AND LOWER(column_name)='received_amount'",String.class));
 }
}
