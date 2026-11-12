package br.com.erp.pos;
import jakarta.validation.Valid;
import br.com.erp.pos.SaleContracts.ItemInput;
import jakarta.validation.constraints.*;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;
public final class PosCommercialContracts {
 private PosCommercialContracts(){}
 public record PolicyInput(@NotNull @Min(0) Long version,@NotNull @DecimalMin("0") @DecimalMax("100") @Digits(integer=3,fraction=2) BigDecimal operatorPercent){}
 public record Policy(UUID branchId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal operatorPercent,long version){}
 public record CartInput(@NotNull UUID requestId,@NotNull UUID sessionId,@Min(0) Long version,@NotBlank @Size(min=5,max=500) String note,@NotEmpty @Size(max=100) List<@NotNull @Valid ItemInput> items){}
 public record DecisionInput(@NotNull UUID requestId,@NotNull @Min(0) Long version,boolean approve,@NotBlank @Size(min=5,max=500) String note){}
 public record DiscardInput(@NotNull UUID requestId,@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note){}
 public record Approval(UUID id,UUID branchId,UUID requestedBy,String status,long version,long policyVersion,String note,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal grossTotal,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal discountTotal,List<CartPricing.Line> items,OffsetDateTime createdAt,UUID decidedBy,String decisionNote,OffsetDateTime expiresAt,UUID saleId,String requestedByName){}
 public record Suspension(UUID id,UUID branchId,UUID originalSessionId,UUID lastSessionId,UUID createdBy,UUID updatedBy,String status,long version,String note,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal grossTotal,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal discountTotal,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total,List<CartPricing.Line> items,OffsetDateTime createdAt,OffsetDateTime updatedAt,UUID saleId,String discardNote,String createdByName){}
}
