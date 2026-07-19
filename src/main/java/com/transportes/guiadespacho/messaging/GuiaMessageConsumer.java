package com.transportes.guiadespacho.messaging;

import com.transportes.guiadespacho.dto.GuiaMensaje;
import com.transportes.guiadespacho.model.GuiaDespacho;
import com.transportes.guiadespacho.persistence.GuiaProcesadaEntity;
import com.transportes.guiadespacho.persistence.GuiaProcesadaRepository;
import com.transportes.guiadespacho.service.PdfGeneratorService;
import com.transportes.guiadespacho.service.S3StorageService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.support.ListenerExecutionFailedException;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

/**
 * MS Consumidor de mensajes RabbitMQ.
 *
 * Escucha la cola guias-queue y por cada mensaje:
 *   1. Genera el PDF de la guia de despacho (PdfGeneratorService).
 *   2. Sube el PDF a Amazon S3 (S3StorageService).
 *   3. Guarda los datos de la guia en Oracle Cloud (GuiaProcesadaRepository).
 *
 * Si cualquiera de estos 3 pasos falla con una excepcion, Spring AMQP
 * marca el mensaje como "nack" y RabbitMQ lo reenvía automaticamente
 * a la DLQ (guias-dlq) via el Dead Letter Exchange configurado
 * en la cola principal (RabbitMQConfig).
 *
 * Es un componente DISTINTO al MS Productor (GuiaMessageProducer),
 * cumpliendo el requisito de la actividad: "productores y consumidores
 * de mensajes deben ser componentes distintos".
 */
@Component
public class GuiaMessageConsumer {

    private static final Logger log = LoggerFactory.getLogger(GuiaMessageConsumer.class);

    private final PdfGeneratorService pdfGeneratorService;
    private final S3StorageService s3StorageService;
    private final GuiaProcesadaRepository guiaProcesadaRepository;

    public GuiaMessageConsumer(PdfGeneratorService pdfGeneratorService,
                               S3StorageService s3StorageService,
                               GuiaProcesadaRepository guiaProcesadaRepository) {
        this.pdfGeneratorService = pdfGeneratorService;
        this.s3StorageService = s3StorageService;
        this.guiaProcesadaRepository = guiaProcesadaRepository;
    }

    /**
     * Listener principal de la cola guias-queue.
     *
     * @param mensaje el mensaje deserializado desde JSON por Jackson2JsonMessageConverter
     *
     * IMPORTANTE: si este metodo lanza cualquier excepcion, Spring AMQP
     * hace "nack" del mensaje y RabbitMQ lo envia a la DLQ automaticamente.
     * NO se hace try/catch aqui para que ese mecanismo funcione correctamente.
     */
    @RabbitListener(queues = "${rabbitmq.queue.tickets}")
    public void consumir(GuiaMensaje mensaje) {
        log.info("[CONSUMIDOR] Mensaje recibido de la cola | operacion={} | id={}",
                mensaje.getOperacion(), mensaje.getId());

        // Solo procesamos operaciones de creacion y actualizacion
        // (ELIMINAR no requiere generar PDF ni subir a S3)
        if ("ELIMINAR".equals(mensaje.getOperacion())) {
            log.info("[CONSUMIDOR] Operacion ELIMINAR recibida, eliminando de Oracle | id={}",
                    mensaje.getId());
            guiaProcesadaRepository.deleteById(mensaje.getId());
            log.info("[CONSUMIDOR] Registro eliminado de Oracle | id={}", mensaje.getId());
            return;
        }

        // Paso 1: Construir el modelo de dominio desde el mensaje
        GuiaDespacho guia = construirGuia(mensaje);

        // Paso 2: Generar el PDF
        // Si falla aqui -> excepcion -> DLQ
        log.info("[CONSUMIDOR] Generando PDF | id={}", mensaje.getId());
        byte[] pdfBytes = pdfGeneratorService.generar(guia);
        log.info("[CONSUMIDOR] PDF generado exitosamente | id={}", mensaje.getId());

        // Paso 3: Subir a S3 (PDF + metadata.json)
        // Si falla aqui -> excepcion -> DLQ
        log.info("[CONSUMIDOR] Subiendo a S3 | id={}", mensaje.getId());
        s3StorageService.guardarGuia(guia, pdfBytes);
        String rutaS3 = "guias/" + mensaje.getId() + "/guia.pdf";
        log.info("[CONSUMIDOR] Subida a S3 exitosa | ruta={}", rutaS3);

        // Paso 4: Guardar en Oracle Cloud
        // Si falla aqui -> excepcion -> DLQ
        log.info("[CONSUMIDOR] Guardando en Oracle Cloud | id={}", mensaje.getId());
        GuiaProcesadaEntity entity = construirEntity(mensaje, rutaS3);
        guiaProcesadaRepository.save(entity);
        log.info("[CONSUMIDOR] Guardado en Oracle Cloud exitoso | id={}", mensaje.getId());

        log.info("[CONSUMIDOR] Mensaje procesado completamente | id={}", mensaje.getId());
    }

    private GuiaDespacho construirGuia(GuiaMensaje mensaje) {
        GuiaDespacho guia = new GuiaDespacho();
        guia.setId(mensaje.getId());
        guia.setNumeroPedido(mensaje.getNumeroPedido());
        guia.setTransportista(mensaje.getTransportista());
        guia.setFecha(mensaje.getFecha());
        guia.setDestinatario(mensaje.getDestinatario());
        guia.setDireccionEntrega(mensaje.getDireccionEntrega());
        guia.setDetallePedido(mensaje.getDetallePedido());
        guia.setEstado("CREAR".equals(mensaje.getOperacion()) ? "GENERADA" : "ACTUALIZADA");
        guia.setFechaCreacion(LocalDateTime.now());
        guia.setFechaActualizacion(LocalDateTime.now());
        return guia;
    }

    private GuiaProcesadaEntity construirEntity(GuiaMensaje mensaje, String rutaS3) {
        GuiaProcesadaEntity entity = new GuiaProcesadaEntity();
        entity.setId(mensaje.getId());
        entity.setNumeroPedido(mensaje.getNumeroPedido());
        entity.setTransportista(mensaje.getTransportista());
        entity.setFecha(mensaje.getFecha());
        entity.setDestinatario(mensaje.getDestinatario());
        entity.setDireccionEntrega(mensaje.getDireccionEntrega());
        entity.setDetallePedido(mensaje.getDetallePedido());
        entity.setEstado("CREAR".equals(mensaje.getOperacion()) ? "GENERADA" : "ACTUALIZADA");
        entity.setRutaS3(rutaS3);
        entity.setFechaProcesamiento(LocalDateTime.now());
        return entity;
    }
}
