// Yazka's · Vista "Nosotros"
document.addEventListener('DOMContentLoaded', () => {
    const anio = document.getElementById('anio');
    if (anio) anio.textContent = new Date().getFullYear();
});
