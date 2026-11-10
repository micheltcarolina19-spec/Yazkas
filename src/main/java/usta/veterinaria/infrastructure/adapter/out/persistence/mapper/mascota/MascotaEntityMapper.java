package usta.veterinaria.infrastructure.adapter.out.persistence.mapper.mascota;

import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.MascotaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.UsuarioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.usuario.UsuarioEntityMapper;

// Convierte entre Mascota y MascotaEntity
public final class MascotaEntityMapper {

    private MascotaEntityMapper() {
    }

    // Dominio -> entidad (crea una referencia al usuario solo con el id)
    public static MascotaEntity toEntity(Mascota mascota) {
        if (mascota == null) {
            return null;
        }

        UsuarioEntity usuario = null;
        if (mascota.getUsuario() != null) {
            usuario = new UsuarioEntity();
            usuario.setId(mascota.getUsuario().getId());
        }

        return toEntity(mascota, usuario);
    }

    // Dominio -> entidad (usa el UsuarioEntity ya resuelto/gestionado por JPA)
    public static MascotaEntity toEntity(Mascota mascota, UsuarioEntity usuario) {
        if (mascota == null) {
            return null;
        }

        MascotaEntity entity = new MascotaEntity();
        entity.setId(mascota.getId());
        entity.setNombre(mascota.getNombre());
        entity.setRaza(mascota.getRaza());
        entity.setColor(mascota.getColor());
        entity.setObservaciones(mascota.getObservaciones());
        entity.setFotoMascota(mascota.getFotoMascota());
        entity.setTemperamento(mascota.getTemperamento());
        entity.setFobias(mascota.getFobias());
        entity.setFechaNacimiento(mascota.getFechaNacimiento());
        entity.setTipo(mascota.getTipo());
        entity.setSexo(mascota.getSexo());
        entity.setTamano(mascota.getTamano());
        entity.setPeso(mascota.getPeso());
        entity.setPelaje(mascota.getPelaje());
        entity.setNivelAgresividad(mascota.getNivelAgresividad());
        entity.setUsaBozal(mascota.getUsaBozal());
        entity.setSociable(mascota.getSociable());
        entity.setEnergia(mascota.getEnergia());
        entity.setUsuario(usuario);

        return entity;
    }

    // Entidad -> dominio
    public static Mascota toDomain(MascotaEntity entity) {
        if (entity == null) {
            return null;
        }

        return Mascota.builder()
                .id(entity.getId())
                .nombre(entity.getNombre())
                .raza(entity.getRaza())
                .color(entity.getColor())
                .observaciones(entity.getObservaciones())
                .fotoMascota(entity.getFotoMascota())
                .temperamento(entity.getTemperamento())
                .fobias(entity.getFobias())
                .fechaNacimiento(entity.getFechaNacimiento())
                .tipo(entity.getTipo())
                .sexo(entity.getSexo())
                .tamano(entity.getTamano())
                .peso(entity.getPeso())
                .pelaje(entity.getPelaje())
                .nivelAgresividad(entity.getNivelAgresividad())
                .usaBozal(entity.getUsaBozal())
                .sociable(entity.getSociable())
                .energia(entity.getEnergia())
                .usuario(UsuarioEntityMapper.toDomain(entity.getUsuario()))
                .build();
    }
}