package br.com.erp.finance;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class CashFlowContracts {
 private CashFlowContracts() {}
 public record ActualDay(LocalDate date,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal inflows,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outflows,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal transferIn,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal transferOut,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal closingBalance) {}
 public record PendingDay(LocalDate date,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal receivables,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal payables,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal net) {}
 public record Summary(@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal currentBalance,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal openingBalance,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal closingBalance,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal inflows,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal outflows,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal transferIn,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal transferOut,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal pendingReceivables,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal pendingPayables,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal overdueReceivables,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal overduePayables,
  @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unlinkedReceipts,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unlinkedPayments) {}
 public record Report(LocalDate from,LocalDate to,UUID branchId,LocalDate today,String zone,OffsetDateTime generatedAt,
  Summary summary,List<ActualDay> actual,List<PendingDay> pending) {}
}
