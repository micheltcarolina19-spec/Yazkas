package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.admin.TipoBano;

import java.math.BigDecimal;
import java.util.List;

// Puerto primario: bitácora de los servicios prestados en una cita
public interface GestionarBitacoraServicioUseCase {

    BitacoraServicio registrar(Long agendamientoId, String tipoServicio, String fotoSalida, TipoBano tipoBano,
                               BigDecimal precioSubtotal, String observacionesPeluqueria);

    List<BitacoraServicio> listarPorAgendamiento(Long agendamientoId);
}
