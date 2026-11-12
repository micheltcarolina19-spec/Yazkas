(() => {
    'use strict';

    const API = '/api/finanzas';
    const API_CITAS = '/api/citas';
    const API_USUARIOS = '/api/usuarios';
    const API_PRODUCTOS = '/api/productos';
    const UMBRAL_STOCK_BAJO = 5;          // un producto con 5 unidades o menos genera alerta
    const MAX_ALERTAS_CITAS = 3;          // cuántas citas próximas se muestran como alerta
    const ZONA = 'America/Bogota';        // la hora de las alertas siempre es la de Colombia
    const LIMITE_TOP = 5;

    // Series de la gráfica de rendimiento (la clave es el campo que devuelve /api/finanzas/serie)
    // Solo "Ingresos" lleva relleno: tres áreas encimadas se veían sucias
    const SERIES = [
        { clave: 'ingresos', nombre: 'Ingresos', color: '#2f9be0', relleno: 0.22 },
        { clave: 'gastos',   nombre: 'Gastos',   color: '#f59f2b', relleno: 0 },
        { clave: 'utilidad', nombre: 'Utilidad', color: '#1e9e5a', relleno: 0 }
    ];

    const COLORES = ['#1a5c82', '#2f9be0', '#5cbdf8', '#9bd8fb', '#cfeafd'];
    const NOMBRES_GASTO = {
        INSUMOS: 'Insumos',
        SERVICIOS_PUBLICOS: 'Servicios públicos',
        ALQUILER_LOCAL: 'Alquiler del local',
        MANTENIMIENTO_EQUIPOS: 'Mantenimiento de equipos',
        PERSONAL: 'Personal'
    };

    // Estados de la cita (los del proyecto). Las CANCELADA no se muestran en la agenda
    const ESTADOS_CITA = {
        PROGRAMADA: { texto: 'Programada', clase: 'is-programada' },
        REPROGRAMADA: { texto: 'Reprogramada', clase: 'is-reprogramada' },
        COMPLETADA: { texto: 'Completada', clase: 'is-completada' }
    };

    const $ = (id) => document.getElementById(id);
    const peso = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
    const num = (v) => Number(v) || 0;

    /* ---------- Fechas (en hora local, sin pasar por UTC) ---------- */
    const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const diasDelMes = (y, m) => new Date(y, m + 1, 0).getDate();

    // Cada periodo se compara contra el periodo equivalente anterior
    function rangos(preset) {
        // "hoy" según Colombia, no según la zona horaria del equipo
        const [y, m1, d] = ahoraColombia().fecha.split('-').map(Number);
        const m = m1 - 1;
        const hoy = new Date(y, m, d);

        switch (preset) {
            case 'mes-anterior':
                return { ini: new Date(y, m - 1, 1), fin: new Date(y, m, 0),
                    pIni: new Date(y, m - 2, 1), pFin: new Date(y, m - 1, 0) };
            case '7d': {
                const ini = new Date(y, m, d - 6);
                return { ini, fin: hoy, pIni: new Date(y, m, d - 13), pFin: new Date(y, m, d - 7) };
            }
            case 'anio':
                return { ini: new Date(y, 0, 1), fin: hoy, pIni: new Date(y - 1, 0, 1), pFin: new Date(y - 1, m, d) };
            default: { // mes en curso vs. los mismos días del mes anterior
                const prevM = new Date(y, m - 1, 1);
                const diaPrev = Math.min(d, diasDelMes(prevM.getFullYear(), prevM.getMonth()));
                return { ini: new Date(y, m, 1), fin: hoy,
                    pIni: prevM, pFin: new Date(prevM.getFullYear(), prevM.getMonth(), diaPrev) };
            }
        }
    }

    /* ---------- API ---------- */
    async function getJson(url) {
        const res = await fetch(url, { headers: { Accept: 'application/json' } });
        if (!res.ok) {
            let msg = 'No se pudo cargar la información';
            try { msg = (await res.json()).message || msg; } catch { /* sin cuerpo */ }
            throw new Error(msg);
        }
        return res.json();
    }

    const qs = (ini, fin) => `inicio=${iso(ini)}&fin=${iso(fin)}`;

    /* ---------- Pintado ---------- */
    // Flecha y porcentaje contra el periodo anterior (en gastos, subir es malo: invertir = true).
    // Devuelve false si no hay base de comparación (periodo anterior en 0)
    function aplicarDelta(el, cur, prev, invertir = false) {
        el.className = 'kpi__delta';
        if (prev === 0) {
            el.textContent = '';
            return false;
        }
        const pct = ((cur - prev) / Math.abs(prev)) * 100;
        if (Math.abs(pct) < 0.05) {
            el.textContent = '0.0%';
            el.classList.add('is-flat');
            return true;
        }
        const sube = pct > 0;
        el.textContent = `${sube ? '↑' : '↓'} ${Math.abs(pct).toFixed(1)}%`;
        el.classList.add(sube !== invertir ? 'is-good' : 'is-bad');
        return true;
    }

    function pintarKpis(actual, previo) {
        document.querySelectorAll('[data-kpi]').forEach((el) => {
            const campo = el.dataset.kpi;
            const invertir = el.dataset.invert === 'true'; // en gastos, subir es malo
            const cur = num(actual[campo]);
            const prev = num(previo[campo]);

            const valor = el.querySelector('[data-value]');
            valor.textContent = peso.format(cur);
            valor.classList.toggle('is-negative', cur < 0); // valores negativos en rojo

            aplicarDelta(el.querySelector('[data-delta]'), cur, prev, invertir);
        });

        $('dash-margen').textContent = `Margen ${num(actual.margenUtilidad).toFixed(1)}%`;
    }

    function pintarTop(lista) {
        const ol = $('dash-top');
        ol.replaceChildren();
        $('dash-top-empty').hidden = lista.length > 0;
        const max = Math.max(1, ...lista.map((p) => p.unidadesVendidas));

        lista.forEach((p, i) => {
            const li = document.createElement('li');
            li.className = 'top__item';

            const rank = document.createElement('span');
            rank.className = 'top__rank';
            rank.textContent = i + 1;

            const thumb = document.createElement('span');
            thumb.className = 'top__thumb';
            const icono = '<svg viewBox="0 0 24 24" aria-hidden="true"><use href="#dash-i-box"/></svg>';
            if (p.fotoProducto) {
                const img = document.createElement('img');
                img.src = p.fotoProducto;
                img.alt = '';
                img.loading = 'lazy';
                img.addEventListener('error', () => { thumb.innerHTML = icono; }, { once: true });
                thumb.append(img);
            } else {
                thumb.innerHTML = icono;
            }

            const info = document.createElement('div');
            const name = document.createElement('div');
            name.className = 'top__name';
            name.textContent = p.nombre;
            name.title = p.nombre;
            const bar = document.createElement('div');
            bar.className = 'top__bar';
            const fill = document.createElement('span');
            fill.style.width = `${(p.unidadesVendidas / max) * 100}%`;
            bar.append(fill);
            info.append(name, bar);

            const units = document.createElement('span');
            units.className = 'top__units';
            units.textContent = `${p.unidadesVendidas} und.`;
            const total = document.createElement('small');
            total.textContent = peso.format(num(p.totalVendido));
            units.append(total);

            li.append(rank, thumb, info, units);
            ol.append(li);
        });
    }

    function pintarGastos(porCategoria, total) {
        const filas = Object.entries(porCategoria || {})
            .map(([k, v]) => ({ clave: k, valor: num(v) }))
            .sort((a, b) => b.valor - a.valor);
        const suma = filas.reduce((acc, f) => acc + f.valor, 0);

        $('dash-gastos-total').textContent = peso.format(num(total));

        const donut = $('dash-donut');
        const legend = $('dash-legend');
        legend.replaceChildren();

        if (suma <= 0) {
            donut.style.background = 'conic-gradient(#e3f0f9 0 100%)';
            $('dash-donut-pct').textContent = '0%';
            $('dash-donut-name').textContent = 'Sin gastos';
            return;
        }

        let acumulado = 0;
        const tramos = [];
        filas.forEach((f, i) => {
            const color = COLORES[i % COLORES.length];
            const desde = acumulado;
            acumulado += (f.valor / suma) * 100;
            if (f.valor > 0) tramos.push(`${color} ${desde}% ${acumulado}%`);

            const li = document.createElement('li');
            const dot = document.createElement('i');
            dot.style.background = color;
            const nombre = document.createElement('span');
            nombre.textContent = NOMBRES_GASTO[f.clave] || f.clave;
            const pct = document.createElement('b');
            pct.textContent = `${Math.round((f.valor / suma) * 100)}%`;
            li.append(dot, nombre, pct);
            legend.append(li);
        });
        donut.style.background = `conic-gradient(${tramos.join(', ')})`;

        const top = filas[0];
        $('dash-donut-pct').textContent = `${Math.round((top.valor / suma) * 100)}%`;
        $('dash-donut-name').textContent = NOMBRES_GASTO[top.clave] || top.clave;
    }

    /* ---------- Agenda de hoy ---------- */
    // "09:00:00" -> "9:00 a. m."
    function hora12(hora) {
        if (!hora) return '';
        const [h, m] = hora.split(':').map(Number);
        const d = new Date();
        d.setHours(h, m || 0, 0, 0);
        return d.toLocaleTimeString('es-CO', { hour: 'numeric', minute: '2-digit' });
    }

    function pintarAgenda(citas) {
        const ul = $('dash-agenda');
        const vacio = $('dash-agenda-empty');
        const contador = $('dash-agenda-count');
        ul.replaceChildren();

        const lista = (citas || [])
            .filter((c) => c.estado !== 'CANCELADA')
            .sort((a, b) => String(a.horaAgendamiento).localeCompare(String(b.horaAgendamiento)));

        vacio.textContent = 'No hay citas para hoy.';
        vacio.hidden = lista.length > 0;
        contador.hidden = lista.length === 0;
        contador.textContent = `${lista.length} ${lista.length === 1 ? 'cita' : 'citas'}`;

        lista.forEach((c) => {
            const li = document.createElement('li');
            li.className = 'agenda__item';

            // Foto de la mascota (si no hay o falla la URL, se muestra la huella)
            const thumb = document.createElement('span');
            thumb.className = 'agenda__thumb';
            const icono = '<svg viewBox="0 0 24 24" aria-hidden="true"><use href="#dash-i-paw"/></svg>';
            if (c.fotoMascota) {
                const img = document.createElement('img');
                img.src = c.fotoMascota;
                img.alt = c.nombreMascota || 'Mascota';
                img.loading = 'lazy';
                img.addEventListener('error', () => { thumb.innerHTML = icono; }, { once: true });
                thumb.append(img);
            } else {
                thumb.innerHTML = icono;
            }

            const info = document.createElement('div');
            info.className = 'agenda__info';
            const hora = document.createElement('div');
            hora.className = 'agenda__time';
            hora.textContent = hora12(c.horaAgendamiento);
            const nombre = document.createElement('div');
            nombre.className = 'agenda__name';
            nombre.textContent = c.nombreMascota || 'Mascota';
            const meta = document.createElement('div');
            meta.className = 'agenda__meta';
            meta.textContent = [c.razaMascota, c.nombrePropietario].filter(Boolean).join(' · ');
            meta.title = meta.textContent;
            info.append(hora, nombre, meta);

            const estado = ESTADOS_CITA[c.estado] || { texto: c.estado || '', clase: 'is-programada' };
            const badge = document.createElement('span');
            badge.className = `agenda__badge ${estado.clase}`;
            badge.textContent = estado.texto;

            li.append(thumb, info, badge);
            ul.append(li);
        });
    }

    /* ---------- Hora de Colombia ---------- */
    // Fecha (YYYY-MM-DD) y minutos desde las 00:00 en Colombia, sin importar la zona horaria del equipo
    function ahoraColombia() {
        const p = Object.fromEntries(new Intl.DateTimeFormat('en-CA', {
            timeZone: ZONA, year: 'numeric', month: '2-digit', day: '2-digit',
            hour: '2-digit', minute: '2-digit', hourCycle: 'h23'
        }).formatToParts(new Date()).map((x) => [x.type, x.value]));
        return { fecha: `${p.year}-${p.month}-${p.day}`, minutos: Number(p.hour) * 60 + Number(p.minute) };
    }

    // "16:00:00" -> 960
    function minutosDe(hora) {
        const [h, m] = String(hora || '0:0').split(':').map(Number);
        return h * 60 + (m || 0);
    }

    function textoFaltan(min) {
        if (min <= 0) return 'ahora';
        if (min < 60) return `en ${min} min`;
        const h = Math.floor(min / 60), m = min % 60;
        return m ? `en ${h} h ${m} min` : `en ${h} h`;
    }

    /* ---------- Alertas (stock bajo + próximas citas) ---------- */
    let agendaHoy = [];        // citas de hoy tal como las devolvió la API
    let productosBajos = [];   // productos con stock <= UMBRAL_STOCK_BAJO

    function crearAlerta(clase, icono, titulo, detalle) {
        const li = document.createElement('li');
        li.className = `alerta ${clase}`;
        li.innerHTML = `<svg class="alerta__ico" viewBox="0 0 24 24" aria-hidden="true"><use href="#dash-i-${icono}"/></svg>`;
        const txt = document.createElement('div');
        txt.className = 'alerta__txt';
        const t = document.createElement('strong');
        t.textContent = titulo;
        const d = document.createElement('span');
        d.textContent = detalle;
        d.title = detalle;
        txt.append(t, d);
        li.append(txt);
        return li;
    }

    // Se vuelve a calcular cada minuto: apenas pasa la hora de una cita, su alerta desaparece
    function pintarAlertas() {
        const ul = $('dash-alertas');
        const ahora = ahoraColombia();
        ul.replaceChildren();

        const citas = agendaHoy
            .filter((c) => (c.estado === 'PROGRAMADA' || c.estado === 'REPROGRAMADA')
                && c.fechaAgendamiento === ahora.fecha
                && minutosDe(c.horaAgendamiento) >= ahora.minutos)
            .sort((a, b) => minutosDe(a.horaAgendamiento) - minutosDe(b.horaAgendamiento))
            .slice(0, MAX_ALERTAS_CITAS);

        citas.forEach((c) => {
            const faltan = textoFaltan(minutosDe(c.horaAgendamiento) - ahora.minutos);
            ul.append(crearAlerta('alerta--cita', 'calendar', 'Cita próxima',
                `${c.nombreMascota || 'Mascota'} · ${hora12(c.horaAgendamiento)} · ${faltan}`));
        });

        productosBajos.forEach((p) => {
            const agotado = num(p.stock) <= 0;
            ul.append(crearAlerta('alerta--stock', 'box',
                agotado ? 'Producto agotado' : 'Producto con stock bajo',
                `${p.nombre} · ${num(p.stock)} und.`));
        });

        const total = ul.children.length;
        $('dash-alertas-empty').hidden = total > 0;
        $('dash-alertas-count').hidden = total === 0;
        $('dash-alertas-count').textContent = total;
    }

    async function cargarStock() {
        try {
            const lista = await getJson(API_PRODUCTOS);
            productosBajos = lista
                .filter((p) => num(p.stock) <= UMBRAL_STOCK_BAJO)
                .sort((a, b) => num(a.stock) - num(b.stock));
        } catch {
            productosBajos = [];
        }
        pintarAlertas();
    }

    /* ---------- Clientes ---------- */
    async function cargarClientes() {
        try {
            const usuarios = await getJson(API_USUARIOS);
            $('dash-clientes').textContent =
                new Intl.NumberFormat('es-CO').format(usuarios.filter((u) => u.rol === 'CLIENTE').length);
        } catch {
            $('dash-clientes').textContent = '—';
        }
    }

    // La agenda es siempre la de hoy (fecha de Colombia): no depende del filtro de periodo
    async function cargarAgenda() {
        try {
            const hoy = ahoraColombia().fecha;
            agendaHoy = await getJson(`${API_CITAS}?inicio=${hoy}&fin=${hoy}`);
            pintarAgenda(agendaHoy);
        } catch (e) {
            agendaHoy = [];
            $('dash-agenda').replaceChildren();
            $('dash-agenda-count').hidden = true;
            const vacio = $('dash-agenda-empty');
            vacio.textContent = e.message || 'No se pudo cargar la agenda';
            vacio.hidden = false;
        }
        pintarAlertas();
    }

    /* ---------- Gráficas (Chart.js) ---------- */
    let agrupacion = 'DIA';        // DIA, SEMANA o MES (lo que entiende /api/finanzas/serie)
    let chartFinanzas = null;
    let chartVentas = null;
    let puntosFinanzas = [];       // tramos que se están dibujando (para los tooltips)
    let puntosVentas = [];
    let tokenGraf = 0;             // ignora respuestas viejas si se cambia el periodo o la agrupación rápido

    // Las fechas llegan como YYYY-MM-DD: se leen como UTC para que ninguna zona horaria las corra de día
    const utc = (s) => new Date(`${s}T00:00:00Z`);
    const fmtDia = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', timeZone: 'UTC' });
    const fmtDiaAnio = new Intl.DateTimeFormat('es-CO', { day: '2-digit', month: 'short', year: 'numeric', timeZone: 'UTC' });
    const fmtMes = new Intl.DateTimeFormat('es-CO', { month: 'short', timeZone: 'UTC' });
    const sinPunto = (t) => t.replace(/\./g, '');
    const capitalizar = (t) => t.charAt(0).toUpperCase() + t.slice(1);

    const etiquetaPunto = (p) => agrupacion === 'MES'
        ? capitalizar(sinPunto(fmtMes.format(utc(p.inicio))))
        : sinPunto(fmtDia.format(utc(p.inicio)));

    const rangoPunto = (p) => p.inicio === p.fin
        ? sinPunto(fmtDiaAnio.format(utc(p.inicio)))
        : `${sinPunto(fmtDia.format(utc(p.inicio)))} – ${sinPunto(fmtDiaAnio.format(utc(p.fin)))}`;

    // 2.450.000 -> $2,5M · 500.000 -> $500K
    function pesoCorto(v) {
        const abs = Math.abs(v);
        let s;
        if (abs >= 1e6) s = `${(abs / 1e6).toFixed(abs % 1e6 === 0 ? 0 : 1)}M`;
        else if (abs >= 1e3) s = `${Math.round(abs / 1e3)}K`;
        else s = String(abs);
        return `${v < 0 ? '-' : ''}$${s.replace('.', ',')}`;
    }

    function rgba(hex, alfa) {
        const n = parseInt(hex.slice(1), 16);
        return `rgba(${(n >> 16) & 255}, ${(n >> 8) & 255}, ${n & 255}, ${alfa})`;
    }

    // Relleno degradado (de arriba hacia abajo) que se calcula cuando la gráfica ya tiene tamaño
    function degradado(colorArriba, colorAbajo) {
        return (c) => {
            const { ctx, chartArea } = c.chart;
            if (!chartArea) return 'transparent';
            const g = ctx.createLinearGradient(0, chartArea.top, 0, chartArea.bottom);
            g.addColorStop(0, colorArriba);
            g.addColorStop(1, colorAbajo);
            return g;
        };
    }

    const TOOLTIP = {
        backgroundColor: '#0f3a57', padding: 10, cornerRadius: 10, boxPadding: 4,
        titleFont: { weight: '700' }
    };

    // Interpolación "monotone": la curva no se pasa de los datos. Solo se rellena la serie con "relleno" > 0
    function crearGraficaFinanzas() {
        Chart.defaults.font.family = 'system-ui, -apple-system, "Segoe UI", Roboto, sans-serif';
        Chart.defaults.color = '#5d7a90';

        return new Chart($('chart-finanzas'), {
            type: 'line',
            data: {
                labels: [],
                datasets: SERIES.map((s) => ({
                    label: s.nombre,
                    data: [],
                    borderColor: s.color,
                    borderWidth: 2.5,
                    tension: 0,
                    cubicInterpolationMode: 'monotone',
                    fill: s.relleno > 0 ? 'origin' : false,
                    backgroundColor: s.relleno > 0
                        ? degradado(rgba(s.color, s.relleno), rgba(s.color, 0))
                        : 'transparent',
                    pointBackgroundColor: s.color,
                    pointBorderColor: '#fff',
                    pointBorderWidth: 1.5,
                    pointRadius: 3,
                    pointHoverRadius: 6
                }))
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        ...TOOLTIP,
                        usePointStyle: true,
                        callbacks: {
                            title: (items) => rangoPunto(puntosFinanzas[items[0].dataIndex]),
                            label: (c) => ` ${c.dataset.label}: ${peso.format(c.parsed.y)}`
                        }
                    }
                },
                scales: {
                    x: { grid: { display: false }, border: { display: false },
                        ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 8 } },
                    y: {
                        beginAtZero: true, grace: '8%', border: { display: false },
                        // la línea del $0 se marca más fuerte para leer bien los valores negativos
                        grid: { color: (ctx) => (ctx.tick && ctx.tick.value === 0 ? '#b7d3e6' : '#e3f0f9') },
                        ticks: { callback: (v) => pesoCorto(v), maxTicksLimit: 6 }
                    }
                }
            }
        });
    }

    function crearGraficaVentas() {
        return new Chart($('chart-ventas'), {
            type: 'bar',
            data: {
                labels: [],
                datasets: [{
                    label: 'Ventas',
                    data: [],
                    borderRadius: 6,
                    borderSkipped: false,
                    barPercentage: 0.7,
                    categoryPercentage: 0.9,
                    maxBarThickness: 28,
                    backgroundColor: degradado('#9bd8fb', '#2f9be0'),
                    hoverBackgroundColor: '#1a5c82'
                }]
            },
            options: {
                responsive: true,
                maintainAspectRatio: false,
                interaction: { mode: 'index', intersect: false },
                plugins: {
                    legend: { display: false },
                    tooltip: {
                        ...TOOLTIP,
                        displayColors: false,
                        callbacks: {
                            title: (items) => rangoPunto(puntosVentas[items[0].dataIndex]),
                            label: (c) => {
                                const p = puntosVentas[c.dataIndex];
                                const n = num(p.cantidadVentas);
                                return [`${n} ${n === 1 ? 'venta' : 'ventas'}`, peso.format(num(p.valorVentas))];
                            }
                        }
                    }
                },
                scales: {
                    x: {
                        grid: { display: false }, border: { display: false },
                        ticks: { maxRotation: 0, autoSkip: true, maxTicksLimit: 6, font: { size: 11 } }
                    },
                    y: { display: false, beginAtZero: true, grace: '10%' }
                }
            }
        });
    }

    function pintarGraficaFinanzas(puntos) {
        puntosFinanzas = puntos;
        const hay = puntos.some((p) => num(p.ingresos) || num(p.gastos));
        $('chart-finanzas-empty').textContent = 'Sin movimientos en este periodo.';
        $('chart-finanzas-box').hidden = !hay;
        $('chart-finanzas-empty').hidden = hay;
        if (!hay) return;

        if (!chartFinanzas) chartFinanzas = crearGraficaFinanzas();
        chartFinanzas.data.labels = puntos.map(etiquetaPunto);
        SERIES.forEach((s, i) => {
            const ds = chartFinanzas.data.datasets[i];
            ds.data = puntos.map((p) => num(p[s.clave]));
            ds.pointRadius = puntos.length > 45 ? 0 : 3;   // con muchos días los puntos estorban
        });
        chartFinanzas.update();
    }

    function pintarVentas(puntos, previos) {
        puntosVentas = puntos;
        const sumar = (lista, campo) => lista.reduce((acc, p) => acc + num(p[campo]), 0);
        const cantidad = sumar(puntos, 'cantidadVentas');
        const valor = sumar(puntos, 'valorVentas');

        $('ventas-total').textContent = new Intl.NumberFormat('es-CO').format(cantidad);
        $('ventas-valor').textContent = peso.format(valor);
        $('ventas-ticket').textContent = peso.format(cantidad ? Math.round(valor / cantidad) : 0);
        $('ventas-vs').hidden = !aplicarDelta($('ventas-delta'), cantidad, sumar(previos, 'cantidadVentas'));

        $('chart-ventas-empty').textContent = 'Sin ventas en este periodo.';
        $('chart-ventas-box').hidden = cantidad === 0;
        $('chart-ventas-empty').hidden = cantidad !== 0;
        if (cantidad === 0) return;

        if (!chartVentas) chartVentas = crearGraficaVentas();
        chartVentas.data.labels = puntos.map(etiquetaPunto);
        chartVentas.data.datasets[0].data = puntos.map((p) => num(p.cantidadVentas));
        chartVentas.update();
    }

    function mostrarErrorGraficas(msg) {
        ['finanzas', 'ventas'].forEach((k) => {
            $(`chart-${k}-box`).hidden = true;
            const aviso = $(`chart-${k}-empty`);
            aviso.textContent = msg;
            aviso.hidden = false;
        });
    }

    // Una sola consulta por periodo: el backend reparte ingresos, gastos y ventas por tramo
    async function cargarGraficas() {
        const mio = ++tokenGraf;
        if (typeof Chart === 'undefined') {
            mostrarErrorGraficas('No se cargó la librería de gráficas');
            return;
        }
        const r = rangos($('dash-periodo').value);
        const ag = agrupacion;
        try {
            const [actual, previa] = await Promise.all([
                getJson(`${API}/serie?${qs(r.ini, r.fin)}&agrupar=${ag}`),
                getJson(`${API}/serie?${qs(r.pIni, r.pFin)}&agrupar=${ag}`)
            ]);
            if (mio !== tokenGraf) return;
            pintarGraficaFinanzas(actual);
            pintarVentas(actual, previa);
        } catch (e) {
            if (mio !== tokenGraf) return;
            mostrarErrorGraficas(e.message || 'No se pudieron cargar las gráficas');
        }
    }

    function marcarAgrupacion() {
        document.querySelectorAll('[data-agrupar]').forEach((btn) => {
            const activo = btn.dataset.agrupar === agrupacion;
            btn.classList.toggle('is-active', activo);
            btn.setAttribute('aria-pressed', String(activo));
        });
    }

    // Un año se ve mejor por meses; el resto de periodos, por días
    const agrupacionPorDefecto = () => ($('dash-periodo').value === 'anio' ? 'MES' : 'DIA');

    function mostrarError(msg) {
        const box = $('dash-error');
        box.textContent = msg || '';
        box.hidden = !msg;
    }

    /* ---------- Carga ---------- */
    let token = 0; // ignora respuestas viejas si el usuario cambia el periodo rápido

    async function cargar() {
        const mio = ++token;
        const r = rangos($('dash-periodo').value);
        $('dash-kpis').setAttribute('aria-busy', 'true');
        mostrarError('');
        cargarGraficas();

        try {
            const [actual, previo, top] = await Promise.all([
                getJson(`${API}/resumen?${qs(r.ini, r.fin)}`),
                getJson(`${API}/resumen?${qs(r.pIni, r.pFin)}`),
                getJson(`${API}/productos-mas-vendidos?${qs(r.ini, r.fin)}&limite=${LIMITE_TOP}`)
            ]);
            if (mio !== token) return;

            pintarKpis(actual, previo);
            pintarTop(top);
            pintarGastos(actual.gastosPorCategoria, actual.totalGastos);
            $('dash-updated').textContent =
                `Hoy, ${new Date().toLocaleTimeString('es-CO', { hour: '2-digit', minute: '2-digit' })}`;
        } catch (e) {
            if (mio !== token) return;
            mostrarError(e.message || 'No se pudo cargar el dashboard');
        } finally {
            if (mio === token) $('dash-kpis').setAttribute('aria-busy', 'false');
        }
    }

    $('dash-periodo').addEventListener('change', () => {
        agrupacion = agrupacionPorDefecto();
        marcarAgrupacion();
        cargar();
    });

    document.querySelectorAll('[data-agrupar]').forEach((btn) => {
        btn.addEventListener('click', () => {
            agrupacion = btn.dataset.agrupar;
            marcarAgrupacion();
            cargarGraficas();
        });
    });

    // Mostrar u ocultar una serie tocando su nombre en la leyenda
    document.querySelectorAll('[data-serie]').forEach((btn) => {
        btn.setAttribute('aria-pressed', 'true');
        btn.addEventListener('click', () => {
            if (!chartFinanzas) return;
            const i = Number(btn.dataset.serie);
            const visible = chartFinanzas.isDatasetVisible(i);
            chartFinanzas.setDatasetVisibility(i, !visible);
            btn.classList.toggle('is-off', visible);
            btn.setAttribute('aria-pressed', String(!visible));
            chartFinanzas.update();
        });
    });

    agrupacion = agrupacionPorDefecto();
    marcarAgrupacion();
    cargar();
    // Clientes, agenda y alertas: se cargan al abrir y se refrescan solos
    function refrescarOperativo() {
        cargarClientes();
        cargarAgenda();
        cargarStock();
    }
    refrescarOperativo();
    setInterval(refrescarOperativo, 5 * 60 * 1000); // datos nuevos cada 5 minutos
    setInterval(pintarAlertas, 60 * 1000);          // la hora se revisa cada minuto
})();