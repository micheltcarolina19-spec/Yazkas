package usta.veterinaria.application.usecase.admin;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.port.in.admin.GestionarPlanillaSanidadUseCase;
import usta.veterinaria.application.port.out.admin.PlanillaSanidadRepository;
import usta.veterinaria.application.usecase.Validaciones;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.AreaLimpieza;
import usta.veterinaria.domain.model.admin.Herramienta;
import usta.veterinaria.domain.model.admin.PlanillaSanidad;
import usta.veterinaria.domain.service.admin.PlanillaSanidadService;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

@ApplicationScoped
public class GestionarPlanillaSanidadService implements GestionarPlanillaSanidadUseCase {

    private static final ZoneId ZONA_COLOMBIA = ZoneId.of("America/Bogota");

    // Orden de la planilla: por fecha y, dentro del mismo día, por orden de registro
    private static final Comparator<PlanillaSanidad> ORDEN_PLANILLA = Comparator
            .comparing(PlanillaSanidad::getFechaPlanilla)
            .thenComparing(PlanillaSanidad::getId, Comparator.nullsLast(Comparator.naturalOrder()));

    private final PlanillaSanidadService planillaService = new PlanillaSanidadService();

    private final PlanillaSanidadRepository planillaRepository;

    @Inject
    public GestionarPlanillaSanidadService(PlanillaSanidadRepository planillaRepository) {
        this.planillaRepository = planillaRepository;
    }

    @Override
    @Transactional
    public PlanillaSanidad registrar(LocalDate fecha, AreaLimpieza areaLimpieza, Herramienta herramienta,
                                     Boolean limpiezaRealizada, Boolean desinfeccion, String observaciones) {
        PlanillaSanidad planilla = PlanillaSanidad.builder()
                .fechaPlanilla(fecha)
                .areaLimpieza(areaLimpieza)
                .herramienta(herramienta)
                .limpiezaRealizada(limpiezaRealizada)
                .desinfeccion(desinfeccion)
                .observaciones(observaciones)
                .build();

        // Si no llega fecha el dominio usa hoy, así que los registros existentes se buscan en esa misma fecha
        LocalDate fechaConsulta = fecha != null ? fecha : LocalDate.now(ZONA_COLOMBIA);
        List<PlanillaSanidad> existentes = planillaRepository.findByFechaPlanilla(fechaConsulta);

        return planillaRepository.save(planillaService.registrar(planilla, existentes));
    }

    @Override
    @Transactional
    public PlanillaSanidad actualizar(Long id, LocalDate fecha, AreaLimpieza areaLimpieza, Herramienta herramienta,
                                      Boolean limpiezaRealizada, Boolean desinfeccion, String observaciones) {
        PlanillaSanidad actual = buscarPorId(id);
        LocalDate fechaNueva = fecha != null ? fecha : actual.getFechaPlanilla();

        PlanillaSanidad planilla = PlanillaSanidad.builder()
                .id(actual.getId())
                .fechaPlanilla(fechaNueva)
                .areaLimpieza(areaLimpieza)
                .herramienta(herramienta)
                .limpiezaRealizada(limpiezaRealizada)
                .desinfeccion(desinfeccion)
                .observaciones(observaciones)
                .build();

        List<PlanillaSanidad> existentes = planillaRepository.findByFechaPlanilla(fechaNueva);

        return planillaRepository.save(planillaService.registrar(planilla, existentes));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        PlanillaSanidad existente = buscarPorId(id);
        planillaRepository.deleteById(existente.getId());
    }

    @Override
    public PlanillaSanidad buscarPorId(Long id) {
        if (id == null) {
            throw new BusinessRuleException("El id del registro de sanidad es obligatorio");
        }
        return planillaRepository.findById(id)
                .orElseThrow(() -> new BusinessRuleException("No existe un registro de sanidad con id " + id));
    }

    @Override
    public List<PlanillaSanidad> listarPorFecha(LocalDate fecha) {
        return ordenar(planillaRepository.findByFechaPlanilla(
                Validaciones.requerirFecha(fecha, "fecha de la planilla")));
    }

    @Override
    public List<PlanillaSanidad> listarPorRango(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return ordenar(planillaRepository.findByFechaPlanillaBetween(inicio, fin));
    }

    @Override
    public BigDecimal porcentajeCumplimiento(LocalDate inicio, LocalDate fin) {
        Validaciones.rangoFechas(inicio, fin);
        return planillaService.porcentajeCumplimiento(planillaRepository.findByFechaPlanillaBetween(inicio, fin));
    }

    private List<PlanillaSanidad> ordenar(List<PlanillaSanidad> registros) {
        List<PlanillaSanidad> copia = new ArrayList<>(registros);
        copia.sort(ORDEN_PLANILLA);
        return copia;
    }
}