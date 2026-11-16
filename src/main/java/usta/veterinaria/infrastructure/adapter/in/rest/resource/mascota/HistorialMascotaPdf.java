package usta.veterinaria.infrastructure.adapter.in.rest.resource.admin;

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
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.HistoriaClinica;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.domain.service.RelojColombia;

import java.awt.Color;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

// Arma el PDF del historial clínico de una mascota: ficha, historia clínica, vacunas y desparasitaciones.
public final class HistorialMascotaPdf {

    private static final String LOGO_RUTA = "META-INF/resources/img/usuario/logo.png";
    private static final String RAIZ_ESTATICOS = "META-INF/resources/";

    private static final String MARCA = "YAZKAS";
    private static final String SUBMARCA = "Peluquería Canina";

    private static final Locale ES = Locale.forLanguageTag("es-CO");
    private static final DateTimeFormatter FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy");
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
    private static final Font F_COLUMNA = new Font(Font.HELVETICA, 8.5f, Font.BOLD, NAVY);
    private static final Font F_CELDA = new Font(Font.HELVETICA, 8.5f, Font.NORMAL, TEXTO);

    private HistorialMascotaPdf() {
    }

    // historia puede ser null (la mascota aún no tiene historia clínica)
    public static byte[] generar(Mascota mascota, HistoriaClinica historia,
                                 List<Vacunacion> vacunaciones, List<Desparasitacion> desparasitaciones) {
        Document document = new Document(PageSize.LETTER, 36, 36, 36, 64);
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            PdfWriter writer = PdfWriter.getInstance(document, output);
            writer.setPageEvent(new PiePagina());
            document.open();

            document.add(encabezado());
            document.add(titulo("Historial Clínico"));
            document.add(fichaMascota(mascota));
            document.add(propietario(mascota.getUsuario()));
            document.add(historiaClinica(historia));
            document.add(tablaVacunaciones(vacunaciones));
            document.add(tablaDesparasitaciones(desparasitaciones));

            document.close();
        } catch (Exception e) {
            throw new IllegalStateException("No se pudo generar el PDF del historial de la mascota", e);
        }
        return output.toByteArray();
    }

    // ------------------------------------ Secciones ------------------------------------

    private static PdfPTable encabezado() {
        PdfPTable marca = new PdfPTable(new float[]{1f, 3.6f});
        marca.setWidthPercentage(100);
        Image logo = cargarRecurso(LOGO_RUTA, 52);
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

    // Datos de la mascota a la izquierda, foto a la derecha
    private static PdfPTable fichaMascota(Mascota m) {
        PdfPTable datos = infoTable("Datos de la Mascota", new String[][]{
                {"Nombre:", texto(m.getNombre())},
                {"Tipo / Raza:", texto(m.getTipo()) + " / " + texto(m.getRaza())},
                {"Sexo:", texto(m.getSexo())},
                {"Nacimiento / Edad:", fecha(m.getFechaNacimiento()) + " (" + edad(m.getFechaNacimiento()) + ")"},
                {"Peso / Tamaño:", texto(m.getPeso()) + " kg / " + texto(m.getTamano())},
                {"Color / Pelaje:", texto(m.getColor()) + " / " + texto(m.getPelaje())},
                {"Temperamento:", texto(m.getTemperamento())},
                {"Agresividad / Bozal:", texto(m.getNivelAgresividad()) + " / " + siNo(m.getUsaBozal())},
                {"Sociable / Energía:", siNo(m.getSociable()) + " / " + texto(m.getEnergia())},
                {"Fobias:", texto(m.getFobias())},
                {"Observaciones:", texto(m.getObservaciones())}
        });

        PdfPTable ficha = new PdfPTable(new float[]{3.6f, 1.4f});
        ficha.setWidthPercentage(100);
        ficha.setSpacingAfter(12);

        PdfPCell izquierda = new PdfPCell(datos);
        izquierda.setBorder(Rectangle.NO_BORDER);
        ficha.addCell(izquierda);

        Image foto = cargarFoto(m.getFotoMascota(), 120);
        PdfPCell derecha = foto != null
                ? celdaImagen(foto, Element.ALIGN_CENTER)
                : celdaTexto("Sin foto", Element.ALIGN_CENTER);
        derecha.setVerticalAlignment(Element.ALIGN_MIDDLE);
        derecha.setPaddingLeft(10);
        ficha.addCell(derecha);
        return ficha;
    }

    private static PdfPTable propietario(Usuario u) {
        if (u == null) {
            return infoTable("Propietario", new String[][]{{"Propietario:", "-"}});
        }
        PdfPTable table = infoTable("Propietario", new String[][]{
                {"Nombre:", texto(u.getNombre()) + " " + (u.getApellido() == null ? "" : u.getApellido())},
                {"Celular:", texto(u.getCelular())},
                {"Correo:", texto(u.getCorreo())}
        });
        table.setSpacingAfter(12);
        return table;
    }

    private static PdfPTable historiaClinica(HistoriaClinica h) {
        PdfPTable table;
        if (h == null) {
            table = infoTable("Historia Clínica", new String[][]{{"Estado:", "La mascota aún no tiene historia clínica."}});
        } else {
            table = infoTable("Historia Clínica", new String[][]{
                    {"Alergias a medicamentos:", texto(h.getAlergiasMedicamentos())},
                    {"Alergias cosméticas:", texto(h.getAlergiasCosmeticas())},
                    {"Alergias alimenticias:", texto(h.getAlergiasAlimenticias())},
                    {"Enfermedades crónicas:", texto(h.getEnfermedadesCronicas())},
                    {"Condiciones físicas:", texto(h.getCondicionesFisicas())},
                    {"Problemas cardíacos:", texto(h.getProblemasCardiacos())},
                    {"Problemas ortopédicos:", texto(h.getProblemasOrtopedicos())},
                    {"Diagnóstico dermatológico:", texto(h.getDiagnosticoDermatologico())},
                    {"Presencia de parásitos:", siNo(h.getPresenciaParasitos())},
                    {"Cirugías recientes:", texto(h.getCirugiasRecientes())},
                    {"Lesiones:", texto(h.getLesiones())},
                    {"Esterilización:", siNo(h.getEsterilizacion())},
                    {"Observaciones:", texto(h.getObservaciones())}
            });
        }
        table.setSpacingAfter(12);
        return table;
    }

    private static PdfPTable tablaVacunaciones(List<Vacunacion> vacunaciones) {
        List<String[]> filas = new ArrayList<>();
        vacunaciones.stream()
                .sorted(Comparator.comparing(Vacunacion::getFechaAplicacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(v -> filas.add(new String[]{
                        texto(v.getNombreVacuna()), fecha(v.getFechaAplicacion()), fecha(v.getFechaRefuerzo()),
                        texto(v.getNumeroLote()), texto(v.getNumeroDosis()), texto(v.getObservaciones())}));
        return tabla("Vacunaciones",
                new String[]{"Vacuna", "Aplicación", "Refuerzo", "Lote", "Dosis", "Observaciones"},
                new float[]{2f, 1.3f, 1.3f, 1.2f, 0.8f, 2.4f},
                filas, "Sin vacunas registradas.");
    }

    private static PdfPTable tablaDesparasitaciones(List<Desparasitacion> desparasitaciones) {
        List<String[]> filas = new ArrayList<>();
        desparasitaciones.stream()
                .sorted(Comparator.comparing(Desparasitacion::getFechaAplicacion,
                        Comparator.nullsLast(Comparator.reverseOrder())))
                .forEach(d -> filas.add(new String[]{
                        texto(d.getMarca()), fecha(d.getFechaAplicacion()), texto(d.getTipo()),
                        texto(d.getNumeroLote()), texto(d.getObservaciones())}));
        return tabla("Desparasitaciones",
                new String[]{"Marca", "Aplicación", "Tipo", "Lote", "Observaciones"},
                new float[]{2f, 1.3f, 1.4f, 1.2f, 2.6f},
                filas, "Sin desparasitaciones registradas.");
    }

    // ------------------------------------ Componentes ------------------------------------

    // Bloque con cabecera azul y filas etiqueta / valor
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
        cell.setPaddingBottom(5);
        return cell;
    }

    // Tabla con título y encabezados que se repiten en cada página; la última columna va alineada a la izquierda
    private static PdfPTable tabla(String titulo, String[] columnas, float[] anchos,
                                   List<String[]> filas, String vacio) {
        PdfPTable table = new PdfPTable(anchos);
        table.setWidthPercentage(100);
        table.setHeaderRows(2);
        table.setSpacingAfter(14);

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

        if (filas.isEmpty()) {
            PdfPCell cell = new PdfPCell(new Phrase(vacio, F_CELDA));
            cell.setColspan(columnas.length);
            cell.setBorderColor(BORDE);
            cell.setPadding(10);
            cell.setHorizontalAlignment(Element.ALIGN_CENTER);
            table.addCell(cell);
            return table;
        }

        for (String[] fila : filas) {
            for (int i = 0; i < fila.length; i++) {
                PdfPCell cell = new PdfPCell(new Phrase(fila[i], F_CELDA));
                cell.setBorderColor(BORDE);
                cell.setPadding(6);
                cell.setHorizontalAlignment(i == fila.length - 1 ? Element.ALIGN_LEFT : Element.ALIGN_CENTER);
                cell.setVerticalAlignment(Element.ALIGN_MIDDLE);
                table.addCell(cell);
            }
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

    private static PdfPCell celdaImagen(Image imagen, int alineacion) {
        PdfPCell cell = new PdfPCell(imagen);
        cell.setBorderColor(BORDE);
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

    private static String fecha(LocalDate fecha) {
        return fecha == null ? "-" : FECHA.format(fecha);
    }

    private static String siNo(Boolean valor) {
        return valor == null ? "-" : (valor ? "Sí" : "No");
    }

    private static String edad(LocalDate nacimiento) {
        if (nacimiento == null) {
            return "-";
        }
        Period p = Period.between(nacimiento, LocalDate.now(RelojColombia.reloj()));
        return p.getYears() + " años, " + p.getMonths() + " meses";
    }

    // ------------------------------------ Imágenes ------------------------------------

    // Foto de la mascota: acepta data URI (base64) o ruta estática dentro de META-INF/resources.
    // No lee rutas del disco ni URLs externas (el valor viene del cliente).
    private static Image cargarFoto(String origen, float maximo) {
        if (origen == null || origen.isBlank()) {
            return null;
        }
        String valor = origen.trim();
        try {
            if (valor.startsWith("data:")) {
                int coma = valor.indexOf(',');
                if (coma < 0) {
                    return null;
                }
                return escalar(Base64.getMimeDecoder().decode(valor.substring(coma + 1)), maximo);
            }
            if (valor.contains("..") || valor.startsWith("http")) {
                return null;
            }
            return cargarRecurso(RAIZ_ESTATICOS + valor.replaceFirst("^/+", ""), maximo);
        } catch (Exception e) {
            return null;
        }
    }

    private static Image cargarRecurso(String ruta, float maximo) {
        try (InputStream in = Thread.currentThread().getContextClassLoader().getResourceAsStream(ruta)) {
            return in == null ? null : escalar(in.readAllBytes(), maximo);
        } catch (Exception e) {
            return null;
        }
    }

    private static Image escalar(byte[] bytes, float maximo) throws Exception {
        Image imagen = Image.getInstance(bytes);
        imagen.scaleToFit(maximo, maximo);
        return imagen;
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