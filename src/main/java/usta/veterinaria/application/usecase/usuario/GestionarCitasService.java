package usta.veterinaria.application.usecase.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.mascota.GestionarMascotasUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.application.port.out.admin.BitacoraServicioRepository;
import usta.veterinaria.application.port.out.mascota.MascotaCitaRepository;
import usta.veterinaria.application.port.out.usuario.AgendamientoCitaRepository;
import usta.veterinaria.application.port.out.usuario.EstimadorDuracionCitaPort;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.usuario.DuracionCita;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.usuario.CitaService;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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
                // Puede tener otras citas programadas, pero máximo 2 en la semana
                citaService.validarLimiteSemanal(mascota, fechaAgendamiento,
                        mascotaCitaRepository.findByMascotaId(mascotaId), null);

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

        // En la semana de la nueva fecha tampoco puede pasar de 2 citas (sin contar esta misma)
        for (MascotaCita cita : citas) {
            if (cita.getMascota() != null && cita.getMascota().getId() != null) {
                citaService.validarLimiteSemanal(cita.getMascota(), nuevaFecha,
                        mascotaCitaRepository.findByMascotaId(cita.getMascota().getId()), agendamiento);
            }
        }

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
    public Map<Long, DuracionCita> estimarDuraciones(Long usuarioId, List<Long> mascotaIds) {
        Validaciones.requerirId(usuarioId, "id del usuario");
        Map<Long, DuracionCita> duraciones = new LinkedHashMap<>();
        if (mascotaIds != null) {
            for (Long mascotaId : mascotaIds) {
                Mascota mascota = mascotas.buscarPorId(mascotaId);
                validarPropietario(mascota, usuarioId);
                citaService.validarDatosParaEstimacion(mascota);
                duraciones.put(mascotaId, estimador.estimar(mascota));
            }
        }
        return duraciones;
    }

    @Override
    public Map<LocalTime, String> disponibilidad(Long usuarioId, List<Long> mascotaIds, LocalDate fecha,
                                                 Long reprogramarAgendamientoId) {
        Validaciones.requerirId(usuarioId, "id del usuario");
        if (fecha == null) {
            throw new BusinessRuleException("La fecha es obligatoria");
        }
        if (mascotaIds == null || mascotaIds.isEmpty()) {
            throw new BusinessRuleException("Debe seleccionar al menos una mascota");
        }

        // Al reprogramar se ignora el propio agendamiento y se conservan sus duraciones
        AgendamientoCita ignorar = null;
        Map<Long, DuracionCita> guardadas = new LinkedHashMap<>();
        if (reprogramarAgendamientoId != null) {
            ignorar = buscarPorId(reprogramarAgendamientoId);
            for (MascotaCita c : mascotaCitaRepository.findByAgendamientoCitaId(reprogramarAgendamientoId)) {
                if (c.getMascota() != null) {
                    guardadas.put(c.getMascota().getId(), c.getDuracionCita());
                }
            }
        }

        List<Mascota> seleccionadas = new ArrayList<>();
        List<DuracionCita> duraciones = new ArrayList<>();
        boolean limiteSemanal = false;
        for (Long mascotaId : mascotaIds) {
            Mascota mascota = mascotas.buscarPorId(mascotaId);
            validarPropietario(mascota, usuarioId);
            citaService.validarDatosParaEstimacion(mascota);
            seleccionadas.add(mascota);
            DuracionCita guardada = guardadas.get(mascotaId);
            duraciones.add(guardada != null ? guardada : estimador.estimar(mascota));
            if (citaService.citasEnLaSemana(mascota, fecha, mascotaCitaRepository.findByMascotaId(mascotaId), ignorar)
                    >= CitaService.MAXIMO_CITAS_POR_SEMANA) {
                limiteSemanal = true;
            }
        }

        List<MascotaCita> existentes = citasDesde(fecha);
        Map<LocalTime, String> horarios = new LinkedHashMap<>();
        for (LocalTime hora : citaService.horariosDelDia()) {
            if (limiteSemanal) {
                horarios.put(hora, CitaService.NO_DISPONIBLE_LIMITE);
                continue;
            }
            List<MascotaCita> prueba = new ArrayList<>();
            for (int i = 0; i < seleccionadas.size(); i++) {
                prueba.add(MascotaCita.builder()
                        .mascota(seleccionadas.get(i))
                        .fechaAgendamiento(fecha)
                        .horaAgendamiento(hora)
                        .duracionCita(duraciones.get(i))
                        .build());
            }
            horarios.put(hora, citaService.motivoNoDisponible(prueba, existentes, ignorar));
        }
        return horarios;
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

    // Una cita puede terminar después de medianoche, así que se consulta desde el día anterior
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