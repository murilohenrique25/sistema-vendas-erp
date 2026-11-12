package br.com.erp.pos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
public final class SaleReturnContracts {
 private SaleReturnContracts(){}
 public record ItemInput(@NotNull UUID productId,@NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity){}
 public record RefundInput(@NotNull UUID paymentId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@Size(max=160) String reference){}
 public record ReceiptRefundInput(@NotNull UUID receiptId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotNull SaleContracts.Method method,UUID accountId,@Size(max=160) String reference){}
 public record DebtCredit(UUID receivableId,String reference,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount){}
 public record ReceiptRefund(UUID receiptId,UUID receivableId,SaleContracts.Method method,UUID accountId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,String reference,UUID treasuryOperationId){}
 public record Input(@NotNull UUID requestId,@NotNull UUID sessionId,@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note,boolean externalRefundsConfirmed,@NotEmpty @Size(max=100) List<@NotNull @Valid ItemInput> items,@NotNull @Size(max=100) List<@NotNull @Valid RefundInput> refunds,@DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal expectedDebtReduction,@Size(max=100) List<@NotNull @Valid ReceiptRefundInput> receiptRefunds){}
 public record Item(UUID productId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,UUID stockOperationId){}
 public record Refund(UUID paymentId,SaleContracts.Method method,UUID accountId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,String reference,UUID treasuryOperationId){}
 public record Return(UUID id,UUID saleId,UUID sessionId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total,String note,UUID createdBy,OffsetDateTime createdAt,List<Item> items,List<Refund> refunds,List<DebtCredit> debtCredits,List<ReceiptRefund> receiptRefunds){
  @JsonProperty @JsonFormat(shape=JsonFormat.Shape.STRING) public BigDecimal debtReduction(){return debtCredits.stream().map(DebtCredit::amount).reduce(BigDecimal.ZERO,BigDecimal::add);}
 }
}
