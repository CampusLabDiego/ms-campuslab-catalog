package cl.campuslab.catalog.stock;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record StockCommand(

        @NotNull
        @Positive
        Long resourceId,

        @Positive
        int quantity,

        @NotNull
        StockRules.Action action

) {
}
