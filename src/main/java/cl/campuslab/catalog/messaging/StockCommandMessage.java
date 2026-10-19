package cl.campuslab.catalog.messaging;

import java.time.Instant;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * Comando de stock recibido desde reservas.
 */
public record StockCommandMessage(

        @NotNull
        UUID operationId,

        @NotNull
        @Positive
        Long bookingId,

        @NotNull
        @Positive
        Long resourceId,

        @NotNull
        @Positive
        Integer quantity,

        @NotNull
        Action action,

        @NotNull
        Instant requestedAt

) {

    public enum Action {
        RESERVE,
        RELEASE,
        FINISH
    }
}
