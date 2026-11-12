package br.com.erp.finance;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.*;
import java.util.*;
public final class ReconciliationContracts {
 private ReconciliationContracts(){}
 public record LineInput(@NotBlank @Size(max=100) String externalId,@NotNull LocalDate postedOn,
  @NotNull @Digits(integer=17,fraction=2) @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,@NotNull @Size(max=500) String description){}
 public record Input(@NotNull UUID requestId,@NotNull UUID accountId,@NotBlank @Size(max=160) String reference,@NotNull @Size(min=1,max=500) List<@NotNull @Valid LineInput> lines){}
 public record CsvInput(@NotNull UUID requestId,@NotNull UUID accountId,@NotBlank @Size(max=160) String reference,@NotBlank @Size(max=1048576) String content){}
 public record Review(@NotNull UUID requestId,@NotNull @Min(0) Long version,UUID entryId,@NotBlank @Size(min=5,max=500) String note){}
 public record ImportView(UUID id,UUID accountId,String reference,int newCount,int reusedCount,UUID createdBy,OffsetDateTime createdAt){}
 public record Line(UUID id,UUID accountId,String externalId,LocalDate postedOn,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,String description,String status,long version,UUID matchedEntryId,String reviewNote,UUID reviewedBy,OffsetDateTime reviewedAt){}
 public record Candidate(UUID id,UUID operationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount,String note,String origin,OffsetDateTime createdAt){}
 public record History(UUID id,String action,UUID entryId,String note,UUID createdBy,OffsetDateTime createdAt,long lineVersion){}
}
