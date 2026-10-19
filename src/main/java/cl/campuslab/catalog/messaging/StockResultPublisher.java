package cl.campuslab.catalog.messaging;

import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.rabbit.connection.CorrelationData;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
@EnableScheduling
public class StockResultPublisher {

    private static final Logger log =
            LoggerFactory.getLogger(StockResultPublisher.class);

    private final RabbitTemplate rabbitTemplate;
    private final StockResultOutboxStore store;

    public StockResultPublisher(
            RabbitTemplate rabbitTemplate,
            StockResultOutboxStore store) {

        this.rabbitTemplate = rabbitTemplate;
        this.store = store;
    }

    @Scheduled(
            fixedDelayString =
                    "${campuslab.messaging.result-delay-ms:5000}"
    )
    public void publishPendingResults() {
        for (String operationId : store.findReadyOperationIds()) {
            if (Thread.currentThread().isInterrupted()) {
                return;
            }

            store.claim(operationId).ifPresent(this::publish);
        }
    }

    private void publish(
            StockResultOutboxStore.Publication publication) {

        String operationId = publication.operationId();
        int attempt = publication.attempt();

        CorrelationData correlation = new CorrelationData(
                operationId + ":result:" + attempt);

        try {
            rabbitTemplate.convertAndSend(
                    RabbitNames.COMMAND_EXCHANGE,
                    RabbitNames.STOCK_RESULT_ROUTING_KEY,
                    publication.message(),
                    message -> {
                        message.getMessageProperties()
                                .setMessageId(operationId + ":result");

                        message.getMessageProperties()
                                .setCorrelationId(operationId);

                        message.getMessageProperties()
                                .setDeliveryMode(
                                        MessageDeliveryMode.PERSISTENT);

                        return message;
                    },
                    correlation
            );

            CorrelationData.Confirm confirmation = correlation
                    .getFuture()
                    .get(5, TimeUnit.SECONDS);

            if (correlation.getReturned() != null) {
                store.markFailed(
                        operationId,
                        attempt,
                        "El resultado no encontro una cola de destino.");

                log.warn(
                        "Resultado sin cola de destino: operacion={}, intento={}",
                        operationId,
                        attempt);

                return;
            }

            if (!confirmation.isAck()) {
                store.markFailed(
                        operationId,
                        attempt,
                        "El broker rechazo la publicacion del resultado.");

                log.warn(
                        "Resultado rechazado por el broker: operacion={}, intento={}",
                        operationId,
                        attempt);

                return;
            }

            store.markPublished(operationId, attempt);

            log.info(
                    "Resultado publicado: operacion={}, reserva={}, resultado={}, intento={}",
                    operationId,
                    publication.message().bookingId(),
                    publication.message().outcome(),
                    attempt);

        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();

            store.markFailed(
                    operationId,
                    attempt,
                    "La publicacion del resultado fue interrumpida.");

        } catch (Exception exception) {
            store.markFailed(
                    operationId,
                    attempt,
                    "No se pudo confirmar la publicacion del resultado: "
                            + exception.getClass().getSimpleName());

            log.warn(
                    "Resultado pendiente de reintento: operacion={}, intento={}, causa={}",
                    operationId,
                    attempt,
                    exception.getClass().getSimpleName());
        }
    }
}