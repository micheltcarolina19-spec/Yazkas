package usta.veterinaria.infrastructure.adapter.in.web.admin;

import io.quarkus.qute.Location;
import io.quarkus.qute.Template;
import io.quarkus.qute.TemplateInstance;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.inject.Inject;
import jakarta.ws.rs.Consumes;
import jakarta.ws.rs.FormParam;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import org.jboss.logging.Logger;
import usta.veterinaria.application.exception.ResourceNotFoundException;
import usta.veterinaria.application.port.in.admin.GestionarGastosUseCase;
import usta.veterinaria.domain.exception.BusinessRuleException;
import usta.veterinaria.domain.model.admin.CategoriaGasto;
import usta.veterinaria.domain.model.admin.Gasto;
import usta.veterinaria.domain.service.RelojColombia;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.AtajoVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.CategoriaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.DesgloseVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.DialogoVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.EliminarVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.FilaVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.FiltroVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.PaginaItemVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.PaginacionVista;
import usta.veterinaria.infrastructure.adapter.in.web.admin.GastoVista.ResumenVista;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

// Adaptador de entrada WEB: gestión de gastos del administrador (renderizado en el servidor).
//   - GET  /admin/gasto?desde=&hasta=&categoria=&pagina=        -> listado
//   - GET  /admin/gasto?...&nuevo=1 | &editar=ID | &eliminar=ID -> listado con el diálogo abierto
//   - POST /admin/gasto/guardar | /eliminar                     -> ejecuta la acción y redirige (303)
// Usa el MISMO caso de uso que el adaptador REST (GestionarGastosUseCase).
@Path(AdminGastoResource.BASE)
@ApplicationScoped
public class AdminGastoResource {

    static final String BASE = "/admin/gastos";
    private static final Logger LOG = Logger.getLogger(AdminGastoResource.class);

    private static final int POR_PAGINA = 8;

    private static final String[] MESES_CORTO = {"Ene", "Feb", "Mar", "Abr", "May", "Jun", "Jul", "Ago", "Sep", "Oct", "Nov", "Dic"};

    private static final DecimalFormat PESOS = new DecimalFormat("#,##0", simbolosColombia());

    @Inject
    @Location("admin/adminGastos.html")
    Template index;

    @Inject
    GestionarGastosUseCase gastos;

    // Filtro vigente del listado (se conserva al abrir diálogos, paginar y guardar)
    private record Filtro(LocalDate desde, LocalDate hasta, CategoriaGasto categoria, int pagina) {
        Filtro conPagina(int p) {
            return new Filtro(desde, hasta, categoria, p);
        }

        Filtro conRango(LocalDate d, LocalDate h) {
            return new Filtro(d, h, categoria, 1);
        }
    }

    // ===================== Páginas =====================

    @GET
    @Produces(MediaType.TEXT_HTML)
    public TemplateInstance mostrar(@QueryParam("desde") String desde,
                                    @QueryParam("hasta") String hasta,
                                    @QueryParam("categoria") String categoria,
                                    @QueryParam("pagina") Integer pagina,
                                    @QueryParam("nuevo") String nuevo,
                                    @QueryParam("editar") Long editar,
                                    @QueryParam("eliminar") Long eliminar,
                                    @QueryParam("ok") String ok) {
        Filtro f = filtro(desde, hasta, categoria, pagina);
        String error = null;
        DialogoVista dialogo = null;
        EliminarVista confirmar = null;
        try {
            if (eliminar != null) {
                confirmar = confirmarEliminar(f, gastos.buscarPorId(eliminar));
            } else if (editar != null) {
                dialogo = dialogoEditar(f, gastos.buscarPorId(editar));
            } else if (nuevo != null) {
                dialogo = dialogo(f, null, "", null, "", LocalDate.now(RelojColombia.reloj()).toString(), null);
            }
        } catch (ResourceNotFoundException e) {
            error = e.getMessage();
        }
        return pagina(f, ok, error, dialogo, confirmar);
    }

    // ===================== Acciones =====================

    @POST
    @Path("/guardar")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response guardar(@FormParam("id") Long id,
                            @FormParam("nombre") String nombre,
                            @FormParam("categoria") String categoria,
                            @FormParam("monto") String monto,
                            @FormParam("fechaGasto") String fechaGasto,
                            @FormParam("desde") String desde,
                            @FormParam("hasta") String hasta,
                            @FormParam("filtroCategoria") String filtroCategoria,
                            @FormParam("pagina") Integer pagina) {
        Filtro f = filtro(desde, hasta, filtroCategoria, pagina);
        try {
            CategoriaGasto cat = categoriaObligatoria(categoria);
            BigDecimal valor = montoDe(monto);
            LocalDate fecha = fechaDe(fechaGasto);

            if (id == null) {
                gastos.registrar(nombre, valor, fecha, cat);
            } else {
                gastos.actualizar(id, nombre, valor, fecha, cat);
            }

            // Si el gasto cae fuera del rango que se está viendo, se lleva el listado a su mes
            Filtro destino = f;
            if (fecha != null && (fecha.isBefore(f.desde()) || fecha.isAfter(f.hasta()))) {
                destino = new Filtro(fecha.withDayOfMonth(1), fecha.withDayOfMonth(fecha.lengthOfMonth()), null, 1);
            }
            return redirigir(destino, id == null ? "Gasto registrado correctamente." : "Gasto actualizado correctamente.");
        } catch (ResourceNotFoundException e) {
            return conError(f, null, e.getMessage());
        } catch (BusinessRuleException e) {
            return conError(f, dialogo(f, id, nombre, categoria, monto, fechaGasto, e.getMessage()), null);
        } catch (RuntimeException e) {
            LOG.error("No se pudo guardar el gasto " + id, e);
            return conError(f, dialogo(f, id, nombre, categoria, monto, fechaGasto,
                    "No se pudo guardar: " + e.getMessage()), null);
        }
    }

    @POST
    @Path("/eliminar")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_HTML)
    public Response eliminar(@FormParam("id") Long id,
                             @FormParam("desde") String desde,
                             @FormParam("hasta") String hasta,
                             @FormParam("filtroCategoria") String filtroCategoria,
                             @FormParam("pagina") Integer pagina) {
        Filtro f = filtro(desde, hasta, filtroCategoria, pagina);
        try {
            gastos.eliminar(id);
            return redirigir(f, "Gasto eliminado.");
        } catch (ResourceNotFoundException | BusinessRuleException e) {
            return conError(f, null, e.getMessage());
        } catch (RuntimeException e) {
            LOG.error("No se pudo eliminar el gasto " + id, e);
            return conError(f, null, "No se pudo eliminar: " + e.getMessage());
        }
    }

    private Response redirigir(Filtro f, String mensaje) {
        String destino = url(f, f.pagina(), "&ok=" + URLEncoder.encode(mensaje, StandardCharsets.UTF_8));
        return Response.seeOther(URI.create(destino)).build();
    }

    private Response conError(Filtro f, DialogoVista dialogo, String error) {
        return Response.ok(pagina(f, null, error, dialogo, null).render()).type(MediaType.TEXT_HTML).build();
    }

    // ===================== Armado de la página =====================

    private TemplateInstance pagina(Filtro f, String ok, String error, DialogoVista dialogo, EliminarVista eliminar) {
        LocalDate hoy = LocalDate.now(RelojColombia.reloj());

        List<Gasto> delRango = new ArrayList<>();
        String aviso = error;
        try {
            delRango.addAll(gastos.listarPorRango(f.desde(), f.hasta()));
        } catch (BusinessRuleException e) {
            aviso = e.getMessage();   // p. ej. rango invertido: se muestra el aviso y el listado queda vacío
        }

        List<Gasto> filtrados = delRango.stream()
                .filter(g -> f.categoria() == null || f.categoria() == g.getCategoria())
                .sorted(Comparator.comparing(Gasto::getFechaGasto, Comparator.nullsLast(Comparator.<LocalDate>reverseOrder()))
                        .thenComparing(Gasto::getId, Comparator.nullsLast(Comparator.<Long>reverseOrder())))
                .toList();

        // ---- Paginación en memoria ----
        int total = filtrados.size();
        int paginas = Math.max(1, (total + POR_PAGINA - 1) / POR_PAGINA);
        int actual = Math.min(Math.max(f.pagina(), 1), paginas);
        int ini = (actual - 1) * POR_PAGINA;
        int fin = Math.min(total, ini + POR_PAGINA);
        Filtro fa = f.conPagina(actual);
        List<FilaVista> filas = filtrados.subList(ini, fin).stream().map(g -> fila(g, fa)).toList();

        return index
                .data("filtro", new FiltroVista(f.desde().toString(), f.hasta().toString(),
                        f.categoria() == null ? "" : f.categoria().name(), actual))
                .data("hoyIso", hoy.toString())
                .data("base", BASE)
                .data("resumen", resumen(filtrados, f))
                .data("atajos", atajos(f, hoy))
                .data("categoriasFiltro", categorias(f.categoria() == null ? null : f.categoria().name()))
                .data("filas", filas)
                .data("desglose", desglose(filtrados))
                .data("paginacion", new PaginacionVista(total == 0 ? 0 : ini + 1, fin, total, actual > 1, actual < paginas,
                        url(fa, actual - 1, ""), url(fa, actual + 1, ""), itemsPagina(fa, actual, paginas)))
                .data("hrefNuevo", url(fa, actual, "&nuevo=1"))
                .data("hrefLimpiar", url(new Filtro(hoy.withDayOfMonth(1), hoy.withDayOfMonth(hoy.lengthOfMonth()), null, 1), 1, ""))
                .data("ok", ok)
                .data("error", aviso)
                .data("dialogo", dialogo)
                .data("eliminar", eliminar);
    }

    private ResumenVista resumen(List<Gasto> lista, Filtro f) {
        BigDecimal total = suma(lista);
        BigDecimal promedio = lista.isEmpty() ? BigDecimal.ZERO
                : total.divide(BigDecimal.valueOf(lista.size()), 0, RoundingMode.HALF_UP);
        return new ResumenVista(cop(total), cop(promedio), lista.size(), fechaCorta(f.desde()) + " – " + fechaCorta(f.hasta()));
    }

    private FilaVista fila(Gasto g, Filtro f) {
        CategoriaGasto c = g.getCategoria();
        return new FilaVista(g.getId(), fechaCorta(g.getFechaGasto()), g.getNombre(),
                c == null ? "Sin categoría" : etiqueta(c), c == null ? "bi-box-seam" : icono(c), c == null ? "t-gris" : tono(c),
                cop(g.getMonto()), url(f, f.pagina(), "&editar=" + g.getId()), url(f, f.pagina(), "&eliminar=" + g.getId()));
    }

    // "En qué se va la plata": gasto por categoría, de mayor a menor
    private List<DesgloseVista> desglose(List<Gasto> lista) {
        Map<CategoriaGasto, BigDecimal> porCategoria = new EnumMap<>(CategoriaGasto.class);
        for (Gasto g : lista) {
            if (g.getCategoria() != null && g.getMonto() != null) {
                porCategoria.merge(g.getCategoria(), g.getMonto(), BigDecimal::add);
            }
        }
        BigDecimal total = suma(lista);
        return porCategoria.entrySet().stream()
                .sorted(Map.Entry.<CategoriaGasto, BigDecimal>comparingByValue().reversed())
                .limit(6)
                .map(e -> new DesgloseVista(etiqueta(e.getKey()), icono(e.getKey()), tono(e.getKey()), cop(e.getValue()),
                        total.signum() == 0 ? 0 : e.getValue().multiply(BigDecimal.valueOf(100))
                                .divide(total, 0, RoundingMode.HALF_UP).intValue()))
                .toList();
    }

    // Rangos rápidos (enlaces, sin JS)
    private List<AtajoVista> atajos(Filtro f, LocalDate hoy) {
        LocalDate iniMes = hoy.withDayOfMonth(1);
        LocalDate mesPrevio = iniMes.minusMonths(1);
        List<LocalDate[]> rangos = List.of(
                new LocalDate[]{iniMes, hoy.withDayOfMonth(hoy.lengthOfMonth())},
                new LocalDate[]{mesPrevio, mesPrevio.withDayOfMonth(mesPrevio.lengthOfMonth())},
                new LocalDate[]{hoy.minusDays(29), hoy},
                new LocalDate[]{hoy.withDayOfYear(1), hoy.withDayOfYear(hoy.lengthOfYear())});
        String[] textos = {"Este mes", "Mes anterior", "Últimos 30 días", "Este año"};

        List<AtajoVista> lista = new ArrayList<>();
        for (int i = 0; i < rangos.size(); i++) {
            LocalDate d = rangos.get(i)[0];
            LocalDate h = rangos.get(i)[1];
            lista.add(new AtajoVista(textos[i], url(f.conRango(d, h), 1, ""), f.desde().equals(d) && f.hasta().equals(h)));
        }
        return lista;
    }

    private List<PaginaItemVista> itemsPagina(Filtro f, int actual, int paginas) {
        List<PaginaItemVista> items = new ArrayList<>();
        for (int p = Math.max(1, actual - 2); p <= Math.min(paginas, actual + 2); p++) {
            items.add(new PaginaItemVista(p, url(f, p, ""), p == actual));
        }
        return items;
    }

    // ===================== Diálogos =====================

    private DialogoVista dialogoEditar(Filtro f, Gasto g) {
        return dialogo(f, g.getId(), g.getNombre(), g.getCategoria() == null ? null : g.getCategoria().name(),
                g.getMonto() == null ? "" : g.getMonto().setScale(0, RoundingMode.HALF_UP).toPlainString(),
                g.getFechaGasto() == null ? "" : g.getFechaGasto().toString(), null);
    }

    private DialogoVista dialogo(Filtro f, Long id, String nombre, String categoria, String monto, String fecha,
                                 String error) {
        return new DialogoVista(id, id != null, id == null ? "Registrar gasto" : "Editar gasto",
                nombre == null ? "" : nombre, monto == null ? "" : monto, fecha == null ? "" : fecha, error,
                url(f, f.pagina(), ""), categorias(categoria));
    }

    private EliminarVista confirmarEliminar(Filtro f, Gasto g) {
        return new EliminarVista(g.getId(), g.getNombre(), cop(g.getMonto()) + " · " + fechaCorta(g.getFechaGasto()),
                url(f, f.pagina(), ""));
    }

    private List<CategoriaVista> categorias(String seleccionada) {
        List<CategoriaVista> lista = new ArrayList<>();
        for (CategoriaGasto c : CategoriaGasto.values()) {
            lista.add(new CategoriaVista(c.name(), etiqueta(c), descripcion(c), icono(c), tono(c),
                    c.name().equals(seleccionada)));
        }
        return lista;
    }

    // ===================== Presentación de las categorías =====================
    // (es de la capa web: el dominio no sabe de textos, iconos ni colores)

    private static String etiqueta(CategoriaGasto c) {
        return switch (c) {
            case INSUMOS -> "Insumos de grooming";
            case SERVICIOS_PUBLICOS -> "Servicios públicos";
            case ALQUILER_LOCAL -> "Alquiler del local";
            case MANTENIMIENTO_EQUIPOS -> "Mantenimiento de equipos";
            case PERSONAL -> "Personal";
            case HERRAMIENTAS -> "Herramientas";
            case LIMPIEZA_DESINFECCION -> "Limpieza y desinfección";
            case INTERNET_TELEFONIA -> "Internet y telefonía";
            case PRODUCTOS_VENTA -> "Productos para la venta";
            case PUBLICIDAD -> "Publicidad y redes";
            case IMPUESTOS_TRAMITES -> "Impuestos y trámites";
            case CAPACITACION -> "Capacitación";
            case OTROS -> "Otros";
        };
    }

    private static String descripcion(CategoriaGasto c) {
        return switch (c) {
            case INSUMOS -> "Shampoos, acondicionadores, perfumes y toallas";
            case SERVICIOS_PUBLICOS -> "Agua, energía y gas";
            case ALQUILER_LOCAL -> "Canon del local y administración";
            case MANTENIMIENTO_EQUIPOS -> "Afilado de cuchillas y reparación de equipos";
            case PERSONAL -> "Sueldos, prestaciones y pagos a groomers";
            case HERRAMIENTAS -> "Tijeras, máquinas de corte, secadores y mesas";
            case LIMPIEZA_DESINFECCION -> "Desinfectantes, guantes, bolsas y aseo";
            case INTERNET_TELEFONIA -> "Internet, plan de celular y línea fija";
            case PRODUCTOS_VENTA -> "Accesorios, snacks y juguetes para reventa";
            case PUBLICIDAD -> "Pauta, diseño, fotos y material impreso";
            case IMPUESTOS_TRAMITES -> "Cámara de Comercio, DIAN, licencias y seguros";
            case CAPACITACION -> "Cursos de grooming y manejo canino";
            case OTROS -> "Cualquier gasto que no encaje en las demás";
        };
    }

    // Nombre del ícono de Bootstrap Icons (se usa como <i class="bi {icono}">)
    private static String icono(CategoriaGasto c) {
        return switch (c) {
            case INSUMOS -> "bi-droplet-half";
            case SERVICIOS_PUBLICOS -> "bi-lightbulb";
            case ALQUILER_LOCAL -> "bi-house-door";
            case MANTENIMIENTO_EQUIPOS -> "bi-wrench-adjustable";
            case PERSONAL -> "bi-people";
            case HERRAMIENTAS -> "bi-scissors";
            case LIMPIEZA_DESINFECCION -> "bi-stars";
            case INTERNET_TELEFONIA -> "bi-wifi";
            case PRODUCTOS_VENTA -> "bi-bag";
            case PUBLICIDAD -> "bi-megaphone";
            case IMPUESTOS_TRAMITES -> "bi-receipt";
            case CAPACITACION -> "bi-mortarboard";
            case OTROS -> "bi-box-seam";
        };
    }

    private static String tono(CategoriaGasto c) {
        return switch (c) {
            case INSUMOS, LIMPIEZA_DESINFECCION -> "t-cielo";
            case HERRAMIENTAS, MANTENIMIENTO_EQUIPOS -> "t-azul";
            case SERVICIOS_PUBLICOS, INTERNET_TELEFONIA -> "t-ambar";
            case ALQUILER_LOCAL, IMPUESTOS_TRAMITES -> "t-violeta";
            case PERSONAL, CAPACITACION -> "t-verde";
            case PRODUCTOS_VENTA, PUBLICIDAD -> "t-rosa";
            case OTROS -> "t-gris";
        };
    }

    // ===================== Utilidades =====================

    private static Filtro filtro(String desde, String hasta, String categoria, Integer pagina) {
        LocalDate hoy = LocalDate.now(RelojColombia.reloj());
        return new Filtro(fechaOPorDefecto(desde, hoy.withDayOfMonth(1)),
                fechaOPorDefecto(hasta, hoy.withDayOfMonth(hoy.lengthOfMonth())),
                categoriaONull(categoria), pagina == null || pagina < 1 ? 1 : pagina);
    }

    private static LocalDate fechaOPorDefecto(String texto, LocalDate porDefecto) {
        try {
            if (texto != null && !texto.isBlank()) {
                return LocalDate.parse(texto);
            }
        } catch (DateTimeParseException ignored) {
            // fecha inválida: se usa la de por defecto
        }
        return porDefecto;
    }

    private static CategoriaGasto categoriaONull(String texto) {
        try {
            return texto == null || texto.isBlank() ? null : CategoriaGasto.valueOf(texto);
        } catch (IllegalArgumentException e) {
            return null;
        }
    }

    private static CategoriaGasto categoriaObligatoria(String texto) {
        CategoriaGasto c = categoriaONull(texto);
        if (c == null) {
            throw new BusinessRuleException("La categoría del gasto es obligatoria");
        }
        return c;
    }

    private static BigDecimal montoDe(String texto) {
        if (texto == null || texto.isBlank()) {
            throw new BusinessRuleException("El monto del gasto es obligatorio");
        }
        try {
            return new BigDecimal(texto.trim());
        } catch (NumberFormatException e) {
            throw new BusinessRuleException("El monto indicado no es válido");
        }
    }

    private static LocalDate fechaDe(String texto) {
        if (texto == null || texto.isBlank()) {
            return null;   // el dominio la completa con la fecha de hoy
        }
        try {
            return LocalDate.parse(texto);
        } catch (DateTimeParseException e) {
            throw new BusinessRuleException("La fecha indicada no es válida");
        }
    }

    private static BigDecimal suma(List<Gasto> lista) {
        return lista.stream().map(Gasto::getMonto).filter(m -> m != null).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static String url(Filtro f, int pagina, String extra) {
        StringBuilder sb = new StringBuilder(BASE)
                .append("?desde=").append(f.desde())
                .append("&hasta=").append(f.hasta());
        if (f.categoria() != null) {
            sb.append("&categoria=").append(f.categoria().name());
        }
        if (pagina > 1) {
            sb.append("&pagina=").append(pagina);
        }
        return sb.append(extra).toString();
    }

    private static String cop(BigDecimal valor) {
        return "$ " + PESOS.format(valor == null ? BigDecimal.ZERO : valor.setScale(0, RoundingMode.HALF_UP));
    }

    private static String fechaCorta(LocalDate d) {
        return d == null ? "" : String.format("%02d %s %d", d.getDayOfMonth(), MESES_CORTO[d.getMonthValue() - 1], d.getYear());
    }

    private static DecimalFormatSymbols simbolosColombia() {
        DecimalFormatSymbols s = DecimalFormatSymbols.getInstance(Locale.ROOT);
        s.setGroupingSeparator('.');
        s.setDecimalSeparator(',');
        return s;
    }


}

    