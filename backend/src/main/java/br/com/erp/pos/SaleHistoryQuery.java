package br.com.erp.pos;

import br.com.erp.auth.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

/** SQL fragments are source constants; every user value is bound as a parameter. */
final class SaleHistoryQuery {
 record Filter(LocalDate from,LocalDate to,String query,String commercialStatus,SaleContracts.Method method,Boolean credit,UUID branchId){}
 record Selection(String where,List<Object> args){}
 private SaleHistoryQuery(){}
 static Selection select(Actor actor,Filter filter){
  var sql=new StringBuilder(" WHERE s.tenant_id=? AND "+BranchAccess.predicate("s.branch_id"));
  var args=new ArrayList<Object>(List.of(actor.tenantId(),actor.tenantId(),actor.userId()));
  if(filter==null)return new Selection(sql.toString(),args);
  for(var date:new LocalDate[]{filter.from(),filter.to()})if(date!=null&&(date.getYear()<1||date.getYear()>9998))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Datas devem estar entre os anos 0001 e 9998.");
  if(filter.from()!=null&&filter.to()!=null&&filter.from().isAfter(filter.to()))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Data inicial deve ser anterior ou igual à final.");
  ZoneId zone=ZoneId.of("America/Sao_Paulo");
  if(filter.from()!=null){sql.append(" AND s.created_at>=?");args.add(filter.from().atStartOfDay(zone).toOffsetDateTime());}
  if(filter.to()!=null){if(filter.to().equals(LocalDate.MAX))throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Data final fora do intervalo permitido.");sql.append(" AND s.created_at<?");args.add(filter.to().plusDays(1).atStartOfDay(zone).toOffsetDateTime());}
  if(filter.branchId()!=null){sql.append(" AND s.branch_id=?");args.add(filter.branchId());}
  if(filter.credit()!=null)sql.append(filter.credit()?" AND s.credit_plan_id IS NOT NULL":" AND s.credit_plan_id IS NULL");
  if(filter.method()!=null){sql.append(" AND EXISTS(SELECT 1 FROM pos_sale_payment hp WHERE hp.tenant_id=s.tenant_id AND hp.sale_id=s.id AND hp.method=?)");args.add(filter.method().name());}
  String status=filter.commercialStatus();
  if(status!=null&&!status.isBlank()){
   String returned="EXISTS(SELECT 1 FROM pos_sale_return hr WHERE hr.tenant_id=s.tenant_id AND hr.sale_id=s.id)";
   String retained="EXISTS(SELECT 1 FROM pos_sale_item hi WHERE hi.tenant_id=s.tenant_id AND hi.sale_id=s.id AND hi.quantity>COALESCE((SELECT SUM(hri.quantity) FROM pos_sale_return_item hri WHERE hri.tenant_id=hi.tenant_id AND hri.sale_id=hi.sale_id AND hri.product_id=hi.product_id),0))";
   switch(status){
    case "CANCELLED" -> sql.append(" AND s.status='CANCELLED'");
    case "COMPLETED" -> sql.append(" AND s.status='COMPLETED' AND NOT ").append(returned);
    case "PARTIALLY_RETURNED" -> sql.append(" AND s.status='COMPLETED' AND ").append(returned).append(" AND ").append(retained);
    case "RETURNED" -> sql.append(" AND s.status='COMPLETED' AND ").append(returned).append(" AND NOT ").append(retained);
    default -> throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Situação comercial inválida.");
   }
  }
  String query=filter.query()==null?"":filter.query().trim();
  if(query.length()>160)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Busca deve ter no máximo 160 caracteres.");
  if(!query.isEmpty()){
   String pattern="%"+query.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
   sql.append(" AND (LOWER(CAST(s.id AS VARCHAR))=? OR EXISTS(SELECT 1 FROM pos_sale_item hs WHERE hs.tenant_id=s.tenant_id AND hs.sale_id=s.id AND (LOWER(hs.sku) LIKE ? ESCAPE '!' OR LOWER(hs.name) LIKE ? ESCAPE '!')) OR EXISTS(SELECT 1 FROM customer hc WHERE hc.tenant_id=s.tenant_id AND hc.id=s.credit_customer_id AND LOWER(hc.name) LIKE ? ESCAPE '!'))");
   Collections.addAll(args,query.toLowerCase(Locale.ROOT),pattern,pattern,pattern);
  }
  return new Selection(sql.toString(),List.copyOf(args));
 }
}
