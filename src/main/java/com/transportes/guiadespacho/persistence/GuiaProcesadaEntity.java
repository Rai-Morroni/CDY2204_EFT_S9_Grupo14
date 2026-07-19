package com.transportes.guiadespacho.persistence;

import jakarta.persistence.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * Entidad JPA que mapea la tabla GUIAS_PROCESADAS en Oracle Cloud.
 *
 * Esta tabla almacena las guias de despacho procesadas por el MS Consumidor
 * luego de leer el mensaje de la cola RabbitMQ (guias-queue), generar el
 * PDF y subirlo a S3.
 *
 * La tabla fue creada en Oracle Cloud SQL Developer con:
 *   CREATE TABLE GUIAS_PROCESADAS (
 *       ID VARCHAR2(36) PRIMARY KEY,
 *       NUMERO_PEDIDO VARCHAR2(100) NOT NULL,
 *       TRANSPORTISTA VARCHAR2(200) NOT NULL,
 *       FECHA DATE NOT NULL,
 *       DESTINATARIO VARCHAR2(200) NOT NULL,
 *       DIRECCION_ENTREGA VARCHAR2(500) NOT NULL,
 *       DETALLE_PEDIDO VARCHAR2(1000),
 *       ESTADO VARCHAR2(50) DEFAULT 'PROCESADA',
 *       RUTA_S3 VARCHAR2(500),
 *       FECHA_PROCESAMIENTO TIMESTAMP DEFAULT SYSTIMESTAMP,
 *       MENSAJE_ERROR VARCHAR2(1000)
 *   );
 */
@Entity
@Table(name = "GUIAS_PROCESADAS")
public class GuiaProcesadaEntity {

    @Id
    @Column(name = "ID", length = 36, nullable = false)
    private String id;

    @Column(name = "NUMERO_PEDIDO", length = 100, nullable = false)
    private String numeroPedido;

    @Column(name = "TRANSPORTISTA", length = 200, nullable = false)
    private String transportista;

    @Column(name = "FECHA", nullable = false)
    private LocalDate fecha;

    @Column(name = "DESTINATARIO", length = 200, nullable = false)
    private String destinatario;

    @Column(name = "DIRECCION_ENTREGA", length = 500, nullable = false)
    private String direccionEntrega;

    @Column(name = "DETALLE_PEDIDO", length = 1000)
    private String detallePedido;

    @Column(name = "ESTADO", length = 50)
    private String estado;

    @Column(name = "RUTA_S3", length = 500)
    private String rutaS3;

    @Column(name = "FECHA_PROCESAMIENTO")
    private LocalDateTime fechaProcesamiento;

    @Column(name = "MENSAJE_ERROR", length = 1000)
    private String mensajeError;

    public GuiaProcesadaEntity() {
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

    public String getEstado() { return estado; }
    public void setEstado(String estado) { this.estado = estado; }

    public String getRutaS3() { return rutaS3; }
    public void setRutaS3(String rutaS3) { this.rutaS3 = rutaS3; }

    public LocalDateTime getFechaProcesamiento() { return fechaProcesamiento; }
    public void setFechaProcesamiento(LocalDateTime fechaProcesamiento) { this.fechaProcesamiento = fechaProcesamiento; }

    public String getMensajeError() { return mensajeError; }
    public void setMensajeError(String mensajeError) { this.mensajeError = mensajeError; }
}
