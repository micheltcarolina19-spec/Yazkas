// Menú de la cuenta del cliente (nav-cliente.html): se abre con clic o pasando el mouse
// y tiene "Cerrar sesión" (/logout borra la cookie y lleva al home sin sesión).
(() => {
    const cuenta = document.querySelector('[data-pc-account]');
    if (!cuenta) return;
    const btn = cuenta.querySelector('.pc-account__btn');
    const conMouse = window.matchMedia('(hover: hover) and (min-width: 901px)');
    let cerrarTimer = null;

    const abrir = (open) => {
        clearTimeout(cerrarTimer);
        cuenta.classList.toggle('is-open', open);
        btn.setAttribute('aria-expanded', String(open));
    };

    btn.addEventListener('click', (e) => {
        e.stopPropagation();
        abrir(!cuenta.classList.contains('is-open'));
    });

    cuenta.addEventListener('mouseenter', () => { if (conMouse.matches) abrir(true); });
    cuenta.addEventListener('mouseleave', () => {
        if (conMouse.matches) cerrarTimer = setTimeout(() => abrir(false), 180);
    });

    document.addEventListener('click', (e) => { if (!cuenta.contains(e.target)) abrir(false); });
    document.addEventListener('keydown', (e) => {
        if (e.key === 'Escape' && cuenta.classList.contains('is-open')) { abrir(false); btn.focus(); }
    });
})();