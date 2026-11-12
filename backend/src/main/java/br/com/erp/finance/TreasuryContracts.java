package br.com.erp.finance;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.UUID;
public final class TreasuryContracts {
 private TreasuryContracts() {}
 public enum Kind { CASH,BANK }
 public record Input(@NotNull UUID requestId,@NotNull UUID branchId,@NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,40}") String code,@NotBlank @Size(max=160) String name,@NotNull Kind kind) {}
 public record Movement(@NotNull UUID requestId,UUID sourceId,UUID destinationId,@NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,@NotBlank @Size(min=5,max=500) String note) {}
 public record Account(UUID id,UUID branchId,String code,String name,Kind kind,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal balance) {}
 public record Operation(UUID id,UUID sourceId,UUID destinationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,String note,UUID createdBy,OffsetDateTime createdAt) {}
 public record Entry(UUID id,UUID operationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal delta,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal balanceAfter,String note,UUID createdBy,OffsetDateTime createdAt,String origin) {}
}
