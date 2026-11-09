package usta.veterinaria.infrastructure.adapter.in.rest.dto;

// Datos que se devuelven de la historia clínica de una mascota
public record HistoriaClinicaResponse(
        Long id,
        Long mascotaId,
        String nombreMascota,
        String alergiasMedicamentos,
        String alergiasCosmeticas,
        String alergiasAlimenticias,
        String enfermedadesCronicas,
        String condicionesFisicas,
        String problemasCardiacos,
        String problemasOrtopedicos,
        String diagnosticoDermatologico,
        Boolean presenciaParasitos,
        String cirugiasRecientes,
        String lesiones,
        Boolean esterilizacion,
        String observaciones) {
}
