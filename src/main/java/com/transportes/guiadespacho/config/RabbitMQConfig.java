package com.transportes.guiadespacho.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuracion de RabbitMQ para el Sistema de Guias de Despacho.
 *
 * Estructura de colas:
 *
 *   guias-exchange  (Direct Exchange)
 *       │
 *       ├── routing key: guias.tickets  ──►  guias-queue  (Cola principal)
 *       │                                         │
 *       │                                         │ si falla (DLX)
 *       │                                         ▼
 *       └── routing key: guias.error   ──►  guias-dlq    (Dead Letter Queue)
 *
 * Flujo:
 *   1. MS Productor publica en guias-exchange con routing key "guias.tickets"
 *   2. El mensaje llega a guias-queue
 *   3. MS Consumidor lee de guias-queue y procesa (genera PDF, sube a S3, guarda en Oracle)
 *   4. Si ocurre cualquier error en el paso 3, el mensaje se reenvía automáticamente
 *      a guias-dlq via el Dead Letter Exchange (guias-dlx)
 */
@Configuration
public class RabbitMQConfig {

    @Value("${rabbitmq.exchange}")
    private String exchange;

    @Value("${rabbitmq.queue.tickets}")
    private String queueTickets;

    @Value("${rabbitmq.queue.dlq}")
    private String queueDlq;

    @Value("${rabbitmq.routing-key.tickets}")
    private String routingKeyTickets;

    @Value("${rabbitmq.routing-key.dlq}")
    private String routingKeyDlq;

    // =========================================================================
    // Dead Letter Exchange y DLQ (se crean primero porque la cola principal
    // los referencia en sus argumentos)
    // =========================================================================

    /**
     * Exchange del Dead Letter Queue.
     * Recibe los mensajes que fallaron en la cola principal.
     */
    @Bean
    public DirectExchange deadLetterExchange() {
        return new DirectExchange("guias-dlx");
    }

    /**
     * Dead Letter Queue: almacena todos los mensajes que fallaron
     * durante el procesamiento en la cola principal.
     */
    @Bean
    public Queue deadLetterQueue() {
        return QueueBuilder
                .durable(queueDlq)
                .build();
    }

    /**
     * Binding entre el DLX y la DLQ con la routing key de error.
     */
    @Bean
    public Binding deadLetterBinding() {
        return BindingBuilder
                .bind(deadLetterQueue())
                .to(deadLetterExchange())
                .with(routingKeyDlq);
    }

    // =========================================================================
    // Exchange principal y Cola de Tickets
    // =========================================================================

    /**
     * Exchange principal. El MS Productor publica aquí.
     */
    @Bean
    public DirectExchange guiasExchange() {
        return new DirectExchange(exchange);
    }

    /**
     * Cola principal de tickets de guías.
     * Configurada con DLX: si un mensaje falla (excepción no recuperable
     * en el consumidor), RabbitMQ lo reenvía automáticamente a guias-dlx,
     * que lo enruta a guias-dlq.
     */
    @Bean
    public Queue guiasQueue() {
        return QueueBuilder
                .durable(queueTickets)
                .withArgument("x-dead-letter-exchange", "guias-dlx")
                .withArgument("x-dead-letter-routing-key", routingKeyDlq)
                .build();
    }

    /**
     * Binding entre el exchange principal y la cola de tickets.
     */
    @Bean
    public Binding guiasBinding() {
        return BindingBuilder
                .bind(guiasQueue())
                .to(guiasExchange())
                .with(routingKeyTickets);
    }

    // =========================================================================
    // Configuracion del template y converter (mensajes en formato JSON)
    // =========================================================================

    /**
     * Convierte los objetos Java a JSON antes de publicarlos en la cola,
     * y de JSON a Java cuando se consumen.
     */
    @Bean
    public MessageConverter jsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }
    /**
     * Configura el contenedor del listener para que cuando el consumidor
     * lance una excepcion, el mensaje se rechace SIN reencolar,
     * activando el mecanismo de Dead Letter Queue automaticamente.
     */
    @Bean
    public org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory
    rabbitListenerContainerFactory(
            ConnectionFactory connectionFactory,
            MessageConverter jsonMessageConverter) {

        org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory factory =
                new org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory();
        factory.setConnectionFactory(connectionFactory);
        factory.setMessageConverter(jsonMessageConverter);
        factory.setDefaultRequeueRejected(false); // NO reencolar si falla -> va a DLQ
        factory.setAcknowledgeMode(
                org.springframework.amqp.core.AcknowledgeMode.AUTO);
        return factory;
    }
    /**
     * Template con el converter JSON configurado.
     * Es el bean que usa el MS Productor para publicar mensajes.
     */
    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate rabbitTemplate = new RabbitTemplate(connectionFactory);
        rabbitTemplate.setMessageConverter(jsonMessageConverter());
        return rabbitTemplate;
    }
}
