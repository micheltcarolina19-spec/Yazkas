package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.mascota;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.mascota.MascotaRepository;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.MascotaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.UsuarioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.mascota.MascotaEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.mascota.MascotaJpaRepository;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.usuario.UsuarioJpaRepository;

import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto MascotaRepository: guarda/lee las mascotas en MySQL
@ApplicationScoped
public class JpaMascotaRepository implements MascotaRepository {

    @Inject
    MascotaJpaRepository repository;

    @Inject
    UsuarioJpaRepository usuarioRepository;

    // Guarda la mascota (inserta o actualiza) resolviendo el propietario por su id
    @Override
    @Transactional
    public Mascota save(Mascota mascota) {
        UsuarioEntity usuarioEntity = usuarioRepository.findByIdOptional(mascota.getUsuario().getId())
                .orElseThrow(() -> new IllegalStateException(
                        "No existe el usuario con id " + mascota.getUsuario().getId()));
        MascotaEntity saved = repository.getEntityManager()
                .merge(MascotaEntityMapper.toEntity(mascota, usuarioEntity));
        return MascotaEntityMapper.toDomain(saved);
    }

    // Busca una mascota por id
    @Override
    public Optional<Mascota> findById(Long id) {
        return repository.findByIdConUsuario(id).map(MascotaEntityMapper::toDomain);
    }
    // Devuelve todas las mascotas
    @Override
    public List<Mascota> findAll() {
        return repository.findAllConUsuario().stream().map(MascotaEntityMapper::toDomain).toList();
    }

    @Override
    public List<Mascota> findByUsuarioId(Long usuarioId) {
        return repository.findByUsuarioId(usuarioId).stream()
                .map(MascotaEntityMapper::toDomain)
                .toList();
    }

    // Cuenta las mascotas de un usuario
    @Override
    public long countByUsuarioId(Long usuarioId) {
        return repository.countByUsuarioId(usuarioId);
    }

    // Elimina la mascota por id
    @Override
    @Transactional
    public void deleteById(Long id) {
        repository.deleteById(id);
    }
}