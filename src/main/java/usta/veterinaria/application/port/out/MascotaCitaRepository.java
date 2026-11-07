package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.MascotaCita;

import java.time.LocalDate;
import java.util.List;

// Puerto secundario: persistencia de las citas de cada mascota.
// Las citas que devuelve deben traer cargados la mascota y el agendamiento (con su estado),
// porque las reglas del dominio los usan para validar cupos y calcular ingresos.
public interface MascotaCitaRepository {

    List<MascotaCita> saveAll(List<MascotaCita> citas);

    List<MascotaCita> findByAgendamientoCitaId(Long agendamientoId);

    List<MascotaCita> findByMascotaId(Long mascotaId);

    // Citas con fecha entre inicio y fin (ambas inclusive)
    List<MascotaCita> findByFechaAgendamientoBetween(LocalDate inicio, LocalDate fin);

    // Citas con fecha igual o posterior a la indicada (para validar cupos hacia adelante)
    List<MascotaCita> findByFechaAgendamientoDesde(LocalDate desde);
}
