package usta.veterinaria.infrastructure.adapter.in.web;

import jakarta.inject.Inject;
import jakarta.ws.rs.*;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.resteasy.reactive.RestForm;
import org.jboss.resteasy.reactive.multipart.FileUpload;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import org.jboss.logging.Logger;

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
    private static final Logger LOG = Logger.getLogger(UploadsResource.class);
    // Se sube ANTES de registrar; el formulario guarda la URL devuelta en fotoMascota


    @Inject
    QuitarFondo quitarFondo;

    @POST
    @jakarta.ws.rs.Path("/mascotas")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Produces(MediaType.APPLICATION_JSON)
    public Response subirMascota(@RestForm("imagen") FileUpload imagen) {
        imagenes.validar(imagen);

        byte[] png = null;
        try (InputStream in = Files.newInputStream(imagen.uploadedFile())) {
            png = quitarFondo.aPngSinFondo(in);
        } catch (IOException | RuntimeException e) {
            LOG.warn("No se pudo quitar el fondo; se guarda la original", e);
        }

        String url = (png != null)
                ? imagenes.guardarPng(png, ImagenStorage.MASCOTAS)
                : imagenes.guardar(imagen, ImagenStorage.MASCOTAS);
        return Response.status(Response.Status.CREATED)
                .entity(new UrlImagenResponse(url))
                .build();
    }
}