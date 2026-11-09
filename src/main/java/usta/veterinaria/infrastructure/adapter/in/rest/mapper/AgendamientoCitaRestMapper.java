package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.AgendamientoCita;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.AgendamientoCitaResponse;

// Convierte un agendamiento del dominio a la respuesta REST.
// Es la vista del ADMINISTRADOR: incluye el precio total que se registró al completar la cita.
public final class AgendamientoCitaRestMapper {

    private AgendamientoCitaRestMapper() {
    }

    public static AgendamientoCitaResponse toResponse(AgendamientoCita agendamiento) {
        return new AgendamientoCitaResponse(
                agendamiento.getId(),
                MapperUtils.nombre(agendamiento.getEstado()),
                agendamiento.getPrecioTotal());
    }
}
