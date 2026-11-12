package br.com.erp.pos;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
public final class SaleContracts {
 private SaleContracts(){}
 public record WarehouseBinding(@NotNull UUID warehouseId,@NotNull @Min(0) Long version){}
 public record ItemInput(@NotNull UUID productId,@NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity,@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal expectedUnitPrice,@DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal discountAmount){}
 public enum Method {CASH,PIX,BANK_TRANSFER}
 public record PaymentInput(@NotNull Method method,UUID accountId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal tendered,@Size(max=160) String reference,Boolean confirmed){}
 public record CreditInput(@NotNull UUID customerId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotNull java.time.LocalDate firstDueOn,@NotNull @Min(1) @Max(120) Integer installmentCount,@NotNull @Min(1) @Max(12) Integer intervalMonths){}
 public record Credit(UUID customerId,String customerName,UUID planId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outstanding,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal creditedAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal receiptRefundedAmount){}
 public record Input(@NotNull UUID requestId,@NotNull UUID sessionId,@DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal tendered,@NotEmpty @Size(max=100) List<@NotNull @Valid ItemInput> items,@Size(min=1,max=100) List<@NotNull @Valid PaymentInput> payments,UUID discountApprovalId,UUID suspensionId,@Min(0) Long suspensionVersion,@Valid CreditInput credit,UUID salespersonId,UUID loyaltyCustomerId){
 public Input(UUID requestId,UUID sessionId,BigDecimal tendered,List<ItemInput> items,List<PaymentInput> payments,UUID discountApprovalId,UUID suspensionId,Long suspensionVersion,CreditInput credit){this(requestId,sessionId,tendered,items,payments,discountApprovalId,suspensionId,suspensionVersion,credit,null,null);}
 }
 public record CancellationInput(@NotNull UUID requestId,@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note,boolean externalRefundsConfirmed){}
 public record Payment(UUID id,Method method,UUID accountId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal tendered,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal changeAmount,String reference,String status,UUID treasuryOperationId,UUID reversalOperationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal refundedAmount){
  @JsonProperty public String refundStatus(){return refundedAmount.signum()==0?"NONE":refundedAmount.compareTo(amount)==0?"FULL":"PARTIAL";}
 }
 public record Item(UUID productId,String sku,String name,String unit,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unitPrice,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total,UUID stockOperationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal returnedQuantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal returnedAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal grossTotal,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal discountAmount){}
 public record Sale(UUID id,UUID sessionId,UUID branchId,UUID warehouseId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal tendered,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal changeAmount,UUID treasuryOperationId,UUID createdBy,OffsetDateTime createdAt,List<Item> items,String status,long version,String cancelNote,UUID cancelledBy,OffsetDateTime cancelledAt,List<Payment> payments,UUID discountApprovalId,UUID suspensionId,Credit credit){
  @JsonProperty @JsonFormat(shape=JsonFormat.Shape.STRING) public BigDecimal grossTotal(){return items.stream().map(Item::grossTotal).reduce(BigDecimal.ZERO,BigDecimal::add);}
  @JsonProperty @JsonFormat(shape=JsonFormat.Shape.STRING) public BigDecimal discountTotal(){return items.stream().map(Item::discountAmount).reduce(BigDecimal.ZERO,BigDecimal::add);}
  @JsonProperty public String commercialStatus(){if(status.equals("CANCELLED"))return status;boolean any=items.stream().anyMatch(i->i.returnedQuantity().signum()>0);return !any?status:items.stream().allMatch(i->i.returnedQuantity().compareTo(i.quantity())==0)?"RETURNED":"PARTIALLY_RETURNED";}
  @JsonProperty @JsonFormat(shape=JsonFormat.Shape.STRING) public BigDecimal refundedAmount(){return payments.stream().map(Payment::refundedAmount).reduce(credit==null?BigDecimal.ZERO:credit.receiptRefundedAmount(),BigDecimal::add);}
  @JsonProperty public String financialStatus(){if(credit!=null){if(status.equals("CANCELLED"))return "CANCELLED";BigDecimal net=total.subtract(items.stream().map(Item::returnedAmount).reduce(BigDecimal.ZERO,BigDecimal::add));if(net.signum()==0)return "SETTLED_BY_RETURN";return credit.outstanding().signum()==0?"PAID":credit.outstanding().compareTo(net)==0?"UNPAID":"PARTIALLY_PAID";}BigDecimal refunded=refundedAmount();return refunded.compareTo(total)==0?"REFUNDED":refunded.signum()>0?"PARTIALLY_REFUNDED":"PAID";}
  @JsonProperty public String fiscalStatus(){return "NOT_ISSUED";}
 }
}
