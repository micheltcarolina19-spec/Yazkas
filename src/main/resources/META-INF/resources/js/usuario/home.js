/* =========================================================
   YAZKAS · Home (index)
   Ruta: src/main/resources/META-INF/resources/js/usuario/home.js
   - Vista previa de los pasos para agendar (el agendamiento real está en /citas)
   - Catálogo de productos desde /api/catalogo (SIN precios) con ventana "Ver detalles"
   - Animación suave al hacer scroll
   ========================================================= */
(() => {
    'use strict';

    const API_CATALOGO = '/api/catalogo';
    const MAX_PRODUCTOS = 10;

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    /* ---------------------------------------------------------
       1. Vista previa de los pasos para agendar
       --------------------------------------------------------- */
    const booking = $('[data-booking]');
    if (booking) {
        const pasos = $$('.hm-step', booking);
        const paneles = $$('.hm-pane', booking);
        const barra = $('[data-bar]', booking);
        const btnAtras = $('[data-prev]', booking);
        const btnSeguir = $('[data-next]', booking);
        const btnIr = $('[data-go]', booking);
        let actual = 0;

        const mostrar = (indice) => {
            actual = Math.max(0, Math.min(indice, pasos.length - 1));

            pasos.forEach((paso, i) => {
                paso.classList.toggle('is-active', i === actual);
                paso.classList.toggle('is-done', i < actual);
                paso.setAttribute('aria-selected', String(i === actual));
            });
            paneles.forEach((panel, i) => { panel.hidden = i !== actual; });

            barra.style.width = `${((actual + 1) / pasos.length) * 100}%`;
            btnAtras.disabled = actual === 0;

            const esUltimo = actual === pasos.length - 1;
            btnSeguir.hidden = esUltimo;
            btnIr.hidden = !esUltimo;
        };

        pasos.forEach((paso, i) => paso.addEventListener('click', () => mostrar(i)));
        btnAtras.addEventListener('click', () => mostrar(actual - 1));
        btnSeguir.addEventListener('click', () => mostrar(actual + 1));

        // Flechas del teclado sobre los pasos
        booking.addEventListener('keydown', (e) => {
            if (!e.target.closest('.hm-step')) return;
            if (e.key === 'ArrowRight') { mostrar(actual + 1); pasos[actual].focus(); }
            if (e.key === 'ArrowLeft') { mostrar(actual - 1); pasos[actual].focus(); }
        });

        mostrar(0);
    }

    /* ---------------------------------------------------------
       2. Catálogo de productos (sin precios)
       --------------------------------------------------------- */
    const contenedor = $('#productos');
    const estado = $('#productos-estado');
    const flechas = $$('[data-scroll]');

    // Texto legible para la categoría: "LACTOREEMPLAZANTES" -> "Lactoreemplazantes"
    const bonito = (texto) => (texto || '')
        .toLowerCase()
        .replace(/_/g, ' ')
        .replace(/^\p{L}/u, (letra) => letra.toUpperCase());

    const crearTarjeta = (producto) => {
        const tarjeta = document.createElement('article');
        tarjeta.className = 'hm-product';

        // Foto del producto (se muestra completa sobre fondo blanco)
        const imagen = document.createElement('div');
        imagen.className = 'hm-product__img';
        const icono = '<svg class="hm-ico" aria-hidden="true"><use href="#h-bag"/></svg>';
        const sinFoto = () => {
            imagen.classList.add('hm-product__img--empty');
            imagen.innerHTML = icono;
            if (producto.categoria) imagen.prepend(chip);
        };

        // Categoría como etiqueta sobre la foto
        const chip = document.createElement('span');
        chip.className = 'hm-product__chip';
        chip.textContent = bonito(producto.categoria);

        if (producto.fotoProducto) {
            const img = document.createElement('img');
            img.src = producto.fotoProducto;
            img.alt = producto.nombre || 'Producto';
            img.loading = 'lazy';
            img.addEventListener('error', sinFoto, { once: true });
            imagen.appendChild(img);
            if (producto.categoria) imagen.appendChild(chip);
        } else {
            sinFoto();
        }

        const cuerpo = document.createElement('div');
        cuerpo.className = 'hm-product__body';

        const nombre = document.createElement('h3');
        nombre.className = 'hm-product__name';
        nombre.textContent = producto.nombre || 'Producto';

        cuerpo.append(nombre);

        if (producto.marca) {
            const marca = document.createElement('span');
            marca.className = 'hm-product__brand';
            marca.textContent = producto.marca;
            cuerpo.appendChild(marca);
        }

        // Descripción corta (máximo 2 líneas; completa en "Ver detalles")
        if (producto.descripcion) {
            const descripcion = document.createElement('p');
            descripcion.className = 'hm-product__desc';
            descripcion.textContent = producto.descripcion;
            cuerpo.appendChild(descripcion);
        }

        const disponible = Number(producto.stock) > 0;
        const stock = document.createElement('span');
        stock.className = 'hm-product__stock' + (disponible ? '' : ' is-out');
        stock.textContent = textoDisponibilidad(producto);

        const detalles = document.createElement('button');
        detalles.type = 'button';
        detalles.className = 'hm-product__more';
        detalles.innerHTML = 'Ver detalles <svg class="hm-ico" aria-hidden="true"><use href="#h-arrow"/></svg>';
        detalles.setAttribute('aria-label', `Ver detalles de ${producto.nombre || 'este producto'}`);
        detalles.addEventListener('click', () => abrirDetalle(producto));

        const pie = document.createElement('div');
        pie.className = 'hm-product__foot';
        pie.append(stock, detalles);
        cuerpo.appendChild(pie);

        tarjeta.append(imagen, cuerpo);
        return tarjeta;
    };

    function textoDisponibilidad(producto) {
        return Number(producto.stock) > 0 ? 'Disponible en tienda' : 'Pregunta por él';
    }

    /* ---------------------------------------------------------
       2.1 Ventana "Ver detalles" (sin precio)
       Pide el detalle a /api/catalogo/{id}; si falla, usa los datos de la tarjeta.
       --------------------------------------------------------- */
    const modal = $('#detalle-producto');
    const d = {
        img: $('#detalle-img'),
        categoria: $('#detalle-categoria'),
        nombre: $('#detalle-nombre'),
        marca: $('#detalle-marca'),
        descripcion: $('#detalle-descripcion'),
        tipo: $('#detalle-tipo'),
        stock: $('#detalle-stock'),
    };
    let disparador = null;

    const pintarDetalle = (producto) => {
        d.img.replaceChildren();
        if (producto.fotoProducto) {
            const img = document.createElement('img');
            img.src = producto.fotoProducto;
            img.alt = producto.nombre || 'Producto';
            img.addEventListener('error', () => {
                d.img.innerHTML = '<svg class="hm-ico" aria-hidden="true"><use href="#h-bag"/></svg>';
            }, { once: true });
            d.img.appendChild(img);
        } else {
            d.img.innerHTML = '<svg class="hm-ico" aria-hidden="true"><use href="#h-bag"/></svg>';
        }
        d.categoria.textContent = bonito(producto.categoria);
        d.nombre.textContent = producto.nombre || 'Producto';
        d.marca.textContent = producto.marca || '';
        d.descripcion.textContent = producto.descripcion || 'Pregúntanos por este producto en tu próxima visita.';
        d.tipo.textContent = producto.tipo ? bonito(producto.tipo) : 'No especificada';
        d.stock.textContent = textoDisponibilidad(producto);
    };

    async function abrirDetalle(producto) {
        if (!modal) return;
        disparador = document.activeElement;
        pintarDetalle(producto);
        if (typeof modal.showModal === 'function') modal.showModal();
        else modal.setAttribute('open', '');

        try {
            const resp = await fetch(`${API_CATALOGO}/${encodeURIComponent(producto.id)}`, { headers: { Accept: 'application/json' } });
            if (resp.ok) pintarDetalle(await resp.json());
        } catch (error) {
            // Sin conexión: se quedan los datos que ya tenía la tarjeta
        }
    }

    const cerrarDetalle = () => {
        if (!modal) return;
        if (typeof modal.close === 'function') modal.close();
        else modal.removeAttribute('open');
    };

    if (modal) {
        $('[data-cerrar]', modal).addEventListener('click', cerrarDetalle);
        // Cerrar al hacer clic fuera de la ventana
        modal.addEventListener('click', (e) => { if (e.target === modal) cerrarDetalle(); });
        // Devolver el foco al botón que abrió la ventana
        modal.addEventListener('close', () => { disparador?.focus?.(); });
    }

    const mostrarEstado = (mensaje) => {
        contenedor.hidden = true;
        estado.hidden = false;
        estado.textContent = mensaje;
        flechas.forEach((b) => { b.hidden = true; });
    };

    const actualizarFlechas = () => {
        const max = contenedor.scrollWidth - contenedor.clientWidth - 2;
        const [anterior, siguiente] = flechas;
        if (!anterior || !siguiente) return;
        anterior.disabled = contenedor.scrollLeft <= 2;
        siguiente.disabled = contenedor.scrollLeft >= max;
    };

    const cargarCatalogo = async () => {
        if (!contenedor) return;
        try {
            const resp = await fetch(API_CATALOGO, { headers: { Accept: 'application/json' } });
            if (!resp.ok) throw new Error(`HTTP ${resp.status}`);
            const productos = await resp.json();

            if (!Array.isArray(productos) || productos.length === 0) {
                mostrarEstado('Muy pronto verás aquí nuestros productos.');
                return;
            }

            contenedor.replaceChildren(...productos.slice(0, MAX_PRODUCTOS).map(crearTarjeta));
            actualizarFlechas();
        } catch (error) {
            console.error('[home] No se pudo cargar el catálogo', error);
            mostrarEstado('No pudimos cargar los productos en este momento. Intenta de nuevo más tarde.');
        } finally {
            contenedor.setAttribute('aria-busy', 'false');
        }
    };

    flechas.forEach((boton) => boton.addEventListener('click', () => {
        const direccion = Number(boton.dataset.scroll);
        contenedor.scrollBy({ left: direccion * contenedor.clientWidth * 0.8, behavior: 'smooth' });
    }));
    contenedor?.addEventListener('scroll', actualizarFlechas, { passive: true });
    window.addEventListener('resize', actualizarFlechas);

    cargarCatalogo();

    /* ---------------------------------------------------------
       3. Aparición suave de las secciones al hacer scroll
       --------------------------------------------------------- */
    const secciones = $$('.hm-trust, .hm-services__intro, .hm-service, .hm-agenda__head, .hm-booking, .hm-shop__intro, .hm-shop__rail');
    if ('IntersectionObserver' in window) {
        const observador = new IntersectionObserver((entradas) => {
            entradas.forEach((entrada) => {
                if (entrada.isIntersecting) {
                    entrada.target.classList.add('is-visible');
                    observador.unobserve(entrada.target);
                }
            });
        }, { threshold: 0.12 });

        secciones.forEach((el, i) => {
            el.classList.add('hm-reveal');
            el.style.transitionDelay = `${(i % 3) * 90}ms`;
            observador.observe(el);
        });
    }

    /* ---------------------------------------------------------
       4. Año actual en el pie de página
       --------------------------------------------------------- */
    const anio = $('#anio');
    if (anio) anio.textContent = String(new Date().getFullYear());
})();



/* ---------------------------------------------------------
   Huellitas al hacer clic
   - Una huella grande que aparece con rebote y se desvanece
   - Huellitas pequeñas que salen disparadas alrededor
   --------------------------------------------------------- */
(() => {
    'use strict';
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const HUELLA = '<svg viewBox="0 0 48 48" aria-hidden="true">'
        + '<ellipse cx="24" cy="31" rx="10.5" ry="8.5"/>'
        + '<ellipse cx="9.5" cy="22" rx="4.2" ry="5.6" transform="rotate(-18 9.5 22)"/>'
        + '<ellipse cx="18" cy="12.5" rx="4.4" ry="6" transform="rotate(-6 18 12.5)"/>'
        + '<ellipse cx="30" cy="12.5" rx="4.4" ry="6" transform="rotate(6 30 12.5)"/>'
        + '<ellipse cx="38.5" cy="22" rx="4.2" ry="5.6" transform="rotate(18 38.5 22)"/>'
        + '</svg>';

    const COLORES = ['#2f95e0', '#4fb3f5', '#ff8fb1', '#3fcfdc', '#0b3a63'];
    const MINIS = 5;
    const azar = (min, max) => Math.random() * (max - min) + min;
    const color = () => COLORES[Math.floor(Math.random() * COLORES.length)];

    const crear = (clase, x, y) => {
        const el = document.createElement('span');
        el.className = clase;
        el.innerHTML = HUELLA;
        el.style.left = `${x}px`;
        el.style.top = `${y}px`;
        el.style.color = color();
        el.style.setProperty('--giro', `${Math.round(azar(-35, 35))}deg`);
        el.addEventListener('animationend', () => el.remove(), { once: true });
        document.body.appendChild(el);
        return el;
    };

    document.addEventListener('pointerdown', (e) => {
        if (e.button !== 0) return;                                // solo clic izquierdo / toque
        if (e.target.closest('input, textarea, select')) return;   // no molestar al escribir

        // Huella principal
        crear('paw-click', e.clientX, e.clientY);

        // Huellitas que salen alrededor
        for (let i = 0; i < MINIS; i++) {
            const angulo = (i / MINIS) * Math.PI * 2 + azar(-0.3, 0.3);
            const distancia = azar(30, 55);
            const mini = crear('paw-mini', e.clientX, e.clientY);
            mini.style.setProperty('--dx', `${Math.cos(angulo) * distancia}px`);
            mini.style.setProperty('--dy', `${Math.sin(angulo) * distancia}px`);
        }
    });
})();



/* ---------------------------------------------------------
   Bloquear selección de texto y arrastre de imágenes
   (los campos de formulario siguen funcionando normal)
   --------------------------------------------------------- */
(() => {
    'use strict';

    const esCampo = (el) => el instanceof Element
        && el.closest('input, textarea, select, [contenteditable="true"]');

    // Evita que se seleccione texto con el mouse o con doble clic
    document.addEventListener('selectstart', (e) => {
        if (!esCampo(e.target)) e.preventDefault();
    });

    // Evita arrastrar imágenes y enlaces
    document.addEventListener('dragstart', (e) => {
        if (!esCampo(e.target)) e.preventDefault();
    });

    // Evita Ctrl+A (seleccionar todo) fuera de los campos
    document.addEventListener('keydown', (e) => {
        if ((e.ctrlKey || e.metaKey) && e.key.toLowerCase() === 'a' && !esCampo(e.target)) {
            e.preventDefault();
        }
    });

    // Menú del clic derecho sobre imágenes ("Guardar imagen como...")
    document.addEventListener('contextmenu', (e) => {
        if (e.target instanceof HTMLImageElement) e.preventDefault();
    });
})();


/* ---------------------------------------------------------
   Rastro de huellitas al hacer scroll
   - Junto a la barra van quedando pisadas alternadas (izq/der)
   - Miran hacia donde se avanza: abajo al bajar, arriba al subir
   - Quedan pegadas a la página, así se ven "atrás" como un rastro
   --------------------------------------------------------- */
(() => {
    'use strict';
    if (window.matchMedia('(prefers-reduced-motion: reduce)').matches) return;

    const root = document.documentElement;
    const PASO = 70;          // px de scroll entre pisada y pisada
    const VIDA = 2300;        // ms que dura cada pisada
    const TAMANO = 18;        // px
    const MAX = 40;           // pisadas simultáneas como máximo
    const COLOR = '#1b6fc2';

    const HUELLA = '<svg viewBox="0 0 48 48" aria-hidden="true">'
        + '<ellipse cx="24" cy="31" rx="10.5" ry="8.5"/>'
        + '<ellipse cx="9.5" cy="22" rx="4.2" ry="5.6" transform="rotate(-18 9.5 22)"/>'
        + '<ellipse cx="18" cy="12.5" rx="4.4" ry="6" transform="rotate(-6 18 12.5)"/>'
        + '<ellipse cx="30" cy="12.5" rx="4.4" ry="6" transform="rotate(6 30 12.5)"/>'
        + '<ellipse cx="38.5" cy="22" rx="4.2" ry="5.6" transform="rotate(18 38.5 22)"/>'
        + '</svg>';

    const azar = (min, max) => Math.random() * (max - min) + min;

    // Estilos propios (no hace falta tocar el CSS)
    const estilos = document.createElement('style');
    estilos.textContent = `
        .paw-trail-capa { position: absolute; left: 0; top: 0; width: 100%;
            overflow: hidden; overflow: clip; pointer-events: none; z-index: 9999; }
        .paw-trail { position: absolute; left: 0; top: 0; width: ${TAMANO}px; height: ${TAMANO}px;
            color: ${COLOR}; pointer-events: none; will-change: transform, opacity; }
        .paw-trail svg { display: block; width: 100%; height: 100%; fill: currentColor;
            filter: drop-shadow(0 0 1px rgba(255, 255, 255, .9)); }
        @media print { .paw-trail-capa { display: none; } }`;
    document.head.appendChild(estilos);

    // Capa del alto de la página: las pisadas se mueven con el contenido.
    // overflow: clip evita que una pisada en el borde agrande la página.
    const capa = document.createElement('div');
    capa.className = 'paw-trail-capa';
    capa.setAttribute('aria-hidden', 'true');
    root.appendChild(capa);

    const ajustarCapa = () => {
        capa.style.height = '0px';
        capa.style.height = `${Math.max(root.scrollHeight, document.body.scrollHeight)}px`;
    };

    const vivas = [];
    let ultimo = window.scrollY;
    let acumulado = 0;
    let lado = 1;
    let tiempo = 0;

    const pisada = (dir) => {
        lado *= -1;
        ajustarCapa();

        const el = document.createElement('span');
        el.className = 'paw-trail';
        el.innerHTML = HUELLA;

        const x = root.clientWidth - 30 + lado * TAMANO * 0.38;
        const y = window.scrollY + window.innerHeight * (dir > 0 ? 0.62 : 0.38);
        el.style.translate = `${x - TAMANO / 2}px ${y - TAMANO / 2}px`;
        el.style.rotate = `${(dir > 0 ? 180 : 0) + lado * dir * 9 + azar(-4, 4)}deg`;

        capa.appendChild(el);
        vivas.push(el);
        while (vivas.length > MAX) vivas.shift().remove();

        el.animate([
            { opacity: 0,   scale: 0.55 },
            { opacity: 0.6, scale: 1.06, offset: 0.08 },
            { opacity: 0.55, scale: 1,   offset: 0.14 },
            { opacity: 0.45, scale: 1,   offset: 0.6 },
            { opacity: 0,   scale: 0.94 }
        ], { duration: VIDA, easing: 'ease-out' }).finished.then(() => {
            el.remove();
            const i = vivas.indexOf(el);
            if (i > -1) vivas.splice(i, 1);
        }).catch(() => {});
    };

    window.addEventListener('scroll', () => {
        const y = window.scrollY;
        const dy = y - ultimo;
        ultimo = y;
        if (!dy) return;

        if (Math.sign(dy) !== Math.sign(acumulado)) acumulado = 0;   // cambió de dirección
        acumulado += dy;

        const ahora = performance.now();
        if (Math.abs(acumulado) < PASO || ahora - tiempo < 65) return;

        acumulado = 0;
        tiempo = ahora;
        pisada(dy > 0 ? 1 : -1);
    }, { passive: true });
})();