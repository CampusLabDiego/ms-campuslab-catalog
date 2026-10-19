package cl.campuslab.catalog.messaging;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class StockResultOutboxStore {

    private final StockResultOutboxRepository repository;

    public StockResultOutboxStore(
            StockResultOutboxRepository repository) {

        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public List<String> findReadyOperationIds() {
        return repository
                .findTop20ByStateAndNextAttemptAtLessThanEqualOrderByProcessedAtAsc(
                        StockResultOutbox.State.PENDING,
                        Instant.now())
                .stream()
                .map(StockResultOutbox::getOperationId)
                .toList();
    }

    @Transactional
    public Optional<Publication> claim(String operationId) {
        StockResultOutbox result = repository
                .findLockedByOperationId(operationId)
                .orElse(null);

        Instant now = Instant.now();

        if (result == null
                || result.getState() != StockResultOutbox.State.PENDING
                || result.getNextAttemptAt().isAfter(now)) {
            return Optional.empty();
        }

        int attempt = result.getAttempts() + 1;

        result.setAttempts(attempt);

        // Recupera el intento si el publicador se interrumpe.
        result.setNextAttemptAt(now.plusSeconds(60));

        return Optional.of(new Publication(
                result.getOperationId(),
                attempt,
                result.toMessage()
        ));
    }

    @Transactional
    public void markPublished(String operationId, int attempt) {
        StockResultOutbox result = repository
                .findLockedByOperationId(operationId)
                .orElse(null);

        if (!canUpdate(result, attempt)) {
            return;
        }

        result.setState(StockResultOutbox.State.PUBLISHED);
        result.setLastPublishedAt(Instant.now());
        result.setLastError(null);
    }

    @Transactional
    public void markFailed(
            String operationId,
            int attempt,
            String detail) {

        StockResultOutbox result = repository
                .findLockedByOperationId(operationId)
                .orElse(null);

        if (!canUpdate(result, attempt)) {
            return;
        }

        long delaySeconds = Math.min(
                300L,
                5L * (1L << Math.min(attempt - 1, 6))
        );

        String message = detail == null || detail.isBlank()
                ? "No se pudo confirmar la publicacion del resultado."
                : detail;

        result.setState(StockResultOutbox.State.PENDING);
        result.setNextAttemptAt(
                Instant.now().plusSeconds(delaySeconds));

        result.setLastError(
                message.substring(0, Math.min(message.length(), 500)));
    }

    private boolean canUpdate(
            StockResultOutbox result,
            int attempt) {

        return result != null
                && result.getState() == StockResultOutbox.State.PENDING
                && result.getAttempts() == attempt;
    }

    public record Publication(
            String operationId,
            int attempt,
            StockResultMessage message
    ) {
    }
}