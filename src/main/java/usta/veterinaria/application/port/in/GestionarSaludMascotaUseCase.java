package usta.veterinaria.application.port.in;

import usta.veterinaria.domain.model.Desparasitacion;
import usta.veterinaria.domain.model.TipoDesparasitacion;
import usta.veterinaria.domain.model.Vacunacion;

import java.time.LocalDate;
import java.util.List;

// Puerto primario: carnet de vacunas y desparasitaciones
public interface GestionarSaludMascotaUseCase {

    Vacunacion registrarVacunacion(Long mascotaId, String nombreVacuna, LocalDate fechaAplicacion,
                                   LocalDate fechaRefuerzo, String numeroLote, Integer numeroDosis,
                                   String observaciones);

    Desparasitacion registrarDesparasitacion(Long mascotaId, String marca, LocalDate fechaAplicacion,
                                             TipoDesparasitacion tipo, String numeroLote, String observaciones);

    List<Vacunacion> listarVacunaciones(Long mascotaId);

    List<Desparasitacion> listarDesparasitaciones(Long mascotaId);

    // Vacunas de todas las mascotas cuyo refuerzo vence dentro de los próximos días
    List<Vacunacion> refuerzosProximos(int dias);

    List<Vacunacion> refuerzosVencidos();
}
