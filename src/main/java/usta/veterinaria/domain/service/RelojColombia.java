package usta.veterinaria.domain.service;

import java.time.Clock;
import java.time.ZoneId;

// Reloj del negocio en la zona horaria de Colombia.
// Los servicios reciben un Clock para que las fechas se puedan controlar en las pruebas unitarias.
final class RelojColombia {

    static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private RelojColombia() {
        // Clase utilitaria: no se instancia
    }

    static Clock reloj() {
        return Clock.system(ZONA_COLOMBIA);
    }
}
