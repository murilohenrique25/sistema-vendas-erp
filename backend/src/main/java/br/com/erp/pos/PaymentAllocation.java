package br.com.erp.pos;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.pos.SaleContracts.*;

/** Monetary allocation only. Account access, bank proof uniqueness and posting belong to SalePayments. */
public final class PaymentAllocation {
 private static final BigDecimal MAX=new BigDecimal("99999999999999999.99");
 private PaymentAllocation(){}
 public record Line(Method method,UUID accountId,BigDecimal amount,BigDecimal tendered,BigDecimal change,String reference){}
 public record Summary(List<Line> lines,BigDecimal applied,BigDecimal tendered,BigDecimal change,BigDecimal remaining,BigDecimal excess){
  public boolean balanced(){return remaining.signum()==0&&excess.signum()==0;}
 }
 private static ResponseStatusException invalid(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 private static BigDecimal money(BigDecimal value,boolean zero){
  if(value==null||value.signum()<0||(!zero&&value.signum()==0)||value.compareTo(MAX)>0)throw invalid("Valor monetário fora do limite permitido.");
  try{return value.setScale(2,RoundingMode.UNNECESSARY);}catch(ArithmeticException ex){throw invalid("Valores monetários devem ter no máximo duas casas decimais.");}
 }
 public static Summary calculate(BigDecimal total,BigDecimal legacyTendered,List<PaymentInput> payments,UUID drawer){
  total=money(total,false);List<PaymentInput> inputs=payments;
  if(inputs==null){if(legacyTendered==null)throw invalid("Informe pagamentos ou dinheiro recebido.");inputs=List.of(new PaymentInput(Method.CASH,null,total,legacyTendered,"",true));}
  else if(legacyTendered!=null)throw invalid("Use pagamentos ou o formato antigo em dinheiro, sem combinar os dois.");
  if(inputs.size()>100)throw invalid("Use no máximo cem pagamentos.");
  var lines=new ArrayList<Line>();var references=new HashSet<List<Object>>();int cash=0;
  BigDecimal applied=new BigDecimal("0.00"),received=applied,change=applied;
  for(var p:inputs){
   if(p==null||p.method()==null)throw invalid("Informe o meio de pagamento.");
   BigDecimal amount=money(p.amount(),false),tendered=money(p.tendered()==null?p.amount():p.tendered(),false);
   boolean isCash=p.method()==Method.CASH;UUID account=isCash?drawer:p.accountId();String reference=p.reference()==null?"":p.reference().trim();
   if(reference.length()>160)throw invalid("Referência deve ter no máximo 160 caracteres.");
   if(isCash){if(++cash>1||p.accountId()!=null&&!p.accountId().equals(account)||!reference.isEmpty())throw invalid("Use uma parcela de dinheiro, vinculada ao caixa e sem referência externa.");}
   else{
    if(account==null||!Boolean.TRUE.equals(p.confirmed())||reference.isEmpty())throw invalid("Pix/transferência exige conta bancária, referência e confirmação explícita do recebimento.");
    if(!references.add(List.of(account,p.method(),reference)))throw new ResponseStatusException(HttpStatus.CONFLICT,"Comprovante já registrado nesta conta e meio de pagamento.");
   }
   if(tendered.compareTo(amount)<0||!isCash&&tendered.compareTo(amount)!=0)throw invalid("Troco é permitido somente em dinheiro; recebido deve cobrir a parcela.");
   BigDecimal lineChange=tendered.subtract(amount);applied=applied.add(amount);received=received.add(tendered);change=change.add(lineChange);
   lines.add(new Line(p.method(),account,amount,tendered,lineChange,reference));
  }
  if(received.compareTo(MAX)>0)throw invalid("Recebimento excede limite monetário.");
  return new Summary(List.copyOf(lines),applied,received,change,total.subtract(applied).max(BigDecimal.ZERO),applied.subtract(total).max(BigDecimal.ZERO));
 }
}
