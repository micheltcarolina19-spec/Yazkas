/* =========================================================
   YAZKAS · Navbar (público y cliente)
   Ruta: src/main/resources/META-INF/resources/js/usuario/Navbar.js
   1. Sombra del navbar al hacer scroll
   2. Botón hamburguesa (móvil)
   3. Menú de la cuenta del cliente (nav-cliente.html):
      se abre con clic o pasando el mouse y tiene "Cerrar sesión".
   ========================================================= */
(() => {
    'use strict';

    const init = () => {
        const header = document.querySelector('.pc-header');
        if (!header) return;

        const escritorio = window.matchMedia('(min-width: 901px)');
        const conMouse = window.matchMedia('(hover: hover) and (min-width: 901px)');

        /* ---------- 1. Sombra al hacer scroll ---------- */
        const alHacerScroll = () => header.classList.toggle('is-scrolled', window.scrollY > 10);
        alHacerScroll();
        window.addEventListener('scroll', alHacerScroll, { passive: true });

        /* ---------- 2. Menú hamburguesa (móvil) ---------- */
        const toggle = header.querySelector('.pc-toggle');
        const menu = header.querySelector('.pc-menu');

        const abrirMenu = (open) => {
            if (!toggle || !menu) return;
            menu.classList.toggle('is-open', open);
            toggle.setAttribute('aria-expanded', String(open));
            toggle.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
        };

        if (toggle && menu) {
            toggle.addEventListener('click', (e) => {
                e.stopPropagation();
                abrirMenu(!menu.classList.contains('is-open'));
            });

            // Al tocar un enlace del menú, se cierra
            menu.querySelectorAll('a').forEach((a) => a.addEventListener('click', () => abrirMenu(false)));

            // Si se pasa a escritorio, se limpia el estado del menú móvil
            escritorio.addEventListener('change', (e) => { if (e.matches) abrirMenu(false); });
        }

        /* ---------- 3. Menú de la cuenta (solo en nav-cliente) ---------- */
        const cuenta = header.querySelector('[data-pc-account]');
        const btnCuenta = cuenta?.querySelector('.pc-account__btn');
        let cerrarTimer = null;
        let abiertoPorHoverEn = 0;

        const cuentaAbierta = () => cuenta?.classList.contains('is-open');

        const abrirCuenta = (open) => {
            if (!cuenta || !btnCuenta) return;
            clearTimeout(cerrarTimer);
            cuenta.classList.toggle('is-open', open);
            btnCuenta.setAttribute('aria-expanded', String(open));
        };

        if (cuenta && btnCuenta) {
            btnCuenta.addEventListener('click', (e) => {
                e.preventDefault();
                e.stopPropagation();
                // Si se acaba de abrir con el mouse, el clic no lo cierra
                if (conMouse.matches && cuentaAbierta() && Date.now() - abiertoPorHoverEn < 600) {
                    abrirCuenta(true);
                    return;
                }
                abrirCuenta(!cuentaAbierta());
            });

            cuenta.addEventListener('mouseenter', () => {
                if (!conMouse.matches) return;
                if (!cuentaAbierta()) abiertoPorHoverEn = Date.now();
                abrirCuenta(true);
            });

            cuenta.addEventListener('mouseleave', () => {
                if (conMouse.matches) cerrarTimer = setTimeout(() => abrirCuenta(false), 180);
            });
        }

        /* ---------- Cerrar con clic afuera o con Escape ---------- */
        document.addEventListener('click', (e) => {
            if (!header.contains(e.target)) abrirMenu(false);
            if (cuenta && !cuenta.contains(e.target)) abrirCuenta(false);
        });

        document.addEventListener('keydown', (e) => {
            if (e.key !== 'Escape') return;
            if (cuentaAbierta()) { abrirCuenta(false); btnCuenta.focus(); return; }
            if (menu?.classList.contains('is-open')) { abrirMenu(false); toggle.focus(); }
        });
    };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', init);
    } else {
        init();
    }
})();