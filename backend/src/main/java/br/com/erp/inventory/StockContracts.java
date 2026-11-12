package br.com.erp.inventory;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.OffsetDateTime;
import java.util.*;

public final class StockContracts {
    private StockContracts() {}
    public enum Type { RECEIPT, ADJUSTMENT_IN, ADJUSTMENT_OUT, INTERNAL_CONSUMPTION }
    public record WarehouseInput(@NotNull UUID branchId,@NotBlank @Pattern(regexp="[A-Za-z0-9_-]{1,30}") String code,@NotBlank @Size(max=160) String name) {}
    public record Warehouse(UUID id,UUID branchId,String code,String name,boolean active) {}
    public record MovementInput(@NotNull UUID operationId,@NotNull UUID warehouseId,@NotNull UUID productId,@NotNull Type type,
        @NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity,
        @DecimalMin("0") @Digits(integer=13,fraction=6) BigDecimal unitCost,
        @NotBlank @Size(min=5,max=500) String reason) {}
    public record TransferInput(@NotNull UUID operationId,@NotNull UUID sourceWarehouseId,@NotNull UUID destinationWarehouseId,@NotNull UUID productId,
        @NotNull @DecimalMin("0.000001") @Digits(integer=13,fraction=6) BigDecimal quantity,@NotBlank @Size(min=5,max=500) String reason) {}
    public record Balance(UUID productId,String sku,String name,String unit,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal averageCost) {}
    public record Movement(UUID id,UUID operationId,UUID warehouseId,UUID productId,UUID lossId,String type,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal quantity,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal balanceAfter,String unit,
        @JsonFormat(shape=JsonFormat.Shape.STRING) BigDecimal unitCost,String reason,UUID actorId,OffsetDateTime createdAt) {}
    public record Operation(UUID operationId,List<Movement> movements) {}
    public record Snapshot(BigDecimal quantity,BigDecimal averageCost) {}
}
