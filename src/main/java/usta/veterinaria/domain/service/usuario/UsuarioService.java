package usta.veterinaria.domain.service.usuario;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.RolUsuario;
import usta.veterinaria.domain.model.usuario.Usuario;

import java.util.Locale;
import java.util.regex.Pattern;

// Servicio con las reglas de registro de usuarios.
// - Un CLIENTE siempre debe tener al menos una mascota registrada.
// - El correo es único (la autenticación se maneja fuera del dominio).
public class UsuarioService {

    private static final Pattern PATRON_CORREO =
            Pattern.compile("^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$");

    // Celular colombiano: 10 dígitos que empiezan por 3 (se acepta el prefijo 57)
    private static final Pattern PATRON_CELULAR = Pattern.compile("^3\\d{9}$");

    // Deja el correo en minúsculas y sin espacios para compararlo siempre igual
    public String normalizarCorreo(String correo) {
        return correo == null ? null : correo.trim().toLowerCase(Locale.ROOT);
    }

    // Valida los datos del usuario. existentePorCorreo es el usuario que ya tiene ese correo (o null).
    public Usuario validarRegistro(Usuario usuario, Usuario existentePorCorreo) {
        if (usuario == null) {
            throw new BusinessRuleException("El usuario es obligatorio");
        }
        if (usuario.getNombre() == null || usuario.getNombre().isBlank()) {
            throw new BusinessRuleException("El nombre del usuario es obligatorio");
        }
        if (usuario.getApellido() == null || usuario.getApellido().isBlank()) {
            throw new BusinessRuleException("El apellido del usuario es obligatorio");
        }
        if (usuario.getRol() == null) {
            throw new BusinessRuleException("El rol del usuario es obligatorio");
        }

        String celular = normalizarCelular(usuario.getCelular());
        if (!PATRON_CELULAR.matcher(celular).matches()) {
            throw new BusinessRuleException("El celular debe tener 10 dígitos y empezar por 3");
        }

        String correo = normalizarCorreo(usuario.getCorreo());
        if (correo == null || !PATRON_CORREO.matcher(correo).matches()) {
            throw new BusinessRuleException("El correo no es válido");
        }

        if (existentePorCorreo != null
                && (existentePorCorreo.getId() == null || !existentePorCorreo.getId().equals(usuario.getId()))) {
            throw new BusinessRuleException("Ya existe un usuario registrado con el correo " + correo);
        }

        usuario.setNombre(usuario.getNombre().trim());
        usuario.setApellido(usuario.getApellido().trim());
        usuario.setCelular(celular);
        usuario.setCorreo(correo);
        return usuario;
    }

    // Un cliente debe tener al menos una mascota; el administrador no necesita ninguna
    public void validarClienteConMascota(Usuario usuario, long cantidadMascotas) {
        if (usuario == null) {
            throw new BusinessRuleException("El usuario es obligatorio");
        }
        if (usuario.getRol() == RolUsuario.CLIENTE && cantidadMascotas < 1) {
            throw new BusinessRuleException("Un cliente debe tener al menos una mascota registrada");
        }
    }

    // No se puede eliminar la única mascota de un cliente
    public void validarEliminarMascota(Usuario propietario, long cantidadMascotasActuales) {
        if (propietario != null && propietario.getRol() == RolUsuario.CLIENTE && cantidadMascotasActuales <= 1) {
            throw new BusinessRuleException(
                    "No se puede eliminar la única mascota del cliente; debe tener al menos una");
        }
    }

    // Deja solo los dígitos y quita el prefijo 57 de Colombia si viene incluido
    private String normalizarCelular(String celular) {
        if (celular == null) {
            return "";
        }
        String digitos = celular.replaceAll("\\D", "");
        if (digitos.length() == 12 && digitos.startsWith("57")) {
            digitos = digitos.substring(2);
        }
        return digitos;
    }
}
