package usta.veterinaria.domain.service.admin;

import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.service.MonedaColombiana;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

// Servicio con las reglas del inventario y del catálogo de productos.
// El administrador ve los precios; el cliente en la página web solo ve el catálogo SIN precios.
public class ProductoService {

    // Valida un producto antes de crearlo o actualizarlo (lo hace el administrador)
    public Producto validar(Producto producto) {
        if (producto == null) {
            throw new BusinessRuleException("El producto es obligatorio");
        }
        if (producto.getNombre() == null || producto.getNombre().isBlank()) {
            throw new BusinessRuleException("El nombre del producto es obligatorio");
        }
        if (producto.getCategoria() == null) {
            throw new BusinessRuleException("La categoría del producto es obligatoria");
        }
        producto.setNombre(producto.getNombre().trim());
        producto.setPrecioBase(MonedaColombiana.validarMontoPositivo(producto.getPrecioBase(), "precio base"));
        producto.setPrecioVenta(MonedaColombiana.validarMontoPositivo(producto.getPrecioVenta(), "precio de venta"));

        if (producto.getStock() == null) {
            producto.setStock(0);
        }
        if (producto.getStock() < 0) {
            throw new BusinessRuleException("El stock no puede ser negativo");
        }
        return producto;
    }

    // Suma unidades al inventario cuando llega mercancía
    public Producto reabastecer(Producto producto, int cantidad) {
        if (producto == null) {
            throw new BusinessRuleException("El producto no existe");
        }
        if (cantidad <= 0) {
            throw new BusinessRuleException("La cantidad a ingresar debe ser mayor a cero");
        }
        int stockActual = producto.getStock() == null ? 0 : producto.getStock();
        producto.setStock(stockActual + cantidad);
        return producto;
    }

    // Ganancia por unidad para el panel del administrador: precioVenta - precioBase
    public BigDecimal gananciaUnitaria(Producto producto) {
        if (producto == null || producto.getPrecioBase() == null || producto.getPrecioVenta() == null) {
            throw new BusinessRuleException("El producto debe tener precio base y precio de venta");
        }
        return MonedaColombiana.normalizar(producto.getPrecioVenta().subtract(producto.getPrecioBase()));
    }

    public boolean disponible(Producto producto) {
        return producto != null && producto.getStock() != null && producto.getStock() > 0;
    }

    // Copia del producto para el catálogo público: se quitan los precios para que el cliente no los vea
    public Producto vistaCatalogo(Producto producto) {
        if (producto == null) {
            return null;
        }
        return Producto.builder()
                .id(producto.getId())
                .nombre(producto.getNombre())
                .marca(producto.getMarca())
                .categoria(producto.getCategoria())
                .descripcion(producto.getDescripcion())
                .tipo(producto.getTipo())
                .fotoProducto(producto.getFotoProducto())
                .stock(producto.getStock())
                .build();
    }

    public List<Producto> vistaCatalogo(List<Producto> productos) {
        List<Producto> catalogo = new ArrayList<>();
        if (productos != null) {
            for (Producto producto : productos) {
                if (producto != null) {
                    catalogo.add(vistaCatalogo(producto));
                }
            }
        }
        return catalogo;
    }
}
