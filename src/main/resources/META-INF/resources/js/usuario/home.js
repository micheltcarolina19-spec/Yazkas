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
