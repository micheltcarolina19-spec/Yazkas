// PetCare · comportamiento del navbar (menú móvil + sombra al hacer scroll)
(() => {
    const header = document.querySelector('.pc-header');
    const toggle = document.querySelector('.pc-toggle');
    const menu = document.getElementById('pc-menu');
    if (!header || !toggle || !menu) return;

    const setOpen = (open) => {
        toggle.setAttribute('aria-expanded', String(open));
        toggle.setAttribute('aria-label', open ? 'Cerrar menú' : 'Abrir menú');
        menu.classList.toggle('is-open', open);
    };

    // Abrir / cerrar con el botón
    toggle.addEventListener('click', () =>
        setOpen(toggle.getAttribute('aria-expanded') !== 'true'));

    // Cerrar al elegir un enlace o al pulsar Escape
    menu.addEventListener('click', (e) => { if (e.target.closest('a')) setOpen(false); });
    document.addEventListener('keydown', (e) => { if (e.key === 'Escape') setOpen(false); });

    // Al pasar a pantalla ancha, dejar el menú cerrado y limpio
    window.matchMedia('(min-width: 901px)')
        .addEventListener('change', (e) => { if (e.matches) setOpen(false); });

    // Refuerza la sombra cuando la página baja
    const onScroll = () => header.classList.toggle('is-scrolled', window.scrollY > 8);
    onScroll();
    window.addEventListener('scroll', onScroll, { passive: true });
})();