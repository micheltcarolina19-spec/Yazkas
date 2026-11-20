/* =========================================================
   Yazka's · Catálogo de productos (vista del cliente)
   Ruta: src/main/resources/META-INF/resources/js/usuario/catalogo.js
   - Carga los productos desde /api/catalogo
   - Muestra SOLO: foto, categoría, marca, nombre y descripción
     (nunca precios ni stock, aunque el API los llegara a enviar)
   - Búsqueda, filtros por grupo de categorías, orden y favoritos
   - Los favoritos se guardan en el navegador (localStorage) mientras no haya login
   - La URL guarda la búsqueda y el filtro: /catalogo?q=shampoo&cat=higiene
   ========================================================= */
(() => {
    'use strict';

    const API_CATALOGO = '/api/catalogo';
    const CLAVE_FAVORITOS = 'yazkas:favoritos';

    // Grupos de los filtros -> categorías reales del enum CategoriaProducto
    const GRUPOS = {
        alimentos: ['CONCENTRADOS', 'SNACKS', 'LACTOREEMPLAZANTES'],
        juguetes: ['JUGUETES'],
        accesorios: ['ACCESORIOS'],
        higiene: ['COSMETICOS', 'ARENAS'],
        salud: ['DESPARASITANTES', 'MEDICAMENTOS', 'VITAMINAS'],
    };

    // Nombre bonito de cada categoría para la etiqueta de la tarjeta
    const NOMBRE_CATEGORIA = {
        CONCENTRADOS: 'Concentrados',
        SNACKS: 'Snacks',
        LACTOREEMPLAZANTES: 'Lactoreemplazantes',
        JUGUETES: 'Juguetes',
        ACCESORIOS: 'Accesorios',
        COSMETICOS: 'Higiene y aseo',
        ARENAS: 'Arenas',
        DESPARASITANTES: 'Desparasitantes',
        MEDICAMENTOS: 'Medicamentos',
        VITAMINAS: 'Vitaminas',
    };

    // Ícono que se muestra cuando el producto no tiene foto, según su grupo
    const ICONO_GRUPO = {
        alimentos: 'c-bowl',
        juguetes: 'c-ball',
        accesorios: 'c-tag',
        higiene: 'c-drop',
        salud: 'c-health',
    };

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const el = {
        grid: $('#ct-grid'),
        resultado: $('#ct-resultado'),
        buscar: $('#ct-buscar'),
        limpiar: $('[data-limpiar-busqueda]'),
        form: $('[data-busqueda]'),
        orden: $('#ct-orden'),
        pills: $$('.ct-pill'),
        vacio: $('#ct-vacio'),
        vacioTitulo: $('#ct-vacio-titulo'),
        vacioTexto: $('#ct-vacio-texto'),
        vacioAccion: $('#ct-vacio-accion'),
        modal: $('#ct-detalle'),
    };

    const estado = {
        productos: [],
        grupo: 'todos',
        busqueda: '',
        orden: 'nombre-asc',
        favoritos: new Set(),
        cargado: false,
    };

    /* ---------------------------------------------------------
       Utilidades
       --------------------------------------------------------- */

    // Quita tildes y pasa a minúsculas: "Jabón" -> "jabon"
    const normalizar = (texto) => (texto || '')
        .toString()
        .normalize('NFD')
        .replace(/[̀-ͯ]/g, '')
        .toLowerCase()
        .trim();

    const nombreCategoria = (cat) => NOMBRE_CATEGORIA[cat]
        || (cat ? cat.charAt(0) + cat.slice(1).toLowerCase().replace(/_/g, ' ') : 'Producto');

    const grupoDe = (cat) => Object.keys(GRUPOS).find((g) => GRUPOS[g].includes(cat)) || null;

    // Solo los campos permitidos para el cliente (se descarta cualquier otro, como precios o stock)
    const limpiarProducto = (p) => ({
        id: p.id,
        nombre: p.nombre || 'Producto',
        marca: p.marca || '',
        categoria: p.categoria || '',
        descripcion: p.descripcion || '',
        fotoProducto: p.fotoProducto || '',
    });

    // Escribe el texto y resalta lo buscado con <mark>, sin usar innerHTML con datos del servidor
    const escribirResaltado = (nodo, texto, busqueda) => {
        nodo.textContent = '';
        const q = normalizar(busqueda);
        if (!q) { nodo.textContent = texto; return; }
        const base = normalizar(texto);
        let desde = 0;
        let i = base.indexOf(q);
        if (i === -1 || base.length !== texto.length) { nodo.textContent = texto; return; }
        while (i !== -1) {
            if (i > desde) nodo.append(texto.slice(desde, i));
            const mark = document.createElement('mark');
            mark.textContent = texto.slice(i, i + q.length);
            nodo.append(mark);
            desde = i + q.length;
            i = base.indexOf(q, desde);
        }
        if (desde < texto.length) nodo.append(texto.slice(desde));
    };

    const icono = (id, extra = '') => `<svg class="hm-ico ${extra}" aria-hidden="true"><use href="#${id}"/></svg>`;

    /* ---------------------------------------------------------
       Favoritos (localStorage, protegido con try/catch)
       --------------------------------------------------------- */
    const cargarFavoritos = () => {
        try {
            const guardados = JSON.parse(localStorage.getItem(CLAVE_FAVORITOS) || '[]');
            if (Array.isArray(guardados)) estado.favoritos = new Set(guardados.map(String));
        } catch (e) {
            estado.favoritos = new Set();
        }
    };

    const guardarFavoritos = () => {
        try {
            localStorage.setItem(CLAVE_FAVORITOS, JSON.stringify([...estado.favoritos]));
        } catch (e) {
            // Navegador sin almacenamiento (modo privado): los favoritos duran mientras la página esté abierta
        }
    };

    const esFavorito = (id) => estado.favoritos.has(String(id));

    const alternarFavorito = (producto) => {
        const id = String(producto.id);
        if (estado.favoritos.has(id)) estado.favoritos.delete(id);
        else estado.favoritos.add(id);
        guardarFavoritos();
        actualizarConteos();
        // Si estamos viendo "Mis favoritos", la lista cambia
        if (estado.grupo === 'favoritos') render();
        else sincronizarBotonesFavorito(id);
    };

    const pintarBotonFavorito = (boton, producto) => {
        const activo = esFavorito(producto.id);
        boton.setAttribute('aria-pressed', String(activo));
        boton.setAttribute('aria-label', `${activo ? 'Quitar de' : 'Agregar a'} favoritos: ${producto.nombre}`);
        const texto = $('span', boton);
        if (texto) texto.textContent = activo ? 'En tus favoritos' : 'Agregar a favoritos';
    };

    const sincronizarBotonesFavorito = (id) => {
        const producto = estado.productos.find((p) => String(p.id) === id);
        if (!producto) return;
        $$(`[data-fav="${CSS.escape(id)}"]`).forEach((b) => pintarBotonFavorito(b, producto));
    };

    /* ---------------------------------------------------------
       Filtrado y orden
       --------------------------------------------------------- */
    const coincideBusqueda = (p, q) => {
        if (!q) return true;
        const texto = normalizar([p.nombre, p.marca, p.descripcion, nombreCategoria(p.categoria), p.categoria].join(' '));
        // Todas las palabras buscadas deben aparecer (en cualquier orden)
        return q.split(/\s+/).every((palabra) => texto.includes(palabra));
    };

    const enGrupo = (p, grupo) => {
        if (grupo === 'todos') return true;
        if (grupo === 'favoritos') return esFavorito(p.id);
        return (GRUPOS[grupo] || []).includes(p.categoria);
    };

    const ordenar = (lista) => {
        const cmp = (a, b) => a.localeCompare(b, 'es', { sensitivity: 'base' });
        const copia = [...lista];
        switch (estado.orden) {
            case 'nombre-desc': return copia.sort((a, b) => cmp(b.nombre, a.nombre));
            case 'marca': return copia.sort((a, b) => cmp(a.marca, b.marca) || cmp(a.nombre, b.nombre));
            case 'categoria': return copia.sort((a, b) => cmp(nombreCategoria(a.categoria), nombreCategoria(b.categoria)) || cmp(a.nombre, b.nombre));
            default: return copia.sort((a, b) => cmp(a.nombre, b.nombre));
        }
    };

    const productosVisibles = () => {
        const q = normalizar(estado.busqueda);
        return ordenar(estado.productos.filter((p) => enGrupo(p, estado.grupo) && coincideBusqueda(p, q)));
    };

    /* ---------------------------------------------------------
       Tarjeta de producto
       --------------------------------------------------------- */
    const crearTarjeta = (p, indice) => {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'ct-card';
        tarjeta.style.animationDelay = `${Math.min(indice, 11) * 40}ms`;

        // Foto (clic = ver detalles)
        const media = document.createElement('button');
        media.type = 'button';
        media.className = 'ct-card__media';
        media.setAttribute('aria-label', `Ver detalles de ${p.nombre}`);
        const sinFoto = () => {
            const grupo = grupoDe(p.categoria);
            media.classList.add('ct-card__media--empty', `ct-card__media--${grupo || 'otro'}`);
            media.innerHTML = icono(ICONO_GRUPO[grupo] || 'c-bag');
        };
        if (p.fotoProducto) {
            const img = document.createElement('img');
            img.src = p.fotoProducto;
            img.alt = p.nombre;
            img.loading = 'lazy';
            img.decoding = 'async';
            img.addEventListener('error', sinFoto, { once: true });
            media.appendChild(img);
        } else {
            sinFoto();
        }
        media.addEventListener('click', () => abrirDetalle(p));

        // Categoría
        const badge = document.createElement('span');
        badge.className = 'ct-badge';
        badge.textContent = nombreCategoria(p.categoria);

        // Favorito
        const fav = document.createElement('button');
        fav.type = 'button';
        fav.className = 'ct-fav';
        fav.dataset.fav = String(p.id);
        fav.innerHTML = icono('c-heart');
        pintarBotonFavorito(fav, p);
        fav.addEventListener('click', () => {
            alternarFavorito(p);
            fav.classList.remove('is-pop');
            void fav.offsetWidth; // reinicia la animación
            fav.classList.add('is-pop');
        });

        // Cuerpo: marca, nombre, descripción y "Ver detalles"
        const cuerpo = document.createElement('div');
        cuerpo.className = 'ct-card__body';

        if (p.marca) {
            const marca = document.createElement('p');
            marca.className = 'ct-brand';
            escribirResaltado(marca, p.marca, estado.busqueda);
            cuerpo.appendChild(marca);
        }

        const nombre = document.createElement('h3');
        nombre.className = 'ct-card__name';
        escribirResaltado(nombre, p.nombre, estado.busqueda);
        cuerpo.appendChild(nombre);

        if (p.descripcion) {
            const desc = document.createElement('p');
            desc.className = 'ct-card__desc';
            escribirResaltado(desc, p.descripcion, estado.busqueda);
            cuerpo.appendChild(desc);
        }

        const mas = document.createElement('button');
        mas.type = 'button';
        mas.className = 'ct-card__more';
        mas.innerHTML = `Ver detalles ${icono('c-arrow')}`;
        mas.setAttribute('aria-label', `Ver detalles de ${p.nombre}`);
        mas.addEventListener('click', () => abrirDetalle(p));
        cuerpo.appendChild(mas);

        tarjeta.append(media, badge, fav, cuerpo);
        return tarjeta;
    };

    /* ---------------------------------------------------------
       Pintar la página
       --------------------------------------------------------- */
    const mostrarVacio = (titulo, texto, textoBoton, accion) => {
        el.vacioTitulo.textContent = titulo;
        el.vacioTexto.textContent = texto;
        el.vacioAccion.textContent = textoBoton;
        el.vacioAccion.onclick = accion;
        el.vacio.hidden = false;
    };

    const actualizarResultado = (cantidad) => {
        const total = estado.productos.length;
        if (!estado.cargado) return;
        const palabra = cantidad === 1 ? 'producto' : 'productos';
        el.resultado.innerHTML = '';
        const fuerte = document.createElement('strong');
        fuerte.textContent = `${cantidad} ${palabra}`;
        el.resultado.append('Mostrando ', fuerte);
        if (cantidad !== total) el.resultado.append(` de ${total}`);
        if (estado.busqueda) el.resultado.append(` para “${estado.busqueda}”`);
    };

    const actualizarConteos = () => {
        const q = normalizar(estado.busqueda);
        const base = estado.productos.filter((p) => coincideBusqueda(p, q));
        const contar = (grupo) => base.filter((p) => enGrupo(p, grupo)).length;
        $$('[data-cuenta]').forEach((span) => { span.textContent = String(contar(span.dataset.cuenta)); });
    };

    const render = () => {
        const lista = productosVisibles();
        el.grid.replaceChildren(...lista.map(crearTarjeta));
        el.grid.setAttribute('aria-busy', 'false');
        el.grid.hidden = lista.length === 0;
        el.vacio.hidden = true;
        actualizarResultado(lista.length);
        actualizarConteos();

        if (lista.length > 0) return;

        if (estado.productos.length === 0) {
            mostrarVacio('Muy pronto verás nuestros productos',
                'Estamos preparando el catálogo. Mientras tanto, pregunta por ellos en tu próxima visita.',
                'Volver al inicio', () => { window.location.href = '/'; });
        } else if (estado.grupo === 'favoritos' && !estado.busqueda) {
            mostrarVacio('Aún no tienes favoritos',
                'Toca el corazón de los productos que te gusten para encontrarlos rápido aquí.',
                'Ver todos los productos', () => seleccionarGrupo('todos'));
        } else {
            mostrarVacio('No encontramos productos',
                estado.busqueda
                    ? `No hay resultados para “${estado.busqueda}” en esta categoría. Prueba con otra palabra.`
                    : 'Todavía no hay productos en esta categoría.',
                'Ver todos los productos', () => { limpiarBusqueda(); seleccionarGrupo('todos'); });
        }
    };

    const mostrarError = () => {
        estado.cargado = false;
        el.grid.hidden = true;
        el.grid.setAttribute('aria-busy', 'false');
        el.resultado.textContent = 'No pudimos cargar los productos.';
        mostrarVacio('No pudimos cargar el catálogo',
            'Revisa tu conexión e inténtalo de nuevo en un momento.',
            'Intentar de nuevo', () => { el.vacio.hidden = true; cargar(); });
        el.vacioAccion.insertAdjacentHTML('afterbegin', icono('c-refresh'));
    };

    /* ---------------------------------------------------------
       Filtros, búsqueda, orden y URL
       --------------------------------------------------------- */
    const actualizarURL = () => {
        const params = new URLSearchParams();
        if (estado.busqueda) params.set('q', estado.busqueda);
        if (estado.grupo !== 'todos') params.set('cat', estado.grupo);
        const nueva = `${location.pathname}${params.toString() ? `?${params}` : ''}`;
        history.replaceState(null, '', nueva);
    };

    const seleccionarGrupo = (grupo) => {
        estado.grupo = grupo;
        el.pills.forEach((pill) => {
            const activo = pill.dataset.grupo === grupo;
            pill.classList.toggle('is-active', activo);
            pill.setAttribute('aria-pressed', String(activo));
        });
        actualizarURL();
        if (estado.cargado) render();
    };

    const limpiarBusqueda = () => {
        el.buscar.value = '';
        estado.busqueda = '';
        el.limpiar.hidden = true;
        actualizarURL();
    };

    let temporizador = null;
    el.buscar.addEventListener('input', () => {
        el.limpiar.hidden = el.buscar.value.length === 0;
        clearTimeout(temporizador);
        temporizador = setTimeout(() => {
            estado.busqueda = el.buscar.value.trim();
            actualizarURL();
            if (estado.cargado) render();
        }, 180);
    });
    el.form.addEventListener('submit', (e) => e.preventDefault());
    el.buscar.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && el.buscar.value) { limpiarBusqueda(); render(); }
    });
    el.limpiar.addEventListener('click', () => { limpiarBusqueda(); render(); el.buscar.focus(); });

    el.pills.forEach((pill) => pill.addEventListener('click', () => seleccionarGrupo(pill.dataset.grupo)));

    el.orden.addEventListener('change', () => { estado.orden = el.orden.value; if (estado.cargado) render(); });

    /* ---------------------------------------------------------
       Ventana "Ver detalles"
       --------------------------------------------------------- */
    let productoAbierto = null;
    let disparador = null;
    const favDetalle = $('#ct-detalle-fav');

    const abrirDetalle = (p) => {
        if (!el.modal) return;
        productoAbierto = p;
        disparador = document.activeElement;

        const media = $('#ct-detalle-img');
        media.replaceChildren();
        if (p.fotoProducto) {
            const img = document.createElement('img');
            img.src = p.fotoProducto;
            img.alt = p.nombre;
            img.addEventListener('error', () => { media.innerHTML = icono('c-bag'); }, { once: true });
            media.appendChild(img);
        } else {
            media.innerHTML = icono(ICONO_GRUPO[grupoDe(p.categoria)] || 'c-bag');
        }
        $('#ct-detalle-categoria').textContent = nombreCategoria(p.categoria);
        $('#ct-detalle-marca').textContent = p.marca;
        $('#ct-detalle-marca').hidden = !p.marca;
        $('#ct-detalle-nombre').textContent = p.nombre;
        $('#ct-detalle-descripcion').textContent = p.descripcion || 'Pregúntanos por este producto en tu próxima visita.';
        favDetalle.dataset.fav = String(p.id);
        pintarBotonFavorito(favDetalle, p);

        if (typeof el.modal.showModal === 'function') el.modal.showModal();
        else el.modal.setAttribute('open', '');
    };

    const cerrarDetalle = () => {
        if (typeof el.modal.close === 'function') el.modal.close();
        else el.modal.removeAttribute('open');
    };

    if (el.modal) {
        $('[data-cerrar]', el.modal).addEventListener('click', cerrarDetalle);
        el.modal.addEventListener('click', (e) => { if (e.target === el.modal) cerrarDetalle(); });
        el.modal.addEventListener('close', () => { disparador?.focus?.(); productoAbierto = null; });
        favDetalle.addEventListener('click', () => { if (productoAbierto) alternarFavorito(productoAbierto); });
    }

    /* ---------------------------------------------------------
       Carga inicial
       --------------------------------------------------------- */
    async function cargar() {
        el.grid.hidden = false;
        el.grid.setAttribute('aria-busy', 'true');
        el.resultado.textContent = 'Cargando productos…';
        try {
            const resp = await fetch(API_CATALOGO, { headers: { Accept: 'application/json' } });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const datos = await resp.json();
            estado.productos = Array.isArray(datos) ? datos.map(limpiarProducto) : [];
            estado.cargado = true;
            render();
        } catch (error) {
            console.error('[catalogo] No se pudo cargar el catálogo', error);
            mostrarError();
        }
    }

    // Lee la búsqueda y el filtro de la URL (permite enlazar, por ejemplo, /catalogo?cat=juguetes)
    const params = new URLSearchParams(location.search);
    const grupoURL = params.get('cat');
    if (grupoURL && (grupoURL === 'favoritos' || GRUPOS[grupoURL])) seleccionarGrupo(grupoURL);
    if (params.get('q')) {
        el.buscar.value = params.get('q');
        estado.busqueda = el.buscar.value.trim();
        el.limpiar.hidden = false;
    }

    // En celular el texto de ayuda de la búsqueda se acorta para que no se corte
    const textoAyuda = window.matchMedia('(max-width: 560px)');
    const ajustarAyuda = () => {
        el.buscar.placeholder = textoAyuda.matches
            ? '¿Qué buscas para tu mascota?'
            : '¿Qué estás buscando para tu mascota?';
    };
    ajustarAyuda();
    textoAyuda.addEventListener?.('change', ajustarAyuda);

    cargarFavoritos();
    cargar();

    const anio = $('#anio');
    if (anio) anio.textContent = String(new Date().getFullYear());
})();


