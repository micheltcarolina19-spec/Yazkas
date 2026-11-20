/* ==========================================================
   huellitas.js · Huellas al hacer clic y rastro al hacer scroll
   Yazkas Peluquería Canina — sin dependencias.

   Uso:   <script src="/js/huellitas.js" defer></script>
          (se inicia solo; va de la mano con huellitas.css)

   Opcional:
   - Rastro también dentro de un contenedor con scroll propio:
         <div data-huellas-scroll> … </div>
   - Zona donde NO se estampan huellas al hacer clic:
         <div data-huellas-off> … </div>
   - Apagarlo en una vista:   <html data-huellitas="off">
   - Cambiar opciones:        Huellitas.iniciar({ paso: 90, color: '#e0662f' })
   - Estampar a mano (ej. al guardar con éxito):
         Huellitas.estampar(x, y)        // coordenadas de pantalla
         Huellitas.celebrar(elemento)    // caminito de huellas sobre un elemento
   ========================================================== */
(() => {
    'use strict';
    if (window.Huellitas) return;

    const root = document.documentElement;

    const DEFAULTS = {
        clic:          true,
        scroll:        true,
        color:         '#2f95e0',   // huella del clic
        colorScroll:   '#1b6fc2',   // huellas del rastro
        tamano:        30,          // px, huella del clic
        tamanoScroll:  18,          // px, huellas del rastro
        paso:          70,          // px de scroll entre pisada y pisada
        vida:          1500,        // ms que dura la huella del clic
        vidaScroll:    2300,        // ms que dura cada pisada
        max:           60,          // huellas simultáneas como máximo
        ignorar: 'textarea, [contenteditable=""], [contenteditable="true"], [data-huellas-off], ' +
            'input:not([type="checkbox"], [type="radio"], [type="button"], [type="submit"], [type="reset"], [type="file"])'
    };

    /* Misma huella que el cursor (viewBox 32×32) */
    const PAW =
        '<svg viewBox="0 0 32 32" aria-hidden="true" focusable="false">' +
        '<path d="M16 15.5c3.2 0 6.5 3.6 6.5 6.6 0 2.2-1.7 3.4-3.6 3.4-1.2 0-1.9-.6-2.9-.6s-1.7.6-2.9.6c-1.9 0-3.6-1.2-3.6-3.4 0-3 3.3-6.6 6.5-6.6z"/>' +
        '<ellipse cx="9.5"  cy="14"  rx="2.1" ry="2.7" transform="rotate(-20 9.5 14)"/>' +
        '<ellipse cx="13.3" cy="9.3" rx="2.2" ry="2.9" transform="rotate(-8 13.3 9.3)"/>' +
        '<ellipse cx="18.7" cy="9.3" rx="2.2" ry="2.9" transform="rotate(8 18.7 9.3)"/>' +
        '<ellipse cx="22.5" cy="14"  rx="2.1" ry="2.7" transform="rotate(20 22.5 14)"/>' +
        '</svg>';

    const CSS = `
.yz-huellas-capa{position:absolute;left:0;top:0;width:100%;overflow:hidden;overflow:clip;
  pointer-events:none;z-index:2147483000;contain:layout style}
.yz-huella{position:absolute;left:0;top:0;pointer-events:none;will-change:transform,opacity}
.yz-huella svg{display:block;width:100%;height:100%;fill:currentColor;overflow:visible}
.yz-huella--clic svg{filter:drop-shadow(0 0 1.2px #fff) drop-shadow(0 0 1.2px #fff) drop-shadow(0 4px 5px rgba(13,47,110,.25))}
.yz-huella--paso svg{filter:drop-shadow(0 0 1px rgba(255,255,255,.9))}
.yz-onda{position:absolute;left:0;top:0;border-radius:50%;border:2px solid currentColor;pointer-events:none}
@media print{.yz-huellas-capa{display:none}}`;

    let cfg = { ...DEFAULTS };
    let activo = false;
    const capas = new Map();        // host → capa
    const estados = new WeakMap();  // host → estado del scroll
    const vivas = [];               // huellas en pantalla (para respetar cfg.max)

    const rand = (a, b) => a + Math.random() * (b - a);
    const reducirMovimiento = matchMedia('(prefers-reduced-motion: reduce)');

    /* ---------- Capa donde viven las huellas ----------
       Es absoluta y del alto del contenido, así las huellas quedan
       "pegadas" a la página y se desplazan con ella al hacer scroll.
       overflow:clip evita que una huella en el borde agrande la página. */
    function capa(host) {
        let c = capas.get(host);
        if (!c || !c.isConnected) {
            c = document.createElement('div');
            c.className = 'yz-huellas-capa';
            c.setAttribute('aria-hidden', 'true');
            if (host !== root && getComputedStyle(host).position === 'static') host.style.position = 'relative';
            host.appendChild(c);
            capas.set(host, c);
        }
        c.style.height = '0px';
        const alto = host === root
            ? Math.max(root.scrollHeight, document.body ? document.body.scrollHeight : 0)
            : host.scrollHeight;
        c.style.height = alto + 'px';
        return c;
    }

    function crearHuella(host, { x, y, tamano, giro, color, clase }) {
        const el = document.createElement('span');
        el.className = 'yz-huella ' + clase;
        el.innerHTML = PAW;
        el.style.width = el.style.height = tamano + 'px';
        el.style.color = color;
        el.style.translate = `${x - tamano / 2}px ${y - tamano / 2}px`;
        el.style.rotate = giro + 'deg';
        capa(host).appendChild(el);

        vivas.push(el);
        while (vivas.length > cfg.max) vivas.shift().remove();
        return el;
    }

    function retirar(el, anim) {
        anim.finished.then(() => {
            el.remove();
            const i = vivas.indexOf(el);
            if (i > -1) vivas.splice(i, 1);
        }).catch(() => {});
    }

    /* ---------- Huella del clic ---------- */
    function estampar(clientX, clientY, opciones = {}) {
        const o = { ...cfg, ...opciones };
        const x = clientX + window.scrollX;
        const y = clientY + window.scrollY;
        const giro = opciones.giro ?? rand(-28, 18);   // ligeramente hacia la izquierda, como el cursor

        const el = crearHuella(root, { x, y, tamano: o.tamano, giro, color: o.color, clase: 'yz-huella--clic' });
        retirar(el, el.animate([
            { opacity: 0,   scale: .35 },
            { opacity: .95, scale: 1.18, offset: .1 },
            { opacity: .9,  scale: .96,  offset: .18 },
            { opacity: .9,  scale: 1,    offset: .24 },
            { opacity: .8,  scale: 1,    offset: .7 },
            { opacity: 0,   scale: 1.06 }
        ], { duration: o.vida, easing: 'cubic-bezier(.2,.7,.3,1)' }));

        if (opciones.onda === false) return el;

        // Onda suave alrededor, como un "pisotón"
        const d = o.tamano * 1.7;
        const onda = document.createElement('span');
        onda.className = 'yz-onda';
        onda.style.width = onda.style.height = d + 'px';
        onda.style.color = o.color;
        onda.style.translate = `${x - d / 2}px ${y - d / 2}px`;
        capa(root).appendChild(onda);
        onda.animate(
            [{ opacity: .45, scale: .3 }, { opacity: 0, scale: 1 }],
            { duration: 520, easing: 'cubic-bezier(.1,.6,.3,1)' }
        ).finished.then(() => onda.remove()).catch(() => {});
        return el;
    }

    function alHacerClic(e) {
        if (e.button !== 0 || !e.isPrimary) return;
        // Clic sobre la barra de scroll del documento: no estampar
        if (e.clientX >= root.clientWidth || e.clientY >= root.clientHeight) return;
        if (e.target instanceof Element && e.target.closest(cfg.ignorar)) return;
        estampar(e.clientX, e.clientY);
    }

    /* ---------- Rastro del scroll ----------
       Un "caminante" invisible junto a la barra deja pisadas
       alternadas izquierda/derecha, mirando hacia donde se avanza. */
    function pisada(host, dir, lado) {
        const esRaiz = host === root;
        const ancho = esRaiz ? root.clientWidth : host.clientWidth;
        const alto  = esRaiz ? window.innerHeight : host.clientHeight;
        const arriba = esRaiz ? window.scrollY : host.scrollTop;
        const margen = esRaiz ? 30 : 20;
        const tamano = esRaiz ? cfg.tamanoScroll : Math.round(cfg.tamanoScroll * .8);

        const x = ancho - margen + lado * (tamano * .38);
        const y = arriba + alto * (dir > 0 ? .62 : .38);
        const giro = (dir > 0 ? 180 : 0) + lado * dir * 9 + rand(-4, 4);

        const el = crearHuella(host, { x, y, tamano, giro, color: cfg.colorScroll, clase: 'yz-huella--paso' });
        retirar(el, el.animate([
            { opacity: 0,   scale: .55 },
            { opacity: .6,  scale: 1.06, offset: .08 },
            { opacity: .55, scale: 1,    offset: .14 },
            { opacity: .45, scale: 1,    offset: .6 },
            { opacity: 0,   scale: .94 }
        ], { duration: cfg.vidaScroll, easing: 'ease-out' }));
    }

    function posicion(host) { return host === root ? window.scrollY : host.scrollTop; }

    function alHacerScroll(e) {
        const host = e.target === document ? root : e.target;
        if (host !== root && !(host instanceof Element && host.hasAttribute('data-huellas-scroll'))) return;

        const y = posicion(host);
        let st = estados.get(host);
        if (!st) { estados.set(host, { ultimo: y, acum: 0, lado: 1, t: 0 }); return; }

        const dy = y - st.ultimo;
        st.ultimo = y;
        if (!dy) return;

        if (Math.sign(dy) !== Math.sign(st.acum)) st.acum = 0;   // cambió de dirección
        st.acum += dy;

        const ahora = performance.now();
        if (Math.abs(st.acum) < cfg.paso || ahora - st.t < 65) return;

        st.acum = 0;
        st.t = ahora;
        st.lado *= -1;
        pisada(host, dy > 0 ? 1 : -1, st.lado);
    }

    /* ---------- Caminito para celebrar (ej. "¡Mascota registrada!") ---------- */
    function celebrar(elemento, { pasos = 7, separacion = 34 } = {}) {
        if (!activo || !elemento) return;
        const r = elemento.getBoundingClientRect();
        const y0 = r.top + r.height / 2;
        const x0 = r.left + r.width / 2 - ((pasos - 1) * separacion) / 2;
        for (let i = 0; i < pasos; i++) {
            setTimeout(() => estampar(
                x0 + i * separacion,
                y0 + (i % 2 ? -9 : 9),
                { tamano: cfg.tamano * .8, giro: 90 + (i % 2 ? -8 : 8), onda: false, vida: 1700 }
            ), i * 120);
        }
    }

    /* ---------- Ciclo de vida ---------- */
    function inyectarEstilos() {
        if (document.getElementById('yz-huellitas-css')) return;
        const s = document.createElement('style');
        s.id = 'yz-huellitas-css';
        s.textContent = CSS;
        document.head.appendChild(s);
    }

    function iniciar(opciones = {}) {
        detener();
        cfg = { ...DEFAULTS, ...opciones };
        if (root.dataset.huellitas === 'off' || reducirMovimiento.matches) return;

        inyectarEstilos();
        if (cfg.clic) document.addEventListener('pointerdown', alHacerClic, { passive: true });
        if (cfg.scroll) {
            estados.set(root, { ultimo: window.scrollY, acum: 0, lado: 1, t: 0 });
            document.addEventListener('scroll', alHacerScroll, { capture: true, passive: true });
        }
        activo = true;
    }

    function detener() {
        document.removeEventListener('pointerdown', alHacerClic);
        document.removeEventListener('scroll', alHacerScroll, { capture: true });
        vivas.splice(0).forEach(el => el.remove());
        capas.forEach(c => c.remove());
        capas.clear();
        activo = false;
    }

    // Si el usuario activa "reducir movimiento" con la página abierta
    reducirMovimiento.addEventListener?.('change', () => iniciar(cfg));

    window.Huellitas = {
        iniciar,
        detener,
        celebrar,
        estampar: (x, y, o) => activo ? estampar(x, y, o) : null
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', () => iniciar(), { once: true });
    } else {
        iniciar();
    }
})();