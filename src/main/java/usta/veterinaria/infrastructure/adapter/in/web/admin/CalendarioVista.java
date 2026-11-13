package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.TemplateData;

import java.util.List;

// Objetos de solo lectura que el Resource le entrega a la plantilla adminCitas.html
public final class CalendarioVista {

    private CalendarioVista() {
    }

    @TemplateData
    public record MascotaVista(String nombre, String raza, String edad, String foto, String inicial,
                               String duenio, String celular, String duracion) {
    }

    // Un bloque = un agendamiento (puede tener varias mascotas a la misma hora)
    @TemplateData
    public record BloqueVista(String categoria, String servicio, String estadoClase, String estadoTexto,
                              String titulo, String rango, String fechaLarga,
                              int top, int alto, double izquierda, double ancho,
                              String lado, String vertical, boolean modificable, boolean varias,
                              List<MascotaVista> mascotas, String hrefReprogramar, String hrefCancelar) {
    }

    @TemplateData
    public record DiaVista(String dia, String fechaCorta, boolean hoy, int ahoraTop,
                           List<BloqueVista> bloques, String href) {
    }

    @TemplateData
    public record ChipVista(String hora, String titulo, String categoria, String estadoClase, String href) {
    }

    @TemplateData
    public record CeldaVista(int numero, boolean fuera, boolean hoy, List<ChipVista> chips, int extra, String href) {
    }

    @TemplateData
    public record ResumenVista(int total, int programadas, int reprogramadas, int completadas) {
    }

    // Datos del diálogo de reprogramar / cancelar
    @TemplateData
    public record AccionVista(Long agendamientoId, String resumen, String fecha, String hora, String hrefCerrar) {
    }
}