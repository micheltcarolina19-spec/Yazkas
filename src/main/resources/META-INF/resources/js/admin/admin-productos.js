/* =========================================================
   Inventario de productos · YAZKAS Admin
   Ruta sugerida: src/main/resources/META-INF/resources/js/admin-productos.js
   ========================================================= */
(() => {
    'use strict';

    const API = '/api/productos';          // crear / actualizar / eliminar (ProductoResource)
    const PAGINA = '/admin/productos';     // filas e imágenes (AdminProductosResource)
    const POR_PAGINA = 10;
    const PREFIJO_FOTO = '/uploads/productos/';
    const MAX_BYTES = 2 * 1024 * 1024;
    const TIPOS_IMAGEN = ['image/jpeg', 'image/png', 'image/webp'];

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const tbody = $('#filas');
    const buscador = $('#buscador');
    const btnBuscarLimpiar = $('#buscador-limpiar');
    const selCategoria = $('#filtro-categoria');
    const selTipo = $('#filtro-tipo');
    const btnQuitarFiltros = $('#limpiar-filtros');
    const tablaWrap = $('#tabla-wrap');
    const vacioTotal = $('#vacio-total');
    const vacioFiltro = $('#vacio-filtro');
    const info = $('#info');
    const paginas = $('#paginas');

    const drawer = $('#drawer');
    const scrim = $('#scrim');
    const form = $('#form');
    const formError = $('#form-error');
    const tituloDrawer = $('#drawer-title');
    const btnGuardar = $('#btn-guardar');
    const btnEliminar = $('#btn-eliminar');
    const drop = $('#drop');
    const fileInput = $('#file');
    const dropImg = $('#drop-img');

    const f = {
        id: null,
        nombre: $('#f-nombre'), marca: $('#f-marca'), categoria: $('#f-categoria'), tipo: $('#f-tipo'),
        base: $('#f-base'), venta: $('#f-venta'), stock: $('#f-stock'), desc: $('#f-desc'), foto: $('#f-foto'),
    };

    const state = {
        filas: [],
        q: '', cat: '', tipo: '',
        orden: { clave: 'nombre', dir: 1 },
        pagina: 1,
        editando: null,       // id del producto abierto en el panel (null = nuevo)
        abierto: false,
        snapshot: '',
        fotoOriginal: '',     // foto que tenía el producto al abrir el panel
        subidas: new Set(),   // fotos subidas en esta sesión que todavía no están guardadas en un producto
        guardando: false,
    };

    /* ---------------------------------------------------------
       Utilidades
       --------------------------------------------------------- */
    const dinero = (v) => {
        const n = Number(v);
        return '$' + (Number.isFinite(n) ? n : 0).toLocaleString('es-CO', { maximumFractionDigits: 0 });
    };
    const soloDigitos = (t) => (t || '').replace(/\D/g, '');
    const normalizar = (t) => (t || '').toString().normalize('NFD').replace(/[\u0300-\u036f]/g, '').toLowerCase().trim();
    const bonito = (n) => {
        const t = (n || '').replace(/_/g, ' ').toLowerCase();
        return t.charAt(0).toUpperCase() + t.slice(1);
    };
    const hue = (t) => { let h = 0; for (const c of t || '') h = (h * 31 + c.charCodeAt(0)) % 360; return h; };
    const esc = (t) => String(t ?? '').replace(/[&<>"']/g, (c) => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    function aviso(mensaje, tipo = 'error') {
        const el = document.createElement('div');
        el.className = 'toast toast--' + tipo;
        el.textContent = mensaje;
        $('#toasts').appendChild(el);
        setTimeout(() => { el.classList.add('is-leaving'); setTimeout(() => el.remove(), 260); }, 3600);
    }

    // Icono según palabras de la categoría (los enum pueden cambiar sin tocar el front)
    function iconoCategoria(nombre) {
        const n = normalizar(nombre);
        if (/aliment|concentrad|comida|snack|premio/.test(n)) return 'i-bowl';
        if (/jugue/.test(n)) return 'i-ball';
        if (/higien|shampoo|cuidado|aseo/.test(n)) return 'i-drop';
        if (/salud|medic|vitamin|farma/.test(n)) return 'i-heart';
        if (/accesor|collar|correa/.test(n)) return 'i-tag';
        return 'i-paw';
    }

    /* ---------------------------------------------------------
       Pintar filas (formato, chips, datos en memoria)
       --------------------------------------------------------- */
    function decorarFila(tr) {
        $$('.money', tr).forEach((el) => { el.textContent = dinero(el.dataset.v); });

        const cat = $('.tag--cat', tr);
        if (cat) {
            $('.tag__t', cat).textContent = bonito(cat.dataset.v);
            cat.style.setProperty('--h', hue(cat.dataset.v));
            $('use', cat).setAttribute('href', '#' + iconoCategoria(cat.dataset.v));
        }
        const tipo = $('.tag--tipo', tr);
        if (tipo) $('.tag__t', tipo).textContent = bonito(tipo.dataset.v);

        const d = tr.dataset;
        tr._d = {
            id: d.id, nombre: d.nombre, marca: d.marca, cat: d.cat, tipo: d.tipo,
            base: Number(d.base) || 0, venta: Number(d.venta) || 0, stock: Number(d.stock) || 0,
            foto: d.foto, desc: d.desc,
            busqueda: normalizar(d.nombre + ' ' + d.marca),
        };
    }

    function cargarFilas() {
        state.filas = Array.from(tbody.children);
        state.filas.forEach(decorarFila);
    }

    // Si una foto no carga (archivo borrado o URL rota) se muestra el marcador de posición
    document.addEventListener('error', (e) => {
        const img = e.target;
        if (img instanceof HTMLImageElement && img.classList.contains('pimg')) {
            const ph = document.createElement('div');
            ph.className = 'ph';
            ph.innerHTML = '<svg class="i"><use href="#i-image"/></svg>';
            img.replaceWith(ph);
        }
    }, true);

    /* ---------------------------------------------------------
       Filtros, orden y paginación (todo en el navegador)
       --------------------------------------------------------- */
    function listaFiltrada() {
        const q = normalizar(state.q);
        const { clave, dir } = state.orden;
        const valor = (d) => ({ nombre: normalizar(d.nombre), stock: d.stock, base: d.base, venta: d.venta }[clave]);

        return state.filas
            .filter((tr) => {
                const d = tr._d;
                return (!q || d.busqueda.includes(q))
                    && (!state.cat || d.cat === state.cat)
                    && (!state.tipo || d.tipo === state.tipo);
            })
            .sort((a, b) => {
                const x = valor(a._d), y = valor(b._d);
                const r = typeof x === 'string' ? x.localeCompare(y, 'es') : x - y;
                return r * dir;
            });
    }

    function render() {
        const lista = listaFiltrada();
        const total = lista.length;
        const nPaginas = Math.max(1, Math.ceil(total / POR_PAGINA));
        state.pagina = Math.min(Math.max(1, state.pagina), nPaginas);
        const ini = (state.pagina - 1) * POR_PAGINA;
        const visibles = lista.slice(ini, ini + POR_PAGINA);

        tbody.replaceChildren(...visibles);
        visibles.forEach((tr) => tr.classList.toggle('is-active', state.abierto && tr._d.id === String(state.editando)));

        const sinProductos = state.filas.length === 0;
        tablaWrap.hidden = total === 0;
        vacioTotal.hidden = !sinProductos;
        vacioFiltro.hidden = sinProductos || total !== 0;
        info.textContent = total
            ? `Mostrando ${ini + 1} - ${ini + visibles.length} de ${total} ${total === 1 ? 'producto' : 'productos'}`
            : '';

        const hayFiltros = Boolean(state.q || state.cat || state.tipo);
        btnQuitarFiltros.hidden = !hayFiltros;
        btnBuscarLimpiar.hidden = !state.q;

        $$('th[data-sort]').forEach((th) => {
            if (th.dataset.sort === state.orden.clave) th.setAttribute('aria-sort', state.orden.dir === 1 ? 'ascending' : 'descending');
            else th.removeAttribute('aria-sort');
        });

        pintarPaginacion(nPaginas);
    }

    function pintarPaginacion(n) {
        paginas.replaceChildren();
        if (n <= 1) return;

        const boton = (txt, pag, { actual = false, desactivado = false, etiqueta } = {}) => {
            const b = document.createElement('button');
            b.type = 'button';
            b.className = 'pg' + (actual ? ' is-current' : '');
            b.innerHTML = txt;
            b.disabled = desactivado;
            if (etiqueta) b.setAttribute('aria-label', etiqueta);
            if (actual) b.setAttribute('aria-current', 'page');
            b.addEventListener('click', () => { state.pagina = pag; render(); });
            return b;
        };

        paginas.appendChild(boton('<svg class="i i--sm"><use href="#i-left"/></svg>', state.pagina - 1, { desactivado: state.pagina === 1, etiqueta: 'Página anterior' }));

        const mostrar = new Set([1, n, state.pagina - 1, state.pagina, state.pagina + 1]);
        let previo = 0;
        for (let p = 1; p <= n; p++) {
            if (!mostrar.has(p)) continue;
            if (p - previo > 1) {
                const puntos = document.createElement('span');
                puntos.className = 'pg pg--dots';
                puntos.textContent = '…';
                paginas.appendChild(puntos);
            }
            paginas.appendChild(boton(String(p), p, { actual: p === state.pagina }));
            previo = p;
        }

        paginas.appendChild(boton('<svg class="i i--sm"><use href="#i-right"/></svg>', state.pagina + 1, { desactivado: state.pagina === n, etiqueta: 'Página siguiente' }));
    }

    buscador.addEventListener('input', () => { state.q = buscador.value; state.pagina = 1; render(); });
    btnBuscarLimpiar.addEventListener('click', () => { buscador.value = ''; state.q = ''; state.pagina = 1; render(); buscador.focus(); });
    selCategoria.addEventListener('change', () => { state.cat = selCategoria.value; state.pagina = 1; render(); });
    selTipo.addEventListener('change', () => { state.tipo = selTipo.value; state.pagina = 1; render(); });
    btnQuitarFiltros.addEventListener('click', () => {
        buscador.value = ''; selCategoria.value = ''; selTipo.value = '';
        state.q = state.cat = state.tipo = ''; state.pagina = 1; render();
    });

    $$('th[data-sort] button').forEach((b) => b.addEventListener('click', () => {
        const clave = b.closest('th').dataset.sort;
        state.orden = state.orden.clave === clave ? { clave, dir: -state.orden.dir } : { clave, dir: 1 };
        state.pagina = 1;
        render();
    }));

    buscador.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && buscador.value) { e.stopPropagation(); btnBuscarLimpiar.click(); }
    });

    /* ---------------------------------------------------------
       Refrescar la tabla desde el servidor
       --------------------------------------------------------- */
    async function refrescarFilas(resaltarId) {
        const resp = await fetch(PAGINA + '/filas', { credentials: 'same-origin', headers: { Accept: 'text/html' }, cache: 'no-store' });
        if (!resp.ok) throw new Error('No se pudo actualizar la lista de productos');
        tbody.innerHTML = await resp.text();
        cargarFilas();

        if (resaltarId != null) mostrarFila(resaltarId, true);
        else render();
    }

    // Lleva la tabla a la página donde está el producto y lo resalta
    function mostrarFila(id, destello) {
        const lista = listaFiltrada();
        let idx = lista.findIndex((tr) => tr._d.id === String(id));
        if (idx === -1) { // el filtro lo esconde: se quitan los filtros para que se vea
            buscador.value = ''; selCategoria.value = ''; selTipo.value = '';
            state.q = state.cat = state.tipo = '';
            idx = listaFiltrada().findIndex((tr) => tr._d.id === String(id));
        }
        state.pagina = Math.floor(Math.max(idx, 0) / POR_PAGINA) + 1;
        render();
        const tr = state.filas.find((x) => x._d.id === String(id));
        if (tr && destello) {
            tr.classList.add('flash');
            tr.scrollIntoView({ block: 'nearest', behavior: 'smooth' });
            setTimeout(() => tr.classList.remove('flash'), 1700);
        }
    }

    /* ---------------------------------------------------------
       Errores del servidor
       --------------------------------------------------------- */
    async function leerError(resp) {
        let texto = '';
        try { texto = await resp.text(); } catch (_) { /* sin cuerpo */ }

        const msgs = [];
        const juntar = (x) => {
            if (!x) return;
            if (typeof x === 'string') { msgs.push(x); return; }
            if (Array.isArray(x)) { x.forEach(juntar); return; }
            if (typeof x === 'object') {
                ['message', 'mensaje', 'detalle', 'detail'].forEach((k) => { if (typeof x[k] === 'string') msgs.push(x[k]); });
                ['violations', 'errors', 'errores', 'detalles', 'details'].forEach((k) => { if (x[k]) juntar(x[k]); });
            }
        };
        try { juntar(JSON.parse(texto)); } catch (_) {
            const plano = texto.replace(/<[^>]*>/g, ' ').replace(/\s+/g, ' ').trim();
            if (plano && plano.length <= 200) msgs.push(plano);
        }

        const unicos = [...new Set(msgs.map((m) => m.trim()).filter(Boolean))];
        if (!unicos.length) {
            if (resp.status === 404) unicos.push('El producto ya no existe');
            else if (resp.status === 409) unicos.push('No se puede completar: el producto está en uso');
            else if (resp.status === 401 || resp.status === 403) unicos.push('Tu sesión expiró. Vuelve a ingresar.');
            else unicos.push('No se pudo completar la acción. Intenta de nuevo.');
        }
        return unicos;
    }

    const CAMPOS = [['nombre', 'nombre'], ['marca', 'marca'], ['categor', 'categoria'], ['tipo', 'tipo'],
        ['precio base', 'precioBase'], ['precio de venta', 'precioVenta'], ['stock', 'stock'], ['descrip', 'descripcion'], ['foto', 'foto']];

    function mostrarErroresServidor(mensajes) {
        limpiarErrores();
        const sinCampo = [];
        mensajes.forEach((m) => {
            const n = normalizar(m);
            const par = CAMPOS.find(([clave]) => n.includes(clave));
            if (par) marcarError(par[1], m); else sinCampo.push(m);
        });
        const primero = $('.field.has-error', form);
        if (sinCampo.length) { formError.textContent = sinCampo.join(' · '); formError.hidden = false; }
        if (primero) primero.scrollIntoView({ block: 'center', behavior: 'smooth' });
        else if (!formError.hidden) formError.scrollIntoView({ block: 'nearest' });
    }

    /* ---------------------------------------------------------
       Formulario
       --------------------------------------------------------- */
    function marcarError(campo, mensaje) {
        const box = $(`.field[data-f="${campo}"]`, form);
        if (!box) return;
        box.classList.add('has-error');
        const p = $(`[data-err="${campo}"]`, box);
        if (p) { p.textContent = mensaje; p.hidden = false; }
    }

    function limpiarErrores() {
        $$('.field.has-error', form).forEach((b) => b.classList.remove('has-error'));
        $$('.field__err', form).forEach((p) => { p.hidden = true; p.textContent = ''; });
        formError.hidden = true;
        formError.textContent = '';
    }

    function datos() {
        return {
            nombre: f.nombre.value.trim(),
            marca: f.marca.value.trim(),
            categoria: f.categoria.value,
            tipo: f.tipo.value,
            base: soloDigitos(f.base.value),
            venta: soloDigitos(f.venta.value),
            stock: f.stock.value.trim(),
            desc: f.desc.value.trim(),
            foto: f.foto.value,
        };
    }

    const sucio = () => JSON.stringify(datos()) !== state.snapshot;

    function validar() {
        limpiarErrores();
        const d = datos();
        const e = {};
        if (!d.nombre) e.nombre = 'Escribe el nombre del producto';
        else if (d.nombre.length > 255) e.nombre = 'El nombre no puede superar 255 caracteres';
        if (d.marca.length > 255) e.marca = 'La marca no puede superar 255 caracteres';
        if (!d.categoria) e.categoria = 'Selecciona una categoría';
        if (!d.tipo) e.tipo = 'Selecciona un tipo';
        if (!d.base || Number(d.base) <= 0) e.precioBase = 'Ingresa un precio base mayor a 0';
        if (!d.venta || Number(d.venta) <= 0) e.precioVenta = 'Ingresa un precio de venta mayor a 0';
        if (d.stock === '' || !/^\d+$/.test(d.stock)) e.stock = 'Ingresa el stock (0 o más)';
        if (d.desc.length > 1000) e.descripcion = 'La descripción no puede superar 1000 caracteres';
        Object.entries(e).forEach(([campo, msg]) => marcarError(campo, msg));
        const primero = $('.field.has-error input, .field.has-error select, .field.has-error textarea', form);
        if (primero) primero.focus();
        return Object.keys(e).length === 0;
    }

    // Miles con punto mientras se escribe: 25000 -> 25.000
    function formatearPrecio(input) {
        const n = soloDigitos(input.value);
        input.value = n ? Number(n).toLocaleString('es-CO') : '';
        actualizarMargen();
    }
    [f.base, f.venta].forEach((i) => i.addEventListener('input', () => formatearPrecio(i)));

    function actualizarMargen() {
        const box = $('#margen');
        const base = Number(soloDigitos(f.base.value));
        const venta = Number(soloDigitos(f.venta.value));
        if (!base || !venta) { box.hidden = true; return; }
        const ganancia = venta - base;
        const pct = Math.round((ganancia / base) * 100);
        box.hidden = false;
        box.classList.toggle('is-warn', ganancia <= 0);
        box.textContent = ganancia > 0
            ? `Ganancia por unidad: ${dinero(ganancia)} (${pct}% sobre el costo)`
            : 'El precio de venta no supera el precio base: venderías sin ganancia.';
    }

    f.desc.addEventListener('input', () => { $('#desc-count').textContent = `${f.desc.value.length} / 1000`; });
    $$('input, select, textarea', form).forEach((el) => el.addEventListener('input', () => {
        const box = el.closest('.field');
        if (box && box.classList.contains('has-error')) {
            box.classList.remove('has-error');
            const p = $('.field__err', box);
            if (p) p.hidden = true;
        }
    }));

    /* ---------------------------------------------------------
       Imagen (se sube como archivo; en el producto solo va la URL)
       --------------------------------------------------------- */
    function estadoDrop(estado) { drop.dataset.state = estado; }

    function mostrarFoto(url) {
        f.foto.value = url || '';
        if (url) { dropImg.src = url; estadoDrop('preview'); }
        else { dropImg.removeAttribute('src'); estadoDrop('empty'); }
    }

    function borrarArchivoSubido(url) {
        if (!url || !url.startsWith(PREFIJO_FOTO)) return;
        fetch(`${PAGINA}/imagen?url=${encodeURIComponent(url)}`, { method: 'DELETE', credentials: 'same-origin' }).catch(() => {});
    }

    async function subirImagen(archivo) {
        if (!archivo) return;
        if (!TIPOS_IMAGEN.includes(archivo.type)) { aviso('Usa una imagen JPG, PNG o WebP'); return; }
        if (archivo.size > MAX_BYTES) { aviso('La imagen no puede superar 2 MB'); return; }

        const anterior = f.foto.value;
        estadoDrop('loading');
        const datosForm = new FormData();
        datosForm.append('imagen', archivo);

        try {
            const resp = await fetch(PAGINA + '/imagen', { method: 'POST', body: datosForm, credentials: 'same-origin', headers: { Accept: 'application/json' } });
            if (!resp.ok) throw new Error((await leerError(resp)).join(' · '));
            const { url } = await resp.json();

            // Si había una foto subida en esta sesión y sin guardar, ya no se necesita
            if (state.subidas.has(anterior)) { state.subidas.delete(anterior); borrarArchivoSubido(anterior); }
            state.subidas.add(url);
            mostrarFoto(url);
            const box = $('.field[data-f="foto"]');
            box.classList.remove('has-error');
            $('[data-err="foto"]').hidden = true;
        } catch (err) {
            mostrarFoto(anterior);
            aviso(err.message || 'No se pudo subir la imagen');
        }
    }

    drop.addEventListener('click', (e) => { if (!e.target.closest('#drop-quitar')) fileInput.click(); });
    drop.addEventListener('keydown', (e) => { if (e.key === 'Enter' || e.key === ' ') { e.preventDefault(); fileInput.click(); } });
    fileInput.addEventListener('change', () => { subirImagen(fileInput.files[0]); fileInput.value = ''; });
    $('#drop-quitar').addEventListener('click', (e) => {
        e.stopPropagation();
        const url = f.foto.value;
        if (state.subidas.has(url)) { state.subidas.delete(url); borrarArchivoSubido(url); }
        mostrarFoto('');
    });
    ['dragenter', 'dragover'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.add('is-over'); }));
    ['dragleave', 'drop'].forEach((ev) => drop.addEventListener(ev, (e) => { e.preventDefault(); drop.classList.remove('is-over'); }));
    drop.addEventListener('drop', (e) => subirImagen(e.dataTransfer.files[0]));

    /* ---------------------------------------------------------
       Confirmaciones
       --------------------------------------------------------- */
    const dlg = $('#dlg');
    function confirmar({ titulo, texto, ok = 'Confirmar', peligro = false, icono = 'i-box' }) {
        return new Promise((resolve) => {
            $('#dlg-body').innerHTML = `
                <form method="dialog">
                    <div class="dlg__icon ${peligro ? 'dlg__icon--danger' : ''}"><svg class="i"><use href="#${icono}"/></svg></div>
                    <h3>${esc(titulo)}</h3>
                    <p>${esc(texto)}</p>
                    <div class="dlg__actions">
                        <button value="cancel" class="btn btn--outline">Cancelar</button>
                        <button value="ok" class="btn ${peligro ? 'btn--danger' : 'btn--primary'}">${esc(ok)}</button>
                    </div>
                </form>`;
            dlg.returnValue = '';
            dlg.addEventListener('close', () => resolve(dlg.returnValue === 'ok'), { once: true });
            dlg.showModal();
        });
    }

    dlg.addEventListener('click', (e) => { if (e.target === dlg) dlg.close(); });

    /* ---------------------------------------------------------
       Panel lateral
       --------------------------------------------------------- */
    function ponerFormulario(d) {
        f.nombre.value = d.nombre || '';
        f.marca.value = d.marca || '';
        f.categoria.value = d.cat || '';
        f.tipo.value = d.tipo || '';
        f.base.value = d.base ? Number(d.base).toLocaleString('es-CO') : '';
        f.venta.value = d.venta ? Number(d.venta).toLocaleString('es-CO') : '';
        f.stock.value = d.stock ?? '';
        f.desc.value = d.desc || '';
        $('#desc-count').textContent = `${f.desc.value.length} / 1000`;
        mostrarFoto(d.foto || '');
        actualizarMargen();
        limpiarErrores();
    }

    async function abrirPanel(fila) {
        // Si hay cambios sin guardar en el producto que está abierto, se pregunta antes de cambiar
        if (state.abierto && sucio()) {
            const seguir = await confirmar({
                titulo: '¿Descartar los cambios?',
                texto: 'Tienes cambios sin guardar en este producto.',
                ok: 'Descartar', peligro: true, icono: 'i-edit',
            });
            if (!seguir) return;
            descartarSubidas();
        }

        const d = fila ? fila._d : null;
        state.editando = d ? d.id : null;
        state.fotoOriginal = d ? d.foto : '';
        state.subidas = new Set();

        tituloDrawer.textContent = d ? 'Editar producto' : 'Agregar producto';
        $('span', btnGuardar).textContent = d ? 'Guardar cambios' : 'Guardar producto';
        btnEliminar.hidden = !d;
        ponerFormulario(d || {});
        state.snapshot = JSON.stringify(datos());

        state.abierto = true;
        document.body.classList.add('drawer-open');
        drawer.classList.add('is-open');
        drawer.setAttribute('aria-hidden', 'false');
        scrim.hidden = false;
        $('.drawer__body').scrollTop = 0;
        render(); // marca la fila activa
        setTimeout(() => f.nombre.focus({ preventScroll: true }), 220);
    }

    function descartarSubidas() {
        state.subidas.forEach(borrarArchivoSubido);
        state.subidas = new Set();
    }

    function cerrarPanel() {
        state.abierto = false;
        state.editando = null;
        document.body.classList.remove('drawer-open');
        drawer.classList.remove('is-open');
        drawer.setAttribute('aria-hidden', 'true');
        scrim.hidden = true;
        render();
    }

    async function pedirCierre() {
        if (!state.abierto) return;
        if (sucio()) {
            const seguir = await confirmar({
                titulo: '¿Descartar los cambios?',
                texto: 'Si cierras ahora, se perderá lo que escribiste.',
                ok: 'Descartar', peligro: true, icono: 'i-edit',
            });
            if (!seguir) return;
        }
        descartarSubidas();
        cerrarPanel();
    }

    $('#drawer-cerrar').addEventListener('click', pedirCierre);
    $('#btn-cancelar').addEventListener('click', pedirCierre);
    scrim.addEventListener('click', pedirCierre);
    $('#btn-nuevo').addEventListener('click', () => abrirPanel(null));
    document.addEventListener('click', (e) => { if (e.target.closest('[data-nuevo]')) abrirPanel(null); });

    document.addEventListener('keydown', (e) => {
        const escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName);
        if (e.key === 'Escape' && state.abierto && !dlg.open && !escribiendo) pedirCierre();
        if (e.key === 'Escape' && state.abierto && !dlg.open && escribiendo && document.activeElement !== buscador) {
            document.activeElement.blur();
        }
        if (e.key === '/' && !escribiendo && !dlg.open) { e.preventDefault(); buscador.focus(); }
    });

    /* ---------------------------------------------------------
       Tabla: abrir, editar y eliminar
       --------------------------------------------------------- */
    tbody.addEventListener('click', (e) => {
        const tr = e.target.closest('tr.row');
        if (!tr) return;
        const boton = e.target.closest('[data-accion]');
        if (boton && boton.dataset.accion === 'eliminar') { eliminarProducto(tr._d); return; }
        abrirPanel(tr);
    });
    tbody.addEventListener('keydown', (e) => {
        const tr = e.target.closest('tr.row');
        if (tr && e.target === tr && (e.key === 'Enter' || e.key === ' ')) { e.preventDefault(); abrirPanel(tr); }
    });

    btnEliminar.addEventListener('click', () => {
        const tr = state.filas.find((x) => x._d.id === String(state.editando));
        if (tr) eliminarProducto(tr._d);
    });

    async function eliminarProducto(d) {
        const ok = await confirmar({
            titulo: '¿Eliminar producto?',
            texto: `“${d.nombre}” se eliminará del inventario. Esta acción no se puede deshacer.`,
            ok: 'Eliminar', peligro: true, icono: 'i-trash',
        });
        if (!ok) return;

        try {
            const resp = await fetch(`${API}/${d.id}`, { method: 'DELETE', credentials: 'same-origin', headers: { Accept: 'application/json' } });
            if (!resp.ok) throw new Error((await leerError(resp)).join(' · '));

            borrarArchivoSubido(d.foto);
            if (state.abierto && state.editando === d.id) { descartarSubidas(); cerrarPanel(); }
            await refrescarFilas();
            aviso('Producto eliminado', 'ok');
        } catch (err) {
            aviso(err.message || 'No se pudo eliminar el producto');
        }
    }

    /* ---------------------------------------------------------
       Guardar (crear o actualizar)
       --------------------------------------------------------- */
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        if (state.guardando || !validar()) return;

        const d = datos();
        const payload = {
            nombre: d.nombre,
            marca: d.marca || null,
            categoria: d.categoria,
            descripcion: d.desc || null,
            tipo: d.tipo,
            precioBase: Number(d.base),
            precioVenta: Number(d.venta),
            fotoProducto: d.foto || null,
            stock: Number(d.stock),
        };

        const nuevo = state.editando === null;
        state.guardando = true;
        btnGuardar.disabled = true;
        $('span', btnGuardar).textContent = 'Guardando…';

        try {
            const resp = await fetch(nuevo ? API : `${API}/${state.editando}`, {
                method: nuevo ? 'POST' : 'PUT',
                credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify(payload),
            });
            if (!resp.ok) { mostrarErroresServidor(await leerError(resp)); return; }
            const guardado = await resp.json();

            // La foto nueva ya quedó en el producto; la anterior (si era nuestra) y las demás subidas sobran
            state.subidas.delete(d.foto);
            descartarSubidas();
            if (state.fotoOriginal && state.fotoOriginal !== d.foto) borrarArchivoSubido(state.fotoOriginal);

            state.snapshot = JSON.stringify(datos());
            cerrarPanel();
            await refrescarFilas(guardado.id ?? state.editando);
            aviso(nuevo ? 'Producto creado' : 'Cambios guardados', 'ok');
        } catch (err) {
            aviso('No se pudo conectar con el servidor. Revisa tu conexión e intenta de nuevo.');
        } finally {
            state.guardando = false;
            btnGuardar.disabled = false;
            $('span', btnGuardar).textContent = state.editando === null ? 'Guardar producto' : 'Guardar cambios';
        }
    });

    // Si se cierra la pestaña con fotos subidas sin guardar, se intenta borrarlas
    window.addEventListener('pagehide', () => {
        state.subidas.forEach((url) => {
            if (url.startsWith(PREFIJO_FOTO)) fetch(`${PAGINA}/imagen?url=${encodeURIComponent(url)}`, { method: 'DELETE', keepalive: true, credentials: 'same-origin' }).catch(() => {});
        });
    });

    /* ---------------------------------------------------------
       Inicio
       --------------------------------------------------------- */
    $$('option[data-pretty]').forEach((o) => { o.textContent = bonito(o.value); });
    estadoDrop('empty');
    cargarFilas();
    render();
})();