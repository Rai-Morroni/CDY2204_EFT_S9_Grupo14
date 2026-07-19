package com.transportes.guiadespacho;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Sistema de Gestion de Pedidos y Generacion de Guias de Despacho.
 *
 * CDY2204 - Desarrollo Cloud Native - Exp 2 - Semana 6.
 *
 * Este backend se securitiza con Spring Security validando JWT emitidos por
 * Azure AD B2C (IDaaS) y persiste tanto el documento PDF de cada guia como
 * sus metadatos directamente en AWS S3.
 */
@SpringBootApplication
public class GuiaDespachoApplication {

    public static void main(String[] args) {
        SpringApplication.run(GuiaDespachoApplication.class, args);
    }
}
