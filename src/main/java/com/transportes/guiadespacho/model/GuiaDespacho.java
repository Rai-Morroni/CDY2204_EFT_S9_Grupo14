package com.transportes.guiadespacho.model;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Representa una Guia de Despacho del sistema de gestion de pedidos.
 *
 * Esta clase se serializa como "metadata.json" y se guarda en AWS S3 junto
 * al PDF generado (guia.pdf), bajo la misma carpeta logica del id de la guia.
 * No se usa base de datos: S3 es la unica fuente de verdad de los datos,
 * tal como exige la pauta de evaluacion ("Configura el almacenamiento en
 * AWS S3 para la gestion de datos").
 */
public class GuiaDespacho {

    private String id;
    private String numeroPedido;
    private String transportista;

    @JsonFormat(pattern = "yyyy-MM-dd")
    private LocalDate fecha;

    private String destinatario;
    private String direccionEntrega;
    private String detallePedido;
    private String estado; // GENERADA, ACTUALIZADA

    private LocalDateTime fechaCreacion;
    private LocalDateTime fechaActualizacion;

    public GuiaDespacho() {
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getNumeroPedido() {
        return numeroPedido;
    }

    public void setNumeroPedido(String numeroPedido) {
        this.numeroPedido = numeroPedido;
    }

    public String getTransportista() {
        return transportista;
    }

    public void setTransportista(String transportista) {
        this.transportista = transportista;
    }

    public LocalDate getFecha() {
        return fecha;
    }

    public void setFecha(LocalDate fecha) {
        this.fecha = fecha;
    }

    public String getDestinatario() {
        return destinatario;
    }

    public void setDestinatario(String destinatario) {
        this.destinatario = destinatario;
    }

    public String getDireccionEntrega() {
        return direccionEntrega;
    }

    public void setDireccionEntrega(String direccionEntrega) {
        this.direccionEntrega = direccionEntrega;
    }

    public String getDetallePedido() {
        return detallePedido;
    }

    public void setDetallePedido(String detallePedido) {
        this.detallePedido = detallePedido;
    }

    public String getEstado() {
        return estado;
    }

    public void setEstado(String estado) {
        this.estado = estado;
    }

    public LocalDateTime getFechaCreacion() {
        return fechaCreacion;
    }

    public void setFechaCreacion(LocalDateTime fechaCreacion) {
        this.fechaCreacion = fechaCreacion;
    }

    public LocalDateTime getFechaActualizacion() {
        return fechaActualizacion;
    }

    public void setFechaActualizacion(LocalDateTime fechaActualizacion) {
        this.fechaActualizacion = fechaActualizacion;
    }
}
