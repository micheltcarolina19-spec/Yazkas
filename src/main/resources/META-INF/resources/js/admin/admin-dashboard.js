(() => {
    'use strict';

    const API = '/api/finanzas';
    const LIMITE_TOP = 5;

    const COLORES = ['#1a5c82', '#2f9be0', '#5cbdf8', '#9bd8fb', '#cfeafd'];
    const NOMBRES_GASTO = {
        INSUMOS: 'Insumos',
        SERVICIOS_PUBLICOS: 'Servicios públicos',
        ALQUILER_LOCAL: 'Alquiler del local',
        MANTENIMIENTO_EQUIPOS: 'Mantenimiento de equipos',
        PERSONAL: 'Personal'
    };

    const $ = (id) => document.getElementById(id);
    const peso = new Intl.NumberFormat('es-CO', { style: 'currency', currency: 'COP', maximumFractionDigits: 0 });
    const num = (v) => Number(v) || 0;

    /* ---------- Fechas (en hora local, sin pasar por UTC) ---------- */
    const iso = (d) => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const diasDelMes = (y, m) => new Date(y, m + 1, 0).getDate();

    // Cada periodo se compara contra el periodo equivalente anterior
    function rangos(preset) {
        const hoy = new Date();
        hoy.setHours(0, 0, 0, 0);
        const y = hoy.getFullYear(), m = hoy.getMonth(), d = hoy.getDate();

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
    function pintarKpis(actual, previo) {
        document.querySelectorAll('[data-kpi]').forEach((el) => {
            const campo = el.dataset.kpi;
            const invertir = el.dataset.invert === 'true'; // en gastos, subir es malo
            const cur = num(actual[campo]);
            const prev = num(previo[campo]);

            el.querySelector('[data-value]').textContent = peso.format(cur);

            const delta = el.querySelector('[data-delta]');
            delta.className = 'kpi__delta';
            if (prev === 0) {
                delta.textContent = '';            // sin base de comparación
                return;
            }
            const pct = ((cur - prev) / Math.abs(prev)) * 100;
            if (Math.abs(pct) < 0.05) {
                delta.textContent = '0.0%';
                delta.classList.add('is-flat');
                return;
            }
            const sube = pct > 0;
            delta.textContent = `${sube ? '↑' : '↓'} ${Math.abs(pct).toFixed(1)}%`;
            delta.classList.add(sube !== invertir ? 'is-good' : 'is-bad');
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
            const icono = '<svg viewBox="0 0 24 24" aria-hidden="true"><use href="#adm-i-box"/></svg>';
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

    $('dash-periodo').addEventListener('change', cargar);
    cargar();
})();