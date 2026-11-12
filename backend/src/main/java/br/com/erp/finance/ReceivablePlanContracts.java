package br.com.erp.finance;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.*;

public final class ReceivablePlanContracts {
 private ReceivablePlanContracts() {}
 public record Input(@NotNull UUID requestId,@NotNull UUID branchId,@NotNull UUID customerId,
  @NotBlank @Pattern(regexp="[A-Za-z0-9._-]{1,50}") String reference,
  @NotNull @Size(max=160) String document,@NotBlank @Size(min=5,max=500) String description,
  @NotNull LocalDate issuedOn,@NotNull LocalDate firstDueOn,
  @NotNull @DecimalMin("0.01") @Digits(integer=17,fraction=2) BigDecimal amount,
  @NotNull @Min(1) @Max(120) Integer installmentCount,@NotNull @Min(1) @Max(12) Integer intervalMonths) {}
 public record Installment(int position,String reference,LocalDate dueOn,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount) {}
 public record Preview(@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,List<Installment> installments) {}
 public record Plan(UUID id,UUID branchId,UUID customerId,String reference,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,List<ReceivableContracts.View> installments) {}
}
