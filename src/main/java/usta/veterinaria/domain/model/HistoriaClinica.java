package usta.veterinaria.domain.model;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

/** Clase que representa la historia clínica de una mascota. */
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@ToString
public class HistoriaClinica {

    private Long id;
    private String alergiasMedicamentos;
    private String alergiasCosmeticas;
    private String alergiasAlimenticias;
    private String enfermedadesCronicas;
    private String condicionesFisicas;
    private String problemasCardiacos;
    private String problemasOrtopedicos;
    private String diagnosticoDermatologico;
    private Boolean presenciaParasitos;
    private String cirugiasRecientes;
    private String lesiones;
    private Boolean esterilizacion;
    private String observaciones;
    // Mascota dueña de la historia clínica (relación 1 a 1)
    private Mascota mascota;
}
