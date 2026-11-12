package br.com.erp.purchase;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public final class PurchaseContracts {
    private PurchaseContracts() {}
    public record Item(@NotNull UUID productId,@NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity,@NotNull @DecimalMin("0") @Digits(integer=13,fraction=6) BigDecimal unitCost) {}
    public record Input(@NotNull UUID requestId,@NotNull UUID branchId,@NotNull UUID supplierId,@NotNull @Size(max=500) String notes,@NotNull @Size(min=1,max=100) List<@NotNull @Valid Item> items) {}
    public record ReceivedItem(@NotNull UUID orderLineId,@NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity) {}
    public record ReceiptInput(@NotNull UUID receiptId,@NotNull UUID warehouseId,@NotBlank @Size(min=5,max=500) String note,@NotNull @Size(min=1,max=100) List<@NotNull @Valid ReceivedItem> items) {}
    public record Cancellation(@NotNull @Min(0) Long version,@NotBlank @Size(min=5,max=500) String note) {}
    public record Header(UUID id,UUID branchId,UUID supplierId,String supplierName,String notes,String status,long version,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total,UUID createdBy,OffsetDateTime createdAt,String cancellationNote) {}
    public record Line(UUID id,UUID productId,String sku,String name,String unit,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal receivedQuantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unitCost,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal total) {}
    public record Order(Header header,List<Line> lines) {}
    public record ReceiptLine(UUID orderLineId,UUID operationId,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,@JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal amount) {}
    public record Receipt(UUID id,UUID orderId,UUID warehouseId,String note,UUID createdBy,OffsetDateTime createdAt,List<ReceiptLine> lines) {}
}
