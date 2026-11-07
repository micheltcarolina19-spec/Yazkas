package usta.veterinaria.infrastructure.adapter.out.persistence.mapper;

import usta.veterinaria.domain.model.HistoriaClinica;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.HistorialClinicaEntity;
import usta.veterinaria.infrastructure.adapter.out.persistence.entity.MascotaEntity;

// Convierte entre HistoriaClinica y HistoriaClinicaEntity
public final class HistorialClinicaEntityMapper {

    private HistorialClinicaEntityMapper() {
    }

    // Dominio -> entidad
    public static HistorialClinicaEntity toEntity(HistoriaClinica historia) {
        if (historia == null) {
            return null;
        }

        HistorialClinicaEntity entity = new HistorialClinicaEntity();
        entity.setId(historia.getId());
        entity.setAlergiasMedicamentos(historia.getAlergiasMedicamentos());
        entity.setAlergiasCosmeticas(historia.getAlergiasCosmeticas());
        entity.setAlergiasAlimenticias(historia.getAlergiasAlimenticias());
        entity.setEnfermedadesCronicas(historia.getEnfermedadesCronicas());
        entity.setCondicionesFisicas(historia.getCondicionesFisicas());
        entity.setProblemasCardiacos(historia.getProblemasCardiacos());
        entity.setProblemasOrtopedicos(historia.getProblemasOrtopedicos());
        entity.setDiagnosticoDermatologico(historia.getDiagnosticoDermatologico());
        entity.setCirugiasRecientes(historia.getCirugiasRecientes());
        entity.setLesiones(historia.getLesiones());
        entity.setObservaciones(historia.getObservaciones());
        entity.setPresenciaParasitos(historia.getPresenciaParasitos());
        entity.setEsterilizacion(historia.getEsterilizacion());

        if (historia.getMascota() != null) {
            MascotaEntity mascota = new MascotaEntity();
            mascota.setId(historia.getMascota().getId());
            entity.setMascota(mascota);
        }

        return entity;
    }

    // Entidad -> dominio
    public static HistoriaClinica toDomain(HistorialClinicaEntity entity) {
        if (entity == null) {
            return null;
        }

        return HistoriaClinica.builder()
                .id(entity.getId())
                .alergiasMedicamentos(entity.getAlergiasMedicamentos())
                .alergiasCosmeticas(entity.getAlergiasCosmeticas())
                .alergiasAlimenticias(entity.getAlergiasAlimenticias())
                .enfermedadesCronicas(entity.getEnfermedadesCronicas())
                .condicionesFisicas(entity.getCondicionesFisicas())
                .problemasCardiacos(entity.getProblemasCardiacos())
                .problemasOrtopedicos(entity.getProblemasOrtopedicos())
                .diagnosticoDermatologico(entity.getDiagnosticoDermatologico())
                .cirugiasRecientes(entity.getCirugiasRecientes())
                .lesiones(entity.getLesiones())
                .observaciones(entity.getObservaciones())
                .presenciaParasitos(entity.getPresenciaParasitos())
                .esterilizacion(entity.getEsterilizacion())
                .mascota(
                        MascotaEntityMapper.toDomain(entity.getMascota())
                )
                .build();
    }
}