package usta.veterinaria.infrastructure.adapter.out.persistence.repository.mascota;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.MascotaCitaEntity;

import java.time.LocalDate;
import java.util.List;

// Repositorio Panache de citas de mascotas.
// Todas las consultas traen cargados la mascota (con su propietario) y el agendamiento, porque las reglas
// del dominio usan el estado de la cita, el precio total y el id de la mascota.
@ApplicationScoped
public class MascotaCitaJpaRepository implements PanacheRepository<MascotaCitaEntity> {

    private static final String SELECT_CON_RELACIONES =
            "select c from MascotaCitaEntity c "
                    + "join fetch c.mascota m "
                    + "join fetch m.usuario "
                    + "join fetch c.agendamientoCita a ";

    // Citas de un agendamiento
    public List<MascotaCitaEntity> findByAgendamientoCitaId(Long agendamientoId) {
        return list(SELECT_CON_RELACIONES + "where a.id = ?1", agendamientoId);
    }

    // Citas de una mascota, de la más reciente a la más antigua
    public List<MascotaCitaEntity> findByMascotaId(Long mascotaId) {
        return list(SELECT_CON_RELACIONES
                + "where m.id = ?1 order by c.fechaAgendamiento desc, c.horaAgendamiento desc", mascotaId);
    }

    // Citas con fecha entre inicio y fin (ambas inclusive)
    public List<MascotaCitaEntity> findByFechaAgendamientoBetween(LocalDate inicio, LocalDate fin) {
        return list(SELECT_CON_RELACIONES
                + "where c.fechaAgendamiento between ?1 and ?2 "
                + "order by c.fechaAgendamiento, c.horaAgendamiento", inicio, fin);
    }

    // Citas con fecha igual o posterior a la indicada
    public List<MascotaCitaEntity> findByFechaAgendamientoDesde(LocalDate desde) {
        return list(SELECT_CON_RELACIONES
                + "where c.fechaAgendamiento >= ?1 "
                + "order by c.fechaAgendamiento, c.horaAgendamiento", desde);
    }
}