package usta.veterinaria.application.usecase;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.transaction.Transactional;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.GestionarProductosUseCase;
import usta.veterinaria.application.port.out.ProductoRepository;
import usta.veterinaria.domain.model.CategoriaProducto;
import usta.veterinaria.domain.model.Producto;
import usta.veterinaria.domain.model.TipoProducto;
import usta.veterinaria.domain.service.ProductoService;

import java.math.BigDecimal;
import java.util.List;

@ApplicationScoped
public class GestionarProductosService implements GestionarProductosUseCase {

    private final ProductoService productoService = new ProductoService();

    private final ProductoRepository productoRepository;

    @Inject
    public GestionarProductosService(ProductoRepository productoRepository) {
        this.productoRepository = productoRepository;
    }

    @Override
    @Transactional
    public Producto crear(String nombre, String marca, CategoriaProducto categoria, String descripcion,
                          TipoProducto tipo, BigDecimal precioBase, BigDecimal precioVenta,
                          String fotoProducto, Integer stock) {
        Producto producto = construir(nombre, marca, categoria, descripcion, tipo, precioBase, precioVenta,
                fotoProducto, stock);
        return productoRepository.save(productoService.validar(producto));
    }

    @Override
    @Transactional
    public Producto actualizar(Long id, String nombre, String marca, CategoriaProducto categoria,
                               String descripcion, TipoProducto tipo, BigDecimal precioBase,
                               BigDecimal precioVenta, String fotoProducto, Integer stock) {
        Producto existente = buscarPorId(id);

        Producto actualizado = construir(nombre, marca, categoria, descripcion, tipo, precioBase, precioVenta,
                fotoProducto, stock);
        actualizado.setId(existente.getId());

        return productoRepository.save(productoService.validar(actualizado));
    }

    @Override
    @Transactional
    public Producto reabastecer(Long id, int cantidad) {
        Producto producto = buscarPorId(id);
        return productoRepository.save(productoService.reabastecer(producto, cantidad));
    }

    @Override
    @Transactional
    public void eliminar(Long id) {
        Producto producto = buscarPorId(id);
        productoRepository.deleteById(producto.getId());
    }

    @Override
    public Producto buscarPorId(Long id) {
        return productoRepository.findById(Validaciones.requerirId(id, "id del producto"))
                .orElseThrow(() -> new ResourceNotFoundException("No existe el producto con id " + id));
    }

    @Override
    public List<Producto> listar() {
        return productoRepository.findAll();
    }

    @Override
    public Producto buscarEnCatalogo(Long id) {
        return productoService.vistaCatalogo(buscarPorId(id));
    }

    @Override
    public List<Producto> listarCatalogo() {
        return productoService.vistaCatalogo(productoRepository.findAll());
    }

    @Override
    public BigDecimal gananciaUnitaria(Long id) {
        return productoService.gananciaUnitaria(buscarPorId(id));
    }

    private Producto construir(String nombre, String marca, CategoriaProducto categoria, String descripcion,
                               TipoProducto tipo, BigDecimal precioBase, BigDecimal precioVenta,
                               String fotoProducto, Integer stock) {
        return Producto.builder()
                .nombre(nombre)
                .marca(marca)
                .categoria(categoria)
                .descripcion(descripcion)
                .tipo(tipo)
                .precioBase(precioBase)
                .precioVenta(precioVenta)
                .fotoProducto(fotoProducto)
                .stock(stock)
                .build();
    }
}
