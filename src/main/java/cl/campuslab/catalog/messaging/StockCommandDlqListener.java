package cl.campuslab.catalog.messaging;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Map;

import com.rabbitmq.client.Channel;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
public class StockCommandDlqListener {

    private static final Logger log =
            LoggerFactory.getLogger(StockCommandDlqListener.class);

    private static final int MAX_LOG_BODY_LENGTH = 2_000;

    @RabbitListener(queues = RabbitNames.STOCK_DLQ)
    public void receive(
            Message message,
            Channel channel) throws IOException {

        Map<String, Object> headers =
                message.getMessageProperties().getHeaders();

        String body = new String(
                message.getBody(),
                StandardCharsets.UTF_8);

        String preview = body
                .substring(0, Math.min(body.length(), MAX_LOG_BODY_LENGTH))
                .replace("\r", "\\r")
                .replace("\n", "\\n");

        log.error(
                "DLQ comando stock: mensaje={}, colaOrigen={}, motivo={}, contenido={}, truncado={}",
                message.getMessageProperties().getMessageId(),
                headers.getOrDefault(
                        "x-first-death-queue",
                        RabbitNames.STOCK_QUEUE),
                headers.getOrDefault(
                        "x-first-death-reason",
                        "desconocido"),
                preview,
                body.length() > MAX_LOG_BODY_LENGTH);

        channel.basicAck(
                message.getMessageProperties().getDeliveryTag(),
                false);
    }
}