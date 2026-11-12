package br.com.erp.finance;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class ReceivableContracts {
    private ReceivableContracts() {}
    public record Totals(@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal originalAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal receivedAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal creditedAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outstandingAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal overdueAmount){}
    public record Origin(UUID planId,Integer position,Integer installmentCount,UUID saleId,String reference){}
    public enum Method { CASH,BANK_TRANSFER,PIX,OTHER }
    public record Input(@NotNull UUID requestId,@NotNull UUID branchId,@NotNull UUID customerId, 
        @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,60}") String reference,@NotNull @Size(max=160) String document,
        @NotBlank @Size(min=5,max=500) String description,@NotNull LocalDate issuedOn,@NotNull LocalDate dueOn,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount) {}
    public record ReceiptInput(@NotNull UUID receiptId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
        @NotNull LocalDate receivedOn,@NotNull Method method,@NotNull @Size(max=160) String reference,@NotBlank @Size(min=5,max=500) String note,UUID treasuryAccountId) {}
    public record Reversal(@NotNull UUID requestId,@NotBlank @Size(min=5,max=500) String note) {}
    public record Cancellation(@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note) {}
    public record Adjustment(UUID returnId,UUID saleId,String kind,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,OffsetDateTime createdAt,String note,UUID receiptId,UUID treasuryOperationId) {}
    public record View(UUID id,UUID branchId,UUID customerId,String customerName,String reference,String document,String description,
        LocalDate issuedOn,LocalDate dueOn,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal receivedAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outstanding,
        String status,long version,UUID createdBy,OffsetDateTime createdAt,String cancellationNote,boolean overdue,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal creditedAmount) {}
    public record Receipt(UUID id,UUID receivableId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,LocalDate receivedOn,Method method,String reference,String note,String status,UUID createdBy,OffsetDateTime createdAt,UUID reversalRequestId,String reversalNote,UUID reversedBy,OffsetDateTime reversedAt,UUID treasuryAccountId,UUID treasuryOperationId,UUID treasuryReversalOperationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal refundedAmount) {}
}
