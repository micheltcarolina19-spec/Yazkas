package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.inject.Inject;
import jakarta.ws.rs.CookieParam;
import jakarta.ws.rs.DELETE;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.PUT;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.PathParam;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.NewCookie;
import jakarta.ws.rs.core.Response;
import usta.veterinaria.application.port.in.admin.GestionarProductosUseCase;
import usta.veterinaria.application.port.in.admin.RegistrarVentaUseCase;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.CategoriaProducto;
import usta.veterinaria.domain.model.admin.DetalleVenta;
import usta.veterinaria.domain.model.admin.Producto;
import usta.veterinaria.domain.model.admin.Venta;
import usta.veterinaria.infrastructure.adapter.in.web.sesion.SoloAdmin;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

// Punto de venta del administrador.
//  - GET /admin/productos          -> la página (productos + ticket que está guardado en la cookie)
//  - /admin/productos/ticket/...   -> acciones del ticket; responden con el panel del ticket ya pintado (HTML)
//
// El ticket vive en una cookie con el formato "idProducto:cantidad_idProducto:cantidad" (ej. "12:1_15:2").
// La cookie NO guarda precios: en cada petición el ticket se reconstruye desde la base de datos
// (precio y stock reales), así que nadie puede alterar un precio editando la cookie.
@Path("/admin/ventas")
@Produces(MediaType.TEXT_HTML)
@SoloAdmin
public class AdminVentasResource {

    private static final String COOKIE_TICKET = "ticket_venta";      // ticket actual
    private static final String COOKIE_PREVIO = "ticket_venta_prev"; // estado anterior (para "Deshacer")
    private static final String RUTA_COOKIE = "/admin";               // solo viaja a las páginas del administrador
    private static final int VIDA_COOKIE_SEGUNDOS = 12 * 60 * 60;     // el ticket se conserva 12 horas
    private static final int MAX_LINEAS = 100;                        // evita que la cookie pase el límite de 4 KB
    private static final int UMBRAL_STOCK_BAJO = 5;                   // mismo umbral de las alertas del dashboard
    private static final String ESTADO_VACIO = "-";                   // "antes el ticket estaba vacío" (Deshacer)

    @Inject
    @Location("admin/adminVentas.html")
    Template index;

    @Inject
    GestionarProductosUseCase productos;

    @Inject
    RegistrarVentaUseCase ventas;

    // ---------------------------------------------------------------- Página

    // Página completa: productos para la cuadrícula + ticket guardado en la cookie
    @GET
    public Response pagina(@CookieParam(COOKIE_TICKET) String ticketCookie,
                           @CookieParam(COOKIE_PREVIO) String previoCookie) {
        List<DetalleVenta> lineas = armar(leer(ticketCookie));
        String previo = previoValido(previoCookie);

        TemplateInstance pagina = datosTicket(index.instance(), lineas, !previo.isEmpty())
                .data("productos", productos.listar())
                .data("categorias", CategoriaProducto.values())
                .data("stockBajo", UMBRAL_STOCK_BAJO);

        return Response.ok(pagina.render())
                .cookie(cookie(COOKIE_TICKET, escribir(mapaDe(lineas))), cookie(COOKIE_PREVIO, previo))
                .build();
    }

    // ---------------------------------------------------------------- Ticket (en cookie)

    // Devuelve el panel del ticket (también corrige la cookie si cambió el stock o se borró un producto)
    @GET
    @Path("/ticket")
    public Response ticket(@CookieParam(COOKIE_TICKET) String ticketCookie,
                           @CookieParam(COOKIE_PREVIO) String previoCookie) {
        List<DetalleVenta> lineas = armar(leer(ticketCookie));
        return panel(lineas, escribir(mapaDe(lineas)), previoValido(previoCookie));
    }

    // Suma unidades de un producto al ticket (por defecto 1)
    @POST
    @Path("/ticket/agregar/{id}")
    public Response agregar(@PathParam("id") Long id,
                            @QueryParam("cantidad") @DefaultValue("1") int cantidad,
                            @CookieParam(COOKIE_TICKET) String ticketCookie,
                            @CookieParam(COOKIE_PREVIO) String previoCookie) {
        if (cantidad <= 0) {
            throw new BusinessRuleException("La cantidad debe ser mayor a cero");
        }
        Map<Long, Integer> antes = mapaDe(armar(leer(ticketCookie)));
        Map<Long, Integer> despues = new LinkedHashMap<>(antes);
        despues.merge(id, cantidad, Integer::sum);
        return cambiar(antes, despues, id, previoCookie);
    }

    // Fija la cantidad de un producto (con 0 o menos se quita del ticket)
    @PUT
    @Path("/ticket/{id}")
    public Response cambiarCantidad(@PathParam("id") Long id,
                                    @QueryParam("cantidad") int cantidad,
                                    @CookieParam(COOKIE_TICKET) String ticketCookie,
                                    @CookieParam(COOKIE_PREVIO) String previoCookie) {
        Map<Long, Integer> antes = mapaDe(armar(leer(ticketCookie)));
        Map<Long, Integer> despues = new LinkedHashMap<>(antes);
        if (cantidad <= 0) {
            despues.remove(id);
        } else {
            despues.put(id, cantidad);
        }
        return cambiar(antes, despues, id, previoCookie);
    }

    // Quita un producto del ticket
    @DELETE
    @Path("/ticket/{id}")
    public Response quitar(@PathParam("id") Long id,
                           @CookieParam(COOKIE_TICKET) String ticketCookie,
                           @CookieParam(COOKIE_PREVIO) String previoCookie) {
        Map<Long, Integer> antes = mapaDe(armar(leer(ticketCookie)));
        Map<Long, Integer> despues = new LinkedHashMap<>(antes);
        despues.remove(id);
        return cambiar(antes, despues, null, previoCookie);
    }

    // Vacía el ticket (se puede recuperar con "Deshacer")
    @DELETE
    @Path("/ticket")
    public Response vaciar(@CookieParam(COOKIE_TICKET) String ticketCookie,
                           @CookieParam(COOKIE_PREVIO) String previoCookie) {
        Map<Long, Integer> antes = mapaDe(armar(leer(ticketCookie)));
        return cambiar(antes, new LinkedHashMap<>(), null, previoCookie);
    }

    // Vuelve al estado anterior del ticket (un solo paso)
    @POST
    @Path("/ticket/deshacer")
    public Response deshacer(@CookieParam(COOKIE_PREVIO) String previoCookie) {
        if (previoValido(previoCookie).isEmpty()) {
            throw new BusinessRuleException("No hay nada que deshacer");
        }
        List<DetalleVenta> lineas = armar(leer(previoCookie));
        return panel(lineas, escribir(mapaDe(lineas)), "");
    }

    // Registra la venta (descuenta el stock) y deja el ticket vacío.
    // Responde con el panel vacío y, en las cabeceras, el número y el total de la venta.
    @POST
    @Path("/ticket/finalizar")
    public Response finalizar(@CookieParam(COOKIE_TICKET) String ticketCookie) {
        Map<Long, Integer> pedido = leer(ticketCookie);
        if (pedido.isEmpty()) {
            throw new BusinessRuleException("El ticket está vacío");
        }
        // Si el stock cambió (o un producto desapareció) desde que se armó el ticket, no se vende algo distinto a lo que se ve
        if (!mapaDe(armar(pedido)).equals(pedido)) {
            throw new BusinessRuleException("El stock cambió mientras armabas el ticket; revisa las cantidades");
        }

        Venta venta = ventas.registrar(new ArrayList<>(pedido.keySet()), new ArrayList<>(pedido.values()), null);

        String html = datosTicket(index.getFragment("ticket").instance(), List.of(), false).render();
        return Response.ok(html)
                .header("Cache-Control", "no-store")
                .header("X-Venta-Numero", String.format("VT-%06d", venta.getId()))
                .header("X-Venta-Total", venta.getTotalVenta().toPlainString())
                .cookie(cookie(COOKIE_TICKET, ""), cookie(COOKIE_PREVIO, ""))
                .build();
    }

    // ---------------------------------------------------------------- Apoyo

    // Aplica un cambio al ticket. "tocado" es el producto que el usuario modificó: si no se pudo dejar
    // la cantidad pedida (poco stock o agotado) se avisa y el ticket queda como estaba.
    private Response cambiar(Map<Long, Integer> antes, Map<Long, Integer> despues, Long tocado, String previoCookie) {
        List<DetalleVenta> lineas = armar(despues);
        Map<Long, Integer> real = mapaDe(lineas);

        if (tocado != null && despues.containsKey(tocado)
                && !Objects.equals(real.get(tocado), despues.get(tocado))) {
            Producto producto = productos.buscarPorId(tocado); // 404 si el producto ya no existe
            int stock = producto.getStock() == null ? 0 : producto.getStock();
            throw new BusinessRuleException(stock <= 0
                    ? producto.getNombre() + " está agotado"
                    : "Solo hay " + stock + " unidades disponibles de " + producto.getNombre());
        }

        String antesTexto = escribir(antes);
        String realTexto = escribir(real);
        String previo = antesTexto.equals(realTexto)
                ? previoValido(previoCookie)
                : (antesTexto.isEmpty() ? ESTADO_VACIO : antesTexto);
        return panel(lineas, realTexto, previo);
    }

    // Renderiza SOLO el panel del ticket ({#fragment id=ticket} de la plantilla) y actualiza las cookies
    private Response panel(List<DetalleVenta> lineas, String ticketTexto, String previoTexto) {
        String html = datosTicket(index.getFragment("ticket").instance(), lineas, !previoTexto.isEmpty()).render();
        return Response.ok(html)
                .header("Cache-Control", "no-store")
                .cookie(cookie(COOKIE_TICKET, ticketTexto), cookie(COOKIE_PREVIO, previoTexto))
                .build();
    }

    // Datos que usa la plantilla para pintar el ticket
    private static TemplateInstance datosTicket(TemplateInstance plantilla, List<DetalleVenta> lineas, boolean hayPrevio) {
        BigDecimal total = lineas.stream()
                .map(DetalleVenta::getSubtotal)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        int unidades = lineas.stream().mapToInt(DetalleVenta::getCantidad).sum();
        return plantilla
                .data("lineas", lineas)
                .data("total", total)
                .data("unidades", unidades)
                .data("hayPrevio", hayPrevio);
    }

    // Convierte lo que hay en el ticket en líneas con precio y subtotal reales (ya ajustadas al stock)
    private List<DetalleVenta> armar(Map<Long, Integer> ticket) {
        if (ticket.isEmpty()) {
            return List.of();
        }
        return ventas.armarTicket(new ArrayList<>(ticket.keySet()), new ArrayList<>(ticket.values()));
    }

    private static Map<Long, Integer> mapaDe(List<DetalleVenta> lineas) {
        Map<Long, Integer> mapa = new LinkedHashMap<>();
        for (DetalleVenta linea : lineas) {
            mapa.put(linea.getProducto().getId(), linea.getCantidad());
        }
        return mapa;
    }

    // "12:1_15:2" -> {12=1, 15=2}. Lo que venga mal formado (cookie manipulada) se ignora
    private static Map<Long, Integer> leer(String valor) {
        Map<Long, Integer> ticket = new LinkedHashMap<>();
        if (valor == null || valor.isBlank()) {
            return ticket;
        }
        for (String par : valor.split("_")) {
            String[] partes = par.split(":");
            if (partes.length != 2) {
                continue;
            }
            try {
                long id = Long.parseLong(partes[0].trim());
                int cantidad = Integer.parseInt(partes[1].trim());
                if (id > 0 && cantidad > 0 && ticket.size() < MAX_LINEAS) {
                    ticket.merge(id, cantidad, Integer::sum);
                }
            } catch (NumberFormatException ignorada) {
                // valor inválido: se descarta
            }
        }
        return ticket;
    }

    // {12=1, 15=2} -> "12:1_15:2"
    private static String escribir(Map<Long, Integer> ticket) {
        return ticket.entrySet().stream()
                .map(e -> e.getKey() + ":" + e.getValue())
                .collect(Collectors.joining("_"));
    }

    // Estado anterior guardado para "Deshacer": "" = no hay, "-" = antes estaba vacío
    private static String previoValido(String crudo) {
        if (crudo == null || crudo.isBlank()) {
            return "";
        }
        String limpio = escribir(leer(crudo));
        return limpio.isEmpty() ? ESTADO_VACIO : limpio;
    }

    // Cookie del ticket: no la puede leer JavaScript y solo se envía desde el mismo sitio.
    // Con valor vacío se borra. En producción con HTTPS agrega .secure(true)
    private static NewCookie cookie(String nombre, String valor) {
        boolean vacia = valor == null || valor.isEmpty();
        return new NewCookie.Builder(nombre)
                .value(vacia ? "" : valor)
                .path(RUTA_COOKIE)
                .maxAge(vacia ? 0 : VIDA_COOKIE_SEGUNDOS)
                .httpOnly(true)
                .sameSite(NewCookie.SameSite.STRICT)
                .build();
    }
}