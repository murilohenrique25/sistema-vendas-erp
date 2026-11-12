package br.com.erp.finance;

import br.com.erp.auth.*;
import java.time.LocalDate;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PayableSearch {
 record Filter(String query,LocalDate dueFrom,LocalDate dueTo,UUID branchId,UUID supplierId,UUID receiptId,Boolean withReceipt){}
 record Selection(String where,List<Object> args){}
 private PayableSearch(){}
 static Selection select(Actor a,String status,boolean overdue,LocalDate today,Filter filter){
  var sql=new StringBuilder(" WHERE t.tenant_id=? AND "+BranchAccess.predicate("t.branch_id"));var args=new ArrayList<Object>(List.of(a.tenantId(),a.tenantId(),a.userId()));
  if(!status.isEmpty()){if(!List.of("OPEN","PARTIAL","PAID","CREDITED","CANCELLED").contains(status))throw bad("Situação inválida.");sql.append(" AND t.status=?");args.add(status);}
  if(overdue){sql.append(" AND t.status IN ('OPEN','PARTIAL') AND t.due_on<?");args.add(today);}
  if(filter==null)return new Selection(sql.toString(),List.copyOf(args));
  if(filter.dueFrom()!=null&&filter.dueTo()!=null&&filter.dueFrom().isAfter(filter.dueTo()))throw bad("Vencimento inicial deve ser anterior ou igual ao final.");
  for(var date:new LocalDate[]{filter.dueFrom(),filter.dueTo()})if(date!=null&&(date.getYear()<1||date.getYear()>9999))throw bad("Datas fora do intervalo permitido.");
  if(filter.dueFrom()!=null){sql.append(" AND t.due_on>=?");args.add(filter.dueFrom());}if(filter.dueTo()!=null){sql.append(" AND t.due_on<=?");args.add(filter.dueTo());}
  if(filter.branchId()!=null){sql.append(" AND t.branch_id=?");args.add(filter.branchId());}if(filter.supplierId()!=null){sql.append(" AND t.supplier_id=?");args.add(filter.supplierId());}
  if(filter.receiptId()!=null){sql.append(" AND t.purchase_receipt_id=?");args.add(filter.receiptId());}
  if(filter.withReceipt()!=null)sql.append(filter.withReceipt()?" AND t.purchase_receipt_id IS NOT NULL":" AND t.purchase_receipt_id IS NULL");
  String query=filter.query()==null?"":filter.query().trim();if(query.length()>200)throw bad("Busca deve ter no máximo 200 caracteres.");
  if(!query.isEmpty()){
   String pattern="%"+query.toLowerCase(Locale.ROOT).replace("!","!!").replace("%","!%").replace("_","!_")+"%";
   sql.append(" AND (LOWER(CAST(t.id AS VARCHAR))=? OR LOWER(t.reference) LIKE ? ESCAPE '!' OR LOWER(t.document) LIKE ? ESCAPE '!' OR LOWER(t.description) LIKE ? ESCAPE '!' OR LOWER(t.supplier_name) LIKE ? ESCAPE '!')");
   Collections.addAll(args,query.toLowerCase(Locale.ROOT),pattern,pattern,pattern,pattern);
  }
  return new Selection(sql.toString(),List.copyOf(args));
 }
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
}
