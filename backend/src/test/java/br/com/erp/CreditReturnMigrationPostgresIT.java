package br.com.erp;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.*;
@Testcontainers
class CreditReturnMigrationPostgresIT extends CreditReturnMigrationTest {
 @Container static final PostgreSQLContainer<?> postgres=new PostgreSQLContainer<>("postgres:17.6-alpine");
 @Override String url(){return postgres.getJdbcUrl();}@Override String user(){return postgres.getUsername();}@Override String password(){return postgres.getPassword();}
}
