
package usta.veterinaria.infrastructure.adapter.in.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.core.Response;

import java.nio.file.Path;

@jakarta.ws.rs.Path("/uploads/productos")
public class UploadsResource {

    @Inject
    ImagenProductoStorage imagenes;

    @GET
    @jakarta.ws.rs.Path("/{nombre}")
    public Response ver(@PathParam("nombre") String nombre) {

        Path archivo = imagenes.ubicar(nombre)
                .orElseThrow(() -> new NotFoundException("Imagen no encontrada"));

        return Response.ok(archivo.toFile())
                .type(imagenes.tipoDe(nombre))
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }
}
