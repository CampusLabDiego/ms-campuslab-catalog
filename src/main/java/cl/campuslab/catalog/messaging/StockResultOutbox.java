package cl.campuslab.catalog.messaging;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Entity
@Table(
        name = "stock_result_outbox",
        indexes = {
                @Index(
                        name = "idx_stock_result_pending",
                        columnList = "state,next_attempt_at"),
                @Index(
                        name = "idx_stock_result_booking",
                        columnList = "booking_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class StockResultOutbox {

    @Id
    @Column(name = "operation_id", nullable = false, length = 36)
    private String operationId;

    @Column(name = "booking_id", nullable = false)
    private Long bookingId;

    @Column(name = "resource_id", nullable = false)
    private Long resourceId;

    @Column(nullable = false)
    private Integer quantity;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockCommandMessage.Action action;

    @Column(name = "requested_at", nullable = false)
    private Instant requestedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StockResultMessage.Outcome outcome;

    @Column(nullable = false, length = 500)
    private String detail;

    @Column(name = "processed_at", nullable = false)
    private Instant processedAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private State state;

    @Column(nullable = false)
    private int attempts;

    @Column(name = "next_attempt_at", nullable = false)
    private Instant nextAttemptAt;

    @Column(name = "last_published_at")
    private Instant lastPublishedAt;

    @Column(name = "last_error", length = 500)
    private String lastError;

    @Version
    private Long version;

    public static StockResultOutbox pending(
            StockCommandMessage command,
            StockResultMessage.Outcome outcome,
            String detail) {

        Instant now = Instant.now();

        StockResultOutbox result = new StockResultOutbox();

        result.setOperationId(command.operationId().toString());
        result.setBookingId(command.bookingId());
        result.setResourceId(command.resourceId());
        result.setQuantity(command.quantity());
        result.setAction(command.action());
        result.setRequestedAt(command.requestedAt());
        result.setOutcome(outcome);
        result.setDetail(detail);
        result.setProcessedAt(now);

        result.setState(State.PENDING);
        result.setAttempts(0);
        result.setNextAttemptAt(now);

        return result;
    }

    public StockResultMessage toMessage() {
        return new StockResultMessage(
                UUID.fromString(operationId),
                bookingId,
                resourceId,
                action,
                outcome,
                detail,
                processedAt
        );
    }

    public enum State {
        PENDING,
        PUBLISHED
    }
}