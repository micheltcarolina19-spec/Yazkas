package usta.veterinaria.application.usecase.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.in.admin.GestionarBitacoraServicioUseCase;
import usta.veterinaria.application.port.in.mascota.GestionarSaludMascotaUseCase;
import usta.veterinaria.application.port.in.usuario.GestionarCitasUseCase;
import usta.veterinaria.application.port.out.admin.BitacoraServicioRepository;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.TipoBano;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.service.admin.BitacoraServicioService;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

@ApplicationScoped
public class GestionarBitacoraServicioService implements GestionarBitacoraServicioUseCase {

    private final BitacoraServicioService bitacoraService = new BitacoraServicioService();

    private final BitacoraServicioRepository bitacoraRepository;
    private final GestionarCitasUseCase citas;
    private final GestionarSaludMascotaUseCase salud;

    @Inject
    public GestionarBitacoraServicioService(BitacoraServicioRepository bitacoraRepository,
                                            GestionarCitasUseCase citas,
                                            GestionarSaludMascotaUseCase salud) {
        this.bitacoraRepository = bitacoraRepository;
        this.citas = citas;
        this.salud = salud;
    }

    @Override
    @Transactional
    public BitacoraServicio registrar(Long agendamientoId, String tipoServicio, String fotoSalida,
                                      TipoBano tipoBano, BigDecimal precioSubtotal,
                                      String observacionesPeluqueria) {
        AgendamientoCita agendamiento = citas.buscarPorId(agendamientoId);

        BitacoraServicio servicio = BitacoraServicio.builder()
                .tipoServicio(tipoServicio)
                .fotoSalida(fotoSalida)
                .tipoBano(tipoBano)
                .precioSubtotal(precioSubtotal)
                .observacionesPeluqueria(observacionesPeluqueria)
                .build();

        return bitacoraRepository.save(bitacoraService.registrar(agendamiento, servicio));
    }

    @Override
    @Transactional
    public BitacoraServicio registrarParaMascota(Long agendamientoId, Long mascotaCitaId, String tipoServicio,
                                                 String fotoSalida, TipoBano tipoBano, BigDecimal precioSubtotal,
                                                 String observacionesPeluqueria,
                                                 Vacunacion vacunacion, Desparasitacion desparasitacion) {
        AgendamientoCita agendamiento = citas.buscarPorId(agendamientoId);
        List<MascotaCita> mascotasDeLaCita = citas.listarCitasDelAgendamiento(agendamientoId);

        MascotaCita mascotaCita = mascotasDeLaCita.stream()
                .filter(c -> Objects.equals(c.getId(), mascotaCitaId))
                .findFirst()
                .orElseThrow(() -> new BusinessRuleException("La mascota no pertenece a esta cita"));

        // Si la mascota ya tenía bitácora se actualiza (mismo id) en lugar de crear otra
        BitacoraServicio existente = bitacoraRepository.findByAgendamientoCitaId(agendamientoId).stream()
                .filter(b -> b.getMascotaCita() != null && Objects.equals(b.getMascotaCita().getId(), mascotaCitaId))
                .findFirst()
                .orElse(null);
        Long vacunaAnterior = existente == null || existente.getVacunacion() == null
                ? null : existente.getVacunacion().getId();
        Long desparasitacionAnterior = existente == null || existente.getDesparasitacion() == null
                ? null : existente.getDesparasitacion().getId();

        BitacoraServicio servicio = BitacoraServicio.builder()
                .id(existente == null ? null : existente.getId())
                .vacunacion(vacunacion)
                .desparasitacion(desparasitacion)
                .tipoServicio(tipoServicio)
                .fotoSalida(fotoSalida)
                .tipoBano(tipoBano)
                .precioSubtotal(precioSubtotal)
                .observacionesPeluqueria(observacionesPeluqueria)
                .build();

        // 1) Reglas de la bitácora (deja vacuna/desparasitación en null si no se marcaron)
        BitacoraServicio validado = bitacoraService.registrarParaMascota(agendamiento, mascotaCita, servicio);

        // 2) Carnet de la mascota: crea o actualiza la vacuna y la desparasitación (mismo registro al editar)
        Long mascotaId = mascotaCita.getMascota().getId();
        if (validado.getVacunacion() != null) {
            Vacunacion v = validado.getVacunacion();
            validado.setVacunacion(salud.guardarVacunacion(vacunaAnterior, mascotaId, v.getNombreVacuna(),
                    v.getFechaAplicacion(), v.getFechaRefuerzo(), v.getNumeroLote(), v.getNumeroDosis(),
                    v.getObservaciones()));
        }
        if (validado.getDesparasitacion() != null) {
            Desparasitacion d = validado.getDesparasitacion();
            validado.setDesparasitacion(salud.guardarDesparasitacion(desparasitacionAnterior, mascotaId, d.getMarca(),
                    d.getFechaAplicacion(), d.getTipo(), d.getNumeroLote(), d.getObservaciones()));
        }

        // 3) Bitácora
        BitacoraServicio guardado = bitacoraRepository.save(validado);

        // 4) Si se desmarcó Vacunación / Desparasitación al editar, se borra el registro que había creado la bitácora
        if (vacunaAnterior != null && validado.getVacunacion() == null) {
            salud.eliminarVacunacion(vacunaAnterior);
        }
        if (desparasitacionAnterior != null && validado.getDesparasitacion() == null) {
            salud.eliminarDesparasitacion(desparasitacionAnterior);
        }

        // Si ya todas las mascotas tienen su precio, la cita se completa y se calcula el total
        List<BitacoraServicio> bitacoras = bitacoraRepository.findByAgendamientoCitaId(agendamientoId);
        if (bitacoraService.todasLasMascotasRegistradas(mascotasDeLaCita, bitacoras)) {
            citas.completar(agendamientoId);
        }
        return guardado;
    }

    @Override
    public List<BitacoraServicio> listarPorAgendamiento(Long agendamientoId) {
        citas.buscarPorId(agendamientoId);
        return bitacoraRepository.findByAgendamientoCitaId(agendamientoId);
    }

    @Override
    public List<BitacoraServicio> listarPorAgendamientos(Collection<Long> agendamientoIds) {
        if (agendamientoIds == null || agendamientoIds.isEmpty()) {
            return List.of();
        }
        return bitacoraRepository.findByAgendamientoCitaIds(agendamientoIds);
    }
}
