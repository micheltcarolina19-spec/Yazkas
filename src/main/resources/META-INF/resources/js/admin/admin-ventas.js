/* =========================================================
   Punto de venta · YAZKAS Admin
   Ruta sugerida: src/main/resources/META-INF/resources/js/admin-productos.js
   ========================================================= */
(() => {
    'use strict';

    const BASE = '/admin/ventas';
    const MAX_CHIPS = 4; // categorías visibles; el resto va en "Más"

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const panel = $('#ticket-panel');
    const grid = $('#productos');
    const buscador = $('#buscador');
    const btnLimpiarBusqueda = $('#buscador-limpiar');
    const chipsBox = $('#chips');
    const sinResultados = $('#sin-resultados');
    const dlg = $('#dlg');
    const dlgBody = $('#dlg-body');
    const toasts = $('#toasts');

    let categoriaActiva = '';

    /* ---------------------------------------------------------
       Utilidades
       --------------------------------------------------------- */
    const dinero = (valor) => {
        const n = Number(valor);
        return '$' + (Number.isFinite(n) ? n : 0).toLocaleString('es-CO', { maximumFractionDigits: 0 });
    };

    const normalizar = (t) => (t || '').toString().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();

    const bonito = (enumName) => {
        const t = (enumName || '').replace(/_/g, ' ').toLowerCase();
        return t.charAt(0).toUpperCase() + t.slice(1);
    };

    const hue = (texto) => {
        let h = 0;
        for (const c of texto || '') h = (h * 31 + c.charCodeAt(0)) % 360;
        return h;
    };

    function aviso(mensaje, tipo = 'error') {
        const el = document.createElement('div');
        el.className = 'toast toast--' + tipo;
        el.textContent = mensaje;
        toasts.appendChild(el);
        setTimeout(() => {
            el.classList.add('is-leaving');
            setTimeout(() => el.remove(), 260);
        }, 3600);
    }

    /* ---------------------------------------------------------
       Peticiones al backend
       El ticket vive en una cookie, así que las acciones se hacen en fila:
       dos clics rápidos no pueden leer la misma cookie vieja.
       --------------------------------------------------------- */
    let cola = Promise.resolve();

    function enviar(metodo, ruta) {
        const tarea = cola.then(async () => {
            panel.classList.add('is-busy');
            try {
                const resp = await fetch(BASE + ruta, {
                    method: metodo,
                    credentials: 'same-origin',
                    headers: { 'Accept': 'text/html' },
                });
                if (!resp.ok) {
                    throw new Error(await mensajeDeError(resp));
                }
                return resp;
            } finally {
                panel.classList.remove('is-busy');
            }
        });
        cola = tarea.catch(() => {}); // un error no bloquea las siguientes acciones
        return tarea;
    }

    async function mensajeDeError(resp) {
        let texto = '';
        try { texto = await resp.text(); } catch (_) { /* sin cuerpo */ }

        try {
            const json = JSON.parse(texto);
            const m = json.message || json.mensaje || json.error || json.detail || json.title;
            if (m) return String(m);
        } catch (_) { /* no era JSON */ }

        const plano = texto.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
        if (plano && plano.length <= 160) return plano;

        if (resp.status === 404) return 'El producto ya no existe';
        if (resp.status === 401 || resp.status === 403) return 'Tu sesión expiró. Vuelve a ingresar.';
        return 'No se pudo completar la acción. Intenta de nuevo.';
    }

    async function actualizarTicket(metodo, ruta) {
        try {
            const resp = await enviar(metodo, ruta);
            pintarPanel(await resp.text());
            return true;
        } catch (err) {
            aviso(err.message || 'No se pudo actualizar el ticket');
            // Si el stock cambió, refrescamos el panel con el estado real
            recargarPanel();
            return false;
        }
    }

    async function recargarPanel() {
        try {
            const resp = await enviar('GET', '/ticket');
            pintarPanel(await resp.text());
        } catch (_) { /* se deja el panel como está */ }
    }

    function pintarPanel(html) {
        panel.innerHTML = html;
        despuesDePintar();
    }

    /* ---------------------------------------------------------
       Después de cada cambio del ticket
       --------------------------------------------------------- */
    function despuesDePintar(raiz = document) {
        // Dinero con formato colombiano
        $$('.money', raiz).forEach((el) => { el.textContent = dinero(el.dataset.v); });

        // Colores de las miniaturas según el nombre del producto
        $$('.thumb[data-seed]', raiz).forEach((el) => el.style.setProperty('--h', hue(el.dataset.seed)));

        // Etiquetas de categoría legibles
        $$('.cat-label', raiz).forEach((el) => {
            if (!el.dataset.pretty) {
                el.textContent = bonito(el.textContent.trim());
                el.dataset.pretty = '1';
            }
        });

        // "+" deshabilitado cuando ya se llegó al stock
        $$('.line', panel).forEach((li) => {
            const cant = Number(li.dataset.cant);
            const stock = Number(li.dataset.stock);
            const mas = $('[data-action="mas"]', li);
            if (mas) mas.disabled = cant >= stock;
            const input = $('.qty__input', li);
            if (input) input.max = stock;
        });

        sincronizarTarjetas();
    }

    // Marca en el catálogo cuántas unidades de cada producto hay en el ticket
    function sincronizarTarjetas() {
        const enTicket = new Map();
        $$('.line', panel).forEach((li) => enTicket.set(li.dataset.id, Number(li.dataset.cant)));

        $$('.card', grid).forEach((card) => {
            const cant = enTicket.get(card.dataset.id) || 0;
            const chip = $('.in-ticket', card);
            card.classList.toggle('has-qty', cant > 0);
            chip.hidden = cant === 0;
            chip.textContent = '×' + cant;
            chip.setAttribute('aria-label', cant + ' en el ticket');
        });
    }

    /* ---------------------------------------------------------
       Acciones del ticket (delegación de eventos)
       --------------------------------------------------------- */
    panel.addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-action]');
        if (!btn || btn.disabled || btn.tagName === 'INPUT') return;

        const accion = btn.dataset.action;
        const linea = btn.closest('.line');
        const id = linea ? linea.dataset.id : null;
        const cant = linea ? Number(linea.dataset.cant) : 0;

        switch (accion) {
            case 'mas':      return actualizarTicket('POST', `/ticket/agregar/${id}`);
            case 'menos':    return actualizarTicket('PUT', `/ticket/${id}?cantidad=${cant - 1}`);
            case 'quitar':   return actualizarTicket('DELETE', `/ticket/${id}`);
            case 'vaciar':   return actualizarTicket('DELETE', '/ticket');
            case 'deshacer': return actualizarTicket('POST', '/ticket/deshacer');
            case 'finalizar': return confirmarVenta();
        }
    });

    // Escribir la cantidad a mano
    panel.addEventListener('change', (e) => {
        const input = e.target.closest('.qty__input');
        if (!input) return;
        const linea = input.closest('.line');
        const valor = Math.floor(Number(input.value));
        if (!Number.isFinite(valor)) {
            input.value = linea.dataset.cant;
            return;
        }
        actualizarTicket('PUT', `/ticket/${linea.dataset.id}?cantidad=${valor}`);
    });

    panel.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' && e.target.classList.contains('qty__input')) e.target.blur();
    });

    /* ---------------------------------------------------------
       Agregar desde el catálogo
       --------------------------------------------------------- */
    async function agregarProducto(id, boton) {
        const ok = await actualizarTicket('POST', `/ticket/agregar/${id}`);
        if (ok && boton) {
            boton.classList.add('is-added');
            setTimeout(() => boton.classList.remove('is-added'), 450);
        }
    }

    grid.addEventListener('click', (e) => {
        const fav = e.target.closest('[data-fav]');
        if (fav) {
            alternarFavorito(fav);
            return;
        }
        const btn = e.target.closest('[data-add]');
        if (btn && !btn.disabled) agregarProducto(btn.dataset.add, btn);
    });

    /* ---------------------------------------------------------
       Favoritos (solo en este navegador)
       --------------------------------------------------------- */
    const FAV_KEY = 'yazkas_pos_favoritos';

    function leerFavoritos() {
        try { return new Set(JSON.parse(localStorage.getItem(FAV_KEY) || '[]')); }
        catch (_) { return new Set(); }
    }

    function guardarFavoritos(set) {
        try { localStorage.setItem(FAV_KEY, JSON.stringify([...set])); } catch (_) { /* sin almacenamiento */ }
    }

    function alternarFavorito(btn) {
        const favs = leerFavoritos();
        const id = btn.dataset.fav;
        favs.has(id) ? favs.delete(id) : favs.add(id);
        guardarFavoritos(favs);
        btn.setAttribute('aria-pressed', favs.has(id) ? 'true' : 'false');
    }

    function pintarFavoritos() {
        const favs = leerFavoritos();
        $$('[data-fav]', grid).forEach((b) => b.setAttribute('aria-pressed', favs.has(b.dataset.fav) ? 'true' : 'false'));
    }

    /* ---------------------------------------------------------
       Búsqueda y filtros
       --------------------------------------------------------- */
    function filtrar() {
        const q = normalizar(buscador.value);
        let visibles = 0;

        $$('.card', grid).forEach((card) => {
            const coincideNombre = !q || normalizar(card.dataset.name).includes(q);
            const coincideCat = !categoriaActiva || card.dataset.cat === categoriaActiva;
            const mostrar = coincideNombre && coincideCat;
            card.hidden = !mostrar;
            if (mostrar) visibles++;
        });

        sinResultados.hidden = visibles !== 0;
        btnLimpiarBusqueda.hidden = !buscador.value;
    }

    buscador.addEventListener('input', filtrar);

    btnLimpiarBusqueda.addEventListener('click', () => {
        buscador.value = '';
        filtrar();
        buscador.focus();
    });

    // Enter con un único producto disponible lo agrega (ideal para teclear rápido)
    buscador.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            buscador.value = '';
            filtrar();
            return;
        }
        if (e.key !== 'Enter') return;
        const disponibles = $$('.card:not([hidden]):not(.is-out)', grid);
        if (disponibles.length === 1) {
            e.preventDefault();
            agregarProducto(disponibles[0].dataset.id, $('.btn-add', disponibles[0]));
        }
    });

    // Atajo: "/" lleva el cursor al buscador
    document.addEventListener('keydown', (e) => {
        const escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName);
        if (e.key === '/' && !escribiendo && !dlg.open) {
            e.preventDefault();
            buscador.focus();
        }
    });

    function seleccionarCategoria(cat) {
        categoriaActiva = cat;
        $$('.chip[data-cat]', chipsBox).forEach((c) => c.classList.toggle('is-active', c.dataset.cat === cat));
        $$('#more-menu button').forEach((b) => b.classList.toggle('is-active', b.dataset.cat === cat));

        // Si la categoría elegida está dentro de "Más", el botón muestra su nombre
        const dentroDeMas = $(`#more-menu button[data-cat="${CSS.escape(cat)}"]`);
        const masBtn = $('#more-btn');
        masBtn.classList.toggle('is-active', Boolean(dentroDeMas));
        $('#more-label').textContent = dentroDeMas ? dentroDeMas.textContent : 'Más';
        filtrar();
    }

    chipsBox.addEventListener('click', (e) => {
        const chip = e.target.closest('.chip[data-cat]');
        if (chip) seleccionarCategoria(chip.dataset.cat);
    });

    /* Menú "Más": las categorías después de la cuarta se mueven al desplegable */
    function armarMenuMas() {
        const chips = $$('.chip[data-cat]', chipsBox).filter((c) => c.dataset.cat !== '');
        const extra = chips.slice(MAX_CHIPS);
        if (!extra.length) return;

        const more = $('#more');
        const menu = $('#more-menu');
        const btn = $('#more-btn');

        extra.forEach((chip) => {
            const item = document.createElement('button');
            item.type = 'button';
            item.setAttribute('role', 'menuitem');
            item.dataset.cat = chip.dataset.cat;
            item.textContent = bonito(chip.dataset.cat);
            item.addEventListener('click', () => {
                seleccionarCategoria(item.dataset.cat);
                cerrarMenu();
            });
            menu.appendChild(item);
            chip.remove();
        });

        more.hidden = false;

        const abrir = () => { menu.hidden = false; btn.setAttribute('aria-expanded', 'true'); };
        function cerrarMenu() { menu.hidden = true; btn.setAttribute('aria-expanded', 'false'); }

        btn.addEventListener('click', (e) => {
            e.stopPropagation();
            menu.hidden ? abrir() : cerrarMenu();
        });
        document.addEventListener('click', (e) => { if (!more.contains(e.target)) cerrarMenu(); });
        document.addEventListener('keydown', (e) => { if (e.key === 'Escape') cerrarMenu(); });
    }

    /* ---------------------------------------------------------
       Finalizar venta: confirmar → registrar → resultado
       --------------------------------------------------------- */
    function vistaDialogo({ icono, claseIcono = '', titulo, texto, total, acciones }) {
        dlgBody.innerHTML = `
            <div class="dlg__icon ${claseIcono}"><svg class="i"><use href="#${icono}"/></svg></div>
            <h3 id="dlg-title">${titulo}</h3>
            <p>${texto}</p>
            ${total ? `<div class="dlg__total">${total}</div>` : '<div style="height:22px"></div>'}
            <div class="dlg__actions">${acciones}</div>`;
    }

    function confirmarVenta() {
        const unidades = $('#ticket-unidades', panel)?.textContent || '0';
        const total = dinero($('#ticket-total', panel)?.dataset.v);

        vistaDialogo({
            icono: 'i-receipt',
            titulo: 'Confirmar venta',
            texto: `Se descontarán ${unidades} ${unidades === '1' ? 'unidad' : 'unidades'} del inventario.`,
            total,
            acciones: `
                <button type="button" class="btn btn--outline" data-dlg="cancelar">Revisar</button>
                <button type="button" class="btn btn--primary" data-dlg="confirmar">
                    <svg class="i"><use href="#i-check"/></svg> Confirmar
                </button>`,
        });
        dlg.showModal();
        $('[data-dlg="confirmar"]', dlgBody).focus();
    }

    dlgBody.addEventListener('click', async (e) => {
        const btn = e.target.closest('[data-dlg]');
        if (!btn) return;

        if (btn.dataset.dlg === 'cancelar' || btn.dataset.dlg === 'cerrar') {
            dlg.close();
            if (btn.dataset.dlg === 'cerrar') buscador.focus();
            return;
        }

        if (btn.dataset.dlg === 'confirmar') {
            btn.disabled = true;
            try {
                const resp = await enviar('POST', '/ticket/finalizar');
                const numero = resp.headers.get('X-Venta-Numero') || '';
                const total = resp.headers.get('X-Venta-Total');
                pintarPanel(await resp.text());

                vistaDialogo({
                    icono: 'i-check',
                    claseIcono: 'dlg__icon--ok',
                    titulo: '¡Venta registrada!',
                    texto: numero ? `Ticket <strong>${numero}</strong> guardado correctamente.` : 'La venta se guardó correctamente.',
                    total: total !== null ? dinero(total) : '',
                    acciones: `<button type="button" class="btn btn--primary" data-dlg="cerrar">Nueva venta</button>`,
                });
                $('[data-dlg="cerrar"]', dlgBody).focus();
                actualizarStockTarjetas();
            } catch (err) {
                dlg.close();
                aviso(err.message || 'No se pudo registrar la venta');
                recargarPanel();
            }
        }
    });

    // El stock del catálogo cambió: recargamos la página en segundo plano para mostrar cifras reales
    async function actualizarStockTarjetas() {
        try {
            const resp = await fetch(BASE, { credentials: 'same-origin', headers: { 'Accept': 'text/html' } });
            if (!resp.ok) return;
            const doc = new DOMParser().parseFromString(await resp.text(), 'text/html');
            const nuevo = $('#productos', doc);
            if (!nuevo) return;
            grid.innerHTML = nuevo.innerHTML;
            despuesDePintar(grid);
            pintarFavoritos();
            filtrar();
        } catch (_) { /* si falla, el próximo refresco lo corrige */ }
    }

    dlg.addEventListener('click', (e) => { if (e.target === dlg) dlg.close(); }); // clic fuera

    /* ---------------------------------------------------------
       Cabecera: "Nueva venta"
       --------------------------------------------------------- */
    $('#btn-nueva-venta').addEventListener('click', async () => {
        if ($$('.line', panel).length) await actualizarTicket('DELETE', '/ticket'); // se puede recuperar con Deshacer
        buscador.value = '';
        seleccionarCategoria('');
        buscador.focus();
    });

    /* ---------------------------------------------------------
       Inicio
       --------------------------------------------------------- */
    armarMenuMas();
    despuesDePintar();
    pintarFavoritos();
    filtrar();
})();