package usta.veterinaria.infrastructure.adapter.out.persistence.repository;

import io.quarkus.hibernate.orm.panache.PanacheRepository;
import jakarta.enterprise.context.ApplicationScoped;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaEntity;

import java.util.List;
import java.util.Optional;

// Repositorio Panache de mascotas: aquí viven las consultas con nombre.
// Todas traen cargado el propietario (usuario), porque el mapper lo convierte al dominio
// y las reglas de citas validan que la mascota pertenezca a quien agenda.
@ApplicationScoped
public class MascotaJpaRepository implements PanacheRepository<MascotaEntity> {

    private static final String SELECT_CON_USUARIO =
            "select m from MascotaEntity m join fetch m.usuario u ";

    // Mascota por id, con su propietario
    public Optional<MascotaEntity> findByIdConUsuario(Long id) {
        return find(SELECT_CON_USUARIO + "where m.id = ?1", id).firstResultOptional();
    }

    // Todas las mascotas, con su propietario, ordenadas por nombre
    public List<MascotaEntity> findAllConUsuario() {
        return list(SELECT_CON_USUARIO + "order by m.nombre");
    }

    // Mascotas de un usuario, ordenadas por nombre
    public List<MascotaEntity> findByUsuarioId(Long usuarioId) {
        return list(SELECT_CON_USUARIO + "where u.id = ?1 order by m.nombre", usuarioId);
    }

    // Cantidad de mascotas de un usuario
    public long countByUsuarioId(Long usuarioId) {
        return count("usuario.id", usuarioId);
    }
}