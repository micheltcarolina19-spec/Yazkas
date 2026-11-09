package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.Desparasitacion;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.DesparasitacionResponse;

import java.util.List;

// Convierte una desparasitación del dominio a la respuesta REST
public final class DesparasitacionRestMapper {

    private DesparasitacionRestMapper() {
    }

    public static DesparasitacionResponse toResponse(Desparasitacion desparasitacion) {
        Mascota mascota = desparasitacion.getMascota();
        return new DesparasitacionResponse(
                desparasitacion.getId(),
                mascota == null ? null : mascota.getId(),
                mascota == null ? null : mascota.getNombre(),
                desparasitacion.getMarca(),
                desparasitacion.getFechaAplicacion(),
                MapperUtils.nombre(desparasitacion.getTipo()),
                desparasitacion.getNumeroLote(),
                desparasitacion.getObservaciones());
    }

    public static List<DesparasitacionResponse> toResponseList(List<Desparasitacion> desparasitaciones) {
        return MapperUtils.lista(desparasitaciones, DesparasitacionRestMapper::toResponse);
    }
}
