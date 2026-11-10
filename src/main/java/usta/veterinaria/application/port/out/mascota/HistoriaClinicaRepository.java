package usta.veterinaria.application.port.out.mascota;

import usta.veterinaria.domain.model.mascota.HistoriaClinica;

import java.util.Optional;

// Puerto secundario: persistencia de historias clínicas (una por mascota)
public interface HistoriaClinicaRepository {

    HistoriaClinica save(HistoriaClinica historiaClinica);

    Optional<HistoriaClinica> findByMascotaId(Long mascotaId);
}
