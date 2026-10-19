package cl.campuslab.catalog.messaging;

import java.time.Instant;

import jakarta.validation.Validator;

import cl.campuslab.catalog.exception.ResourceNotFoundException;
import cl.campuslab.catalog.stock.StockCommand;
import cl.campuslab.catalog.stock.StockRules;
import cl.campuslab.catalog.stock.StockService;

import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.web.server.ResponseStatusException;

@Service
public class StockCommandService {

    private final StockService stock;
    private final StockResultOutboxRepository results;
    private final Validator validator;
    private final TransactionTemplate transaction;

    public StockCommandService(
            StockService stock,
            StockResultOutboxRepository results,
            Validator validator,
            PlatformTransactionManager transactionManager) {

        this.stock = stock;
        this.results = results;
        this.validator = validator;

        this.transaction = new TransactionTemplate(transactionManager);

        this.transaction.setPropagationBehavior(
                TransactionDefinition.PROPAGATION_REQUIRES_NEW);
    }

    public void process(StockCommandMessage command) {
        if (command == null
                || !validator.validate(command).isEmpty()) {
            throw new IllegalArgumentException(
                    "El comando de stock tiene datos invalidos.");
        }

        try {
            transaction.executeWithoutResult(
                    status -> applyAndStore(command));

        } catch (ResourceNotFoundException exception) {
            storeRejection(
                    command,
                    "El recurso solicitado no existe.");

        } catch (ResponseStatusException exception) {
            int status = exception.getStatusCode().value();

            if (status != 400 && status != 404 && status != 409) {
                throw exception;
            }

            String reason = exception.getReason();

            storeRejection(
                    command,
                    reason == null || reason.isBlank()
                            ? "La operacion fue rechazada por catalogo."
                            : reason);
        }
    }

    private void applyAndStore(StockCommandMessage command) {
        if (rescheduleExisting(command)) {
            return;
        }

        StockCommand movement = new StockCommand(
                command.resourceId(),
                command.quantity(),
                StockRules.Action.valueOf(command.action().name())
        );

        // StockService participa en esta misma transaccion.
        stock.apply(command.bookingId(), movement);

        // El movimiento y su resultado se confirman juntos.
        results.saveAndFlush(
                StockResultOutbox.pending(
                        command,
                        StockResultMessage.Outcome.APPLIED,
                        "Operacion de stock aplicada."
                )
        );
    }

    private void storeRejection(
            StockCommandMessage command,
            String detail) {

        // La transaccion del movimiento ya fue revertida.
        // El rechazo se registra en una transaccion nueva.
        transaction.executeWithoutResult(status -> {
            if (rescheduleExisting(command)) {
                return;
            }

            String safeDetail = detail.substring(
                    0,
                    Math.min(detail.length(), 500));

            results.saveAndFlush(
                    StockResultOutbox.pending(
                            command,
                            StockResultMessage.Outcome.REJECTED,
                            safeDetail
                    )
            );
        });
    }

    private boolean rescheduleExisting(StockCommandMessage command) {
        StockResultOutbox existing = results
                .findLockedByOperationId(
                        command.operationId().toString())
                .orElse(null);

        if (existing == null) {
            return false;
        }

        boolean sameCommand =
                existing.getBookingId().equals(command.bookingId())
                && existing.getResourceId().equals(command.resourceId())
                && existing.getQuantity().equals(command.quantity())
                && existing.getAction() == command.action();

        if (!sameCommand) {
            throw new IllegalArgumentException(
                    "El identificador de operacion ya fue utilizado "
                            + "con otros datos.");
        }

        // Reenvia el resultado original sin repetir el movimiento.
        existing.setState(StockResultOutbox.State.PENDING);
        existing.setNextAttemptAt(Instant.now());

        results.save(existing);

        return true;
    }
}