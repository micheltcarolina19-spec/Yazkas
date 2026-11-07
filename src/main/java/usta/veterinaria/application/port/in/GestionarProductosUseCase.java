package usta.veterinaria.application.port.in;

import usta.veterinaria.domain.model.CategoriaProducto;
import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.domain.model.TipoProducto;

import java.math.BigDecimal;
import java.util.List;

// Puerto primario: inventario de productos
public interface GestionarProductosUseCase {

    Producto crear(String nombre, String marca, CategoriaProducto categoria, String descripcion,
                   TipoProducto tipo, BigDecimal precioBase, BigDecimal precioVenta, String fotoProducto,
                   Integer stock);

    Producto actualizar(Long id, String nombre, String marca, CategoriaProducto categoria, String descripcion,
                        TipoProducto tipo, BigDecimal precioBase, BigDecimal precioVenta, String fotoProducto,
                        Integer stock);

    Producto reabastecer(Long id, int cantidad);

    void eliminar(Long id);

    // Vista completa para el administrador (incluye el precio base)
    Producto buscarPorId(Long id);

    List<Producto> listar();

    // Vista para el cliente (sin precio base)
    Producto buscarEnCatalogo(Long id);

    List<Producto> listarCatalogo();

    BigDecimal gananciaUnitaria(Long id);
}
