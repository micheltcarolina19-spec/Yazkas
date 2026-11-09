package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.Gasto;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.GastoResponse;

import java.util.List;

// Convierte un gasto del dominio a la respuesta REST
public final class GastoRestMapper {

    private GastoRestMapper() {
    }

    public static GastoResponse toResponse(Gasto gasto) {
        return new GastoResponse(
                gasto.getId(),
                gasto.getNombre(),
                gasto.getMonto(),
                gasto.getFechaGasto(),
                MapperUtils.nombre(gasto.getCategoria()));
    }

    public static List<GastoResponse> toResponseList(List<Gasto> gastos) {
        return MapperUtils.lista(gastos, GastoRestMapper::toResponse);
    }
}
