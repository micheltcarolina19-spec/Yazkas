package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.TemplateData;

import java.util.List;

// Modelos de solo lectura que consume la plantilla adminGastos.html
public final class GastoVista {

    private GastoVista() {
    }

    @TemplateData
    public record FiltroVista(String desde, String hasta, String categoria, int pagina) {
    }

    @TemplateData
    public record ResumenVista(String total, String promedio, int registros, String periodo) {
    }

    @TemplateData
    public record AtajoVista(String texto, String href, boolean activo) {
    }

    @TemplateData
    public record CategoriaVista(String codigo, String etiqueta, String descripcion, String icono,
                                 String tono, boolean seleccionada) {
    }

    @TemplateData
    public record FilaVista(Long id, String fecha, String nombre, String categoria, String icono, String tono,
                            String monto, String hrefEditar, String hrefEliminar) {
    }

    @TemplateData
    public record DesgloseVista(String etiqueta, String icono, String tono, String monto, int porcentaje) {
    }

    @TemplateData
    public record PaginaItemVista(int numero, String href, boolean activa) {
    }

    @TemplateData
    public record PaginacionVista(int desde, int hasta, int total, boolean hayAnterior, boolean haySiguiente,
                                  String hrefAnterior, String hrefSiguiente, List<PaginaItemVista> items) {
    }

    // Diálogo de registrar / editar
    @TemplateData
    public record DialogoVista(Long id, boolean edicion, String titulo, String nombre, String monto, String fecha,
                               String error, String hrefCerrar, List<CategoriaVista> categorias) {
    }

    // Diálogo de confirmación para eliminar
    @TemplateData
    public record EliminarVista(Long id, String nombre, String detalle, String hrefCerrar) {
    }
}