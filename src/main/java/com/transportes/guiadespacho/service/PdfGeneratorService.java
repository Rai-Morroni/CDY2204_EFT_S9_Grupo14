package com.transportes.guiadespacho.service;

import com.transportes.guiadespacho.model.GuiaDespacho;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;

/**
 * Genera el PDF de la "Guia de Despacho" a partir de los datos del pedido.
 * El resultado en bytes se sube a S3 desde el servicio principal.
 */
@Service
public class PdfGeneratorService {

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");

    public byte[] generar(GuiaDespacho guia) {
        try (PDDocument document = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            document.addPage(page);

            try (PDPageContentStream content = new PDPageContentStream(document, page)) {
                float margenIzq = 50;
                float y = 770;
                float interlineado = 22;

                var fontBold = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
                var fontNormal = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

                content.beginText();
                content.setFont(fontBold, 18);
                content.newLineAtOffset(margenIzq, y);
                content.showText("GUIA DE DESPACHO");
                content.endText();
                y -= interlineado * 1.5f;

                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "N° Guia: ", guia.getId());
                y -= interlineado;
                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "N° Pedido: ", guia.getNumeroPedido());
                y -= interlineado;
                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "Transportista: ", guia.getTransportista());
                y -= interlineado;
                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "Fecha: ",
                        guia.getFecha() != null ? guia.getFecha().format(FORMATO_FECHA) : "");
                y -= interlineado;
                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "Destinatario: ", guia.getDestinatario());
                y -= interlineado;
                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "Direccion de entrega: ", guia.getDireccionEntrega());
                y -= interlineado;
                escribirLinea(content, fontBold, fontNormal, margenIzq, y, "Estado: ", guia.getEstado());
                y -= interlineado * 1.5f;

                content.beginText();
                content.setFont(fontBold, 12);
                content.newLineAtOffset(margenIzq, y);
                content.showText("Detalle del pedido:");
                content.endText();
                y -= interlineado;

                content.beginText();
                content.setFont(fontNormal, 11);
                content.newLineAtOffset(margenIzq, y);
                String detalle = guia.getDetallePedido() != null ? guia.getDetallePedido() : "-";
                for (String linea : dividirEnLineas(detalle, 90)) {
                    content.showText(linea);
                    content.newLineAtOffset(0, -interlineado);
                }
                content.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            document.save(out);
            return out.toByteArray();

        } catch (IOException e) {
            throw new RuntimeException("Error generando el PDF de la guia de despacho", e);
        }
    }

    private void escribirLinea(PDPageContentStream content, PDType1Font fontBold, PDType1Font fontNormal,
                                float x, float y, String etiqueta, String valor) throws IOException {
        content.beginText();
        content.setFont(fontBold, 12);
        content.newLineAtOffset(x, y);
        content.showText(etiqueta);
        content.endText();

        content.beginText();
        content.setFont(fontNormal, 12);
        content.newLineAtOffset(x + 150, y);
        content.showText(valor != null ? valor : "-");
        content.endText();
    }

    private String[] dividirEnLineas(String texto, int maxCaracteres) {
        if (texto.length() <= maxCaracteres) {
            return new String[]{texto};
        }
        java.util.List<String> lineas = new java.util.ArrayList<>();
        StringBuilder actual = new StringBuilder();
        for (String palabra : texto.split(" ")) {
            if (actual.length() + palabra.length() + 1 > maxCaracteres) {
                lineas.add(actual.toString());
                actual = new StringBuilder();
            }
            if (!actual.isEmpty()) {
                actual.append(" ");
            }
            actual.append(palabra);
        }
        if (!actual.isEmpty()) {
            lineas.add(actual.toString());
        }
        return lineas.toArray(new String[0]);
    }
}
