package br.com.erp.purchase;

import br.com.erp.auth.*;
import br.com.erp.finance.*;
import br.com.erp.inventory.StockService;
import br.com.erp.shared.Audit;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.nio.charset.StandardCharsets;
import java.util.*;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.http.HttpStatus;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController @RequestMapping("/api/v1/purchase-orders/{orderId}/receipts/{receiptId}/payable")
@PreAuthorize("hasAuthority('purchase:read') and hasAuthority('payable:read')")
public class PurchaseFinanceController {
 private final PurchaseFinanceService service;
 public PurchaseFinanceController(PurchaseFinanceService service){this.service=service;}
 public record Input(@NotNull UUID requestId,@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,60}") String reference,@NotNull @Size(max=160) String document,@NotBlank @Size(min=5,max=500) String description,@NotNull LocalDate issuedOn,@NotNull LocalDate dueOn,@Min(1) @Max(120) Integer installmentCount,@Min(1) @Max(12) Integer intervalMonths){}
 public record Summary(UUID receiptId,UUID branchId,UUID supplierId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,long linkedTitleCount,PayableContracts.View generatedTitle,List<PayableContracts.View> generatedTitles){}
 @GetMapping public Summary get(@AuthenticationPrincipal Actor a,@PathVariable UUID orderId,@PathVariable UUID receiptId){return service.get(a,orderId,receiptId);}
 @PostMapping("/preview") public PurchaseInstallments.Preview preview(@AuthenticationPrincipal Actor a,@PathVariable UUID orderId,@PathVariable UUID receiptId,@Valid @RequestBody Input input){return service.preview(a,orderId,receiptId,input);}
 @PostMapping @ResponseStatus(HttpStatus.CREATED) @PreAuthorize("hasAuthority('purchase:read') and hasAuthority('payable:read') and hasAuthority('payable:write')") public Summary generate(@AuthenticationPrincipal Actor a,@PathVariable UUID orderId,@PathVariable UUID receiptId,@Valid @RequestBody Input input){return service.generate(a,orderId,receiptId,input);}
}

@Service
class PurchaseFinanceService {
 private final JdbcTemplate db;private final BranchAccess access;private final PayableService titles;private final Audit audit;
 PurchaseFinanceService(JdbcTemplate db,BranchAccess access,PayableService titles,Audit audit){this.db=db;this.access=access;this.titles=titles;this.audit=audit;}
 private ResponseStatusException conflict(String text){return new ResponseStatusException(HttpStatus.CONFLICT,text);}
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public PurchaseFinanceController.Summary get(Actor a,UUID order,UUID receipt){
  var rows=db.query("SELECT o.branch_id,o.supplier_id FROM purchase_receipt r JOIN purchase_order o ON o.tenant_id=r.tenant_id AND o.id=r.order_id WHERE r.tenant_id=? AND r.id=? AND r.order_id=?",(r,i)->Map.entry(r.getObject(1,UUID.class),r.getObject(2,UUID.class)),a.tenantId(),receipt,order);
  if(rows.isEmpty())throw new ResponseStatusException(HttpStatus.NOT_FOUND,"Recebimento não encontrado.");var scope=rows.getFirst();access.requireBranch(a,scope.getKey());
  BigDecimal amount=db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM purchase_receipt_line WHERE tenant_id=? AND receipt_id=?",BigDecimal.class,a.tenantId(),receipt);
  amount=amount.subtract(db.queryForObject("SELECT COALESCE(SUM(amount),0) FROM supplier_return WHERE tenant_id=? AND receipt_id=?",BigDecimal.class,a.tenantId(),receipt));
  var generated=db.query("SELECT payable_id FROM purchase_receipt_finance WHERE tenant_id=? AND receipt_id=?",(r,i)->r.getObject(1,UUID.class),a.tenantId(),receipt);
  long count=db.queryForObject("SELECT COUNT(*) FROM payable WHERE tenant_id=? AND purchase_receipt_id=? AND status<>'CANCELLED'",Long.class,a.tenantId(),receipt);
  var installmentIds=db.query("SELECT payable_id FROM purchase_receipt_installment WHERE tenant_id=? AND receipt_id=? ORDER BY position",(r,i)->r.getObject(1,UUID.class),a.tenantId(),receipt);
  var generatedTitles=installmentIds.stream().map(id->titles.get(a,id)).toList();
  return new PurchaseFinanceController.Summary(receipt,scope.getKey(),scope.getValue(),amount,count,generated.isEmpty()?null:titles.get(a,generated.getFirst()),generatedTitles);
 }
 @Transactional(readOnly=true,isolation=Isolation.REPEATABLE_READ) public PurchaseInstallments.Preview preview(Actor a,UUID order,UUID receipt,PurchaseFinanceController.Input input){return PurchaseInstallments.calculate(get(a,order,receipt).amount(),input);}
 @Transactional public PurchaseFinanceController.Summary generate(Actor a,UUID order,UUID receipt,PurchaseFinanceController.Input input){
  db.queryForObject("SELECT id FROM tenant WHERE id=? FOR UPDATE",UUID.class,a.tenantId());var summary=get(a,order,receipt);
  String reference=input.reference().toUpperCase(Locale.ROOT),document=input.document().trim(),description=input.description().trim();
  var fields=new ArrayList<Object>(Arrays.asList("PURCHASE_RECEIPT_PAYABLE",order,receipt,reference,document,description,input.issuedOn(),input.dueOn()));
  if(PurchaseInstallments.count(input)>1)Collections.addAll(fields,"INSTALLMENTS",PurchaseInstallments.count(input),PurchaseInstallments.interval(input));
  String hash=StockService.fingerprint(fields.toArray());
  var old=db.query("SELECT receipt_id,fingerprint FROM purchase_receipt_finance WHERE tenant_id=? AND request_id=?",(r,i)->Map.entry(r.getObject(1,UUID.class),r.getString(2)),a.tenantId(),input.requestId());
  if(!old.isEmpty()){if(!old.getFirst().getKey().equals(receipt)||!old.getFirst().getValue().equals(hash))throw conflict("Identificador reutilizado com outros dados.");return summary;}
  if(summary.generatedTitle()!=null)throw conflict("Recebimento já possui geração financeira, inclusive se o título foi cancelado.");
  if(summary.linkedTitleCount()>0)throw conflict("Existem títulos manuais vinculados. Reconcilie esses lançamentos antes de gerar o título do recebimento.");
  if(db.queryForObject("SELECT COUNT(*) FROM payable WHERE tenant_id=? AND request_id=?",Long.class,a.tenantId(),input.requestId())>0)throw conflict("Identificador já utilizado em outro lançamento financeiro.");
  if(summary.amount().signum()<=0)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Recebimento sem valor financeiro positivo não gera título.");
  if(input.issuedOn().getYear()<1||input.issuedOn().getYear()>9999||input.dueOn().getYear()<1||input.dueOn().getYear()>9999)throw new ResponseStatusException(HttpStatus.BAD_REQUEST,"Datas fora do intervalo permitido.");
  var preview=PurchaseInstallments.calculate(summary.amount(),input);var created=new ArrayList<PayableContracts.View>();
  for(var part:preview.installments()){
   UUID key=part.position()==1?input.requestId():UUID.nameUUIDFromBytes(("PURCHASE_PAYABLE|"+a.tenantId()+"|"+input.requestId()+"|"+part.position()).getBytes(StandardCharsets.UTF_8));
   if(db.queryForObject("SELECT COUNT(*) FROM payable WHERE tenant_id=? AND request_id=?",Long.class,a.tenantId(),key)>0)throw conflict("Identificador de parcela já utilizado em outro lançamento.");
   created.add(titles.create(a,new PayableContracts.Input(key,summary.branchId(),summary.supplierId(),receipt,part.reference(),document,description,input.issuedOn(),part.dueOn(),part.amount())));
  }
  db.update("INSERT INTO purchase_receipt_finance(tenant_id,receipt_id,payable_id,request_id,fingerprint,created_by) VALUES(?,?,?,?,?,?)",a.tenantId(),receipt,created.getFirst().id(),input.requestId(),hash,a.userId());
  for(int i=0;i<created.size();i++)db.update("INSERT INTO purchase_receipt_installment(tenant_id,receipt_id,position,payable_id) VALUES(?,?,?,?)",a.tenantId(),receipt,i+1,created.get(i).id());

  audit.record(a,"PURCHASE_RECEIPT_FINANCED",receipt);return get(a,order,receipt);
 }
}
