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
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

// Guarda imágenes como ARCHIVOS en disco (carpetas: productos, mascotas). En la base solo queda la URL pública.
// Seguridad: tipo por magic bytes, nombre generado (UUID), carpeta validada contra lista blanca.
@ApplicationScoped
public class ImagenStorage {

    public static final String PRODUCTOS = "productos";
    public static final String MASCOTAS = "mascotas";
    public static final String RAIZ_URL = "/uploads/";
    public static final String PREFIJO_URL = RAIZ_URL + PRODUCTOS + "/"; // compatibilidad con productos

    private static final Set<String> CARPETAS = Set.of(PRODUCTOS, MASCOTAS);
    private static final long MAX_BYTES = 5L * 1024 * 1024; // 5 MB
    private static final Pattern NOMBRE_VALIDO = Pattern.compile("[0-9a-f]{32}\\.(jpg|png|webp)");
    private static final Logger LOG = Logger.getLogger(ImagenStorage.class);

    @ConfigProperty(name = "yazkas.uploads.dir", defaultValue = "uploads")
    String directorioBase;

    private Path raiz;

    @PostConstruct
    void iniciar() {
        raiz = Paths.get(directorioBase).toAbsolutePath().normalize();
        try {
            for (String carpeta : CARPETAS) {
                Files.createDirectories(raiz.resolve(carpeta));
            }
        } catch (IOException e) {
            throw new UncheckedIOException("No se pudo crear la carpeta de imágenes: " + raiz, e);
        }
    }

    public String guardar(FileUpload archivo) {
        return guardar(archivo, PRODUCTOS);
    }

    public String guardar(FileUpload archivo, String carpeta) {
        if (!CARPETAS.contains(carpeta)) {
            throw new BusinessRuleException("Destino de imagen no válido");
        }
        if (archivo == null || archivo.size() == 0) {
            throw new BusinessRuleException("Selecciona una imagen para subir");
        }
        if (archivo.size() > MAX_BYTES) {
            throw new BusinessRuleException("La imagen no puede superar 5 MB");
        }

        String extension = detectarExtension(archivo.uploadedFile());
        String nombre = UUID.randomUUID().toString().replace("-", "") + "." + extension;

        try {
            Files.copy(archivo.uploadedFile(), raiz.resolve(carpeta).resolve(nombre));
        } catch (IOException e) {
            LOG.error("No se pudo guardar la imagen", e);
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
        return RAIZ_URL + carpeta + "/" + nombre;
    }

    public Optional<Path> ubicar(String nombre) {
        return ubicar(PRODUCTOS, nombre);
    }

    public Optional<Path> ubicar(String carpeta, String nombre) {
        if (!CARPETAS.contains(carpeta) || nombre == null || !NOMBRE_VALIDO.matcher(nombre).matches()) {
            return Optional.empty();
        }
        Path base = raiz.resolve(carpeta);
        Path ruta = base.resolve(nombre).normalize();
        return ruta.startsWith(base) && Files.isRegularFile(ruta) ? Optional.of(ruta) : Optional.empty();
    }

    public String tipoDe(String nombre) {
        if (nombre.endsWith(".png")) return "image/png";
        if (nombre.endsWith(".webp")) return "image/webp";
        return "image/jpeg";
    }

    // Borra una imagen propia (/uploads/{carpeta}/{nombre}); ignora URLs externas
    public void eliminar(String url) {
        if (url == null || !url.startsWith(RAIZ_URL)) {
            return;
        }
        String[] partes = url.substring(RAIZ_URL.length()).split("/");
        if (partes.length != 2) {
            return;
        }
        ubicar(partes[0], partes[1]).ifPresent(ruta -> {
            try {
                Files.deleteIfExists(ruta);
            } catch (IOException e) {
                LOG.warn("No se pudo borrar la imagen " + url, e);
            }
        });
    }

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


    // Valida tamaño y formato ANTES de decodificar la imagen
    public void validar(FileUpload archivo) {
        if (archivo == null || archivo.size() == 0) {
            throw new BusinessRuleException("Selecciona una imagen para subir");
        }
        if (archivo.size() > MAX_BYTES) {
            throw new BusinessRuleException("La imagen no puede superar 5 MB");
        }
        detectarExtension(archivo.uploadedFile()); // lanza si no es JPG/PNG/WebP
    }

    // Guarda bytes ya procesados (PNG sin fondo) como archivo .png
    public String guardarPng(byte[] datos, String carpeta) {
        if (!CARPETAS.contains(carpeta)) {
            throw new BusinessRuleException("Destino de imagen no válido");
        }
        if (datos == null || datos.length == 0) {
            throw new BusinessRuleException("No se pudo procesar la imagen");
        }

        String nombre = UUID.randomUUID().toString().replace("-", "") + ".png";
        try {
            Files.write(raiz.resolve(carpeta).resolve(nombre), datos);
        } catch (IOException e) {
            LOG.error("No se pudo guardar la imagen", e);
            throw new UncheckedIOException("No se pudo guardar la imagen", e);
        }
        return RAIZ_URL + carpeta + "/" + nombre;
    }
}