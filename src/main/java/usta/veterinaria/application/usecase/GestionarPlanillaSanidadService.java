package usta.veterinaria.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.in.GestionarPlanillaSanidadUseCase;
import usta.veterinaria.application.port.out.PlanillaSanidadRepository;
import usta.veterinaria.domain.model.AreaLimpieza;
import usta.veterinaria.domain.model.Herramienta;
import usta.veterinaria.domain.model.PlanillaSanidad;
import usta.veterinaria.domain.service.PlanillaSanidadService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;

@ApplicationScoped
public class GestionarPlanillaSanidadService implements GestionarPlanillaSanidadUseCase {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    private final PlanillaSanidadService planillaService = new PlanillaSanidadService();

    private final PlanillaSanidadRepository planillaRepository;

    @Inject
    public GestionarPlanillaSanidadService(PlanillaSanidadRepository planillaRepository) {
        this.planillaRepository = planillaRepository;
    }

    @Override
    @Transactional
    public PlanillaSanidad registrar(LocalDate fecha, AreaLimpieza areaLimpieza, Herramienta herramienta,
                                     Boolean limpiezaRealizada, Boolean desinfeccion) {
        PlanillaSanidad planilla = PlanillaSanidad.builder()
                .fechaPlanilla(fecha)
                .areaLimpieza(areaLimpieza)
                .herramienta(herramienta)
                .limpiezaRealizada(limpiezaRealizada)
                .desinfeccion(desinfeccion)
                .build();

        // Si no llega fecha el dominio usa hoy, así que los registros existentes se buscan en esa misma fecha
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now(ZONA_COLOMBIA);
        List<PlanillaSanidad> existentes = planillaRepository.findByFechaPlanilla(fechaConsulta);

        return planillaRepository.save(planillaService.registrar(planilla, existentes));
    }

    @Override
    public List<PlanillaSanidad> listarPorFecha(LocalDate fecha) {
        return planillaRepository.findByFechaPlanilla(Validaciones.requerirFecha(fecha, "fecha de la planilla"));
    }

    @Override
    public List<PlanillaSanidad> listarPorRango(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return planillaRepository.findByFechaPlanillaBetween(inicio, fin);
    }

    @Override
    public BigDecimal porcentajeCumplimiento(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return planillaService.porcentajeCumplimiento(planillaRepository.findByFechaPlanillaBetween(inicio, fin));
    }
}
