package com.transportes.guiadespacho.controller;

import com.transportes.guiadespacho.dto.GuiaDespachoRequest;
import com.transportes.guiadespacho.dto.GuiaDespachoResponse;
import com.transportes.guiadespacho.service.GuiaDespachoService;
import jakarta.validation.Valid;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

/**
 * Endpoints REST del Sistema de Gestion de Pedidos y Generacion de Guias
 * de Despacho. Todos estan registrados y securitizados a traves de AWS
 * API Gateway, y protegidos localmente con Spring Security + JWT de
 * Azure AD B2C (ver SecurityConfig).
 */
@RestController
@RequestMapping("/api/guias")
public class GuiaDespachoController {

    private final GuiaDespachoService guiaDespachoService;

    public GuiaDespachoController(GuiaDespachoService guiaDespachoService) {
        this.guiaDespachoService = guiaDespachoService;
    }

    /** Crear guias de despacho (genera el PDF y lo sube a S3). Rol: GESTION. */
    @PostMapping
    public ResponseEntity<GuiaDespachoResponse> crear(@Valid @RequestBody GuiaDespachoRequest request) {
        var guia = guiaDespachoService.crearGuia(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(GuiaDespachoResponse.from(guia));
    }

    /** Consultar guias por transportista y fecha. Rol: GESTION. */
    @GetMapping
    public ResponseEntity<List<GuiaDespachoResponse>> consultar(
            @RequestParam(required = false) String transportista,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate fecha) {
        var guias = guiaDespachoService.consultarGuias(transportista, fecha)
                .stream().map(GuiaDespachoResponse::from).toList();
        return ResponseEntity.ok(guias);
    }

    /** Consultar una guia puntual por id. Rol: GESTION. */
    @GetMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse> obtener(@PathVariable String id) {
        return ResponseEntity.ok(GuiaDespachoResponse.from(guiaDespachoService.obtenerGuia(id)));
    }

    /** Descargar guias con validacion de permisos. Rol: SOLO DESCARGA. */
    @GetMapping("/{id}/descargar")
    public ResponseEntity<byte[]> descargar(@PathVariable String id) {
        byte[] pdf = guiaDespachoService.descargarPdf(id);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"guia-" + id + ".pdf\"")
                .body(pdf);
    }

    /** Modificar o actualizar guias. Rol: GESTION. */
    @PutMapping("/{id}")
    public ResponseEntity<GuiaDespachoResponse> actualizar(@PathVariable String id,
                                                             @Valid @RequestBody GuiaDespachoRequest request) {
        var guia = guiaDespachoService.actualizarGuia(id, request);
        return ResponseEntity.ok(GuiaDespachoResponse.from(guia));
    }

    /** Eliminar guias especificas. Rol: GESTION. */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> eliminar(@PathVariable String id) {
        guiaDespachoService.eliminarGuia(id);
        return ResponseEntity.noContent().build();
    }
}
