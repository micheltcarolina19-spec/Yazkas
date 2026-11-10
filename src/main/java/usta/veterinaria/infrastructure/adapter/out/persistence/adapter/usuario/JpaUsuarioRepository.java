package usta.veterinaria.infrastructure.adapter.out.persistence.adapter.usuario;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.out.usuario.UsuarioRepository;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.UsuarioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.usuario.UsuarioEntityMapper;
import usta.veterinaria.infrastructure.adapter.out.persistence.repository.usuario.UsuarioJpaRepository;

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