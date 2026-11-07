package usta.veterinaria.infrastructure.adapter.out.persistence.adapter;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.UsuarioRepository;
import usta.veterinaria.domain.model.Usuario;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.UsuarioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.UsuarioEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.UsuarioJpaRepository;

import java.util.List;
import java.util.Optional;

// Adaptador JPA del puerto UsuarioRepository: guarda/lee los usuarios en MySQL
@ApplicationScoped
public class JpaUsuarioRepository implements UsuarioRepository {

    @Inject
    UsuarioJpaRepository repository;

    // Guarda el usuario (inserta o actualiza)
    @Override
    @Transactional
    public Usuario save(Usuario usuario) {
        UsuarioEntity saved = repository.getEntityManager()
                .merge(UsuarioEntityMapper.toEntity(usuario));
        return UsuarioEntityMapper.toDomain(saved);
    }

    // Busca un usuario por id
    @Override
    public Optional<Usuario> findById(Long id) {
        return repository.findByIdOptional(id).map(UsuarioEntityMapper::toDomain);
    }

    // Busca un usuario por correo
    @Override
    public Optional<Usuario> findByCorreo(String correo) {
        return repository.findByCorreo(correo).map(UsuarioEntityMapper::toDomain);
    }

    // Devuelve todos los usuarios
    @Override
    public List<Usuario> findAll() {
        return repository.findAllOrdenados().stream()
                .map(UsuarioEntityMapper::toDomain)
                .toList();
    }
}