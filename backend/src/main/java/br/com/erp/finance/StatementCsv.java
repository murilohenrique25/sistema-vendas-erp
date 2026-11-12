package br.com.erp.finance;
import java.util.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;
import static br.com.erp.finance.ReconciliationContracts.*;
public final class StatementCsv {
 private StatementCsv(){}
 private static ResponseStatusException invalid(String message){return new ResponseStatusException(HttpStatus.BAD_REQUEST,message);}
 public static Input parse(CsvInput input){
  String source=input.content();if(source.startsWith("\uFEFF"))source=source.substring(1);
  var rows=new ArrayList<List<String>>();var row=new ArrayList<String>();var field=new StringBuilder();boolean quoted=false,closed=false;
  for(int i=0;i<source.length();i++){
   char c=source.charAt(i);
   if(quoted){if(c=='"'){if(i+1<source.length()&&source.charAt(i+1)=='"'){field.append('"');i++;}else{quoted=false;closed=true;}}else field.append(c);continue;}
   if(c=='"'){if(field.length()!=0||closed)throw invalid("Aspas inválidas no CSV.");quoted=true;continue;}
   if(c==';'||c=='\n'||c=='\r'){row.add(field.toString());field.setLength(0);closed=false;if(c!=';'){if(c=='\r'&&i+1<source.length()&&source.charAt(i+1)=='\n')i++;rows.add(List.copyOf(row));row.clear();if(rows.size()>501)throw invalid("CSV deve conter até 500 lançamentos.");}continue;}
   if(closed)throw invalid("Caracteres após fechamento de aspas no CSV.");field.append(c);
  }
  if(quoted)throw invalid("Aspas não encerradas no CSV.");if(field.length()>0||closed||!row.isEmpty()){row.add(field.toString());rows.add(List.copyOf(row));}
  if(rows.isEmpty()||!rows.getFirst().equals(List.of("externalId","date","amount","description")))throw invalid("Cabeçalho esperado: externalId;date;amount;description.");
  var lines=new ArrayList<LineInput>();
  for(int i=1;i<rows.size();i++){var values=rows.get(i);if(values.size()!=4)throw invalid("Linha "+(i+1)+" deve conter quatro campos.");try{if(!values.get(2).matches("-?[0-9]{1,17}(\\.[0-9]{1,2})?"))throw new IllegalArgumentException();lines.add(new LineInput(values.get(0),LocalDate.parse(values.get(1)),new BigDecimal(values.get(2)),values.get(3)));}catch(RuntimeException error){throw invalid("Data ou valor inválido na linha "+(i+1)+". Use AAAA-MM-DD e ponto para centavos.");}}
  return new Input(input.requestId(),input.accountId(),input.reference(),lines);
 }
}
