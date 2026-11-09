package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.AgendamientoCita;
import usta.veterinaria.domain.model.BitacoraServicio;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.BitacoraServicioResponse;

import java.util.List;

// Convierte un servicio de la bitácora del dominio a la respuesta REST
public final class BitacoraServicioRestMapper {

    private BitacoraServicioRestMapper() {
    }

    public static BitacoraServicioResponse toResponse(BitacoraServicio servicio) {
        AgendamientoCita agendamiento = servicio.getAgendamientoCita();
        return new BitacoraServicioResponse(
                servicio.getId(),
                agendamiento == null ? null : agendamiento.getId(),
                servicio.getTipoServicio(),
                servicio.getFotoSalida(),
                MapperUtils.nombre(servicio.getTipoBano()),
                servicio.getPrecioSubtotal(),
                servicio.getObservacionesPeluqueria());
    }

    public static List<BitacoraServicioResponse> toResponseList(List<BitacoraServicio> servicios) {
        return MapperUtils.lista(servicios, BitacoraServicioRestMapper::toResponse);
    }
}
