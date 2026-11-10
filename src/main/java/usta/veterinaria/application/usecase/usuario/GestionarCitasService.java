package usta.veterinaria.application.usecase.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.out.usuario.AgendamientoCitaRepository;
import usta.veterinaria.application.port.out.admin.BitacoraServicioRepository;
import usta.veterinaria.application.port.out.usuario.EstimadorDuracionCitaPort;
import usta.veterinaria.application.port.out.mascota.MascotaCitaRepository;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.usuario.CitaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;

@ApplicationScoped
public class GestionarCitasService implements GestionarCitasUseCase {

    private final CitaService citaService = new CitaService();

    private final AgendamientoCitaRepository agendamientoRepository;
    private final MascotaCitaRepository mascotaCitaRepository;
    private final BitacoraServicioRepository bitacoraRepository;
    private final EstimadorDuracionCitaPort estimador;
    private final GestionarMascotasUseCase mascotas;

    @Inject
    public GestionarCitasService(AgendamientoCitaRepository agendamientoRepository,
                                 MascotaCitaRepository mascotaCitaRepository,
                                 BitacoraServicioRepository bitacoraRepository,
                                 EstimadorDuracionCitaPort estimador,
                                 GestionarMascotasUseCase mascotas) {
        this.agendamientoRepository = agendamientoRepository;
        this.mascotaCitaRepository = mascotaCitaRepository;
        this.bitacoraRepository = bitacoraRepository;
        this.estimador = estimador;
        this.mascotas = mascotas;
    }

    @Override
    @Transactional
    public List<MascotaCita> agendar(Long usuarioId, List<Long> mascotaIds, LocalDate fechaAgendamiento,
                                     LocalTime horaAgendamiento) {
        Validaciones.requerirId(usuarioId, "id del usuario");

        List<MascotaCita> nuevasCitas = new ArrayList<>();
        if (mascotaIds != null) {
            for (Long mascotaId : mascotaIds) {
                Mascota mascota = mascotas.buscarPorId(mascotaId);
                validarPropietario(mascota, usuarioId);
                citaService.validarDatosParaEstimacion(mascota);

                nuevasCitas.add(MascotaCita.builder()
                        .mascota(mascota)
                        .fechaAgendamiento(fechaAgendamiento)
                        .horaAgendamiento(horaAgendamiento)
                        .duracionCita(estimador.estimar(mascota))
                        .build());
            }
        }

        AgendamientoCita agendamiento = citaService.agendar(nuevasCitas, citasDesde(fechaAgendamiento));

        AgendamientoCita guardado = agendamientoRepository.save(agendamiento);
        nuevasCitas.forEach(cita -> cita.setAgendamientoCita(guardado));
        return mascotaCitaRepository.saveAll(nuevasCitas);
    }

    @Override
    @Transactional
    public List<MascotaCita> reprogramar(Long agendamientoId, LocalDate nuevaFecha, LocalTime nuevaHora) {
        AgendamientoCita agendamiento = buscarPorId(agendamientoId);
        List<MascotaCita> citas = mascotaCitaRepository.findByAgendamientoCitaId(agendamientoId);

        citaService.reprogramar(agendamiento, citas, nuevaFecha, nuevaHora, citasDesde(nuevaFecha));

        agendamientoRepository.save(agendamiento);
        return mascotaCitaRepository.saveAll(citas);
    }

    @Override
    @Transactional
    public AgendamientoCita cancelar(Long agendamientoId) {
        AgendamientoCita agendamiento = buscarPorId(agendamientoId);
        return agendamientoRepository.save(citaService.cancelar(agendamiento));
    }

    @Override
    @Transactional
    public AgendamientoCita completar(Long agendamientoId) {
        AgendamientoCita agendamiento = buscarPorId(agendamientoId);
        List<BitacoraServicio> servicios = bitacoraRepository.findByAgendamientoCitaId(agendamientoId);
        return agendamientoRepository.save(citaService.completar(agendamiento, servicios));
    }

    @Override
    public LocalDateTime siguienteEspacioDisponible(Long mascotaId, LocalDateTime horaDeseada) {
        Mascota mascota = mascotas.buscarPorId(mascotaId);
        citaService.validarDatosParaEstimacion(mascota);

        List<MascotaCita> existentes = horaDeseada == null ? List.of() : citasDesde(horaDeseada.toLocalDate());
        return citaService.siguienteEspacioDisponible(horaDeseada, estimador.estimar(mascota), existentes);
    }

    @Override
    public AgendamientoCita buscarPorId(Long agendamientoId) {
        return agendamientoRepository.findById(Validaciones.requerirId(agendamientoId, "id del agendamiento"))
                .orElseThrow(() -> new ResourceNotFoundException(
                        "No existe el agendamiento con id " + agendamientoId));
    }

    @Override
    public List<MascotaCita> listarCitasDelAgendamiento(Long agendamientoId) {
        buscarPorId(agendamientoId);
        return mascotaCitaRepository.findByAgendamientoCitaId(agendamientoId);
    }

    @Override
    public List<MascotaCita> listarCitasPorMascota(Long mascotaId) {
        return mascotaCitaRepository.findByMascotaId(Validaciones.requerirId(mascotaId, "id de la mascota"));
    }

    @Override
    public List<MascotaCita> listarCitasPorRango(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return mascotaCitaRepository.findByFechaAgendamientoBetween(inicio, fin);
    }

    // Una cita puede terminar después de medianoche, así que se consulta desde el día anterior.
    // Si no hay fecha se devuelve una lista vacía y el dominio responde que la fecha es obligatoria.
    private List<MascotaCita> citasDesde(LocalDate fecha) {
        return fecha == null ? List.of() : mascotaCitaRepository.findByFechaAgendamientoDesde(fecha.minusDays(1));
    }

    // Todas las mascotas de una misma cita deben pertenecer al usuario que agenda
    private void validarPropietario(Mascota mascota, Long usuarioId) {
        if (mascota.getUsuario() == null || !usuarioId.equals(mascota.getUsuario().getId())) {
            throw new BusinessRuleException("La mascota " + mascota.getNombre() + " no pertenece al usuario indicado");
        }
    }
}
