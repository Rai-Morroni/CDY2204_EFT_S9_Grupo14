package com.transportes.guiadespacho.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.transportes.guiadespacho.model.GuiaDespacho;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.*;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Encapsula toda la interaccion con AWS S3.
 *
 * Estructura de objetos usada en el bucket (sin base de datos, tal como
 * pide la pauta de evaluacion: S3 es la unica fuente de datos):
 *
 *   guias/{id}/metadata.json   -> datos de la guia (JSON)
 *   guias/{id}/guia.pdf        -> documento PDF generado
 */
@Service
public class S3StorageService {

    private static final String PREFIX = "guias/";

    private final S3Client s3Client;
    private final ObjectMapper objectMapper;

    @Value("${aws.s3.bucket-name}")
    private String bucketName;

    public S3StorageService(S3Client s3Client) {
        this.s3Client = s3Client;
        this.objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    }

    public void guardarGuia(GuiaDespacho guia, byte[] pdfBytes) {
        guardarMetadata(guia);
        guardarPdf(guia.getId(), pdfBytes);
    }

    public void guardarMetadata(GuiaDespacho guia) {
        try {
            byte[] json = objectMapper.writeValueAsBytes(guia);
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucketName)
                            .key(metadataKey(guia.getId()))
                            .contentType("application/json")
                            .build(),
                    RequestBody.fromBytes(json)
            );
        } catch (IOException e) {
            throw new UncheckedIOException("Error serializando metadata de la guia", e);
        }
    }

    public void guardarPdf(String id, byte[] pdfBytes) {
        s3Client.putObject(
                PutObjectRequest.builder()
                        .bucket(bucketName)
                        .key(pdfKey(id))
                        .contentType("application/pdf")
                        .build(),
                RequestBody.fromBytes(pdfBytes)
        );
    }

    public GuiaDespacho obtenerMetadata(String id) {
        try (ResponseInputStream<GetObjectResponse> obj = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(metadataKey(id)).build())) {
            return objectMapper.readValue(obj, GuiaDespacho.class);
        } catch (NoSuchKeyException e) {
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException("Error leyendo metadata de la guia " + id, e);
        }
    }

    public byte[] obtenerPdf(String id) {
        try (ResponseInputStream<GetObjectResponse> obj = s3Client.getObject(
                GetObjectRequest.builder().bucket(bucketName).key(pdfKey(id)).build())) {
            return obj.readAllBytes();
        } catch (NoSuchKeyException e) {
            return null;
        } catch (IOException e) {
            throw new UncheckedIOException("Error leyendo PDF de la guia " + id, e);
        }
    }

    public void eliminarGuia(String id) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(metadataKey(id)).build());
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(bucketName).key(pdfKey(id)).build());
    }

    /**
     * Lista los metadatos de todas las guias existentes en el bucket.
     * Se usa como base para el endpoint de consulta por transportista/fecha.
     */
    public List<GuiaDespacho> listarTodasLasGuias() {
        List<GuiaDespacho> resultado = new ArrayList<>();
        ListObjectsV2Request request = ListObjectsV2Request.builder()
                .bucket(bucketName)
                .prefix(PREFIX)
                .build();

        ListObjectsV2Response response;
        do {
            response = s3Client.listObjectsV2(request);
            for (S3Object obj : response.contents()) {
                if (obj.key().endsWith("/metadata.json")) {
                    String id = extraerIdDesdeKey(obj.key());
                    GuiaDespacho guia = obtenerMetadata(id);
                    if (guia != null) {
                        resultado.add(guia);
                    }
                }
            }
            request = request.toBuilder().continuationToken(response.nextContinuationToken()).build();
        } while (Boolean.TRUE.equals(response.isTruncated()));

        return resultado;
    }

    private String extraerIdDesdeKey(String key) {
        // guias/{id}/metadata.json -> {id}
        String sinPrefijo = key.substring(PREFIX.length());
        return sinPrefijo.substring(0, sinPrefijo.indexOf('/'));
    }

    private String metadataKey(String id) {
        return PREFIX + id + "/metadata.json";
    }

    private String pdfKey(String id) {
        return PREFIX + id + "/guia.pdf";
    }
}
