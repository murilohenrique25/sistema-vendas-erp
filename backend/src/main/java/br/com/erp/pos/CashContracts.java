package br.com.erp.pos;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
public final class CashContracts {
 private CashContracts(){}
 public enum Kind { SUPPLY,WITHDRAW }
 public record TerminalInput(@NotNull UUID requestId,@NotNull UUID branchId,@NotNull UUID accountId,@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,40}") String code,@NotBlank @Size(max=160) String name){}
 public record Active(@NotNull Boolean active,@NotNull @Min(0) Long version){}
 public record Opening(@NotNull UUID requestId,@NotNull UUID terminalId,@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal openingAmount,@NotBlank @Size(min=5,max=500) String note){}
 public record MovementInput(@NotNull UUID requestId,@NotNull Kind kind,UUID otherAccountId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotBlank @Size(min=5,max=500) String note){}
 public record Closing(@NotNull UUID requestId,@NotNull @Min(0) Long version,@NotNull @DecimalMin("0") @Digits(integer=17,fraction=2) BigDecimal countedAmount,@NotBlank @Size(min=5,max=500) String note){}
 public record Terminal(UUID id,UUID branchId,UUID accountId,UUID warehouseId,String code,String name,boolean active,long version){}
 public record Session(UUID id,UUID terminalId,UUID branchId,UUID accountId,UUID operatorId,String operatorName,String terminalCode,String terminalName,String status,long version,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal openingAmount,String openingNote,OffsetDateTime openedAt,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal expectedAmount,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal countedAmount,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal difference,String closeNote,UUID closedBy,OffsetDateTime closedAt){}
 public record Movement(UUID id,UUID sessionId,Kind kind,UUID otherAccountId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,String note,UUID treasuryOperationId,UUID createdBy,OffsetDateTime createdAt){}
}
