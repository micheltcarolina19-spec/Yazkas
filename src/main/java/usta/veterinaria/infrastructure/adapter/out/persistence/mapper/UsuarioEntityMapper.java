package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.Usuario;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.UsuarioEntity;

// Convierte entre Usuario y UsuarioEntity
public final class UsuarioEntityMapper {

    private UsuarioEntityMapper() {
    }

    // Dominio -> entidad
    public static UsuarioEntity toEntity(Usuario usuario) {
        if (usuario == null) {
            return null;
        }

        UsuarioEntity entity = new UsuarioEntity();
        entity.setId(usuario.getId());
        entity.setNombre(usuario.getNombre());
        entity.setApellido(usuario.getApellido());
        entity.setCelular(usuario.getCelular());
        entity.setCorreo(usuario.getCorreo());
        entity.setRol(usuario.getRol());
        entity.setContrasena(usuario.getContrasena());

        return entity;
    }

    // Entidad -> dominio
    public static Usuario toDomain(UsuarioEntity entity) {
        if (entity == null) {
            return null;
        }

        return Usuario.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .apellido(entity.getApellido())
                .celular(entity.getCelular())
                .correo(entity.getCorreo())
                .rol(entity.getRol())
                .contrasena(entity.getContrasena())
                .build();
    }
}