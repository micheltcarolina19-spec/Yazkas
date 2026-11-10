package usta.veterinaria.infrastructure.adapter.in.rest.mapper.mascota;

import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.EdadMascotaResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MapperUtils;

import java.time.Period;
import java.util.List;

// Convierte una mascota del dominio a la respuesta REST (incluye los datos básicos del propietario)
public final class MascotaRestMapper {

    private MascotaRestMapper() {
    }

    public static DesparasitacionResponse.MascotaResponse toResponse(Mascota mascota) {
        Usuario propietario = mascota.getUsuario();
        return new DesparasitacionResponse.MascotaResponse(
                mascota.getId(),
                propietario == null ? null : propietario.getId(),
                nombreCompleto(propietario),
                mascota.getNombre(),
                mascota.getFechaNacimiento(),
                MapperUtils.nombre(mascota.getTipo()),
                mascota.getRaza(),
                MapperUtils.nombre(mascota.getSexo()),
                mascota.getTamano(),
                mascota.getPeso(),
                mascota.getColor(),
                MapperUtils.nombre(mascota.getPelaje()),
                mascota.getObservaciones(),
                mascota.getFotoMascota(),
                mascota.getTemperamento(),
                MapperUtils.nombre(mascota.getNivelAgresividad()),
                mascota.getUsaBozal(),
                mascota.getFobias(),
                mascota.getSociable(),
                MapperUtils.nombre(mascota.getEnergia()));
    }

    public static List<DesparasitacionResponse.MascotaResponse> toResponseList(List<Mascota> mascotas) {
        return MapperUtils.lista(mascotas, MascotaRestMapper::toResponse);
    }

    // La edad la calcula el caso de uso (Period); aquí solo se separa en años, meses y días
    public static EdadMascotaResponse toEdadResponse(Long mascotaId, Period edad) {
        if (edad == null) {
            return new EdadMascotaResponse(mascotaId, null, null, null);
        }
        return new EdadMascotaResponse(mascotaId, edad.getYears(), edad.getMonths(), edad.getDays());
    }

    private static String nombreCompleto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido();
        return (nombre + " " + apellido).trim();
    }
}
