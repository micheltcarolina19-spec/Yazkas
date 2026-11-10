package usta.veterinaria.application.port.in.admin;

import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.Gasto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

// Puerto primario: gastos del negocio
public interface GestionarGastosUseCase {

    // Si fechaGasto es null se usa la fecha de hoy
    Gasto registrar(String nombre, BigDecimal monto, LocalDate fechaGasto, CategoriaGasto categoria);

    Gasto actualizar(Long id, String nombre, BigDecimal monto, LocalDate fechaGasto, CategoriaGasto categoria);

    void eliminar(Long id);

    Gasto buscarPorId(Long id);

    List<Gasto> listar();

    List<Gasto> listarPorRango(LocalDate inicio, LocalDate fin);
}
