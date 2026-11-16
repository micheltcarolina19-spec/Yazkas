package usta.veterinaria.infrastructure.adapter.in.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.nio.file.Path;

@jakarta.ws.rs.Path("/uploads")
public class UploadsResource {

    public record UrlImagenResponse(String url) {
    }

    @Inject
    ImagenStorage imagenes;

    @GET
    @jakarta.ws.rs.Path("/{carpeta}/{nombre}")
    public Response ver(@PathParam("carpeta") String carpeta, @PathParam("nombre") String nombre) {
        Path archivo = imagenes.ubicar(carpeta, nombre)
                .orElseThrow(() -> new NotFoundException("Imagen no encontrada"));

        return Response.ok(archivo.toFile())
                .type(imagenes.tipoDe(nombre))
                .header("Cache-Control", "public, max-age=31536000, immutable")
                .header("X-Content-Type-Options", "nosniff")
                .build();
    }

    // Se sube ANTES de registrar; el formulario guarda la URL devuelta en fotoMascota
    @POST
    @jakarta.ws.rs.Path("/mascotas")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subirMascota(@RestForm("imagen") FileUpload imagen) {
        String url = imagenes.guardar(imagen, ImagenStorage.MASCOTAS);
        return Response.status(Response.Status.CREATED).entity(new UrlImagenResponse(url)).build();
    }
}