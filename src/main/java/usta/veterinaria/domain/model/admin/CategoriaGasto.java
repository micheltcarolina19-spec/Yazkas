package usta.veterinaria.domain.model.admin;

// Enum con los posibles valores de categoría de un gasto
public enum CategoriaGasto {
    INSUMOS,
    SERVICIOS_PUBLICOS,
    ALQUILER_LOCAL,
    MANTENIMIENTO_EQUIPOS,
    PERSONAL,
    // --- nuevas (agregadas al final para no alterar los valores existentes) ---
    HERRAMIENTAS,
    LIMPIEZA_DESINFECCION,
    INTERNET_TELEFONIA,
    PRODUCTOS_VENTA,
    PUBLICIDAD,
    IMPUESTOS_TRAMITES,
    CAPACITACION,
    OTROS
}