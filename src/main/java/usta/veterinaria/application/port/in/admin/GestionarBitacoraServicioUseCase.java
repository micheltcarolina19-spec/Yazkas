package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.TipoBano;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;

import java.math.BigDecimal;
import java.util.Collection;
import java.util.List;

// Puerto primario: bitácora de los servicios prestados en una cita
public interface GestionarBitacoraServicioUseCase {

    BitacoraServicio registrar(Long agendamientoId, String tipoServicio, String fotoSalida, TipoBano tipoBano,
                               BigDecimal precioSubtotal, String observacionesPeluqueria);

    // Guarda (crea o actualiza) la bitácora de UNA mascota de la cita.
    // Cuando todas las mascotas de la cita tienen su precio, la cita queda COMPLETADA automáticamente.
    // Si se marcó Vacunación / Desparasitación, sus datos se guardan en las tablas vacunaciones / desparasitaciones.
    BitacoraServicio registrarParaMascota(Long agendamientoId, Long mascotaCitaId, String tipoServicio,
                                          String fotoSalida, TipoBano tipoBano, BigDecimal precioSubtotal,
                                          String observacionesPeluqueria,
                                          Vacunacion vacunacion, Desparasitacion desparasitacion);

    List<BitacoraServicio> listarPorAgendamiento(Long agendamientoId);

    // Bitácoras de varias citas a la vez (para pintar el calendario con una sola consulta)
    List<BitacoraServicio> listarPorAgendamientos(Collection<Long> agendamientoIds);
}
