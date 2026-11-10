package usta.veterinaria.application.port.in.usuario;

import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;
import usta.veterinaria.domain.model.usuario.Usuario;

import java.time.LocalDate;
import java.util.List;

// Puerto primario: gestión de usuarios (la autenticación se resuelve fuera de este caso de uso)
public interface GestionarUsuariosUseCase {

    // Registra un cliente junto con su primera mascota (un cliente siempre debe tener al menos una)
    Usuario registrarCliente(String nombre, String apellido, String celular, String correo,
                             String nombreMascota, LocalDate fechaNacimientoMascota, TipoMascota tipoMascota,
                             String razaMascota, Sexo sexoMascota, Float tamanoMascota, Float pesoMascota,
                             String colorMascota, Pelaje pelajeMascota, String observacionesMascota,
                             String fotoMascota, String temperamentoMascota,
                             NivelAgresividad nivelAgresividadMascota, Boolean usaBozalMascota,
                             String fobiasMascota, Boolean sociableMascota, Energia energiaMascota);

    // Registra un administrador (no necesita mascotas)
    Usuario registrarAdministrador(String nombre, String apellido, String celular, String correo);

    // Actualiza los datos personales; el rol no se puede cambiar
    Usuario actualizar(Long id, String nombre, String apellido, String celular, String correo);

    Usuario buscarPorId(Long id);

    // Lo usa el adaptador de autenticación para cargar al usuario que inicia sesión
    Usuario buscarPorCorreo(String correo);

    List<Usuario> listar();
}
