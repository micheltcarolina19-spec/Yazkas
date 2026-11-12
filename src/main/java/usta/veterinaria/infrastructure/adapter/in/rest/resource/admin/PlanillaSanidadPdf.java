package usta.veterinaria.infrastructure.adapter.in.rest.resource.admin;


import com.lowagie.text.Chunk;
import com.lowagie.text.Document;
import com.lowagie.text.Element;
import com.lowagie.text.Font;
import com.lowagie.text.Image;
import com.lowagie.text.PageSize;
import com.lowagie.text.Paragraph;
import com.lowagie.text.Phrase;
import com.lowagie.text.Rectangle;
import com.lowagie.text.pdf.ColumnText;
import com.lowagie.text.pdf.PdfContentByte;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfPageEventHelper;
import com.lowagie.text.pdf.PdfWriter;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin.PlanillaSanidadRestMapper;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Arma el PDF de la planilla de sanidad (limpieza y desinfección) con OpenPDF.
// Sale firmado por el responsable (imagen de firma + hash de integridad del contenido).
public final class PlanillaSanidadPdf {

    // Imágenes del proyecto dentro de src/main/resources (cambie la ruta si su carpeta es otra).
    // Si el archivo no existe el PDF se genera igual, solo sin la imagen.
    private static final String LOGO_RUTA = "META-INF/resources/img/usuario/logo.png";
    private static final String FIRMA_RUTA = "META-INF/resources/img/usuario/logo.png";

    private static final String MARCA = "YAZKAS";
    private static final String SUBMARCA = "Peluquería Canina";

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a", ES);

    // Colores del reporte (azul de la marca)
    private static final Color NAVY = new Color(0x0B, 0x4C, 0x8C);
    private static final Color AZUL_TITULO = new Color(0xCF, 0xE6, 0xFA);
    private static final Color AZUL_SUAVE = new Color(0xE8, 0xF3, 0xFD);
    private static final Color BORDE = new Color(0xB9, 0xD6, 0xF2);
    private static final Color TEXTO = new Color(0x1E, 0x2F, 0x4D);
    private static final Color VERDE = new Color(0x1F, 0x9D, 0x55);
    private static final Color ROJO = new Color(0xD6, 0x45, 0x45);

    private static final Font F_MARCA = new Font(Font.HELVETICA, 26, Font.BOLD, NAVY);
    private static final Font F_SUBMARCA = new Font(Font.HELVETICA, 11, Font.NORMAL, NAVY);
    private static final Font F_LEMA = new Font(Font.HELVETICA, 12, Font.ITALIC, NAVY);
    private static final Font F_TITULO = new Font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE);
    private static final Font F_SECCION = new Font(Font.HELVETICA, 13, Font.BOLD, NAVY);
    private static final Font F_ETIQUETA = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTO);
    private static final Font F_VALOR = new Font(Font.HELVETICA, 10, Font.BOLD, TEXTO);
    private static final Font F_COLUMNA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, NAVY);
    private static final Font F_CELDA = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTO);
    private static final Font F_FIRMA = new Font(Font.HELVETICA, 9, Font.BOLD, NAVY);
    private static final Font F_FIRMA_PEQ = new Font(Font.HELVETICA, 7, Font.NORMAL, TEXTO);

    private PlanillaSanidadPdf() {
    }

    public static byte[] generar(List<PlanillaSanidad> registros, LocalDate inicio, LocalDate fin,
                                 BigDecimal cumplimiento, String sede, String responsable) {
        List<PlanillaSanidad> filas = registros.stream()
                .sorted(Comparator.comparing(PlanillaSanidad::getFechaPlanilla)
                        .thenComparing(PlanillaSanidad::getId, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
        LocalDateTime firmadoEn = LocalDateTime.now(RelojColombia.reloj());
        String hash = calcularHash(filas, inicio, fin, sede, responsable, firmadoEn);

        Document document = new Document(PageSize.LETTER, 36, 36, 36, 64);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PiePagina());
            document.open();

            document.add(encabezado());
            document.add(titulo("Planilla de Sanidad"));
            document.add(infoRegistro(inicio, fin, sede, responsable, cumplimiento));
            document.add(tablaControl(filas));
            document.add(cierre(filas, cumplimiento, responsable, hash, firmadoEn));

            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la planilla de sanidad", e);
        }
        return output.toByteArray();
    }

    // Logo + marca a la izquierda, lema a la derecha
    private static PdfPTable encabezado() throws Exception {
        PdfPTable marca = new PdfPTable(new float[]{1f, 3.6f});
        marca.setWidthPercentage(100);
        Image logo = cargarImagen(LOGO_RUTA, 52);
        marca.addCell(logo != null ? celdaImagen(logo, Element.ALIGN_LEFT) : celdaVacia());

        PdfPCell texto = new PdfPCell();
        texto.setBorder(Rectangle.NO_BORDER);
        texto.setVerticalAlignment(Element.ALIGN_MIDDLE);
        texto.addElement(new Paragraph(MARCA, F_MARCA));
        texto.addElement(new Paragraph(SUBMARCA, F_SUBMARCA));
        marca.addCell(texto);

        PdfPTable table = new PdfPTable(new float[]{3f, 2f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(10);
        PdfPCell izquierda = new PdfPCell(marca);
        izquierda.setBorder(Rectangle.NO_BORDER);
        table.addCell(izquierda);

        PdfPCell lema = new PdfPCell(new Phrase("Cuidamos con amor", F_LEMA));
        lema.setBorder(Rectangle.NO_BORDER);
        lema.setHorizontalAlignment(Element.ALIGN_RIGHT);
        lema.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(lema);
        return table;
    }

    // Barra azul con el título
    private static PdfPTable titulo(String texto) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        PdfPCell cell = new PdfPCell(new Phrase(texto, F_TITULO));
        cell.setBackgroundColor(NAVY);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPadding(10);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cell);
        return table;
    }

    // Bloque "Información del Registro": periodo, sede, responsable y cumplimiento
    private static PdfPTable infoRegistro(LocalDate inicio, LocalDate fin, String sede, String responsable,
                                          BigDecimal cumplimiento) {
        PdfPTable table = new PdfPTable(new float[]{1.6f, 3f});
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);

        PdfPCell cabecera = new PdfPCell(new Phrase("Información del Registro", F_SECCION));
        cabecera.setColspan(2);
        cabecera.setBackgroundColor(AZUL_SUAVE);
        cabecera.setBorder(Rectangle.NO_BORDER);
        cabecera.setPadding(8);
        table.addCell(cabecera);

        filaInfo(table, "Periodo / Mes de control:", periodo(inicio, fin));
        filaInfo(table, "Sede / Sucursal:", sede);
        filaInfo(table, "Responsable:", responsable);
        filaInfo(table, "Cumplimiento:", cumplimiento.stripTrailingZeros().toPlainString() + " %");
        return table;
    }

    private static void filaInfo(PdfPTable table, String etiqueta, String valor) {
        table.addCell(celdaInfo(etiqueta, F_ETIQUETA));
        table.addCell(celdaInfo(valor, F_VALOR));
    }

    private static PdfPCell celdaInfo(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(AZUL_SUAVE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(8);
        cell.setPaddingBottom(5);
        return cell;
    }

    // Tabla "Control de Sanidad" (el título y los encabezados se repiten en cada página)
    private static PdfPTable tablaControl(List<PlanillaSanidad> filas) {
        PdfPTable table = new PdfPTable(new float[]{1.35f, 1.25f, 1.6f, 1.6f, 1.15f, 1.4f, 2.4f});
        table.setWidthPercentage(100);
        table.setHeaderRows(2);
        table.setSpacingAfter(14);

        PdfPCell seccion = new PdfPCell(new Phrase("Control de Sanidad", F_SECCION));
        seccion.setColspan(7);
        seccion.setBackgroundColor(AZUL_TITULO);
        seccion.setBorderColor(BORDE);
        seccion.setPadding(8);
        table.addCell(seccion);

        for (String columna : new String[]{"Fecha", "Día", "Área de Limpieza", "Herramienta",
                "Limpieza", "Desinfección", "Observaciones"}) {
            PdfPCell cell = new PdfPCell(new Phrase(columna, F_COLUMNA));
            cell.setBackgroundColor(AZUL_SUAVE);
            cell.setBorderColor(BORDE);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }

        if (filas.isEmpty()) {
            PdfPCell vacio = new PdfPCell(new Phrase("Sin registros en el periodo seleccionado.", F_CELDA));
            vacio.setColspan(7);
            vacio.setBorderColor(BORDE);
            vacio.setPadding(10);
            vacio.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(vacio);
            return table;
        }

        for (PlanillaSanidad fila : filas) {
            table.addCell(celdaDato(FECHA.format(fila.getFechaPlanilla()), Element.ALIGN_CENTER));
            table.addCell(celdaDato(PlanillaSanidadRestMapper.etiqueta(fila.getDiaSanidad()), Element.ALIGN_CENTER));
            table.addCell(celdaDato(PlanillaSanidadRestMapper.etiqueta(fila.getAreaLimpieza()), Element.ALIGN_CENTER));
            table.addCell(celdaDato(PlanillaSanidadRestMapper.etiqueta(fila.getHerramienta()), Element.ALIGN_CENTER));
            table.addCell(celdaEstado(Boolean.TRUE.equals(fila.getLimpiezaRealizada())));
            table.addCell(celdaEstado(Boolean.TRUE.equals(fila.getDesinfeccion())));
            table.addCell(celdaDato(fila.getObservaciones() == null ? "-" : fila.getObservaciones(),
                    Element.ALIGN_LEFT));
        }
        return table;
    }

    private static PdfPCell celdaDato(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, F_CELDA));
        cell.setBorderColor(BORDE);
        cell.setPadding(6);
        cell.setHorizontalAlignment(alineacion);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    // Marca de verificación (verde) o equis (roja) con el texto Sí / No
    private static PdfPCell celdaEstado(boolean cumplido) {
        Color color = cumplido ? VERDE : ROJO;
        Phrase frase = new Phrase();
        frase.add(new Chunk(cumplido ? "4" : "8", new Font(Font.ZAPFDINGBATS, 9, Font.NORMAL, color)));
        frase.add(new Chunk(cumplido ? "  Sí" : "  No", F_CELDA));
        PdfPCell cell = new PdfPCell(frase);
        cell.setBorderColor(BORDE);
        cell.setPadding(6);
        cell.setHorizontalAlignment(Element.ALIGN_CENTER);
        cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
        return cell;
    }

    // Observaciones generales a la izquierda, firma electrónica a la derecha
    private static PdfPTable cierre(List<PlanillaSanidad> filas, BigDecimal cumplimiento, String responsable,
                                    String hash, LocalDateTime firmadoEn) throws Exception {
        PdfPTable table = new PdfPTable(new float[]{3f, 2.2f});
        table.setWidthPercentage(100);
        table.setKeepTogether(true);

        PdfPCell observaciones = new PdfPCell();
        observaciones.setBackgroundColor(AZUL_SUAVE);
        observaciones.setBorderColor(BORDE);
        observaciones.setPadding(10);
        observaciones.addElement(new Paragraph("Observaciones Generales", F_SECCION));
        Paragraph resumen = new Paragraph(resumenGeneral(filas, cumplimiento), F_ETIQUETA);
        resumen.setSpacingBefore(4);
        observaciones.addElement(resumen);
        table.addCell(observaciones);

        PdfPTable firma = new PdfPTable(1);
        firma.setWidthPercentage(100);
        Image imagenFirma = cargarImagen(FIRMA_RUTA, 56);
        firma.addCell(imagenFirma != null ? celdaImagen(imagenFirma, Element.ALIGN_CENTER) : celdaVacia());

        PdfPCell datos = new PdfPCell();
        datos.setBorder(Rectangle.TOP);
        datos.setBorderColor(NAVY);
        datos.setPaddingTop(4);
        datos.addElement(centrado("Firma Electrónica del Responsable", F_FIRMA));
        datos.addElement(centrado(responsable, F_FIRMA_PEQ));
        datos.addElement(centrado("Hash: " + hash.substring(0, 32), F_FIRMA_PEQ));
        datos.addElement(centrado("Fecha y Hora: " + FECHA_HORA.format(firmadoEn), F_FIRMA_PEQ));
        firma.addCell(datos);

        PdfPCell derecha = new PdfPCell(firma);
        derecha.setBorder(Rectangle.NO_BORDER);
        derecha.setPaddingLeft(14);
        derecha.setVerticalAlignment(Element.ALIGN_BOTTOM);
        table.addCell(derecha);
        return table;
    }

    private static String resumenGeneral(List<PlanillaSanidad> filas, BigDecimal cumplimiento) {
        if (filas.isEmpty()) {
            return "No hay registros de limpieza y desinfección en el periodo seleccionado.";
        }
        long incompletos = filas.stream()
                .filter(f -> !(Boolean.TRUE.equals(f.getLimpiezaRealizada())
                        && Boolean.TRUE.equals(f.getDesinfeccion())))
                .count();
        if (incompletos == 0) {
            return "Se mantiene el protocolo de limpieza y desinfección en todas las áreas.";
        }
        return incompletos + " de " + filas.size() + " registros tienen la limpieza o la desinfección "
                + "pendiente (cumplimiento del " + cumplimiento.stripTrailingZeros().toPlainString()
                + " %). Revise las observaciones de cada fila.";
    }

    // Hash SHA-256 del contenido firmado: si cambia cualquier dato, el hash deja de coincidir
    private static String calcularHash(List<PlanillaSanidad> filas, LocalDate inicio, LocalDate fin,
                                       String sede, String responsable, LocalDateTime firmadoEn) {
        StringBuilder base = new StringBuilder()
                .append(sede).append('|').append(responsable).append('|')
                .append(inicio).append('|').append(fin).append('|').append(firmadoEn);
        for (PlanillaSanidad f : filas) {
            base.append('\n').append(f.getId()).append('|').append(f.getFechaPlanilla())
                    .append('|').append(f.getAreaLimpieza()).append('|').append(f.getHerramienta())
                    .append('|').append(f.getLimpiezaRealizada()).append('|').append(f.getDesinfeccion())
                    .append('|').append(f.getObservaciones());
        }
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(base.toString().getBytes(StandardCharsets.UTF_8));
            return String.format("%064x", new BigInteger(1, digest));
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo calcular el hash de la planilla", e);
        }
    }

    // "Noviembre 2026" si el rango es un mes completo; si no, "01/11/2026 al 15/11/2026"
    private static String periodo(LocalDate inicio, LocalDate fin) {
        YearMonth mes = YearMonth.from(inicio);
        if (mes.equals(YearMonth.from(fin)) && inicio.getDayOfMonth() == 1 && fin.equals(mes.atEndOfMonth())) {
            String nombre = inicio.getMonth().getDisplayName(TextStyle.FULL, ES);
            return Character.toUpperCase(nombre.charAt(0)) + nombre.substring(1) + " " + inicio.getYear();
        }
        return FECHA.format(inicio) + " al " + FECHA.format(fin);
    }

    private static Paragraph centrado(String texto, Font font) {
        Paragraph p = new Paragraph(texto, font);
        p.setAlignment(Element.ALIGN_CENTER);
        return p;
    }

    private static PdfPCell celdaImagen(Image imagen, int alineacion) {
        PdfPCell cell = new PdfPCell(imagen);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setHorizontalAlignment(alineacion);
        cell.setPaddingBottom(4);
        return cell;
    }

    private static PdfPCell celdaVacia() {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(30);
        return cell;
    }

    // Lee una imagen del classpath; si no está devuelve null y el PDF sale sin ella
    private static Image cargarImagen(String ruta, float maximo) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(ruta)) {
            if (in == null) {
                return null;
            }
            Image imagen = Image.getInstance(in.readAllBytes());
            imagen.scaleToFit(maximo, maximo);
            return imagen;
        } catch (Exception e) {
            return null;
        }
    }

    // Franja azul al pie de cada página con la marca y el número de página
    private static final class PiePagina extends PdfPageEventHelper {
        @Override
        public void onEndPage(PdfWriter writer, Document document) {
            PdfContentByte cb = writer.getDirectContent();
            Rectangle pagina = document.getPageSize();
            cb.saveState();
            cb.setColorFill(NAVY);
            cb.rectangle(0, 0, pagina.getWidth(), 40);
            cb.fill();
            cb.restoreState();

            Font blanco = new Font(Font.HELVETICA, 9, Font.BOLD, Color.WHITE);
            ColumnText.showTextAligned(cb, Element.ALIGN_LEFT,
                    new Phrase(MARCA + " " + SUBMARCA, blanco), 36, 15, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_CENTER,
                    new Phrase("Página " + writer.getPageNumber(), blanco), pagina.getWidth() / 2, 15, 0);
            ColumnText.showTextAligned(cb, Element.ALIGN_RIGHT,
                    new Phrase("Gracias por confiar en nosotros", blanco), pagina.getWidth() - 36, 15, 0);
        }
    }
}