/* ==========================================================
   Historial de citas (cliente) — prefijo hc-
   - Cambia de mascota con las pestañas (y deja ?mascota=ID en la URL)
   - Filtra el historial por estado
   - Muestra "Generando…" mientras se descarga la bitácora
   ========================================================== */
(function () {
    'use strict';

    const raiz = document.getElementById('contenido');
    if (!raiz) return;

    const tabs = Array.from(raiz.querySelectorAll('.hc-tab'));
    const paneles = Array.from(raiz.querySelectorAll('.hc-mascota'));

    function mostrar(id, enfocar) {
        tabs.forEach(t => {
            const activa = t.dataset.mascota === String(id);
            t.classList.toggle('is-active', activa);
            t.setAttribute('aria-selected', activa ? 'true' : 'false');
            t.tabIndex = activa ? 0 : -1;
            if (activa && enfocar) t.focus();
            if (activa) t.scrollIntoView({ block: 'nearest', inline: 'nearest', behavior: 'smooth' });
        });
        paneles.forEach(p => { p.hidden = p.id !== 'hc-mascota-' + id; });

        const url = new URL(window.location.href);
        url.searchParams.set('mascota', id);
        window.history.replaceState(null, '', url);
    }

    tabs.forEach((t, i) => {
        t.tabIndex = t.classList.contains('is-active') ? 0 : -1;
        t.addEventListener('click', () => mostrar(t.dataset.mascota, false));
        // Flechas izquierda/derecha entre pestañas
        t.addEventListener('keydown', (e) => {
            if (e.key !== 'ArrowRight' && e.key !== 'ArrowLeft') return;
            e.preventDefault();
            const siguiente = tabs[(i + (e.key === 'ArrowRight' ? 1 : -1) + tabs.length) % tabs.length];
            mostrar(siguiente.dataset.mascota, true);
        });
    });

    // Filtro del historial (por cada mascota)
    raiz.querySelectorAll('[data-historial]').forEach(seccion => {
        const filtros = seccion.querySelectorAll('.hc-filtro');
        const filas = seccion.querySelectorAll('.hc-fila');
        const vacio = seccion.querySelector('[data-sin-resultados]');
        filtros.forEach(f => f.addEventListener('click', () => {
            filtros.forEach(o => {
                o.classList.toggle('is-active', o === f);
                o.setAttribute('aria-pressed', o === f ? 'true' : 'false');
            });
            let visibles = 0;
            filas.forEach(fila => {
                const ver = f.dataset.filtro === 'todas' || fila.dataset.estado === f.dataset.filtro;
                fila.hidden = !ver;
                if (ver) visibles++;
            });
            if (vacio) vacio.hidden = visibles > 0;
        }));
    });

    // Descarga de la bitácora: se hace con fetch para avisar si algo falla
    raiz.querySelectorAll('.hc-descargar').forEach(boton => {
        boton.addEventListener('click', async (e) => {
            if (!window.fetch || !window.URL || !URL.createObjectURL) return; // descarga normal
            e.preventDefault();
            if (boton.classList.contains('is-cargando')) return;

            const texto = boton.querySelector('span');
            const original = texto.textContent;
            boton.classList.add('is-cargando');
            texto.textContent = 'Generando PDF…';
            try {
                const resp = await fetch(boton.href, { credentials: 'same-origin' });
                const tipo = resp.headers.get('Content-Type') || '';
                if (!resp.ok || !tipo.includes('pdf')) {
                    if (resp.redirected) { window.location.href = resp.url; return; }
                    throw new Error((await resp.text()) || 'No se pudo descargar la bitácora');
                }
                const blob = await resp.blob();
                const disp = resp.headers.get('Content-Disposition') || '';
                const nombre = (disp.match(/filename="([^"]+)"/) || [])[1] || 'bitacora.pdf';
                const enlace = document.createElement('a');
                enlace.href = URL.createObjectURL(blob);
                enlace.download = nombre;
                document.body.appendChild(enlace);
                enlace.click();
                setTimeout(() => { URL.revokeObjectURL(enlace.href); enlace.remove(); }, 1000);
            } catch (err) {
                aviso(boton, err.message);
            } finally {
                boton.classList.remove('is-cargando');
                texto.textContent = original;
            }
        });
    });

    function aviso(boton, mensaje) {
        let nota = boton.parentElement.querySelector('.hc-error');
        if (!nota) {
            nota = document.createElement('p');
            nota.className = 'hc-error';
            nota.setAttribute('role', 'alert');
            boton.insertAdjacentElement('afterend', nota);
        }
        nota.textContent = mensaje;
        setTimeout(() => nota.remove(), 6000);
    }
})();
