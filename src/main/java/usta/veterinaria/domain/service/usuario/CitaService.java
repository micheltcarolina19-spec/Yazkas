package usta.veterinaria.domain.service.usuario;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.usuario.DuracionCita;
import usta.veterinaria.domain.model.usuario.EstadoCita;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.MonedaColombiana;
import usta.veterinaria.domain.service.RelojColombia;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.temporal.TemporalAdjusters;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.TreeSet;

// Servicio que aplica las reglas de negocio del agendamiento de citas de peluquería.
// - El cliente agenda SIN ver precio (el precio lo pone el administrador al terminar la cita).
// - En un mismo horario se atienden como máximo 2 mascotas al tiempo.
// - La peluquería atiende de 8:30 a 17:30: la cita debe empezar desde las 8:30
//   y TERMINAR a más tardar a las 17:30 (según su duración ALTA, MEDIA o BAJA).
// - Una mascota puede tener varias citas programadas, pero máximo 2 por semana (lunes a domingo).
// - Al terminar la cita (o al final del día) el administrador registra lo cobrado
//   y ese valor pasa al panel de finanzas.
public class CitaService {

    // Número máximo de mascotas que se pueden atender al mismo tiempo
    private static final int CUPOS_SIMULTANEOS = 2;

    // Horario de atención de la peluquería
    private static final LocalTime HORA_APERTURA = LocalTime.of(8, 30);
    private static final LocalTime HORA_CIERRE = LocalTime.of(17, 30);

    // Límite de días hacia adelante para buscar el siguiente espacio disponible
    private static final int MAXIMO_DIAS_BUSQUEDA = 365;

    // Motivos por los que un horario no se puede elegir (pantalla "Agenda tu cita")
    public static final String NO_DISPONIBLE_PASADO = "PASADO";            // la hora ya pasó
    public static final String NO_DISPONIBLE_CIERRE = "CIERRE";            // no alcanza a terminar antes del cierre
    public static final String NO_DISPONIBLE_LLENO = "LLENO";              // ya hay 2 mascotas en atención
    public static final String NO_DISPONIBLE_MASCOTA = "MASCOTA_OCUPADA";  // la mascota ya tiene una cita que se cruza
    public static final String NO_DISPONIBLE_LIMITE = "LIMITE_SEMANAL";    // la mascota ya tiene 2 citas esa semana

    // Máximo de citas (no canceladas) que puede tener una misma mascota en una semana (lunes a domingo)
    public static final int MAXIMO_CITAS_POR_SEMANA = 2;

    private final Clock clock;

    public CitaService() {
        this(RelojColombia.reloj());
    }

    public CitaService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio");
    }

    // Agenda una o varias mascotas (por ejemplo, las 2 mascotas de un mismo dueño) en un agendamiento.
    // citasExistentes: las citas ya guardadas que podrían cruzarse en horario (las trae la capa de aplicación).
    public AgendamientoCita agendar(List<MascotaCita> nuevasCitas, List<MascotaCita> citasExistentes) {
        if (nuevasCitas == null || nuevasCitas.isEmpty()) {
            throw new BusinessRuleException("Debe seleccionar al menos una mascota para la cita");
        }

        // Se valida cada cita contra las existentes y contra las nuevas que ya se revisaron
        List<MascotaCita> ocupadas = citasActivas(citasExistentes);
        for (MascotaCita cita : nuevasCitas) {
            validarDatosCita(cita);
            validarMascotaSinCruce(cita, ocupadas);
            validarCupoDisponible(cita, ocupadas);
            ocupadas.add(cita);
        }

        // El precio queda en $0 hasta que el administrador lo registre al completar la cita
        AgendamientoCita agendamiento = AgendamientoCita.builder()
                .estado(EstadoCita.PROGRAMADA)
                .precioTotal(BigDecimal.ZERO)
                .build();

        LocalDate hoy = LocalDate.now(clock);
        for (MascotaCita cita : nuevasCitas) {
            cita.setAgendamientoCita(agendamiento);
            cita.setFechaCreacion(hoy);
        }
        return agendamiento;
    }

    // Cambia la fecha y hora de todas las mascotas de un agendamiento
    public AgendamientoCita reprogramar(AgendamientoCita agendamiento,
                                        List<MascotaCita> citasDelAgendamiento,
                                        LocalDate nuevaFecha,
                                        LocalTime nuevaHora,
                                        List<MascotaCita> citasExistentes) {
        validarAgendamientoModificable(agendamiento);
        if (citasDelAgendamiento == null || citasDelAgendamiento.isEmpty()) {
            throw new BusinessRuleException("El agendamiento no tiene mascotas asociadas");
        }
        if (nuevaFecha == null || nuevaHora == null) {
            throw new BusinessRuleException("La nueva fecha y hora son obligatorias");
        }

        // Se ignoran las citas del mismo agendamiento porque son las que se están moviendo
        List<MascotaCita> ocupadas = new ArrayList<>();
        for (MascotaCita existente : citasActivas(citasExistentes)) {
            if (!mismoAgendamiento(existente.getAgendamientoCita(), agendamiento)) {
                ocupadas.add(existente);
            }
        }

        // Primero se valida todo con copias; solo si todo es válido se modifica el original
        for (MascotaCita cita : citasDelAgendamiento) {
            MascotaCita propuesta = MascotaCita.builder()
                    .mascota(cita.getMascota())
                    .agendamientoCita(agendamiento)
                    .fechaAgendamiento(nuevaFecha)
                    .horaAgendamiento(nuevaHora)
                    .duracionCita(cita.getDuracionCita())
                    .build();
            validarDatosCita(propuesta);
            validarMascotaSinCruce(propuesta, ocupadas);
            validarCupoDisponible(propuesta, ocupadas);
            ocupadas.add(propuesta);
        }

        for (MascotaCita cita : citasDelAgendamiento) {
            cita.setFechaAgendamiento(nuevaFecha);
            cita.setHoraAgendamiento(nuevaHora);
        }
        agendamiento.setEstado(EstadoCita.REPROGRAMADA);
        return agendamiento;
    }

    // Cancela un agendamiento: libera los cupos porque las citas canceladas no ocupan horario
    public AgendamientoCita cancelar(AgendamientoCita agendamiento) {
        validarAgendamientoModificable(agendamiento);
        agendamiento.setEstado(EstadoCita.CANCELADA);
        return agendamiento;
    }

    // El administrador cierra la cita registrando lo que cobró por cada servicio.
    // El total del agendamiento es la suma de los subtotales y alimenta el panel de finanzas.
    public AgendamientoCita completar(AgendamientoCita agendamiento, List<BitacoraServicio> servicios) {
        validarAgendamientoModificable(agendamiento);
        if (servicios == null || servicios.isEmpty()) {
            throw new BusinessRuleException("Debe registrar al menos un servicio para completar la cita");
        }

        BigDecimal total = BigDecimal.ZERO;
        for (BitacoraServicio servicio : servicios) {
            if (servicio == null || !mismoAgendamiento(servicio.getAgendamientoCita(), agendamiento)) {
                throw new BusinessRuleException("Todos los servicios deben pertenecer a la cita que se va a completar");
            }
            total = total.add(MonedaColombiana.validarMontoPositivo(
                    servicio.getPrecioSubtotal(), "precio del servicio"));
        }

        agendamiento.setPrecioTotal(total);
        agendamiento.setEstado(EstadoCita.COMPLETADA);
        return agendamiento;
    }

    // Busca la primera hora (desde la deseada) en la que hay cupo para una mascota con esa duración,
    // dentro del horario de atención. Si ese día ya no cabe, sigue con el día siguiente desde las 8:30.
    public LocalDateTime siguienteEspacioDisponible(LocalDateTime horaDeseada,
                                                    DuracionCita duracion,
                                                    List<MascotaCita> citasExistentes) {
        if (horaDeseada == null) {
            throw new BusinessRuleException("La hora deseada es obligatoria");
        }
        long minutos = minutosReservados(duracion);
        List<MascotaCita> ocupadas = citasActivas(citasExistentes);

        LocalDate dia = horaDeseada.toLocalDate();
        LocalDateTime desde = horaDeseada.toLocalTime().isBefore(HORA_APERTURA)
                ? dia.atTime(HORA_APERTURA)
                : horaDeseada;

        for (int i = 0; i < MAXIMO_DIAS_BUSQUEDA; i++) {
            LocalDateTime cierre = dia.atTime(HORA_CIERRE);

            TreeSet<LocalDateTime> candidatos = new TreeSet<>();
            candidatos.add(desde);
            for (MascotaCita cita : ocupadas) {
                LocalDateTime fin = fin(cita);
                if (fin.isAfter(desde) && fin.isBefore(cierre)) {
                    candidatos.add(fin);
                }
            }

            for (LocalDateTime inicio : candidatos) {
                if (inicio.plusMinutes(minutos).isAfter(cierre)) {
                    break;
                }
                if (hayCupo(inicio, inicio.plusMinutes(minutos), ocupadas)) {
                    return inicio;
                }
            }

            dia = dia.plusDays(1);
            desde = dia.atTime(HORA_APERTURA);
        }
        throw new BusinessRuleException("No hay espacios disponibles en los próximos "
                + MAXIMO_DIAS_BUSQUEDA + " días");
    }

    // ============================================================================================
    // ===      DURACIÓN DE LA CITA  (LA DECIDE LA IA EN EstimadorDuracionCitaPort)              ===
    // ============================================================================================
    //     - BAJA  : máximo 2 horas      (se reservan 120 min)
    //     - MEDIA : entre 2 y 4 horas   (se reservan 240 min)
    //     - ALTA  : más de 4 horas      (se reservan 300 min)
    // --------------------------------------------------------------------------------------------

    private static final long MINUTOS_DURACION_BAJA = 120;
    private static final long MINUTOS_DURACION_MEDIA = 240;
    private static final long MINUTOS_DURACION_ALTA = 300;

    // Valida que la mascota tenga lo necesario para que la IA estime la duración
    public void validarDatosParaEstimacion(Mascota mascota) {
        if (mascota == null) {
            throw new BusinessRuleException("La mascota es obligatoria para estimar la duración de la cita");
        }
        if (mascota.getTipo() == null) {
            throw new BusinessRuleException("Debe indicar si la mascota es perro o gato");
        }
        if (mascota.getRaza() == null || mascota.getRaza().isBlank()) {
            throw new BusinessRuleException("Debe indicar la raza de la mascota (si no tiene, escriba Criollo)");
        }
        if (mascota.getPelaje() == null) {
            throw new BusinessRuleException("Debe indicar el tipo de pelaje de la mascota");
        }
        if (mascota.getTamano() == null && mascota.getPeso() == null) {
            throw new BusinessRuleException("Debe indicar el tamaño o el peso de la mascota");
        }
    }

    // Convierte la duración (ALTA, MEDIA, BAJA) en los minutos que se bloquean en la agenda
    public long minutosReservados(DuracionCita duracion) {
        if (duracion == null) {
            throw new BusinessRuleException(
                    "La duración de la cita aún no ha sido estimada (debe calcularla la IA antes de agendar)");
        }
        return switch (duracion) {
            case BAJA -> MINUTOS_DURACION_BAJA;
            case MEDIA -> MINUTOS_DURACION_MEDIA;
            case ALTA -> MINUTOS_DURACION_ALTA;
        };
    }

    // ============================================================================================
    // ===                 DISPONIBILIDAD PARA EL CLIENTE (pantalla "Agenda tu cita")              ===
    // ============================================================================================

    // Cuenta las citas no canceladas de la mascota en la semana (lunes a domingo) de la fecha.
    // "ignorar" es el agendamiento que se está reprogramando (no cuenta contra sí mismo).
    public long citasEnLaSemana(Mascota mascota, LocalDate fecha, List<MascotaCita> citasDeLaMascota,
                                AgendamientoCita ignorar) {
        if (mascota == null || fecha == null || citasDeLaMascota == null) {
            return 0;
        }
        LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
        LocalDate domingo = lunes.plusDays(6);
        return citasDeLaMascota.stream()
                .filter(c -> c != null && c.getFechaAgendamiento() != null)
                .filter(c -> mismaMascota(c.getMascota(), mascota))
                .filter(c -> !c.getFechaAgendamiento().isBefore(lunes) && !c.getFechaAgendamiento().isAfter(domingo))
                .filter(c -> c.getAgendamientoCita() == null || c.getAgendamientoCita().getEstado() != EstadoCita.CANCELADA)
                .filter(c -> ignorar == null || !mismoAgendamiento(c.getAgendamientoCita(), ignorar))
                .map(c -> c.getAgendamientoCita() == null ? c : c.getAgendamientoCita().getId())
                .distinct()
                .count();
    }

    // Una mascota puede tener varias citas programadas (sin cruzarse), pero máximo 2 por semana
    public void validarLimiteSemanal(Mascota mascota, LocalDate fecha, List<MascotaCita> citasDeLaMascota,
                                     AgendamientoCita ignorar) {
        if (citasEnLaSemana(mascota, fecha, citasDeLaMascota, ignorar) >= MAXIMO_CITAS_POR_SEMANA) {
            LocalDate lunes = fecha.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY));
            LocalDate domingo = lunes.plusDays(6);
            throw new BusinessRuleException(nombre(mascota) + " ya tiene " + MAXIMO_CITAS_POR_SEMANA
                    + " citas en la semana del " + lunes.getDayOfMonth() + "/" + lunes.getMonthValue()
                    + " al " + domingo.getDayOfMonth() + "/" + domingo.getMonthValue()
                    + ". Elige un día de otra semana.");
        }
    }

    // Horas de inicio que se le ofrecen al cliente: cada hora desde la apertura (8:30, 9:30, ... 16:30)
    public List<LocalTime> horariosDelDia() {
        List<LocalTime> horas = new ArrayList<>();
        for (LocalTime h = HORA_APERTURA; h.isBefore(HORA_CIERRE); h = h.plusHours(1)) {
            horas.add(h);
        }
        return horas;
    }

    // Revisa SIN guardar nada si las citas nuevas (todas a la misma fecha y hora) se pueden agendar.
    // Devuelve null si el horario está libre, o uno de los motivos NO_DISPONIBLE_*.
    public String motivoNoDisponible(List<MascotaCita> nuevasCitas, List<MascotaCita> citasExistentes) {
        return motivoNoDisponible(nuevasCitas, citasExistentes, null);
    }

    // Igual que el anterior, pero ignorando las citas del agendamiento que se está reprogramando
    public String motivoNoDisponible(List<MascotaCita> nuevasCitas, List<MascotaCita> citasExistentes,
                                     AgendamientoCita ignorar) {
        List<MascotaCita> ocupadas = new ArrayList<>();
        for (MascotaCita existente : citasActivas(citasExistentes)) {
            if (ignorar == null || !mismoAgendamiento(existente.getAgendamientoCita(), ignorar)) {
                ocupadas.add(existente);
            }
        }
        for (MascotaCita cita : nuevasCitas) {
            if (!inicio(cita).isAfter(LocalDateTime.now(clock))) {
                return NO_DISPONIBLE_PASADO;
            }
            if (cita.getHoraAgendamiento().isBefore(HORA_APERTURA)
                    || fin(cita).isAfter(cita.getFechaAgendamiento().atTime(HORA_CIERRE))) {
                return NO_DISPONIBLE_CIERRE;
            }
            for (MascotaCita otra : ocupadas) {
                if (mismaMascota(cita.getMascota(), otra.getMascota()) && seCruzan(cita, otra)) {
                    return NO_DISPONIBLE_MASCOTA;
                }
            }
            if (!hayCupo(inicio(cita), fin(cita), ocupadas)) {
                return NO_DISPONIBLE_LLENO;
            }
            ocupadas.add(cita);   // la segunda mascota del mismo dueño también ocupa cupo
        }
        return null;
    }

    // ============================================================================================
    // ===                                   VALIDACIONES                                        ===
    // ============================================================================================

    private void validarDatosCita(MascotaCita cita) {
        if (cita == null || cita.getMascota() == null) {
            throw new BusinessRuleException("Cada cita debe tener una mascota");
        }
        if (cita.getFechaAgendamiento() == null || cita.getHoraAgendamiento() == null) {
            throw new BusinessRuleException("La fecha y hora de la cita son obligatorias");
        }
        minutosReservados(cita.getDuracionCita());

        if (!inicio(cita).isAfter(LocalDateTime.now(clock))) {
            throw new BusinessRuleException("La cita de " + nombre(cita.getMascota()) + " debe ser en una fecha y hora futura");
        }
        validarHorarioAtencion(cita);
    }

    private void validarHorarioAtencion(MascotaCita cita) {
        if (cita.getHoraAgendamiento().isBefore(HORA_APERTURA)) {
            throw new BusinessRuleException("La cita de " + nombre(cita.getMascota())
                    + " debe empezar desde las " + HORA_APERTURA + ". La peluquería atiende de "
                    + HORA_APERTURA + " a " + HORA_CIERRE);
        }
        LocalDateTime cierre = cita.getFechaAgendamiento().atTime(HORA_CIERRE);
        if (fin(cita).isAfter(cierre)) {
            LocalTime ultimaHoraInicio = HORA_CIERRE.minusMinutes(minutosReservados(cita.getDuracionCita()));
            throw new BusinessRuleException("La cita de " + nombre(cita.getMascota())
                    + " (duración " + cita.getDuracionCita() + ") terminaría a las " + fin(cita).toLocalTime()
                    + " y la peluquería cierra a las " + HORA_CIERRE
                    + ". Para esa duración, la última hora para empezar es " + ultimaHoraInicio);
        }
    }

    private void validarMascotaSinCruce(MascotaCita nueva, List<MascotaCita> ocupadas) {
        for (MascotaCita otra : ocupadas) {
            if (mismaMascota(nueva.getMascota(), otra.getMascota()) && seCruzan(nueva, otra)) {
                throw new BusinessRuleException("La mascota " + nombre(nueva.getMascota())
                        + " ya tiene una cita que se cruza en ese horario");
            }
        }
    }

    private void validarCupoDisponible(MascotaCita nueva, List<MascotaCita> ocupadas) {
        if (!hayCupo(inicio(nueva), fin(nueva), ocupadas)) {
            LocalDateTime sugerencia = siguienteEspacioDisponible(inicio(nueva), nueva.getDuracionCita(), ocupadas);
            throw new BusinessRuleException("No hay cupo para " + nombre(nueva.getMascota())
                    + " en ese horario. El siguiente espacio disponible es " + sugerencia);
        }
    }

    // Revisa que en ningún momento del intervalo [inicio, fin) haya ya el máximo de mascotas
    private boolean hayCupo(LocalDateTime inicio, LocalDateTime fin, List<MascotaCita> ocupadas) {
        List<LocalDateTime> puntos = new ArrayList<>();
        puntos.add(inicio);
        for (MascotaCita cita : ocupadas) {
            LocalDateTime inicioOtra = inicio(cita);
            if (inicioOtra.isAfter(inicio) && inicioOtra.isBefore(fin)) {
                puntos.add(inicioOtra);
            }
        }
        for (LocalDateTime punto : puntos) {
            int mascotasEnAtencion = 0;
            for (MascotaCita cita : ocupadas) {
                if (!inicio(cita).isAfter(punto) && fin(cita).isAfter(punto)) {
                    mascotasEnAtencion++;
                }
            }
            if (mascotasEnAtencion >= CUPOS_SIMULTANEOS) {
                return false;
            }
        }
        return true;
    }

    // Solo ocupan horario las citas programadas o reprogramadas
    private List<MascotaCita> citasActivas(List<MascotaCita> citas) {
        List<MascotaCita> activas = new ArrayList<>();
        if (citas == null) {
            return activas;
        }
        for (MascotaCita cita : citas) {
            if (cita == null || cita.getFechaAgendamiento() == null
                    || cita.getHoraAgendamiento() == null || cita.getDuracionCita() == null) {
                continue;
            }
            AgendamientoCita agendamiento = cita.getAgendamientoCita();
            EstadoCita estado = agendamiento == null ? EstadoCita.PROGRAMADA : agendamiento.getEstado();
            if (estado == EstadoCita.PROGRAMADA || estado == EstadoCita.REPROGRAMADA) {
                activas.add(cita);
            }
        }
        return activas;
    }

    private void validarAgendamientoModificable(AgendamientoCita agendamiento) {
        if (agendamiento == null) {
            throw new BusinessRuleException("La cita no existe");
        }
        if (agendamiento.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessRuleException("La cita ya fue cancelada");
        }
        if (agendamiento.getEstado() == EstadoCita.COMPLETADA) {
            throw new BusinessRuleException("La cita ya fue completada");
        }
    }

    private boolean seCruzan(MascotaCita a, MascotaCita b) {
        return inicio(a).isBefore(fin(b)) && inicio(b).isBefore(fin(a));
    }

    private LocalDateTime inicio(MascotaCita cita) {
        return LocalDateTime.of(cita.getFechaAgendamiento(), cita.getHoraAgendamiento());
    }

    private LocalDateTime fin(MascotaCita cita) {
        return inicio(cita).plusMinutes(minutosReservados(cita.getDuracionCita()));
    }

    private boolean mismaMascota(Mascota a, Mascota b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a == b;
    }

    private boolean mismoAgendamiento(AgendamientoCita a, AgendamientoCita b) {
        if (a == null || b == null) {
            return false;
        }
        if (a.getId() != null && b.getId() != null) {
            return a.getId().equals(b.getId());
        }
        return a == b;
    }

    private String nombre(Mascota mascota) {
        return mascota.getNombre() == null ? "la mascota" : mascota.getNombre();
    }
}