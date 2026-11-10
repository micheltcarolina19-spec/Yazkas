package usta.veterinaria.application.usecase.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.usuario.GestionarUsuariosUseCase;
import usta.veterinaria.application.port.out.mascota.MascotaRepository;
import usta.veterinaria.application.port.out.usuario.UsuarioRepository;
import usta.veterinaria.application.usecase.MascotaFactory;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.usuario.RolUsuario;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;
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

    @Inject
    public GestionarUsuariosService(UsuarioRepository usuarioRepository, MascotaRepository mascotaRepository) {
        this.usuarioRepository = usuarioRepository;
        this.mascotaRepository = mascotaRepository;
    }

    @Override
    @Transactional
    public Usuario registrarCliente(String nombre, String apellido, String celular, String correo,
                                    String nombreMascota, LocalDate fechaNacimientoMascota,
                                    TipoMascota tipoMascota, String razaMascota, Sexo sexoMascota,
                                    Float tamanoMascota, Float pesoMascota, String colorMascota,
                                    Pelaje pelajeMascota, String observacionesMascota, String fotoMascota,
                                    String temperamentoMascota, NivelAgresividad nivelAgresividadMascota,
                                    Boolean usaBozalMascota, String fobiasMascota, Boolean sociableMascota,
                                    Energia energiaMascota) {
        Usuario cliente = validarUsuarioNuevo(nombre, apellido, celular, correo, RolUsuario.CLIENTE);

        Mascota mascota = MascotaFactory.construir(cliente, nombreMascota, fechaNacimientoMascota,
                tipoMascota, razaMascota, sexoMascota, tamanoMascota, pesoMascota, colorMascota,
                pelajeMascota, observacionesMascota, fotoMascota, temperamentoMascota,
                nivelAgresividadMascota, usaBozalMascota, fobiasMascota, sociableMascota, energiaMascota);

        // Se valida todo antes de guardar: el cliente nace con su primera mascota
        usuarioService.validarClienteConMascota(cliente, 1);
        mascotaService.validarRegistro(mascota);

        Usuario guardado = usuarioRepository.save(cliente);
        mascota.setUsuario(guardado);
        mascotaRepository.save(mascota);
        return guardado;
    }

    @Override
    @Transactional
    public Usuario registrarAdministrador(String nombre, String apellido, String celular, String correo) {
        Usuario administrador = validarUsuarioNuevo(nombre, apellido, celular, correo, RolUsuario.ADMIN);
        return usuarioRepository.save(administrador);
    }

    @Override
    @Transactional
    public Usuario actualizar(Long id, String nombre, String apellido, String celular, String correo) {
        Usuario existente = buscarPorId(id);

        Usuario actualizado = Usuario.builder()
                .id(existente.getId())
                .nombre(nombre)
                .apellido(apellido)
                .celular(celular)
                .correo(correo)
                .rol(existente.getRol())
                .build();

        usuarioService.validarRegistro(actualizado, buscarExistentePorCorreo(correo));
        return usuarioRepository.save(actualizado);
    }

    @Override
    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(Validaciones.requerirId(id, "id del usuario"))
                .orElseThrow(() -> new ResourceNotFoundException("No existe el usuario con id " + id));
    }

    @Override
    public Usuario buscarPorCorreo(String correo) {
        String correoNormalizado = usuarioService.normalizarCorreo(correo);
        if (correoNormalizado == null || correoNormalizado.isBlank()) {
            throw new ResourceNotFoundException("No existe un usuario con el correo indicado");
        }
        return usuarioRepository.findByCorreo(correoNormalizado)
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe un usuario con el correo " + correoNormalizado));
    }

    @Override
    public List<Usuario> listar() {
        return usuarioRepository.findAll();
    }

    // Arma el usuario nuevo y valida sus datos y que el correo no esté repetido
    private Usuario validarUsuarioNuevo(String nombre, String apellido, String celular, String correo,
                                        RolUsuario rol) {
        Usuario usuario = Usuario.builder()
                .nombre(nombre)
                .apellido(apellido)
                .celular(celular)
                .correo(correo)
                .rol(rol)
                .build();
        return usuarioService.validarRegistro(usuario, buscarExistentePorCorreo(correo));
    }

    private Usuario buscarExistentePorCorreo(String correo) {
        String correoNormalizado = usuarioService.normalizarCorreo(correo);
        if (correoNormalizado == null || correoNormalizado.isBlank()) {
            return null; // el dominio responderá que el correo no es válido
        }
        return usuarioRepository.findByCorreo(correoNormalizado).orElse(null);
    }
}
