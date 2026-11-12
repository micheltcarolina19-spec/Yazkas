package usta.veterinaria.infrastructure.adapter.in.rest.mapper.admin;

import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.infrastructure.adapter.in.rest.dto.admin.PlanillaSanidadResponse;

import java.util.List;
import java.util.Locale;
import java.util.Map;

// Convierte el modelo de dominio de la planilla de sanidad en la respuesta REST
public final class PlanillaSanidadRestMapper {

    private static final Locale ES = Locale.forLanguageTag("es-CO");

    // Valores del enum que llevan tilde o ñ (agregue aquí los que falten)
    private static final Map<String, String> ESPECIALES = Map.of(
            "MIERCOLES", "Miércoles",
            "SABADO", "Sábado",
            "BANERA", "Bañera");

    private PlanillaSanidadRestMapper() {
    }

    public static PlanillaSanidadResponse toResponse(PlanillaSanidad p) {
        return new PlanillaSanidadResponse(
                p.getId(),
                p.getFechaPlanilla(),
                nombre(p.getDiaSanidad()),
                etiqueta(p.getDiaSanidad()),
                nombre(p.getAreaLimpieza()),
                etiqueta(p.getAreaLimpieza()),
                nombre(p.getHerramienta()),
                etiqueta(p.getHerramienta()),
                p.getLimpiezaRealizada(),
                p.getDesinfeccion(),
                p.getObservaciones());
    }

    public static List<PlanillaSanidadResponse> toResponse(List<PlanillaSanidad> registros) {
        return registros.stream().map(PlanillaSanidadRestMapper::toResponse).toList();
    }

    // ZONA_LAVADO -> "Zona lavado"; los valores con tilde salen del mapa ESPECIALES
    public static String etiqueta(Enum<?> valor) {
        if (valor == null) {
            return "";
        }
        String especial = ESPECIALES.get(valor.name());
        if (especial != null) {
            return especial;
        }
        String texto = valor.name().toLowerCase(ES).replace('_', ' ');
        return Character.toUpperCase(texto.charAt(0)) + texto.substring(1);
    }

    private static String nombre(Enum<?> valor) {
        return valor == null ? null : valor.name();
    }
}