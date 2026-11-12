package usta.veterinaria.infrastructure.adapter.in.rest.mapper.usuario;

import usta.veterinaria.domain.model.usuario.AgendamientoCita;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.domain.model.usuario.Usuario;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.DesparasitacionResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.EspacioDisponibleResponse;
import usta.veterinaria.infrastructure.adapter.in.rest.mapper.MapperUtils;

import java.time.LocalDateTime;
import java.util.List;

// Convierte la cita de una mascota del dominio a la respuesta REST.
// No incluye el precio, solo la duración (ALTA, MEDIA o BAJA).
public final class MascotaCitaRestMapper {

    private MascotaCitaRestMapper() {
    }

    public static DesparasitacionResponse.MascotaCitaResponse toResponse(MascotaCita cita) {
        AgendamientoCita agendamiento = cita.getAgendamientoCita();
        Mascota mascota = cita.getMascota();
        return new DesparasitacionResponse.MascotaCitaResponse(
                cita.getId(),
                agendamiento == null ? null : agendamiento.getId(),
                agendamiento == null ? null : MapperUtils.nombre(agendamiento.getEstado()),
                mascota == null ? null : mascota.getId(),
                mascota == null ? null : mascota.getNombre(),
                mascota == null ? null : mascota.getRaza(),
                mascota == null ? null : mascota.getFotoMascota(),
                mascota == null ? null : nombreCompleto(mascota.getUsuario()),
                cita.getFechaCreacion(),
                cita.getFechaAgendamiento(),
                cita.getHoraAgendamiento(),
                MapperUtils.nombre(cita.getDuracionCita()));
    }

    public static List<DesparasitacionResponse.MascotaCitaResponse> toResponseList(List<MascotaCita> citas) {
        return MapperUtils.lista(citas, MascotaCitaRestMapper::toResponse);
    }

    // Arma la respuesta del siguiente espacio disponible que calculó el caso de uso
    public static EspacioDisponibleResponse toEspacioDisponibleResponse(Long mascotaId,
                                                                        LocalDateTime horaDeseada,
                                                                        LocalDateTime siguienteEspacio) {
        return new EspacioDisponibleResponse(mascotaId, horaDeseada, siguienteEspacio);
    }

    private static String nombreCompleto(Usuario usuario) {
        if (usuario == null) {
            return null;
        }
        String nombre = usuario.getNombre() == null ? "" : usuario.getNombre().trim();
        String apellido = usuario.getApellido() == null ? "" : usuario.getApellido().trim();
        String completo = (nombre + " " + apellido).trim();
        return completo.isEmpty() ? null : completo;
    }
}