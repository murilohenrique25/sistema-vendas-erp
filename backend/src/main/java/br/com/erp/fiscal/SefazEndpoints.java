package br.com.erp.fiscal;
import java.net.URI;
/** GO endpoints verified against official national/GO service listings. No caller supplied URLs. */
public final class SefazEndpoints {
 private SefazEndpoints(){}
 public enum Service {
  AUTHORIZE("NFeAutorizacao4","nfeAutorizacaoLote"),RECEIPT("NFeRetAutorizacao4","nfeRetAutorizacaoLote"),CONSULT("NFeConsultaProtocolo4","nfeConsultaNF"),STATUS("NFeStatusServico4","nfeStatusServicoNF"),EVENT("NFeRecepcaoEvento4","nfeRecepcaoEventoNF"),INVALIDATE("NFeInutilizacao4","nfeInutilizacaoNF");
  private final String path,operation;Service(String path,String operation){this.path=path;this.operation=operation;}
  public String namespace(){return "http://www.portalfiscal.inf.br/nfe/wsdl/"+path;}
  public String action(){return namespace()+"/"+operation;}
 }
 public static URI go(FiscalContext.Environment environment,Service service){return URI.create("https://"+(environment==FiscalContext.Environment.PRODUCTION?"nfe":"homolog")+".sefaz.go.gov.br/nfe/services/"+service.path);}
 public static String qrCode(FiscalContext.Environment environment){return "https://"+(environment==FiscalContext.Environment.PRODUCTION?"nfeweb":"nfewebhomolog")+".sefaz.go.gov.br/nfeweb/sites/nfce/danfeNFCe";}
}
