package usta.veterinaria.infrastructure.adapter.in.web.usuario;

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
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.TipoBano;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.domain.service.admin.BitacoraServicioService;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

// PDF de la bitácora de UNA cita de una mascota (lo que registró el admin al finalizar la cita):
// datos de la cita, servicios, tipo de baño, valor, foto de salida, hallazgos, observaciones,
// recomendaciones y, si se aplicaron, la vacuna y la desparasitación.
// Mismo estilo que HistorialMascotaPdf.
public final class BitacoraCitaPdf {

    private static final String LOGO_RUTA = "META-INF/resources/img/usuario/logo.png";

    private static final String MARCA = "YAZKAS";
    private static final String SUBMARCA = "Peluquería Canina";

    // Mismo formato con el que AdminCitasResource guarda las observaciones
    private static final String PRE_ENCONTRO = "En la mascota se encontró: ";
    private static final String PRE_OBSERVACIONES = "Observaciones: ";
    private static final String PRE_RECOMENDACIONES = "Recomendaciones: ";

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter FECHA_LARGA = DateTimeFormatter.ofPattern("EEEE d 'de' MMMM 'de' yyyy", ES);
    private static final DateTimeFormatter HORA = DateTimeFormatter.ofPattern("hh:mm a", Locale.US);
    private static final DateTimeFormatter FECHA_HORA = DateTimeFormatter.ofPattern("dd/MM/yyyy - hh:mm a", ES);

    private static final Color NAVY = new Color(0x0B, 0x4C, 0x8C);
    private static final Color AZUL_TITULO = new Color(0xCF, 0xE6, 0xFA);
    private static final Color AZUL_SUAVE = new Color(0xE8, 0xF3, 0xFD);
    private static final Color BORDE = new Color(0xB9, 0xD6, 0xF2);
    private static final Color TEXTO = new Color(0x1E, 0x2F, 0x4D);

    private static final Font F_MARCA = new Font(Font.HELVETICA, 26, Font.BOLD, NAVY);
    private static final Font F_SUBMARCA = new Font(Font.HELVETICA, 11, Font.NORMAL, NAVY);
    private static final Font F_META = new Font(Font.HELVETICA, 9, Font.ITALIC, NAVY);
    private static final Font F_TITULO = new Font(Font.HELVETICA, 20, Font.BOLD, Color.WHITE);
    private static final Font F_SECCION = new Font(Font.HELVETICA, 13, Font.BOLD, NAVY);
    private static final Font F_ETIQUETA = new Font(Font.HELVETICA, 10, Font.NORMAL, TEXTO);
    private static final Font F_VALOR = new Font(Font.HELVETICA, 10, Font.BOLD, TEXTO);
    private static final Font F_TOTAL = new Font(Font.HELVETICA, 13, Font.BOLD, NAVY);
    private static final Font F_COLUMNA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, NAVY);
    private static final Font F_CELDA = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTO);
    private static final Font F_PIE_FOTO = new Font(Font.HELVETICA, 8.5f, Font.ITALIC, NAVY);

    private BitacoraCitaPdf() {
    }

    /**
     * @param fotoSalida  bytes de la foto de salida (null = sin foto)
     * @param fotoMascota bytes de la foto de perfil de la mascota (null = sin foto)
     */
    public static byte[] generar(Mascota mascota, MascotaCita cita, BitacoraServicio bitacora,
                                 byte[] fotoSalida, byte[] fotoMascota) {
        Document document = new Document(PageSize.LETTER, 36, 36, 36, 64);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PiePagina());
            document.open();

            document.add(encabezado());
            document.add(titulo("Bitácora del Servicio"));
            document.add(fichaCita(mascota, cita, fotoMascota));
            document.add(servicios(bitacora));
            document.add(fotoSalida(fotoSalida, mascota.getNombre()));
            document.add(hallazgos(bitacora.getObservacionesPeluqueria()));
            if (bitacora.getVacunacion() != null) {
                document.add(tablaVacuna(bitacora.getVacunacion()));
            }
            if (bitacora.getDesparasitacion() != null) {
                document.add(tablaDesparasitacion(bitacora.getDesparasitacion()));
            }

            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF de la bitácora", e);
        }
        return output.toByteArray();
    }

    // ------------------------------------ Secciones ------------------------------------

    private static PdfPTable encabezado() {
        PdfPTable marca = new PdfPTable(new float[]{1f, 3.6f});
        marca.setWidthPercentage(100);
        Image logo = cargarRecurso(LOGO_RUTA, 52);
        marca.addCell(logo != null ? celdaImagen(logo, Element.ALIGN_LEFT, false) : celdaVacia());

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

        PdfPCell generado = new PdfPCell(new Phrase(
                "Generado: " + FECHA_HORA.format(LocalDateTime.now(RelojColombia.reloj())), F_META));
        generado.setBorder(Rectangle.NO_BORDER);
        generado.setHorizontalAlignment(Element.ALIGN_RIGHT);
        generado.setVerticalAlignment(Element.ALIGN_MIDDLE);
        table.addCell(generado);
        return table;
    }

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

    // Datos de la cita y la mascota a la izquierda, foto de perfil a la derecha
    private static PdfPTable fichaCita(Mascota m, MascotaCita c, byte[] fotoMascota) {
        Usuario u = m.getUsuario();
        String propietario = u == null ? "-"
                : texto(u.getNombre()) + (u.getApellido() == null ? "" : " " + u.getApellido());
        PdfPTable datos = infoTable("Datos de la Cita", new String[][]{
                {"Mascota:", texto(m.getNombre())},
                {"Tipo / Raza:", texto(m.getTipo()) + " / " + texto(m.getRaza())},
                {"Propietario:", propietario},
                {"Fecha:", fechaLarga(c.getFechaAgendamiento())},
                {"Hora:", c.getHoraAgendamiento() == null ? "-" : HORA.format(c.getHoraAgendamiento())},
                {"Estado:", "Completada"}
        });

        PdfPTable ficha = new PdfPTable(new float[]{3.6f, 1.4f});
        ficha.setWidthPercentage(100);
        ficha.setSpacingAfter(12);

        PdfPCell izquierda = new PdfPCell(datos);
        izquierda.setBorder(Rectangle.NO_BORDER);
        ficha.addCell(izquierda);

        Image foto = imagen(fotoMascota, 110);
        PdfPCell derecha = foto != null
                ? celdaImagen(foto, Element.ALIGN_CENTER, true)
                : celdaTexto("Sin foto", Element.ALIGN_CENTER);
        derecha.setVerticalAlignment(Element.ALIGN_MIDDLE);
        derecha.setPaddingLeft(10);
        ficha.addCell(derecha);
        return ficha;
    }

    private static PdfPTable servicios(BitacoraServicio b) {
        String tipoServicio = texto(b.getTipoServicio());
        boolean conBano = b.getTipoServicio() != null
                && b.getTipoServicio().toLowerCase(ES).contains(BitacoraServicioService.BANO.toLowerCase(ES));

        List<String[]> filas = new ArrayList<>();
        filas.add(new String[]{"Servicios realizados:", tipoServicio});
        if (conBano) {
            filas.add(new String[]{"Tipo de baño:", tipoBano(b.getTipoBano())});
        }
        PdfPTable table = infoTable("Servicios", filas.toArray(new String[0][]));

        PdfPCell etiqueta = new PdfPCell(new Phrase("Valor del servicio:", F_VALOR));
        etiqueta.setBackgroundColor(AZUL_TITULO);
        etiqueta.setBorder(Rectangle.NO_BORDER);
        etiqueta.setPadding(8);
        table.addCell(etiqueta);
        PdfPCell valor = new PdfPCell(new Phrase(pesos(b.getPrecioSubtotal()), F_TOTAL));
        valor.setBackgroundColor(AZUL_TITULO);
        valor.setBorder(Rectangle.NO_BORDER);
        valor.setPadding(8);
        table.addCell(valor);

        table.setSpacingAfter(12);
        return table;
    }

    private static PdfPTable fotoSalida(byte[] bytes, String nombre) {
        PdfPTable table = new PdfPTable(1);
        table.setWidthPercentage(100);
        table.setSpacingAfter(12);
        table.setKeepTogether(true);

        PdfPCell cabecera = new PdfPCell(new Phrase("Así quedó " + texto(nombre), F_SECCION));
        cabecera.setBackgroundColor(AZUL_TITULO);
        cabecera.setBorder(Rectangle.NO_BORDER);
        cabecera.setPadding(8);
        table.addCell(cabecera);

        Image foto = imagen(bytes, 230);
        PdfPCell cuerpo;
        if (foto != null) {
            cuerpo = new PdfPCell(foto);
            cuerpo.setPadding(10);
        } else {
            cuerpo = new PdfPCell(new Phrase("El servicio no tiene foto de salida.", F_CELDA));
            cuerpo.setPadding(14);
        }
        cuerpo.setBackgroundColor(AZUL_SUAVE);
        cuerpo.setBorder(Rectangle.NO_BORDER);
        cuerpo.setHorizontalAlignment(Element.ALIGN_CENTER);
        table.addCell(cuerpo);

        if (foto != null) {
            PdfPCell pie = new PdfPCell(new Phrase("Foto de referencia tomada al finalizar el servicio", F_PIE_FOTO));
            pie.setBackgroundColor(AZUL_SUAVE);
            pie.setBorder(Rectangle.NO_BORDER);
            pie.setHorizontalAlignment(Element.ALIGN_CENTER);
            pie.setPaddingBottom(8);
            table.addCell(pie);
        }
        return table;
    }

    private static PdfPTable hallazgos(String observaciones) {
        String[] obs = separarObservaciones(observaciones);
        PdfPTable table = infoTable("Hallazgos y Observaciones", new String[][]{
                {"En la mascota se encontró:", obs[0].isEmpty() ? "Sin hallazgos" : obs[0]},
                {"Observaciones:", texto(obs[1])},
                {"Recomendaciones:", texto(obs[2])}
        });
        table.setSpacingAfter(12);
        return table;
    }

    private static PdfPTable tablaVacuna(Vacunacion v) {
        return tabla("Vacuna aplicada",
                new String[]{"Vacuna", "Aplicación", "Refuerzo", "Lote", "Dosis", "Observaciones"},
                new float[]{2f, 1.3f, 1.3f, 1.2f, 0.8f, 2.4f},
                new String[]{texto(v.getNombreVacuna()), fecha(v.getFechaAplicacion()), fecha(v.getFechaRefuerzo()),
                        texto(v.getNumeroLote()), texto(v.getNumeroDosis()), texto(v.getObservaciones())});
    }

    private static PdfPTable tablaDesparasitacion(Desparasitacion d) {
        return tabla("Desparasitación aplicada",
                new String[]{"Marca", "Aplicación", "Tipo", "Lote", "Observaciones"},
                new float[]{2f, 1.3f, 1.4f, 1.2f, 2.6f},
                new String[]{texto(d.getMarca()), fecha(d.getFechaAplicacion()), texto(d.getTipo()),
                        texto(d.getNumeroLote()), texto(d.getObservaciones())});
    }

    // ------------------------------------ Componentes ------------------------------------

    private static PdfPTable infoTable(String titulo, String[][] pares) {
        PdfPTable table = new PdfPTable(new float[]{1.8f, 3f});
        table.setWidthPercentage(100);

        PdfPCell cabecera = new PdfPCell(new Phrase(titulo, F_SECCION));
        cabecera.setColspan(2);
        cabecera.setBackgroundColor(AZUL_TITULO);
        cabecera.setBorder(Rectangle.NO_BORDER);
        cabecera.setPadding(8);
        table.addCell(cabecera);

        for (String[] par : pares) {
            table.addCell(celdaInfo(par[0], F_ETIQUETA));
            table.addCell(celdaInfo(par[1], F_VALOR));
        }
        return table;
    }

    private static PdfPCell celdaInfo(String texto, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, font));
        cell.setBackgroundColor(AZUL_SUAVE);
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setPaddingLeft(8);
        cell.setPaddingTop(3);
        cell.setPaddingBottom(6);
        return cell;
    }

    // Tabla de una fila con título y encabezados
    private static PdfPTable tabla(String titulo, String[] columnas, float[] anchos, String[] fila) {
        PdfPTable table = new PdfPTable(anchos);
        table.setWidthPercentage(100);
        table.setSpacingAfter(14);
        table.setKeepTogether(true);

        PdfPCell seccion = new PdfPCell(new Phrase(titulo, F_SECCION));
        seccion.setColspan(columnas.length);
        seccion.setBackgroundColor(AZUL_TITULO);
        seccion.setBorderColor(BORDE);
        seccion.setPadding(8);
        table.addCell(seccion);

        for (String columna : columnas) {
            PdfPCell cell = new PdfPCell(new Phrase(columna, F_COLUMNA));
            cell.setBackgroundColor(AZUL_SUAVE);
            cell.setBorderColor(BORDE);
            cell.setPadding(6);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
        for (int i = 0; i < fila.length; i++) {
            PdfPCell cell = new PdfPCell(new Phrase(fila[i], F_CELDA));
            cell.setBorderColor(BORDE);
            cell.setPadding(6);
            cell.setHorizontalAlignment(i == fila.length - 1 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
            cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
            table.addCell(cell);
        }
        return table;
    }

    private static PdfPCell celdaTexto(String texto, int alineacion) {
        PdfPCell cell = new PdfPCell(new Phrase(texto, F_CELDA));
        cell.setBorderColor(BORDE);
        cell.setPadding(10);
        cell.setHorizontalAlignment(alineacion);
        return cell;
    }

    private static PdfPCell celdaImagen(Image imagen, int alineacion, boolean conBorde) {
        PdfPCell cell = new PdfPCell(imagen);
        if (conBorde) {
            cell.setBorderColor(BORDE);
        } else {
            cell.setBorder(Rectangle.NO_BORDER);
        }
        cell.setHorizontalAlignment(alineacion);
        cell.setPadding(4);
        return cell;
    }

    private static PdfPCell celdaVacia() {
        PdfPCell cell = new PdfPCell(new Phrase(" "));
        cell.setBorder(Rectangle.NO_BORDER);
        cell.setFixedHeight(30);
        return cell;
    }

    // ------------------------------------ Formato ------------------------------------

    // Separa el texto guardado en: [0] lo encontrado, [1] observaciones, [2] recomendaciones
    static String[] separarObservaciones(String texto) {
        String[] r = {"", "", ""};
        if (texto == null || texto.isBlank()) {
            return r;
        }
        StringBuilder sueltas = new StringBuilder();
        for (String l : texto.split("\\R")) {
            if (l.startsWith(PRE_ENCONTRO)) {
                String resto = l.substring(PRE_ENCONTRO.length()).trim();
                r[0] = resto.endsWith(".") ? resto.substring(0, resto.length() - 1) : resto;
            } else if (l.startsWith(PRE_OBSERVACIONES)) {
                r[1] = l.substring(PRE_OBSERVACIONES.length()).trim();
            } else if (l.startsWith(PRE_RECOMENDACIONES)) {
                r[2] = l.substring(PRE_RECOMENDACIONES.length()).trim();
            } else if (!l.isBlank()) {
                sueltas.append(sueltas.length() == 0 ? "" : " ").append(l.trim());   // texto antiguo sin formato
            }
        }
        if (sueltas.length() > 0) {
            r[1] = r[1].isEmpty() ? sueltas.toString() : sueltas + " " + r[1];
        }
        return r;
    }

    private static String texto(Object valor) {
        if (valor == null || valor.toString().isBlank()) {
            return "-";
        }
        if (valor instanceof Enum<?> e) {
            String limpio = e.name().replace('_', ' ').toLowerCase(ES);
            return Character.toUpperCase(limpio.charAt(0)) + limpio.substring(1);
        }
        return valor.toString();
    }

    private static String tipoBano(TipoBano tipo) {
        return tipo == null ? "Normal" : texto(tipo);
    }

    private static String pesos(BigDecimal valor) {
        if (valor == null) {
            return "-";
        }
        NumberFormat f = NumberFormat.getNumberInstance(ES);
        f.setMaximumFractionDigits(0);
        return "$ " + f.format(valor) + " COP";
    }

    private static String fecha(LocalDate fecha) {
        return fecha == null ? "-" : FECHA.format(fecha);
    }

    private static String fechaLarga(LocalDate fecha) {
        if (fecha == null) {
            return "-";
        }
        String t = FECHA_LARGA.format(fecha);
        return Character.toUpperCase(t.charAt(0)) + t.substring(1);
    }

    // ------------------------------------ Imágenes ------------------------------------

    // OpenPDF lee JPG, PNG y GIF; si la imagen no es válida (p. ej. WebP) se muestra "Sin foto"
    private static Image imagen(byte[] bytes, float maximo) {
        if (bytes == null || bytes.length == 0) {
            return null;
        }
        try {
            Image imagen = Image.getInstance(bytes);
            imagen.scaleToFit(maximo, maximo);
            return imagen;
        } catch (Exception e) {
            return null;
        }
    }

    private static Image cargarRecurso(String ruta, float maximo) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(ruta)) {
            return in == null ? null : imagen(in.readAllBytes(), maximo);
        } catch (Exception e) {
            return null;
        }
    }

    // Franja azul al pie de cada página
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
