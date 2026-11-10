package usta.veterinaria.infrastructure.adapter.in.rest.mapper.mascota;

import usta.veterinaria.domain.model.mascota.HistoriaClinica;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.mascota.HistoriaClinicaResponse;

// Convierte la historia clínica del dominio a la respuesta REST
public final class HistoriaClinicaRestMapper {

    private HistoriaClinicaRestMapper() {
    }

    public static HistoriaClinicaResponse toResponse(HistoriaClinica historia) {
        Mascota mascota = historia.getMascota();
        return new HistoriaClinicaResponse(
                historia.getId(),
                mascota == null ? null : mascota.getId(),
                mascota == null ? null : mascota.getNombre(),
                historia.getAlergiasMedicamentos(),
                historia.getAlergiasCosmeticas(),
                historia.getAlergiasAlimenticias(),
                historia.getEnfermedadesCronicas(),
                historia.getCondicionesFisicas(),
                historia.getProblemasCardiacos(),
                historia.getProblemasOrtopedicos(),
                historia.getDiagnosticoDermatologico(),
                historia.getPresenciaParasitos(),
                historia.getCirugiasRecientes(),
                historia.getLesiones(),
                historia.getEsterilizacion(),
                historia.getObservaciones());
    }
}
