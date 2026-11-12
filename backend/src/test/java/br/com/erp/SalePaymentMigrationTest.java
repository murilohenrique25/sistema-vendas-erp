package br.com.erp;
import java.util.*;
import org.flywaydb.core.Flyway;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DriverManagerDataSource;
import static org.junit.jupiter.api.Assertions.*;
class SalePaymentMigrationTest {
 String url(){return "jdbc:h2:mem:upgrade-"+UUID.randomUUID()+";DB_CLOSE_DELAY=-1;MODE=PostgreSQL";}
 String user(){return "sa";}String password(){return "";}
 @Test void upgradesExistingCashSaleWithoutRepostingMoneyOrGrantingPermissions(){
  String url=url();Flyway.configure().dataSource(url,user(),password()).target("19").load().migrate();var db=new JdbcTemplate(new DriverManagerDataSource(url,user(),password()));
  UUID t=UUID.randomUUID(),u=UUID.randomUUID(),b=UUID.randomUUID(),a=UUID.randomUUID(),w=UUID.randomUUID(),terminal=UUID.randomUUID(),session=UUID.randomUUID(),op=UUID.randomUUID(),sale=UUID.randomUUID();
  db.update("INSERT INTO tenant(id,name,slug) VALUES(?,'Legado',?)",t,"legacy-"+t);
  db.update("INSERT INTO app_user(id,tenant_id,name,email,password_hash) VALUES(?,?,'Operador','legacy@example.com','fixture')",u,t);
  db.update("INSERT INTO branch(id,tenant_id,code,name) VALUES(?,?,'B','Filial')",b,t);
  db.update("INSERT INTO warehouse(id,tenant_id,branch_id,code,name) VALUES(?,?,?,'W','Depósito')",w,t,b);
  db.update("INSERT INTO treasury_account(id,tenant_id,request_id,fingerprint,branch_id,code,name,kind,balance,created_by) VALUES(?,?,?,'hash',?,'C','Caixa','CASH',10,?)",a,t,UUID.randomUUID(),b,u);
  db.update("INSERT INTO pos_terminal(id,tenant_id,request_id,fingerprint,branch_id,account_id,warehouse_id,code,name) VALUES(?,?,?,'hash',?,?,?,'T','Terminal')",terminal,t,UUID.randomUUID(),b,a,w);
  db.update("INSERT INTO pos_cash_session(id,tenant_id,request_id,fingerprint,terminal_id,account_id,branch_id,operator_id,opening_amount,opening_note,opened_at,open_terminal_id,open_operator_id,open_account_id) VALUES(?,?,?,'hash',?,?,?,?,0,'Abertura',CURRENT_TIMESTAMP,?,?,?)",session,t,UUID.randomUUID(),terminal,a,b,u,terminal,u,a);
  db.update("INSERT INTO treasury_operation(id,tenant_id,fingerprint,destination_id,amount,note,created_by,origin) VALUES(?,?,'hash',?,10,'Venda antiga',?,'POS_SALE')",op,t,a,u);
  db.update("INSERT INTO pos_sale(id,tenant_id,request_id,fingerprint,session_id,branch_id,warehouse_id,total,tendered,change_amount,treasury_operation_id,created_by,created_at) VALUES(?,?,?,'hash',?,?,?,10,20,10,?,?,CURRENT_TIMESTAMP)",sale,t,UUID.randomUUID(),session,b,w,op,u);
  UUID product=UUID.randomUUID(),stock=UUID.randomUUID();
  db.update("INSERT INTO product(id,tenant_id,sku,name,description,unit,sale_price) VALUES(?,?,'P','Produto antigo','','UN',10)",product,t);
  db.update("INSERT INTO stock_operation(id,tenant_id,fingerprint,kind,actor_id) VALUES(?,?,'hash','MOVEMENT',?)",stock,t,u);
  db.update("INSERT INTO stock_movement(id,tenant_id,operation_id,warehouse_id,product_id,movement_type,quantity,balance_after,unit,unit_cost,reason) VALUES(?,?,?,?,?,'CONSUMPTION',-1,0,'UN',3,'Venda antiga')",UUID.randomUUID(),t,stock,w,product);
  db.update("INSERT INTO pos_sale_item(id,tenant_id,sale_id,product_id,position,sku,name,unit,quantity,unit_price,total,stock_operation_id) VALUES(?,?,?,?,0,'P','Produto antigo','UN',1,10,10,?)",UUID.randomUUID(),t,sale,product,stock);
  Flyway.configure().dataSource(url,user(),password()).load().migrate();
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM pos_sale_payment WHERE tenant_id=? AND sale_id=? AND method='CASH' AND amount=10 AND tendered=20 AND change_amount=10 AND treasury_operation_id=?",Integer.class,t,sale,op));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM treasury_operation",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM user_permission",Integer.class));assertEquals("COMPLETED",db.queryForObject("SELECT status FROM pos_sale WHERE id=?",String.class,sale));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM pos_sale_item WHERE tenant_id=? AND sale_id=? AND gross_total=10 AND discount_amount=0 AND total=10",Integer.class,t,sale));assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM stock_movement",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM pos_suspension",Integer.class));
  assertEquals(1,db.queryForObject("SELECT COUNT(*) FROM pos_sale WHERE id=? AND credit_amount=0 AND credit_plan_id IS NULL AND credit_customer_id IS NULL AND treasury_operation_id=? AND tendered=20 AND change_amount=10",Integer.class,sale,op));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM customer_credit_policy",Integer.class));assertEquals(0,db.queryForObject("SELECT COUNT(*) FROM receivable_plan",Integer.class));
  assertEquals("NO",db.queryForObject("SELECT is_nullable FROM information_schema.columns WHERE LOWER(table_schema)=LOWER(CURRENT_SCHEMA) AND LOWER(table_name)='pos_sale' AND LOWER(column_name)='tendered'",String.class));
 }
}
