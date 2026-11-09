package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.domain.model.Vacunacion;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.VacunacionResponse;

import java.util.List;

// Convierte una vacuna aplicada del dominio a la respuesta REST
public final class VacunacionRestMapper {

    private VacunacionRestMapper() {
    }

    public static VacunacionResponse toResponse(Vacunacion vacunacion) {
        Mascota mascota = vacunacion.getMascota();
        return new VacunacionResponse(
                vacunacion.getId(),
                mascota == null ? null : mascota.getId(),
                mascota == null ? null : mascota.getNombre(),
                vacunacion.getNombreVacuna(),
                vacunacion.getFechaAplicacion(),
                vacunacion.getFechaRefuerzo(),
                vacunacion.getNumeroLote(),
                vacunacion.getNumeroDosis(),
                vacunacion.getObservaciones());
    }

    public static List<VacunacionResponse> toResponseList(List<Vacunacion> vacunaciones) {
        return MapperUtils.lista(vacunaciones, VacunacionRestMapper::toResponse);
    }
}
