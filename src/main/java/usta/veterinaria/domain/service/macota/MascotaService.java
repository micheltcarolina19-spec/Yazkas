package usta.veterinaria.domain.service.macota;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.mascota.HistoriaClinica;
import usta.veterinaria.domain.model.mascota.Mascota;
import usta.veterinaria.domain.service.RelojColombia;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;
import java.util.Objects;

// Servicio con las reglas de registro de mascotas y de su historia clínica
public class MascotaService {

    private final Clock clock;

    public MascotaService() {
        this(RelojColombia.reloj());
    }

    public MascotaService(Clock clock) {
        this.clock = Objects.requireNonNull(clock, "El reloj es obligatorio");
    }

    // Valida los datos de la mascota. Raza, pelaje y tamaño/peso son obligatorios
    // porque con ellos se estima la duración de las citas.
    public Mascota validarRegistro(Mascota mascota) {
        if (mascota == null) {
            throw new BusinessRuleException("La mascota es obligatoria");
        }
        if (mascota.getUsuario() == null || mascota.getUsuario().getId() == null) {
            throw new IllegalStateException("La mascota debe tener un propietario");
        }
        if (mascota.getNombre() == null || mascota.getNombre().isBlank()) {
            throw new BusinessRuleException("El nombre de la mascota es obligatorio");
        }
        if (mascota.getTipo() == null) {
            throw new BusinessRuleException("Debe indicar si la mascota es perro o gato");
        }
        if (mascota.getRaza() == null || mascota.getRaza().isBlank()) {
            throw new BusinessRuleException("Debe indicar la raza de la mascota (si no tiene, escriba Criollo)");
        }
        if (mascota.getPelaje() == null) {
            throw new BusinessRuleException("Debe indicar el tipo de pelaje de la mascota");
        }
        if (mascota.getTamano() == null && mascota.getPeso() == null) {
            throw new BusinessRuleException("Debe indicar el tamaño o el peso de la mascota");
        }
        if (mascota.getPeso() != null && mascota.getPeso() <= 0) {
            throw new BusinessRuleException("El peso debe ser mayor a cero");
        }
        if (mascota.getTamano() != null && mascota.getTamano() <= 0) {
            throw new BusinessRuleException("El tamaño debe ser mayor a cero");
        }
        if (mascota.getFechaNacimiento() != null && mascota.getFechaNacimiento().isAfter(LocalDate.now(clock))) {
            throw new BusinessRuleException("La fecha de nacimiento no puede ser futura");
        }

        mascota.setNombre(mascota.getNombre().trim());
        mascota.setRaza(mascota.getRaza().trim());
        return mascota;
    }

    // Una mascota solo puede tener una historia clínica (relación 1 a 1)
    public HistoriaClinica validarHistoriaClinica(HistoriaClinica nueva, HistoriaClinica existente) {
        if (nueva == null || nueva.getMascota() == null) {
            throw new BusinessRuleException("La historia clínica debe pertenecer a una mascota");
        }
        if (existente != null && existente != nueva
                && (existente.getId() == null || !existente.getId().equals(nueva.getId()))) {
            throw new BusinessRuleException("La mascota " + nueva.getMascota().getNombre()
                    + " ya tiene una historia clínica; actualícela en lugar de crear otra");
        }
        return nueva;
    }

    // Edad de la mascota (años, meses y días). Si no tiene fecha de nacimiento devuelve null.
    public Period edad(Mascota mascota) {
        if (mascota == null || mascota.getFechaNacimiento() == null) {
            return null;
        }
        return Period.between(mascota.getFechaNacimiento(), LocalDate.now(clock));
    }
}
