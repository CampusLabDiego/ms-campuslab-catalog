package cl.campuslab.catalog.messaging;

import java.io.IOException;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.dao.DataAccessResourceFailureException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.RecoverableDataAccessException;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.CannotCreateTransactionException;

@Component
public class StockCommandListener {

    private static final Logger log =
            LoggerFactory.getLogger(StockCommandListener.class);

    private static final int MAX_ATTEMPTS = 3;

    private final StockCommandService service;

    public StockCommandListener(StockCommandService service) {
        this.service = service;
    }

    @RabbitListener(queues = RabbitNames.STOCK_QUEUE)
    public void receive(
            StockCommandMessage command,
            Message message,
            Channel channel) throws IOException {

        long deliveryTag = message.getMessageProperties()
                .getDeliveryTag();

        for (int attempt = 1; attempt <= MAX_ATTEMPTS; attempt++) {
            try {
                // Retorna despues de confirmar el movimiento
                // y su resultado, o de guardar el rechazo.
                service.process(command);

            } catch (RuntimeException exception) {
                boolean retry =
                        isRetryable(exception)
                                && attempt < MAX_ATTEMPTS;

                if (!retry) {
                    log.error(
                            "Comando enviado a DLQ: mensaje={}, intento={}, causa={}",
                            message.getMessageProperties().getMessageId(),
                            attempt,
                            exception.getClass().getSimpleName());

                    channel.basicNack(deliveryTag, false, false);
                    return;
                }

                log.warn(
                        "Reintentando comando de stock: mensaje={}, intento={}",
                        message.getMessageProperties().getMessageId(),
                        attempt);

                try {
                    Thread.sleep(500L * attempt);
                } catch (InterruptedException interrupted) {
                    Thread.currentThread().interrupt();

                    channel.basicNack(deliveryTag, false, false);
                    return;
                }

                continue;
            }

            // Si falla el ACK, una reentrega reutilizara
            // el resultado almacenado para esta operacion.
            channel.basicAck(deliveryTag, false);

            log.info(
                    "Comando procesado: operacion={}, reserva={}, accion={}",
                    command.operationId(),
                    command.bookingId(),
                    command.action());

            return;
        }
    }

    private boolean isRetryable(Throwable exception) {
        Throwable cause = exception;

        for (int depth = 0; cause != null && depth < 10; depth++) {
            if (cause instanceof TransientDataAccessException
                    || cause instanceof RecoverableDataAccessException
                    || cause instanceof DataAccessResourceFailureException
                    || cause instanceof CannotCreateTransactionException) {
                return true;
            }

            // Dos entregas concurrentes pueden intentar guardar
            // el mismo operationId. Al reintentar se lee el resultado
            // que la otra transaccion ya haya confirmado.
            if (cause instanceof DataIntegrityViolationException) {
                return true;
            }

            cause = cause.getCause();
        }

        return false;
    }
}