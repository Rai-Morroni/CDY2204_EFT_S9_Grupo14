package com.transportes.guiadespacho.messaging;

import com.transportes.guiadespacho.dto.GuiaMensaje;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

/**
 * MS Productor de mensajes RabbitMQ.
 *
 * Componente responsable de publicar mensajes en el exchange principal
 * (guias-exchange) con la routing key de tickets (guias.tickets),
 * desde donde RabbitMQ los enruta a la cola guias-queue.
 *
 * Este componente es invocado por el controlador cada vez que llega
 * una peticion de creacion o actualizacion de guias.
 *
 * Es un componente DISTINTO al MS Consumidor (GuiaMessageConsumer),
 * cumpliendo el requisito de la actividad: "productores y consumidores
 * de mensajes deben ser componentes distintos".
 */
@Component
public class GuiaMessageProducer {

    private static final Logger log = LoggerFactory.getLogger(GuiaMessageProducer.class);

    private final RabbitTemplate rabbitTemplate;

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.routing-key.tickets}")
    private String routingKeyTickets;

    public GuiaMessageProducer(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }

    /**
     * Publica un mensaje de guia en la cola principal.
     * El mensaje viaja en formato JSON gracias al Jackson2JsonMessageConverter
     * configurado en RabbitMQConfig.
     *
     * @param mensaje datos de la guia a publicar
     */
    public void publicar(GuiaMensaje mensaje) {
        log.info("[PRODUCTOR] Publicando mensaje en cola | operacion={} | id={}",
                mensaje.getOperacion(), mensaje.getId());

        rabbitTemplate.convertAndSend(exchange, routingKeyTickets, mensaje);

        log.info("[PRODUCTOR] Mensaje publicado exitosamente | id={}", mensaje.getId());
    }
}
