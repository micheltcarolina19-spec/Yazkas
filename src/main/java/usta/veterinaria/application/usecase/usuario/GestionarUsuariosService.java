
package usta.veterinaria.application.usecase.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;

import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.usuario.GestionarUsuariosUseCase;
import usta.veterinaria.application.port.out.mascota.MascotaRepository;
import usta.veterinaria.application.port.out.usuario.PasswordHasher;
import usta.veterinaria.application.port.out.usuario.UsuarioRepository;
import usta.veterinaria.application.usecase.MascotaFactory;
import usta.veterinaria.application.usecase.Validaciones;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;
import usta.veterinaria.domain.model.usuario.RolUsuario;
import usta.veterinaria.domain.model.usuario.Usuario;

import usta.veterinaria.domain.service.macota.MascotaService;
import usta.veterinaria.domain.service.usuario.UsuarioService;

import java.time.LocalDate;
import java.util.List;

@ApplicationScoped
public class GestionarUsuariosService implements GestionarUsuariosUseCase {

    private final UsuarioService usuarioService = new UsuarioService();
    private final MascotaService mascotaService = new MascotaService();

    private final UsuarioRepository usuarioRepository;
    private final MascotaRepository mascotaRepository;
    private final PasswordHasher passwordHasher;

    @Inject
    public GestionarUsuariosService(
            UsuarioRepository usuarioRepository,
            MascotaRepository mascotaRepository,
            PasswordHasher passwordHasher) {

        this.usuarioRepository = usuarioRepository;
        this.mascotaRepository = mascotaRepository;
        this.passwordHasher = passwordHasher;
    }

    // REGISTRAR CLIENTE CON SU PRIMERA MASCOTA
    @Override
    @Transactional
    public Usuario registrarCliente(
            String nombre,
            String apellido,
            String celular,
            String correo,
            String contrasena,
            String nombreMascota,
            LocalDate fechaNacimientoMascota,
            TipoMascota tipoMascota,
            String razaMascota,
            Sexo sexoMascota,
            Float tamanoMascota,
            Float pesoMascota,
            String colorMascota,
            Pelaje pelajeMascota,
            String observacionesMascota,
            String fotoMascota,
            String temperamentoMascota,
            NivelAgresividad nivelAgresividadMascota,
            Boolean usaBozalMascota,
            String fobiasMascota,
            Boolean sociableMascota,
            Energia energiaMascota) {

        Usuario cliente = validarUsuarioNuevo(nombre, apellido, celular, correo, RolUsuario.CLIENTE, contrasena);

        // Primero se guarda el cliente: la mascota necesita el id del propietario para validarse.
        // Si algo falla después, @Transactional revierte también al usuario.
        Usuario guardado = usuarioRepository.save(cliente);

        Mascota mascota = MascotaFactory.construir(guardado, nombreMascota, fechaNacimientoMascota, tipoMascota,
                razaMascota, sexoMascota, tamanoMascota, pesoMascota, colorMascota, pelajeMascota,
                observacionesMascota, fotoMascota, temperamentoMascota, nivelAgresividadMascota,
                usaBozalMascota, fobiasMascota, sociableMascota, energiaMascota);

        usuarioService.validarClienteConMascota(guardado, 1);
        mascotaRepository.save(mascotaService.validarRegistro(mascota));

        return guardado;
    }

    // REGISTRAR ADMINISTRADOR
    @Override
    @Transactional
    public Usuario registrarAdministrador(
            String nombre,
            String apellido,
            String celular,
            String correo,
            String contrasena) {

        Usuario administrador = validarUsuarioNuevo(
                nombre,
                apellido,
                celular,
                correo,
                RolUsuario.ADMIN,
                contrasena
        );

        return usuarioRepository.save(administrador);
    }

    // ACTUALIZAR USUARIO
    @Override
    @Transactional
    public Usuario actualizar(
            Long id,
            String nombre,
            String apellido,
            String celular,
            String correo) {

        Usuario existente = buscarPorId(id);

        Usuario actualizado = Usuario.builder()
                .id(existente.getId())
                .nombre(nombre)
                .apellido(apellido)
                .celular(celular)
                .correo(correo)
                .rol(existente.getRol())
                // Conservar la contraseña cifrada existente.
                .contrasena(existente.getContrasena())
                .build();

        usuarioService.validarRegistro(
                actualizado,
                buscarExistentePorCorreo(correo)
        );

        return usuarioRepository.save(actualizado);
    }

    // BUSCAR USUARIO POR ID
    @Override
    public Usuario buscarPorId(Long id) {

        return usuarioRepository.findById(
                Validaciones.requerirId(id, "id del usuario")
        ).orElseThrow(() ->
                new ResourceNotFoundException(
                        "No existe el usuario con id " + id
                )
        );
    }

    // BUSCAR USUARIO POR CORREO
    @Override
    public Usuario buscarPorCorreo(String correo) {

        String correoNormalizado =
                usuarioService.normalizarCorreo(correo);

        if (correoNormalizado == null || correoNormalizado.isBlank()) {
            throw new ResourceNotFoundException(
                    "No existe un usuario con el correo indicado"
            );
        }

        return usuarioRepository.findByCorreo(correoNormalizado)
                .orElseThrow(() ->
                        new ResourceNotFoundException(
                                "No existe un usuario con el correo "
                                        + correoNormalizado
                        )
                );
    }

    // LISTAR USUARIOS
    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    // VALIDAR Y CONSTRUIR USUARIO NUEVO
    private Usuario validarUsuarioNuevo(
            String nombre,
            String apellido,
            String celular,
            String correo,
            RolUsuario rol,
            String contrasena) {

        // Validar contraseña obligatoria.
        if (contrasena == null || contrasena.isBlank()) {
            throw new BusinessRuleException(
                    "La contraseña es obligatoria"
            );
        }

        // Validar longitud mínima.
        if (contrasena.length() < 8) {
            throw new BusinessRuleException(
                    "La contraseña debe tener al menos 8 caracteres"
            );
        }

        // Construir usuario y cifrar contraseña.
        Usuario usuario = Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .celular(celular)
                .correo(correo)
                .rol(rol)
                .contrasena(passwordHasher.cifrar(contrasena))
                .build();

        // Validar datos del usuario y correo único.
        return usuarioService.validarRegistro(
                usuario,
                buscarExistentePorCorreo(correo)
        );
    }

    // BUSCAR USUARIO EXISTENTE POR CORREO
    private Usuario buscarExistentePorCorreo(String correo) {

        String correoNormalizado =
                usuarioService.normalizarCorreo(correo);

        if (correoNormalizado == null || correoNormalizado.isBlank()) {
            return null;
        }

        return usuarioRepository.findByCorreo(correoNormalizado)
                .orElse(null);
    }
}
