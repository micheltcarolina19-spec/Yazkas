(() => {
    'use strict';

    const sidebar = document.getElementById('adm-sidebar');
    if (!sidebar) return;

    // El layout correcto es el ancestro más cercano del sidebar, no el primero de la página
    const layout = sidebar.closest('.adm-layout') || sidebar.parentElement;
    if (!layout) return;

    const burger = document.querySelector('[data-adm-open]');
    const overlay = document.querySelector('[data-adm-overlay]');
    const closeBtn = sidebar.querySelector('[data-adm-close]');
    const collapseBtn = sidebar.querySelector('[data-adm-collapse]');

    const KEY = 'adm-sidebar-collapsed';
    const mqMobile = window.matchMedia('(max-width: 768px)');
    const mqTablet = window.matchMedia('(min-width: 769px) and (max-width: 1024px)');

    /* ---- Preferencia guardada (desktop) ---- */
    const store = {
        get() { try { return localStorage.getItem(KEY) === '1'; } catch { return false; } },
        set(v) { try { localStorage.setItem(KEY, v ? '1' : '0'); } catch { /* sin storage */ } }
    };

    /* ---- Logo: si la imagen no existe, queda la huellita ---- */
    const logo = sidebar.querySelector('.adm-brand__logo img');
    if (logo) {
        const fail = () => logo.remove();
        if (logo.complete && logo.naturalWidth === 0) fail();
        else logo.addEventListener('error', fail, { once: true });
    }

    /* ---- Modo contraído ---- */
    const setCollapsed = (value) => {
        layout.classList.toggle('is-collapsed', value);
        if (collapseBtn) {
            collapseBtn.setAttribute('aria-pressed', String(value));
            const text = value ? 'Expandir menú' : 'Contraer menú';
            collapseBtn.setAttribute('aria-label', text);
            collapseBtn.setAttribute('title', text);
        }
    };

    const sync = () => {
        if (mqMobile.matches) {
            setCollapsed(false);
        } else {
            closeDrawer(false);
            setCollapsed(mqTablet.matches ? true : store.get());
        }
    };

    if (collapseBtn) {
        collapseBtn.addEventListener('click', () => {
            const next = !layout.classList.contains('is-collapsed');
            setCollapsed(next);
            store.set(next);
        });
    }

    /* ---- Cajón móvil ---- */
    function openDrawer() {
        sidebar.classList.add('is-open');
        overlay && overlay.classList.add('is-open');
        document.body.classList.add('adm-lock');
        burger && burger.setAttribute('aria-expanded', 'true');
        closeBtn && closeBtn.focus();
    }

    function closeDrawer(restoreFocus = true) {
        if (!sidebar.classList.contains('is-open')) return;
        sidebar.classList.remove('is-open');
        overlay && overlay.classList.remove('is-open');
        document.body.classList.remove('adm-lock');
        burger && burger.setAttribute('aria-expanded', 'false');
        if (restoreFocus && burger) burger.focus();
    }

    burger && burger.addEventListener('click', openDrawer);
    closeBtn && closeBtn.addEventListener('click', () => closeDrawer());
    overlay && overlay.addEventListener('click', () => closeDrawer());
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') closeDrawer(); });
    sidebar.querySelectorAll('.adm-link[href]').forEach((a) =>
        a.addEventListener('click', () => { if (mqMobile.matches) closeDrawer(false); })
    );

    /* ---- Reaccionar a cambios de tamaño ---- */
    mqMobile.addEventListener('change', sync);
    mqTablet.addEventListener('change', sync);
    sync();
})();