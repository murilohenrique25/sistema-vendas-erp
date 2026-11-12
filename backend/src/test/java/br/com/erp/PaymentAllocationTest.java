package br.com.erp;

import br.com.erp.pos.PaymentAllocation;
import br.com.erp.pos.SaleContracts.*;
import java.math.BigDecimal;
import java.util.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.web.server.ResponseStatusException;
import static org.junit.jupiter.api.Assertions.*;

/** Pure unit tests: no Spring context, JDBC, H2 or external services. */
class PaymentAllocationTest {
 final UUID drawer=UUID.randomUUID(),bank=UUID.randomUUID();
 BigDecimal n(String s){return new BigDecimal(s);}
 PaymentInput cash(String amount,String tendered){return new PaymentInput(Method.CASH,null,n(amount),tendered==null?null:n(tendered),"",true);}
 PaymentInput pix(String amount,String tendered,String reference){return new PaymentInput(Method.PIX,bank,n(amount),tendered==null?null:n(tendered),reference,true);}
 PaymentAllocation.Summary split(String total,PaymentInput... lines){return PaymentAllocation.calculate(n(total),null,List.of(lines),drawer);}
 void money(String expected,BigDecimal actual){assertEquals(0,n(expected).compareTo(actual));}
 @Test void cashOnlyChange(){var s=PaymentAllocation.calculate(n("10.25"),n("20"),null,drawer);assertTrue(s.balanced());money("9.75",s.change());money("10.25",s.applied());assertEquals(drawer,s.lines().getFirst().accountId());}
 @Test void mixedAllocation(){var s=split("20.30",pix("10.10",null," proof "),cash("10.20","15"));assertTrue(s.balanced());money("4.80",s.change());money("25.10",s.tendered());assertEquals("proof",s.lines().getFirst().reference());}
 @Test void partialAndEmptyHaveRemaining(){var s=split("20",pix("5",null,"proof"));assertFalse(s.balanced());money("15",s.remaining());money("0",s.excess());money("20",split("20").remaining());}
 @Test void excessIsNotChange(){var s=split("10",cash("11","12"));assertFalse(s.balanced());money("1",s.excess());money("1",s.change());money("0",s.remaining());}
 @Test void moneyIsExactAtMaximum(){var s=PaymentAllocation.calculate(n("99999999999999999.99"),n("99999999999999999.99"),null,drawer);assertTrue(s.balanced());money("99999999999999999.99",s.applied());}
 @Test void onlyOneCashLine(){assertThrows(ResponseStatusException.class,()->split("10",cash("5",null),cash("5",null)));}
 @Test void cashMustUseDrawerAndNoProof(){assertThrows(ResponseStatusException.class,()->split("10",new PaymentInput(Method.CASH,bank,n("10"),null,"",true)));assertThrows(ResponseStatusException.class,()->split("10",new PaymentInput(Method.CASH,null,n("10"),null,"proof",true)));}
 @Test void duplicateProofConflictsAfterTrim(){var e=assertThrows(ResponseStatusException.class,()->split("10",pix("5",null,"proof"),pix("5",null," proof ")));assertEquals(409,e.getStatusCode().value());}
 @Test void proofNamespaceIncludesMethod(){assertTrue(split("10",pix("5",null,"proof"),new PaymentInput(Method.BANK_TRANSFER,bank,n("5"),null,"proof",true)).balanced());}
 @Test void bankRequiresAccountProofAndConfirmation(){for(var line:List.of(new PaymentInput(Method.PIX,null,n("10"),null,"proof",true),new PaymentInput(Method.PIX,bank,n("10"),null," ",true),new PaymentInput(Method.PIX,bank,n("10"),null,"proof",false)))assertThrows(ResponseStatusException.class,()->split("10",line));}
 @Test void onlyCashAllowsChangeAndMustCoverAmount(){assertThrows(ResponseStatusException.class,()->split("10",pix("10","11","proof")));assertThrows(ResponseStatusException.class,()->split("10",cash("10","9")));}
 @Test void aggregatedTenderCannotOverflow(){assertThrows(ResponseStatusException.class,()->split("99999999999999999.99",pix("99999999999999999.98",null,"proof"),cash("0.01","0.02")));}
 @Test void legacyAndSplitMutuallyExclusive(){assertThrows(ResponseStatusException.class,()->PaymentAllocation.calculate(n("10"),n("10"),List.of(cash("10",null)),drawer));}
 @Test void missingOrInvalidMethodAndInputRejected(){assertThrows(ResponseStatusException.class,()->PaymentAllocation.calculate(n("10"),null,null,drawer));assertThrows(ResponseStatusException.class,()->split("10",new PaymentInput(null,null,n("10"),null,null,true)));assertThrows(ResponseStatusException.class,()->PaymentAllocation.calculate(n("10"),null,Collections.singletonList(null),drawer));}
 @Test void moreThanHundredLinesRejected(){assertThrows(ResponseStatusException.class,()->PaymentAllocation.calculate(n("10"),null,Collections.nCopies(101,cash("1",null)),drawer));}
 @ParameterizedTest @ValueSource(strings={"0","-1","0.001","100000000000000000"}) void invalidMoney(String value){assertThrows(ResponseStatusException.class,()->split("10",cash(value,null)));}
 @Test void summaryAndLinesAreImmutable(){var s=split("10",cash("10",null));assertThrows(UnsupportedOperationException.class,()->s.lines().clear());}
}
