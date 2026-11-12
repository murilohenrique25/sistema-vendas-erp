package br.com.erp;

import br.com.erp.tenant.ProvisioningService;
import org.springframework.boot.SpringApplication;

/** Disposable browser-test harness. This class and H2 are excluded from the production JAR. */
public class UiTestServer {
    public static void main(String[] args) {
        var application=new SpringApplication(ErpApplication.class);
        application.setAdditionalProfiles("test");
        var context=application.run("--server.address=127.0.0.1","--server.port=18081","--spring.datasource.url=jdbc:h2:mem:ui-tests;MODE=PostgreSQL;DB_CLOSE_DELAY=-1");
        context.getBean(ProvisioningService.class).create("Ambiente temporário de testes","interface-test","Administrador de teste","ui@example.invalid","Temporary-ui-test-2026");
    }
}
