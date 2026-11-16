package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import usta.veterinaria.application.port.in.admin.GestionarProductosUseCase;
import usta.veterinaria.domain.model.admin.CategoriaProducto;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.model.admin.TipoProducto;
import usta.veterinaria.infrastructure.adapter.in.web.ImagenStorage;

import java.util.Comparator;
import java.util.List;
import java.util.Map;

// Inventario de productos del administrador.
//  - GET    /admin/productos         -> la página (tabla + panel lateral para crear/editar)
//  - GET    /admin/productos/filas   -> SOLO las filas de la tabla ya pintadas (se pide después de guardar o eliminar)
//  - POST   /admin/productos/imagen  -> sube la foto, la guarda como archivo y devuelve { "url": "/uploads/productos/..." }
//  - DELETE /admin/productos/imagen  -> borra una foto subida que ya no se usa
//
// Crear, actualizar y eliminar productos NO se repite aquí: el front usa /api/productos (ProductoResource).
@Path("/admin/productos")
@Produces(MediaType.TEXT_HTML)
public class AdminProductoResource {

    private static final int UMBRAL_STOCK_BAJO = 5;    // mismo umbral de las alertas del dashboard
    private static final int UMBRAL_STOCK_MEDIO = 15;  // por encima de esto el stock se ve "bien"

    @Inject
    @Location("admin/adminProductos.html")
    Template index;

    @Inject
    GestionarProductosUseCase productos;

    @Inject
    ImagenStorage imagenes;

    // ---------------------------------------------------------------- Página

    // Página completa: filas + los enum que usan los filtros y el formulario
    @GET
    public TemplateInstance pagina() {
        return datos(index.instance())
                .data("categorias", CategoriaProducto.values())
                .data("tipos", TipoProducto.values());
    }

    // Solo las filas ({#fragment id=filas} de la plantilla)
    @GET
    @Path("/filas")
    public Response filas() {
        String html = datos(index.getFragment("filas").instance()).render();
        return Response.ok(html).header("Cache-Control", "no-store").build();
    }

    // ---------------------------------------------------------------- Imágenes

    // Recibe la foto (campo "imagen"), la valida y la guarda como archivo. En la base solo se guarda la URL
    @POST
    @Path("/imagen")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subirImagen(@RestForm("imagen") FileUpload imagen) {
        return Response.ok(Map.of("url", imagenes.guardar(imagen))).build();
    }

    // Borra una foto que subimos nosotros (las URLs externas se ignoran)
    @DELETE
    @Path("/imagen")
    @Produces(MediaType.APPLICATION_JSON)
    public Response eliminarImagen(@QueryParam("url") String url) {
        imagenes.eliminar(url);
        return Response.noContent().build();
    }

    // ---------------------------------------------------------------- Apoyo

    // Datos que usa la plantilla para pintar la tabla (orden alfabético por nombre)
    private TemplateInstance datos(TemplateInstance plantilla) {
        List<Producto> lista = productos.listar().stream()
                .sorted(Comparator.comparing(
                        (Producto p) -> p.getNombre() == null ? "" : p.getNombre(),
                        String.CASE_INSENSITIVE_ORDER))
                .toList();
        return plantilla
                .data("productos", lista)
                .data("stockBajo", UMBRAL_STOCK_BAJO)
                .data("stockMedio", UMBRAL_STOCK_MEDIO);
    }
}