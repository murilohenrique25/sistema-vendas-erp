package br.com.erp.finance;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;

public final class PayableContracts {
    private PayableContracts() {}
    public enum Method { CASH,BANK_TRANSFER,PIX,OTHER }
    public record Totals(@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal originalAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal paidAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outstandingAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal overdueAmount){}
    public record Input(@NotNull UUID requestId,@NotNull UUID branchId,@NotNull UUID supplierId,UUID purchaseReceiptId,
        @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,60}") String reference,@NotNull @Size(max=160) String document,
        @NotBlank @Size(min=5,max=500) String description,@NotNull LocalDate issuedOn,@NotNull LocalDate dueOn,
        @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount) {}
    public record PaymentInput(@NotNull UUID paymentId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
        @NotNull LocalDate paidOn,@NotNull Method method,@NotNull @Size(max=160) String reference,@NotBlank @Size(min=5,max=500) String note,UUID treasuryAccountId) {}
    public record Reversal(@NotNull UUID requestId,@NotBlank @Size(min=5,max=500) String note) {}
    public record Cancellation(@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note) {}
    public record View(UUID id,UUID branchId,UUID supplierId,String supplierName,UUID purchaseReceiptId,String reference,String document,String description,
        LocalDate issuedOn,LocalDate dueOn,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal paidAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outstanding,
        String status,long version,UUID createdBy,OffsetDateTime createdAt,String cancellationNote,boolean overdue,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal creditedAmount) {}
    public record Payment(UUID id,UUID payableId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,LocalDate paidOn,Method method,String reference,String note,String status,UUID createdBy,OffsetDateTime createdAt,UUID reversalRequestId,String reversalNote,UUID reversedBy,OffsetDateTime reversedAt,UUID treasuryAccountId,UUID treasuryOperationId,UUID treasuryReversalOperationId) {}
}
