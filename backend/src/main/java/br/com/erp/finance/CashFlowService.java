package br.com.erp.finance;
import br.com.erp.auth.*;
import java.math.BigDecimal;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.util.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.finance.CashFlowContracts.*;
@Service
public class CashFlowService {
 private final JdbcTemplate db;private final BranchAccess access;private final ZoneId zone;
 private static final BigDecimal ZERO=BigDecimal.ZERO.setScale(2);
 public CashFlowService(JdbcTemplate db,BranchAccess access,@Value("${erp.finance-zone:America/Sao_Paulo}") String zone){this.db=db;this.access=access;this.zone=ZoneId.of(zone);}
 public Map<String,String> calendar(){return Map.of("date",LocalDate.now(zone).toString(),"zone",zone.getId());}
 private String scope(String alias,UUID branch){return alias+".tenant_id=? AND "+BranchAccess.predicate(alias+".branch_id")+(branch==null?"":" AND "+alias+".branch_id=?");}
 private Object[] params(Actor actor,UUID branch,Object... rest){var p=new ArrayList<Object>();p.add(actor.tenantId());p.add(actor.tenantId());p.add(actor.userId());if(branch!=null)p.add(branch);Collections.addAll(p,rest);return p.toArray();}
 private BigDecimal money(String sql,Object... parameters){return db.queryForObject(sql,BigDecimal.class,parameters).setScale(2);}
 private record MovementDay(LocalDate day,BigDecimal inflow,BigDecimal outflow,BigDecimal transferIn,BigDecimal transferOut){}
 private record DebtDay(LocalDate day,BigDecimal amount){}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ)
 public Report report(Actor actor,LocalDate from,LocalDate to,UUID branch){
  if(from.getYear()<1 || to.getYear()>9999 || from.isAfter(to) || ChronoUnit.DAYS.between(from,to)>=366)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Selecione um intervalo de até 366 dias, com início anterior ou igual ao fim e anos entre 1 e 9999.");
  if(branch!=null)access.requireBranch(actor,branch);
  LocalDate today=LocalDate.now(zone);OffsetDateTime generated=OffsetDateTime.now(ZoneOffset.UTC),start=from.atStartOfDay(zone).toOffsetDateTime(),end=to.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
  String ledger=" FROM treasury_entry e JOIN treasury_operation o ON o.tenant_id=e.tenant_id AND o.id=e.operation_id JOIN treasury_account a ON a.tenant_id=e.tenant_id AND a.id=e.account_id WHERE "+scope("a",branch);
  BigDecimal current=money("SELECT COALESCE(SUM(a.balance),0) FROM treasury_account a WHERE "+scope("a",branch),params(actor,branch));
  BigDecimal opening=money("SELECT COALESCE(SUM(e.delta),0)"+ledger+" AND o.created_at<?",params(actor,branch,start));
  var dayParams=new ArrayList<Object>();dayParams.add(zone.getId());Collections.addAll(dayParams,params(actor,branch,start,end));
  var movements=db.query("SELECT CAST(o.created_at AT TIME ZONE ? AS DATE) AS flow_day, SUM(CASE WHEN (o.source_id IS NULL OR o.destination_id IS NULL) AND e.delta>0 THEN e.delta ELSE 0 END) AS inflow, SUM(CASE WHEN (o.source_id IS NULL OR o.destination_id IS NULL) AND e.delta<0 THEN -e.delta ELSE 0 END) AS outflow, SUM(CASE WHEN o.source_id IS NOT NULL AND o.destination_id IS NOT NULL AND e.delta>0 THEN e.delta ELSE 0 END) AS transfer_in, SUM(CASE WHEN o.source_id IS NOT NULL AND o.destination_id IS NOT NULL AND e.delta<0 THEN -e.delta ELSE 0 END) AS transfer_out"+ledger+" AND o.created_at>=? AND o.created_at<? GROUP BY 1 ORDER BY 1",(r,i)->new MovementDay(r.getObject("flow_day",LocalDate.class),r.getBigDecimal("inflow"),r.getBigDecimal("outflow"),r.getBigDecimal("transfer_in"),r.getBigDecimal("transfer_out")),dayParams.toArray());
  var movementMap=new HashMap<LocalDate,MovementDay>();for(var d:movements)movementMap.put(d.day(),d);
  var receivables=debts(actor,branch,from,to,true);var payables=debts(actor,branch,from,to,false);
  var actual=new ArrayList<ActualDay>();var pending=new ArrayList<PendingDay>();
  BigDecimal balance=opening,inflows=ZERO,outflows=ZERO,transferIn=ZERO,transferOut=ZERO,pendingIn=ZERO,pendingOut=ZERO;
  for(LocalDate day=from;!day.isAfter(to);day=day.plusDays(1)){
   var m=movementMap.getOrDefault(day,new MovementDay(day,ZERO,ZERO,ZERO,ZERO));balance=balance.add(m.inflow()).subtract(m.outflow()).add(m.transferIn()).subtract(m.transferOut());
   inflows=inflows.add(m.inflow());outflows=outflows.add(m.outflow());transferIn=transferIn.add(m.transferIn());transferOut=transferOut.add(m.transferOut());actual.add(new ActualDay(day,m.inflow(),m.outflow(),m.transferIn(),m.transferOut(),balance));
   BigDecimal dueIn=receivables.getOrDefault(day,ZERO),dueOut=payables.getOrDefault(day,ZERO);pendingIn=pendingIn.add(dueIn);pendingOut=pendingOut.add(dueOut);pending.add(new PendingDay(day,dueIn,dueOut,dueIn.subtract(dueOut)));
  }
  Summary summary=new Summary(current,opening,balance,inflows,outflows,transferIn,transferOut,pendingIn,pendingOut,overdue(actor,branch,today,true),overdue(actor,branch,today,false),unlinked(actor,branch,from,to,true),unlinked(actor,branch,from,to,false));
  return new Report(from,to,branch,today,zone.getId(),generated,summary,actual,pending);
 }
 // Table/column expressions are fixed source constants, never request parameters.
 private Map<LocalDate,BigDecimal> debts(Actor a,UUID branch,LocalDate from,LocalDate to,boolean incoming){String table=incoming?"receivable":"payable",paid=incoming?"received_amount+t.credited_amount":"paid_amount+t.credited_amount";var rows=db.query("SELECT t.due_on,SUM(t.amount-(t."+paid+")) AS amount FROM "+table+" t WHERE "+scope("t",branch)+" AND t.status IN ('OPEN','PARTIAL') AND t.due_on>=? AND t.due_on<=? GROUP BY t.due_on",(r,i)->new DebtDay(r.getObject("due_on",LocalDate.class),r.getBigDecimal("amount")),params(a,branch,from,to));var result=new HashMap<LocalDate,BigDecimal>();for(var row:rows)result.put(row.day(),row.amount());return result;}
 private BigDecimal overdue(Actor a,UUID branch,LocalDate today,boolean incoming){String table=incoming?"receivable":"payable",paid=incoming?"received_amount+t.credited_amount":"paid_amount+t.credited_amount";return money("SELECT COALESCE(SUM(t.amount-(t."+paid+")),0) FROM "+table+" t WHERE "+scope("t",branch)+" AND t.status IN ('OPEN','PARTIAL') AND t.due_on<?",params(a,branch,today));}
 private BigDecimal unlinked(Actor a,UUID branch,LocalDate from,LocalDate to,boolean incoming){String table=incoming?"receivable":"payable",entry=incoming?"receivable_receipt":"payable_payment",date=incoming?"received_on":"paid_on",value=incoming?"p.amount-COALESCE((SELECT SUM(f.amount) FROM pos_sale_return_receipt_refund f WHERE f.tenant_id=p.tenant_id AND f.receipt_id=p.id),0)":"p.amount";return money("SELECT COALESCE(SUM("+value+"),0) FROM "+entry+" p JOIN "+table+" t ON t.tenant_id=p.tenant_id AND t.id=p."+table+"_id WHERE "+scope("t",branch)+" AND p.status='POSTED' AND p.treasury_account_id IS NULL AND p."+date+">=? AND p."+date+"<=?",params(a,branch,from,to));}
}
