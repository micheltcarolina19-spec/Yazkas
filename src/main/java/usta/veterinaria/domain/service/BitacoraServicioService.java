package usta.veterinaria.domain.service;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.AgendamientoCita;
import usta.veterinaria.domain.model.BitacoraServicio;
import usta.veterinaria.domain.model.EstadoCita;

// Servicio que registra lo que se le hizo a la mascota en la cita y cuánto se cobró.
// El administrador lo registra al terminar la cita o al final del día.
public class BitacoraServicioService {

    // Registra un servicio (baño, corte, etc.) con su valor en pesos colombianos
    public BitacoraServicio registrar(AgendamientoCita agendamiento, BitacoraServicio servicio) {
        if (agendamiento == null) {
            throw new BusinessRuleException("La cita no existe");
        }
        if (agendamiento.getEstado() == EstadoCita.CANCELADA) {
            throw new BusinessRuleException("No se pueden registrar servicios en una cita cancelada");
        }
        if (agendamiento.getEstado() == EstadoCita.COMPLETADA) {
            throw new BusinessRuleException("La cita ya fue completada, no se le pueden agregar más servicios");
        }
        if (servicio == null) {
            throw new BusinessRuleException("El servicio es obligatorio");
        }
        if (servicio.getTipoServicio() == null || servicio.getTipoServicio().isBlank()) {
            throw new BusinessRuleException("Debe indicar el tipo de servicio realizado");
        }

        servicio.setTipoServicio(servicio.getTipoServicio().trim());
        servicio.setPrecioSubtotal(MonedaColombiana.validarMontoPositivo(
                servicio.getPrecioSubtotal(), "precio del servicio"));
        servicio.setAgendamientoCita(agendamiento);
        return servicio;
    }
}
