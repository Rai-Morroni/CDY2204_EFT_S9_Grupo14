package com.transportes.guiadespacho.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.transportes.guiadespacho.model.GuiaDespacho;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Datos que se devuelven al cliente. No expone detalles internos de
 * almacenamiento (claves de S3, etc.).
 */
public class GuiaDespachoResponse {

    private String id;
    private String numeroPedido;
    private String transportista;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String destinatario;
    private String direccionEntrega;
    private String detallePedido;
    private String estado;
    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public static GuiaDespachoResponse from(GuiaDespacho guia) {
        GuiaDespachoResponse r = new GuiaDespachoResponse();
        r.id = guia.getId();
        r.numeroPedido = guia.getNumeroPedido();
        r.transportista = guia.getTransportista();
        r.fecha = guia.getFecha();
        r.destinatario = guia.getDestinatario();
        r.direccionEntrega = guia.getDireccionEntrega();
        r.detallePedido = guia.getDetallePedido();
        r.estado = guia.getEstado();
        r.fechaCreacion = guia.getFechaCreacion();
        r.fechaActualizacion = guia.getFechaActualizacion();
        return r;
    }

    public String getId() {
        return id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public String getTransportista() {
        return transportista;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public String getDetallePedido() {
        return detallePedido;
    }

    public String getEstado() {
        return estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }
}
