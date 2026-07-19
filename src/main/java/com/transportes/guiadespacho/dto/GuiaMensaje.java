package com.transportes.guiadespacho.dto;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.io.Serializable;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Mensaje que viaja por la cola RabbitMQ (guias-queue).
 *
 * El MS Productor serializa este objeto a JSON y lo publica en la cola.
 * El MS Consumidor lo deserializa y lo procesa (genera PDF, sube a S3,
 * guarda en Oracle Cloud).
 *
 * Implementa Serializable como buena practica para mensajes de cola.
 */
public class GuiaMensaje implements Serializable {

    private String id;
    private String numeroPedido;
    private String transportista;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String destinatario;
    private String direccionEntrega;
    private String detallePedido;
    private String operacion; // CREAR, ACTUALIZAR, ELIMINAR
    private LocalDateTime fechaPublicacion;

    public GuiaMensaje() {
    }

    public GuiaMensaje(String id, String numeroPedido, String transportista,
                       LocalDate fecha, String destinatario, String direccionEntrega,
                       String detallePedido, String operacion) {
        this.id = id;
        this.numeroPedido = numeroPedido;
        this.transportista = transportista;
        this.fecha = fecha;
        this.destinatario = destinatario;
        this.direccionEntrega = direccionEntrega;
        this.detallePedido = detallePedido;
        this.operacion = operacion;
        this.fechaPublicacion = LocalDateTime.now();
    }

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getNumeroPedido() { return numeroPedido; }
    public void setNumeroPedido(String numeroPedido) { this.numeroPedido = numeroPedido; }

    public String getTransportista() { return transportista; }
    public void setTransportista(String transportista) { this.transportista = transportista; }

    public LocalDate getFecha() { return fecha; }
    public void setFecha(LocalDate fecha) { this.fecha = fecha; }

    public String getDestinatario() { return destinatario; }
    public void setDestinatario(String destinatario) { this.destinatario = destinatario; }

    public String getDireccionEntrega() { return direccionEntrega; }
    public void setDireccionEntrega(String direccionEntrega) { this.direccionEntrega = direccionEntrega; }

    public String getDetallePedido() { return detallePedido; }
    public void setDetallePedido(String detallePedido) { this.detallePedido = detallePedido; }

    public String getOperacion() { return operacion; }
    public void setOperacion(String operacion) { this.operacion = operacion; }

    public LocalDateTime getFechaPublicacion() { return fechaPublicacion; }
    public void setFechaPublicacion(LocalDateTime fechaPublicacion) { this.fechaPublicacion = fechaPublicacion; }

    @Override
    public String toString() {
        return "GuiaMensaje{id='" + id + "', operacion='" + operacion +
               "', transportista='" + transportista + "', fecha=" + fecha + "}";
    }
}
