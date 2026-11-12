package usta.veterinaria.infrastructure.adapter.in.web;

import jakarta.annotation.PostConstruct;
import jakarta.enterprise.context.ApplicationScoped;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import org.jboss.logging.Logger;
import org.jboss.resteasy.reactive.multipart.FileUpload;
import usta.veterinaria.domain.exception.BusinessRuleException;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Optional;
import java.util.UUID;
import java.util.regex.Pattern;

// Guarda las fotos de los productos como ARCHIVOS en disco (nunca como blob en la base de datos).
// En la base solo queda la URL pública (campo fotoProducto), por ejemplo: /uploads/productos/3f9c...a1.png
//
// Seguridad:
//  - el tipo se detecta por los primeros bytes del archivo, no por la extensión ni por el Content-Type que manda el navegador
//  - el nombre se genera aquí (UUID), así que el usuario no controla la ruta (no hay path traversal)
@ApplicationScoped
public class ImagenProductoStorage {

    public static final String PREFIJO_URL = "/uploads/productos/";

    private static final long MAX_BYTES = 2L * 1024 * 1024; // 2 MB
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f]{32}\\.(jpg|png|webp)");
    private static final Logger LOG = Logger.getLogger(ImagenProductoStorage.class);

    // Carpeta base de las subidas. En producción apúntala a una ruta fuera del proyecto y con respaldo
    @ConfigProperty(name = "yazkas.uploads.dir", defaultValue = "uploads")
    String directorioBase;

    private Path carpeta;

    @PostConstruct
    void iniciar() {
        carpeta = Paths.get(directorioBase, "productos").toAbsolutePath().normalize();
        try {
            Files.createDirectories(carpeta);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de imágenes: " + carpeta, e);
        }
    }

    // Valida y guarda la imagen; devuelve la URL pública que se guarda en el producto
    public String guardar(FileUpload archivo) {
        if (archivo == null || archivo.size() == 0) {
            throw new BusinessRuleException("Selecciona una imagen para subir");
        }
        if (archivo.size() > MAX_BYTES) {
            throw new BusinessRuleException("La imagen no puede superar 2 MB");
        }

        String extension = detectarExtension(archivo.uploadedFile());
        String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.copy(archivo.uploadedFile(), carpeta.resolve(nombre));
        } catch (IOException e) {
            LOG.error("No se pudo guardar la imagen del producto", e);
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
        return PREFIJO_URL + nombre;
    }

    // Archivo de una imagen guardada (vacío si el nombre no es válido o no existe)
    public Optional<Path> ubicar(String nombre) {
        if (nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path ruta = carpeta.resolve(nombre).normalize();
        return ruta.startsWith(carpeta) && Files.isRegularFile(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    public String tipoDe(String nombre) {
        if (nombre.endsWith(".png")) {
            return "image/png";
        }
        if (nombre.endsWith(".webp")) {
            return "image/webp";
        }
        return "image/jpeg";
    }

    // Borra una imagen que subimos nosotros. Ignora URLs externas o que no sean de esta carpeta
    public void eliminar(String url) {
        if (url == null || !url.startsWith(PREFIJO_URL)) {
            return;
        }
        String nombre = url.substring(PREFIJO_URL.length());
        ubicar(nombre).ifPresent(ruta -> {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException e) {
                LOG.warn("No se pudo borrar la imagen " + nombre, e);
            }
        });
    }

    // JPG, PNG o WebP según los primeros bytes del archivo
    private String detectarExtension(Path ruta) {
        byte[] b;
        try (InputStream in = Files.newInputStream(ruta)) {
            b = in.readNBytes(12);
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo leer la imagen subida", e);
        }

        if (b.length >= 3 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xD8 && (b[2] & 0xFF) == 0xFF) {
            return "jpg";
        }
        if (b.length >= 8 && (b[0] & 0xFF) == 0x89 && b[1] == 'P' && b[2] == 'N' && b[3] == 'G'
                && b[4] == 0x0D && b[5] == 0x0A && b[6] == 0x1A && b[7] == 0x0A) {
            return "png";
        }
        if (b.length >= 12 && b[0] == 'R' && b[1] == 'I' && b[2] == 'F' && b[3] == 'F'
                && b[8] == 'W' && b[9] == 'E' && b[10] == 'B' && b[11] == 'P') {
            return "webp";
        }
        throw new BusinessRuleException("Formato no válido. Usa una imagen JPG, PNG o WebP");
    }
}