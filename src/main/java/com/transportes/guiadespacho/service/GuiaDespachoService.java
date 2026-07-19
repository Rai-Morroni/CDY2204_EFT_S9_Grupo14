package com.transportes.guiadespacho.service;

import com.transportes.guiadespacho.dto.GuiaDespachoRequest;
import com.transportes.guiadespacho.dto.GuiaMensaje;
import com.transportes.guiadespacho.exception.GuiaNotFoundException;
import com.transportes.guiadespacho.messaging.GuiaMessageProducer;
import com.transportes.guiadespacho.model.GuiaDespacho;
import com.transportes.guiadespacho.persistence.GuiaProcesadaEntity;
import com.transportes.guiadespacho.persistence.GuiaProcesadaRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

/**
 * Servicio principal de negocio de guias de despacho.
 *
 * Cambio respecto a la Exp 2:
 *   - Antes: crear/actualizar procesaba directamente (generaba PDF y subia a S3).
 *   - Ahora: crear/actualizar publica un mensaje en RabbitMQ (via GuiaMessageProducer)
 *     y el MS Consumidor (GuiaMessageConsumer) hace el procesamiento real.
 *
 * Las consultas y descargas siguen leyendo de S3 y Oracle (sin cambios).
 */
@Service
public class GuiaDespachoService {

    private final GuiaMessageProducer guiaMessageProducer;
    private final GuiaProcesadaRepository guiaProcesadaRepository;
    private final S3StorageService s3StorageService;

    public GuiaDespachoService(GuiaMessageProducer guiaMessageProducer,
                               GuiaProcesadaRepository guiaProcesadaRepository,
                               S3StorageService s3StorageService) {
        this.guiaMessageProducer = guiaMessageProducer;
        this.guiaProcesadaRepository = guiaProcesadaRepository;
        this.s3StorageService = s3StorageService;
    }

    /**
     * Crea una guia: genera el ID y publica el mensaje en la cola.
     * El procesamiento real (PDF + S3 + Oracle) lo hace el MS Consumidor.
     */
    public GuiaDespacho crearGuia(GuiaDespachoRequest request) {
        String id = UUID.randomUUID().toString();

        GuiaMensaje mensaje = new GuiaMensaje(
                id,
                request.getNumeroPedido(),
                request.getTransportista(),
                request.getFecha(),
                request.getDestinatario(),
                request.getDireccionEntrega(),
                request.getDetallePedido(),
                "CREAR"
        );

        guiaMessageProducer.publicar(mensaje);

        // Devolvemos una respuesta inmediata con el ID asignado
        // El procesamiento asíncrono continúa en el MS Consumidor
        GuiaDespacho guia = new GuiaDespacho();
        guia.setId(id);
        guia.setNumeroPedido(request.getNumeroPedido());
        guia.setTransportista(request.getTransportista());
        guia.setFecha(request.getFecha());
        guia.setDestinatario(request.getDestinatario());
        guia.setDireccionEntrega(request.getDireccionEntrega());
        guia.setDetallePedido(request.getDetallePedido());
        guia.setEstado("EN_COLA");
        guia.setFechaCreacion(LocalDateTime.now());
        guia.setFechaActualizacion(LocalDateTime.now());
        return guia;
    }

    /**
     * Obtiene una guia ya procesada desde Oracle Cloud.
     */
    public GuiaDespacho obtenerGuia(String id) {
        GuiaProcesadaEntity entity = guiaProcesadaRepository.findById(id)
                .orElseThrow(() -> new GuiaNotFoundException(id));
        return mapEntityToModel(entity);
    }

    /**
     * Descarga el PDF de la guia desde S3.
     */
    public byte[] descargarPdf(String id) {
        // Verificamos que exista en Oracle antes de buscar en S3
        guiaProcesadaRepository.findById(id)
                .orElseThrow(() -> new GuiaNotFoundException(id));

        byte[] pdf = s3StorageService.obtenerPdf(id);
        if (pdf == null) {
            throw new GuiaNotFoundException(id);
        }
        return pdf;
    }

    /**
     * Actualiza una guia: verifica que exista y publica mensaje de actualizacion.
     */
    public GuiaDespacho actualizarGuia(String id, GuiaDespachoRequest request) {
        // Verificamos que exista antes de publicar
        guiaProcesadaRepository.findById(id)
                .orElseThrow(() -> new GuiaNotFoundException(id));

        GuiaMensaje mensaje = new GuiaMensaje(
                id,
                request.getNumeroPedido(),
                request.getTransportista(),
                request.getFecha(),
                request.getDestinatario(),
                request.getDireccionEntrega(),
                request.getDetallePedido(),
                "ACTUALIZAR"
        );

        guiaMessageProducer.publicar(mensaje);

        GuiaDespacho guia = new GuiaDespacho();
        guia.setId(id);
        guia.setNumeroPedido(request.getNumeroPedido());
        guia.setTransportista(request.getTransportista());
        guia.setFecha(request.getFecha());
        guia.setDestinatario(request.getDestinatario());
        guia.setDireccionEntrega(request.getDireccionEntrega());
        guia.setDetallePedido(request.getDetallePedido());
        guia.setEstado("EN_COLA");
        guia.setFechaActualizacion(LocalDateTime.now());
        return guia;
    }

    /**
     * Elimina una guia: verifica que exista, publica mensaje de eliminacion
     * y elimina de S3.
     */
    public void eliminarGuia(String id) {
        guiaProcesadaRepository.findById(id)
                .orElseThrow(() -> new GuiaNotFoundException(id));

        GuiaMensaje mensaje = new GuiaMensaje(
                id, null, null, null, null, null, null, "ELIMINAR"
        );
        guiaMessageProducer.publicar(mensaje);
        s3StorageService.eliminarGuia(id);
    }

    /**
     * Consulta guias procesadas desde Oracle Cloud,
     * filtrando por transportista y/o fecha.
     */
    public List<GuiaDespacho> consultarGuias(String transportista, LocalDate fecha) {
        List<GuiaProcesadaEntity> entities;

        if (transportista != null && !transportista.isBlank() && fecha != null) {
            entities = guiaProcesadaRepository
                    .findByTransportistaIgnoreCaseAndFecha(transportista, fecha);
        } else if (transportista != null && !transportista.isBlank()) {
            entities = guiaProcesadaRepository
                    .findByTransportistaIgnoreCase(transportista);
        } else if (fecha != null) {
            entities = guiaProcesadaRepository.findByFecha(fecha);
        } else {
            entities = guiaProcesadaRepository.findAll();
        }

        return entities.stream().map(this::mapEntityToModel).toList();
    }

    private GuiaDespacho mapEntityToModel(GuiaProcesadaEntity entity) {
        GuiaDespacho guia = new GuiaDespacho();
        guia.setId(entity.getId());
        guia.setNumeroPedido(entity.getNumeroPedido());
        guia.setTransportista(entity.getTransportista());
        guia.setFecha(entity.getFecha());
        guia.setDestinatario(entity.getDestinatario());
        guia.setDireccionEntrega(entity.getDireccionEntrega());
        guia.setDetallePedido(entity.getDetallePedido());
        guia.setEstado(entity.getEstado());
        guia.setFechaActualizacion(entity.getFechaProcesamiento());
        return guia;
    }
}
