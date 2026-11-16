package usta.veterinaria.application.port.in.mascota;

import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.TipoDesparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;

import java.time.LocalDate;
import java.util.List;

// Puerto primario: carnet de vacunas y desparasitaciones
public interface GestionarSaludMascotaUseCase {

    Vacunacion registrarVacunacion(Long mascotaId, String nombreVacuna, LocalDate fechaAplicacion,
                                   LocalDate fechaRefuerzo, String numeroLote, Integer numeroDosis,
                                   String observaciones);

    Desparasitacion registrarDesparasitacion(Long mascotaId, String marca, LocalDate fechaAplicacion,
                                             TipoDesparasitacion tipo, String numeroLote, String observaciones);

    // Crea (id null) o actualiza (id existente) una vacunación. Lo usa la bitácora de la cita.
    Vacunacion guardarVacunacion(Long id, Long mascotaId, String nombreVacuna, LocalDate fechaAplicacion,
                                 LocalDate fechaRefuerzo, String numeroLote, Integer numeroDosis,
                                 String observaciones);

    // Crea (id null) o actualiza (id existente) una desparasitación. Lo usa la bitácora de la cita.
    Desparasitacion guardarDesparasitacion(Long id, Long mascotaId, String marca, LocalDate fechaAplicacion,
                                           TipoDesparasitacion tipo, String numeroLote, String observaciones);

    void eliminarVacunacion(Long id);

    void eliminarDesparasitacion(Long id);

    List<Vacunacion> listarVacunaciones(Long mascotaId);

    List<Desparasitacion> listarDesparasitaciones(Long mascotaId);

    // Vacunas de todas las mascotas cuyo refuerzo vence dentro de los próximos días
    List<Vacunacion> refuerzosProximos(int dias);

    List<Vacunacion> refuerzosVencidos();
}
