package br.com.erp.product;

import br.com.erp.auth.Actor;
import br.com.erp.shared.Audit;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class ProductMaintenanceService {
 public record Update(@NotNull @Min(0) Long version,@NotNull @Valid ProductService.Input product){}
 public record Active(@NotNull @Min(0) Long version,@NotNull Boolean active){}
 private final JdbcTemplate db;private final Audit audit;
 public ProductMaintenanceService(JdbcTemplate db,Audit audit){this.db=db;this.audit=audit;}
 private ResponseStatusException conflict(String message){return new ResponseStatusException(HttpStatus.CONFLICT,message);}
 private ProductService.View lock(Actor a,UUID id){
  db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());
  var rows=db.query("SELECT * FROM product WHERE tenant_id=? AND id=? FOR UPDATE",(r,i)->new ProductService.View(r.getObject("id",UUID.class),r.getString("sku"),r.getString("name"),r.getString("description"),r.getString("unit"),r.getBigDecimal("sale_price"),r.getBoolean("active"),r.getLong("version"),List.of()),a.tenantId(),id);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Produto não encontrado.");
  var p=rows.getFirst();return new ProductService.View(p.id(),p.sku(),p.name(),p.description(),p.unit(),p.salePrice(),p.active(),p.version(),db.query("SELECT code,kind FROM product_barcode WHERE tenant_id=? AND product_id=? ORDER BY code",(r,i)->new Barcode(r.getString(1),Barcode.Kind.valueOf(r.getString(2))),a.tenantId(),id));
 }
 private Set<Barcode> codes(List<Barcode> codes){return new HashSet<>(codes);}
 @Transactional public ProductService.View update(Actor a,UUID id,Update command){
  var p=lock(a,id);var input=command.product();var canonical=input.barcodes().stream().map(Barcode::canonical).toList();
  if(new HashSet<>(canonical).size()!=canonical.size())throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Códigos de barras repetidos no cadastro.");
  if(!input.unit().equals(p.unit()))throw conflict("A unidade do produto não pode ser alterada. Cadastre outro produto para uma unidade diferente.");
  String name=input.name().trim(),description=input.description().trim();
  boolean same=p.sku().equals(input.sku())&&p.name().equals(name)&&p.description().equals(description)&&p.salePrice().compareTo(input.salePrice())==0&&codes(p.barcodes()).equals(codes(input.barcodes()));
  if(p.version()!=command.version()){
   if(command.version()!=Long.MAX_VALUE&&p.version()==command.version()+1&&same)return p;
   throw conflict("Produto alterado por outra operação. Recarregue antes de editar.");
  }
  if(same)return p;
  db.update("UPDATE product SET sku=?,name=?,description=?,sale_price=?,version=version+1 WHERE tenant_id=? AND id=? AND version=?",input.sku(),name,description,input.salePrice(),a.tenantId(),id,command.version());
  db.update("DELETE FROM product_barcode WHERE tenant_id=? AND product_id=?",a.tenantId(),id);
  for(int i=0;i<input.barcodes().size();i++){var b=input.barcodes().get(i);db.update("INSERT INTO product_barcode(tenant_id,product_id,code,canonical_code,kind) VALUES(?,?,?,?,?)",a.tenantId(),id,b.code(),canonical.get(i),b.kind().name());}
  audit.record(a,"PRODUCT_UPDATED",id);return lock(a,id);
 }
 @Transactional public ProductService.View active(Actor a,UUID id,Active command){
  var p=lock(a,id);
  if(p.version()!=command.version()){
   if(command.version()!=Long.MAX_VALUE&&p.version()==command.version()+1&&p.active()==command.active())return p;
   throw conflict("Produto alterado por outra operação. Recarregue antes de alterar a situação.");
  }
  if(p.active()==command.active())return p;
  db.update("UPDATE product SET active=?,version=version+1 WHERE tenant_id=? AND id=? AND version=?",command.active(),a.tenantId(),id,command.version());
  audit.record(a,command.active()?"PRODUCT_ACTIVATED":"PRODUCT_DEACTIVATED",id);return lock(a,id);
 }
}
