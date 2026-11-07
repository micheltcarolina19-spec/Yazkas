package usta.veterinaria.application.port.out;

import usta.veterinaria.domain.model.DuracionCita;
import usta.veterinaria.domain.model.Mascota;

// Puerto secundario: estima la duración de la cita (ALTA, MEDIA o BAJA) según la raza, el pelaje
// y el tamaño de la mascota. Lo implementa un adaptador de infrastructure que usa IA.
public interface EstimadorDuracionCitaPort {

    DuracionCita estimar(Mascota mascota);
}
