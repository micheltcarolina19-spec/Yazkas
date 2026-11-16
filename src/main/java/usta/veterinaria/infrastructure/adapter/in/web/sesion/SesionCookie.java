package usta.veterinaria.infrastructure.adapter.in.web.sesion;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.core.NewCookie;
import org.eclipse.microprofile.config.inject.ConfigProperty;
import usta.veterinaria.domain.model.usuario.RolUsuario;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.GeneralSecurityException;
import java.security.MessageDigest;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;

import static java.nio.charset.StandardCharsets.UTF_8;

/**
 * Crea, lee y elimina la cookie de sesión de Yazka's.
 * Formato: base64(usuarioId|ROL|expiracion) + "." + base64(firma HMAC-SHA256)
 * La firma impide que alguien cambie el id de la cookie para hacerse pasar por otro usuario.
 */
@ApplicationScoped
public class SesionCookie {

    public static final String NOMBRE = "yazkas_sesion";
    private static final Duration DURACION = Duration.ofHours(8);
    private static final Base64.Encoder CODIFICADOR = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder DECODIFICADOR = Base64.getUrlDecoder();

    @ConfigProperty(name = "yazkas.sesion.secreto")
    String secreto;

    @ConfigProperty(name = "yazkas.sesion.segura", defaultValue = "false")
    boolean segura;

    public record DatosSesion(Long usuarioId, RolUsuario rol) {
    }

    // Cookie que se envía al iniciar sesión
    public NewCookie crear(Long usuarioId, RolUsuario rol) {
        long expira = Instant.now().plus(DURACION).getEpochSecond();
        String datos = usuarioId + "|" + rol.name() + "|" + expira;
        String valor = CODIFICADOR.encodeToString(datos.getBytes(UTF_8)) + "." + CODIFICADOR.encodeToString(firmar(datos));
        return construir(valor, (int) DURACION.toSeconds());
    }

    // Cookie vacía y vencida para cerrar sesión
    public NewCookie cerrar() {
        return construir("", 0);
    }

    // Valida la cookie: firma correcta, formato correcto y sin vencer
    public Optional<DatosSesion> leer(String valor) {
        if (valor == null || valor.isBlank()) {
            return Optional.empty();
        }
        try {
            String[] partes = valor.split("\\.");
            if (partes.length != 2) {
                return Optional.empty();
            }
            String datos = new String(DECODIFICADOR.decode(partes[0]), UTF_8);
            byte[] firma = DECODIFICADOR.decode(partes[1]);
            if (!MessageDigest.isEqual(firma, firmar(datos))) {
                return Optional.empty();
            }

            String[] campos = datos.split("\\|");
            if (campos.length != 3 || Instant.now().getEpochSecond() > Long.parseLong(campos[2])) {
                return Optional.empty();
            }
            return Optional.of(new DatosSesion(Long.valueOf(campos[0]), RolUsuario.valueOf(campos[1])));

        } catch (IllegalArgumentException e) {
            // Base64 inválido, número mal formado o rol inexistente
            return Optional.empty();
        }
    }

    private NewCookie construir(String valor, int maxAge) {
        return new NewCookie.Builder(NOMBRE)
                .value(valor)
                .path("/")
                .maxAge(maxAge)
                .httpOnly(true)
                .secure(segura)
                .sameSite(NewCookie.SameSite.LAX)
                .build();
    }

    private byte[] firmar(String datos) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            mac.init(new SecretKeySpec(secreto.getBytes(UTF_8), "HmacSHA256"));
            return mac.doFinal(datos.getBytes(UTF_8));
        } catch (GeneralSecurityException e) {
            throw new IllegalStateException("No se pudo firmar la sesión", e);
        }
    }
}
