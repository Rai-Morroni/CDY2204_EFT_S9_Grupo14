package com.transportes.guiadespacho.exception;

public class GuiaNotFoundException extends RuntimeException {

    public GuiaNotFoundException(String id) {
        super("No se encontro la guia de despacho con id: " + id);
    }
}
