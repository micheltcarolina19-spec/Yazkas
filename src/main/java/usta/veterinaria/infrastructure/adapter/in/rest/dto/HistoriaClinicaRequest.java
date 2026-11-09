package usta.veterinaria.infrastructure.adapter.in.rest.dto;

import jakarta.validation.constraints.Size;
import org.eclipse.microprofile.openapi.annotations.media.Schema;

// Datos que llegan para registrar o actualizar la historia clínica de una mascota.
// El id de la mascota va en la URL. Todos los campos son opcionales
public record HistoriaClinicaRequest(
        @Schema(example = "Penicilina")
        @Size(max = 1000, message = "Las alergias a medicamentos no puede superar 1000 caracteres") String alergiasMedicamentos,

        @Schema(example = "Ninguna")
        @Size(max = 1000, message = "Las alergias cosméticas no puede superar 1000 caracteres") String alergiasCosmeticas,

        @Schema(example = "Pollo")
        @Size(max = 1000, message = "Las alergias alimenticias no puede superar 1000 caracteres") String alergiasAlimenticias,

        @Schema(example = "Ninguna")
        @Size(max = 1000, message = "Las enfermedades crónicas no puede superar 1000 caracteres") String enfermedadesCronicas,

        @Schema(example = "Buena condición")
        @Size(max = 1000, message = "Las condiciones físicas no puede superar 1000 caracteres") String condicionesFisicas,

        @Schema(example = "Ninguno")
        @Size(max = 1000, message = "Los problemas cardíacos no puede superar 1000 caracteres") String problemasCardiacos,

        @Schema(example = "Displasia leve de cadera")
        @Size(max = 1000, message = "Los problemas ortopédicos no puede superar 1000 caracteres") String problemasOrtopedicos,

        @Schema(example = "Piel sana")
        @Size(max = 1000, message = "El diagnóstico dermatológico no puede superar 1000 caracteres") String diagnosticoDermatologico,

        @Schema(example = "false")
        Boolean presenciaParasitos,

        @Schema(example = "Ninguna")
        @Size(max = 1000, message = "Las cirugías recientes no puede superar 1000 caracteres") String cirugiasRecientes,

        @Schema(example = "Ninguna")
        @Size(max = 1000, message = "Las lesiones no puede superar 1000 caracteres") String lesiones,

        @Schema(example = "true")
        Boolean esterilizacion,

        @Schema(example = "Revisar oídos en cada baño")
        @Size(max = 2000, message = "Las observaciones no puede superar 2000 caracteres") String observaciones) {
}
