package usta.veterinaria.infrastructure.adapter.in.rest.mapper;

import usta.veterinaria.domain.model.AgendamientoCita;
import usta.veterinaria.domain.model.Mascota;
import usta.veterinaria.domain.model.MascotaCita;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.EspacioDisponibleResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.MascotaCitaResponse;

import java.time.LocalDateTime;
import java.util.List;

// Convierte la cita de una mascota del dominio a la respuesta REST.
// Es la vista del CLIENTE: no incluye el precio, solo la duración (ALTA, MEDIA o BAJA).
public final class MascotaCitaRestMapper {

    private MascotaCitaRestMapper() {
    }

    public static MascotaCitaResponse toResponse(MascotaCita cita) {
        AgendamientoCita agendamiento = cita.getAgendamientoCita();
        Mascota mascota = cita.getMascota();
        return new MascotaCitaResponse(
                cita.getId(),
                agendamiento == null ? null : agendamiento.getId(),
                agendamiento == null ? null : MapperUtils.nombre(agendamiento.getEstado()),
                mascota == null ? null : mascota.getId(),
                mascota == null ? null : mascota.getNombre(),
                cita.getFechaCreacion(),
                cita.getFechaAgendamiento(),
                cita.getHoraAgendamiento(),
                MapperUtils.nombre(cita.getDuracionCita()));
    }

    public static List<MascotaCitaResponse> toResponseList(List<MascotaCita> citas) {
        return MapperUtils.lista(citas, MascotaCitaRestMapper::toResponse);
    }

    // Arma la respuesta del siguiente espacio disponible que calculó el caso de uso
    public static EspacioDisponibleResponse toEspacioDisponibleResponse(Long mascotaId,
                                                                        LocalDateTime horaDeseada,
                                                                        LocalDateTime siguienteEspacio) {
        return new EspacioDisponibleResponse(mascotaId, horaDeseada, siguienteEspacio);
    }
}
