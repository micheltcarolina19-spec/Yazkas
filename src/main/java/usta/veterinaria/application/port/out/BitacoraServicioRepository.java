package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.BitacoraServicio;

import java.util.List;

// Puerto secundario: persistencia de la bitácora de servicios de una cita
public interface BitacoraServicioRepository {

    BitacoraServicio save(BitacoraServicio servicio);

    List<BitacoraServicio> findByAgendamientoCitaId(Long agendamientoId);
}
