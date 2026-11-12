package br.com.erp.pos;
import br.com.erp.auth.*;
import br.com.erp.inventory.StockService;
import java.math.*;
import java.util.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
@Service
public class CartPricing {
 private final br.com.erp.product.BranchPricing prices;private final JdbcTemplate db;private final ObjectMapper json;private static final BigDecimal MAX=new BigDecimal("99999999999999999.99");
 public CartPricing(JdbcTemplate db,ObjectMapper json,br.com.erp.product.BranchPricing prices){this.db=db;this.json=json;this.prices=prices;}
 public record Line(UUID productId,String sku,String name,String unit,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unitPrice,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal grossTotal,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal discountAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total){public SaleContracts.ItemInput input(){return new SaleContracts.ItemInput(productId,quantity,unitPrice,discountAmount);}}
 public record Quote(List<Line> items,BigDecimal grossTotal,BigDecimal discountTotal,BigDecimal total,String hash){}
 public boolean has(Actor a,String permission){return db.queryForObject("SELECT COUNT(*) FROM user_permission p JOIN app_user u ON u.tenant_id=p.tenant_id AND u.id=p.user_id WHERE p.tenant_id=? AND p.user_id=? AND p.permission=? AND u.active=TRUE",Long.class,a.tenantId(),a.userId(),permission)>0;}
 public void require(Actor a,String permission){if(!has(a,permission))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Sem permissão: "+permission);}
 public static BigDecimal discount(SaleContracts.ItemInput i){return i.discountAmount()==null?BigDecimal.ZERO:i.discountAmount();}
 public Quote quote(Actor a,List<SaleContracts.ItemInput> inputs){return quote(a,null,inputs);}
 public Quote quote(Actor a,UUID branch,List<SaleContracts.ItemInput> inputs){
  var products=new HashMap<UUID,Line>();var seen=new HashSet<UUID>();for(var i:inputs)if(!seen.add(i.productId()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Agrupe as quantidades do mesmo produto.");
  for(UUID id:seen.stream().sorted().toList()){var rows=db.query("SELECT id,sku,name,unit,sale_price FROM product WHERE tenant_id=? AND id=? AND active=TRUE FOR UPDATE",(r,i)->new Line(r.getObject(1,UUID.class),r.getString(2),r.getString(3),r.getString(4),null,r.getBigDecimal(5),null,null,null),a.tenantId(),id);if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Produto ativo não encontrado.");var p=rows.getFirst();products.put(id,new Line(p.productId(),p.sku(),p.name(),p.unit(),null,prices.price(a,branch,id,p.unitPrice()),null,null,null));}
  BigDecimal grossTotal=BigDecimal.ZERO,discountTotal=BigDecimal.ZERO;var lines=new ArrayList<Line>();var fields=new ArrayList<Object>();
  for(var input:inputs){var p=products.get(input.productId());if(p.unitPrice().compareTo(input.expectedUnitPrice())!=0)throw new ResponseStatusException(HttpStatus.CONFLICT,"Preço mudou. Atualize o carrinho.");BigDecimal gross=p.unitPrice().multiply(input.quantity()).setScale(2,RoundingMode.HALF_UP),discount=discount(input);if(discount.signum()<0||discount.compareTo(gross)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Desconto deve estar entre zero e o valor bruto do item.");if(discount.signum()>0)require(a,"pos:discount");grossTotal=grossTotal.add(gross);discountTotal=discountTotal.add(discount);if(grossTotal.compareTo(MAX)>0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Carrinho excede limite monetário.");var line=new Line(p.productId(),p.sku(),p.name(),p.unit(),input.quantity(),p.unitPrice(),gross,discount,gross.subtract(discount));lines.add(line);Collections.addAll(fields,line.productId(),line.sku(),line.name(),line.unit(),line.quantity(),line.unitPrice(),line.discountAmount());}
  BigDecimal total=grossTotal.subtract(discountTotal);if(total.signum()<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Carrinho deve ter total positivo.");return new Quote(List.copyOf(lines),grossTotal,discountTotal,total,StockService.fingerprint(fields.toArray()));
 }
 public String encode(List<Line> items){try{return json.writeValueAsString(items);}catch(Exception e){throw new IllegalStateException("Falha ao registrar carrinho.",e);}}
 public List<Line> decode(String value){try{return json.readValue(value,new TypeReference<List<Line>>(){});}catch(Exception e){throw new IllegalStateException("Carrinho persistido inválido.",e);}}
 public void owner(Actor a,CashContracts.Session s){if(!s.operatorId().equals(a.userId())&&!has(a,"pos:supervise"))throw new ResponseStatusException(HttpStatus.FORBIDDEN,"Exige operador do caixa ou supervisor.");}
}
