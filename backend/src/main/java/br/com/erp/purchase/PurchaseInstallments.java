package br.com.erp.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.*;
import java.time.*;
import java.util.*;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

final class PurchaseInstallments {
 record Line(int position,String reference,LocalDate dueOn,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount){}
 record Preview(@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,List<Line> installments){}
 private PurchaseInstallments(){}
 static int count(PurchaseFinanceController.Input input){return input.installmentCount()==null?1:input.installmentCount();}
 static int interval(PurchaseFinanceController.Input input){return count(input)==1?1:input.intervalMonths()==null?1:input.intervalMonths();}
 private static ResponseStatusException bad(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 static Preview calculate(BigDecimal amount,PurchaseFinanceController.Input input){
  int count=count(input),interval=interval(input);
  if(count<1||count>120||interval<1||interval>12)throw bad("Use de 1 a 120 parcelas e intervalo de 1 a 12 meses.");
  if(input.issuedOn().getYear()<1||input.issuedOn().getYear()>9999||input.dueOn().isBefore(input.issuedOn()))throw bad("Confira emissão e primeiro vencimento.");
  if(input.description().trim().length()<5)throw bad("Descrição deve conter pelo menos cinco caracteres.");
  String reference=input.reference().toUpperCase(Locale.ROOT);
  if(count>1&&reference.length()>56)throw bad("Referência base de parcelamento deve ter no máximo 56 caracteres.");
  BigInteger cents=amount.movePointRight(2).toBigIntegerExact();var division=cents.divideAndRemainder(BigInteger.valueOf(count));
  if(division[0].signum()<=0)throw bad("Cada parcela precisa ter pelo menos um centavo.");
  var result=new ArrayList<Line>();
  for(int i=0;i<count;i++){
   LocalDate due;try{due=input.dueOn().plusMonths((long)i*interval);}catch(DateTimeException e){throw bad("Vencimento fora do intervalo permitido.");}
   if(due.getYear()<1||due.getYear()>9999)throw bad("Vencimento fora do intervalo permitido.");
   BigInteger value=division[0].add(i<division[1].intValueExact()?BigInteger.ONE:BigInteger.ZERO);
   result.add(new Line(i+1,count==1?reference:reference+"."+String.format(Locale.ROOT,"%03d",i+1),due,new BigDecimal(value,2)));
  }
  return new Preview(amount.setScale(2),List.copyOf(result));
 }
}
