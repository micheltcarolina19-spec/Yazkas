package usta.veterinaria.application.usecase.mascota;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarUsuariosUseCase;
import usta.veterinaria.application.port.out.mascota.MascotaRepository;
import usta.veterinaria.application.usecase.MascotaFactory;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.model.mascota.Energia;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.mascota.NivelAgresividad;
import usta.veterinaria.domain.model.mascota.Pelaje;
import usta.veterinaria.domain.model.mascota.Sexo;
import usta.veterinaria.domain.model.mascota.TipoMascota;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.domain.service.macota.MascotaService;
import usta.veterinaria.domain.service.usuario.UsuarioService;

import java.time.LocalDate;
import java.time.Period;
import java.util.List;

@ApplicationScoped
public class GestionarMascotasService implements GestionarMascotasUseCase {

    private final MascotaService mascotaService = new MascotaService();
    private final UsuarioService usuarioService = new UsuarioService();

    private final MascotaRepository mascotaRepository;
    private final GestionarUsuariosUseCase usuarios;

    @Inject
    public GestionarMascotasService(MascotaRepository mascotaRepository, GestionarUsuariosUseCase usuarios) {
        this.mascotaRepository = mascotaRepository;
        this.usuarios = usuarios;
    }

    @Override
    @Transactional
    public Mascota registrar(Long usuarioId, String nombre, LocalDate fechaNacimiento, TipoMascota tipo,
                             String raza, Sexo sexo, Float tamano, Float peso, String color, Pelaje pelaje,
                             String observaciones, String fotoMascota, String temperamento,
                             NivelAgresividad nivelAgresividad, Boolean usaBozal, String fobias,
                             Boolean sociable, Energia energia) {
        Usuario propietario = usuarios.buscarPorId(usuarioId);

        Mascota mascota = MascotaFactory.construir(propietario, nombre, fechaNacimiento, tipo, raza, sexo,
                tamano, peso, color, pelaje, observaciones, fotoMascota, temperamento, nivelAgresividad,
                usaBozal, fobias, sociable, energia);

        return mascotaRepository.save(mascotaService.validarRegistro(mascota));
    }

    @Override
    @Transactional
    public Mascota actualizar(Long id, String nombre, LocalDate fechaNacimiento, TipoMascota tipo, String raza,
                              Sexo sexo, Float tamano, Float peso, String color, Pelaje pelaje,
                              String observaciones, String fotoMascota, String temperamento,
                              NivelAgresividad nivelAgresividad, Boolean usaBozal, String fobias,
                              Boolean sociable, Energia energia) {
        Mascota existente = buscarPorId(id);

        // El propietario no cambia al actualizar
        Mascota actualizada = MascotaFactory.construir(existente.getUsuario(), nombre, fechaNacimiento, tipo,
                raza, sexo, tamano, peso, color, pelaje, observaciones, fotoMascota, temperamento,
                nivelAgresividad, usaBozal, fobias, sociable, energia);
        actualizada.setId(existente.getId());

        return mascotaRepository.save(mascotaService.validarRegistro(actualizada));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Mascota mascota = buscarPorId(id);
        Usuario propietario = mascota.getUsuario();

        long mascotasDelPropietario = propietario == null || propietario.getId() == null
                ? 0
                : mascotaRepository.countByUsuarioId(propietario.getId());
        usuarioService.validarEliminarMascota(propietario, mascotasDelPropietario);

        mascotaRepository.deleteById(mascota.getId());
    }

    @Override
    public Mascota buscarPorId(Long id) {
        return mascotaRepository.findById(Validaciones.requerirId(id, "id de la mascota"))
                .orElseThrow(() -> new ResourceNotFoundException("No existe la mascota con id " + id));
    }

    @Override
    public List<Mascota> listar() {
        return mascotaRepository.findAll();
    }

    @Override
    public List<Mascota> listarPorUsuario(Long usuarioId) {
        return mascotaRepository.findByUsuarioId(Validaciones.requerirId(usuarioId, "id del usuario"));
    }

    @Override
    public Period calcularEdad(Long id) {
        return mascotaService.edad(buscarPorId(id));
    }
}
