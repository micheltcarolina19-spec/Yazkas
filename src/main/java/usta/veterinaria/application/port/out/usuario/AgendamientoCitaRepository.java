package usta.veterinaria.application.port.out.usuario;

import usta.veterinaria.domain.model.usuario.AgendamientoCita;

import java.util.Optional;

// Puerto secundario: persistencia de agendamientos de citas
public interface AgendamientoCitaRepository {

    AgendamientoCita save(AgendamientoCita agendamientoCita);

    Optional<AgendamientoCita> findById(Long id);
}
