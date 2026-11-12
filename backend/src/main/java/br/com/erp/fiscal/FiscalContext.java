package br.com.erp.fiscal;
import java.util.UUID;
public record FiscalContext(UUID tenantId,UUID branchId,Environment environment){
 public enum Environment { HOMOLOGATION, PRODUCTION;public String code(){return this==PRODUCTION?"1":"2";} }
 public enum Model { NFE("55"),NFCE("65");private final String code;Model(String code){this.code=code;}public String code(){return code;} }
}
