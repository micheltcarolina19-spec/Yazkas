package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.TemplateData;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SoloAdmin;

import java.util.List;

// Objetos de solo lectura que el Resource le entrega a la plantilla adminCitas.html
@SoloAdmin
public final class CalendarioVista {

    private CalendarioVista() {
    }

    // registrada = la mascota ya tiene su bitácora con precio; subtotal = precio formateado ("$ 45.000")
    @TemplateData
    public record MascotaVista(Long mascotaCitaId, String nombre, String raza, String edad, String foto, String inicial,
                               String duenio, String celular, String duracion,
                               boolean registrada, String subtotal, String hrefBitacora) {
    }

    // Un bloque = un agendamiento (puede tener varias mascotas a la misma hora).
    // total = suma de las bitácoras registradas; progreso = "1/2" mientras faltan mascotas por registrar
    @TemplateData
    public record BloqueVista(String categoria, String servicio, String estadoClase, String estadoTexto,
                              String titulo, String rango, String fechaLarga,
                              int top, int alto, double izquierda, double ancho,
                              String lado, String vertical, boolean modificable, boolean varias,
                              List<MascotaVista> mascotas, String hrefReprogramar, String hrefCancelar,
                              String total, String progreso, boolean completada, boolean conBitacora) {
    }

    @TemplateData
    public record DiaVista(String dia, String fechaCorta, boolean hoy, int ahoraTop,
                           List<BloqueVista> bloques, String href) {
    }

    @TemplateData
    public record ChipVista(String hora, String titulo, String categoria, String estadoClase, String href, String total) {
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

    // Una opción marcable del formulario de bitácora (servicio, tipo de baño o hallazgo)
    @TemplateData
    public record OpcionVista(String clave, String valor, String etiqueta, boolean marcada) {
    }

    // Datos de la vacuna que se registra desde la bitácora (tabla vacunaciones). Fechas en formato yyyy-MM-dd
    @TemplateData
    public record VacunaVista(String nombre, String fechaAplicacion, String fechaRefuerzo, String lote,
                              String dosis, String observaciones) {
    }

    // Datos de la desparasitación que se registra desde la bitácora (tabla desparasitaciones)
    @TemplateData
    public record DesparasitacionVista(String marca, String fechaAplicacion, String lote, String observaciones) {
    }

    // Datos del modal "Bitácora de finalización" de UNA mascota de la cita
    @TemplateData
    public record BitacoraVista(Long agendamientoId, Long mascotaCitaId, String codigo,
                                String nombre, String inicial, String foto, String sexo,
                                String especie, String raza, String edad, String duenio, String fechaHora,
                                List<MascotaVista> mascotas,
                                List<OpcionVista> servicios, List<OpcionVista> tiposBano, List<OpcionVista> hallazgos,
                                String otroHallazgo, String observaciones, String recomendaciones,
                                String fotoSalida, String precio,
                                VacunaVista vacuna, DesparasitacionVista desparasitacion,
                                List<OpcionVista> tiposDesparasitacion, String hoy,
                                long totalOtras, String totalCita, int registradas, int totalMascotas,
                                boolean completada, boolean editable, String hrefCerrar) {
    }
}
