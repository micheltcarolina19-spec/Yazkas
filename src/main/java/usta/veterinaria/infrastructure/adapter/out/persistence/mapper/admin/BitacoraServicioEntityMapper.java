package usta.veterinaria.infrastructure.adapter.out.persistence.mapper.admin;

import usta.veterinaria.domain.model.admin.BitacoraServicio;
import usta.veterinaria.domain.model.mascota.Desparasitacion;
import usta.veterinaria.domain.model.mascota.Vacunacion;
import usta.veterinaria.domain.model.usuario.MascotaCita;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.DesparasitacionEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.mascota.VacunacionEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.MascotaCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.usuario.AgendamientoCitaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.admin.BitacoraServicioEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.mapper.usuario.AgendamientoCitaEntityMapper;

// Convierte entre BitacoraServicio y BitacoraServicioEntity
public final class BitacoraServicioEntityMapper {

    private BitacoraServicioEntityMapper() {
    }

    // Dominio -> entidad
    public static BitacoraServicioEntity toEntity(BitacoraServicio bitacora) {
        if (bitacora == null) {
            return null;
        }

        BitacoraServicioEntity entity = new BitacoraServicioEntity();
        entity.setId(bitacora.getId());
        entity.setTipoServicio(bitacora.getTipoServicio());
        entity.setFotoSalida(bitacora.getFotoSalida());
        entity.setTipoBano(bitacora.getTipoBano());
        entity.setPrecioSubtotal(bitacora.getPrecioSubtotal());
        entity.setObservacionesPeluqueria(bitacora.getObservacionesPeluqueria());

        if (bitacora.getAgendamientoCita() != null) {
            AgendamientoCitaEntity agendamiento = new AgendamientoCitaEntity();
            agendamiento.setId(bitacora.getAgendamientoCita().getId());
            entity.setAgendamientoCita(agendamiento);
        }

        if (bitacora.getMascotaCita() != null && bitacora.getMascotaCita().getId() != null) {
            MascotaCitaEntity mascotaCita = new MascotaCitaEntity();
            mascotaCita.setId(bitacora.getMascotaCita().getId());
            entity.setMascotaCita(mascotaCita);
        }

        if (bitacora.getVacunacion() != null && bitacora.getVacunacion().getId() != null) {
            VacunacionEntity vacunacion = new VacunacionEntity();
            vacunacion.setId(bitacora.getVacunacion().getId());
            entity.setVacunacion(vacunacion);
        }

        if (bitacora.getDesparasitacion() != null && bitacora.getDesparasitacion().getId() != null) {
            DesparasitacionEntity desparasitacion = new DesparasitacionEntity();
            desparasitacion.setId(bitacora.getDesparasitacion().getId());
            entity.setDesparasitacion(desparasitacion);
        }

        return entity;
    }

    // Entidad -> dominio
    public static BitacoraServicio toDomain(BitacoraServicioEntity entity) {
        if (entity == null) {
            return null;
        }

        return BitacoraServicio.builder()
                .id(entity.getId())
                .tipoServicio(entity.getTipoServicio())
                .fotoSalida(entity.getFotoSalida())
                .tipoBano(entity.getTipoBano())
                .precioSubtotal(entity.getPrecioSubtotal())
                .observacionesPeluqueria(entity.getObservacionesPeluqueria())
                .agendamientoCita(
                        AgendamientoCitaEntityMapper.toDomain(entity.getAgendamientoCita())
                )
                // Solo el id: basta para saber a qué mascota de la cita pertenece (no carga la mascota)
                .mascotaCita(entity.getMascotaCita() == null ? null
                        : MascotaCita.builder().id(entity.getMascotaCita().getId()).build())
                .vacunacion(vacunacion(entity.getVacunacion()))
                .desparasitacion(desparasitacion(entity.getDesparasitacion()))
                .build();
    }

    // Datos de la vacuna sin cargar la mascota (la bitácora ya sabe de qué mascota es)
    private static Vacunacion vacunacion(VacunacionEntity v) {
        if (v == null) {
            return null;
        }
        return Vacunacion.builder()
                .id(v.getId())
                .nombreVacuna(v.getNombreVacuna())
                .fechaAplicacion(v.getFechaAplicacion())
                .fechaRefuerzo(v.getFechaRefuerzo())
                .numeroLote(v.getNumeroLote())
                .numeroDosis(v.getNumeroDosis())
                .observaciones(v.getObservaciones())
                .build();
    }

    private static Desparasitacion desparasitacion(DesparasitacionEntity d) {
        if (d == null) {
            return null;
        }
        return Desparasitacion.builder()
                .id(d.getId())
                .marca(d.getMarca())
                .fechaAplicacion(d.getFechaAplicacion())
                .tipo(d.getTipo())
                .numeroLote(d.getNumeroLote())
                .observaciones(d.getObservaciones())
                .build();
    }
}