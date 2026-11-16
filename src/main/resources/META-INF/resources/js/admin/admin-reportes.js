// Reportes: búsqueda por mascota o propietario y filtro por tipo, sobre las filas ya cargadas
(() => {
    'use strict';

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => Array.from(root.querySelectorAll(sel));

    const buscador = $('#buscador');
    const btnLimpiar = $('#buscador-limpiar');
    const chipsBox = $('#chips');
    const filas = $$('#cuerpoTabla tr');
    const resumen = $('#resumen');
    const mostrando = $('#stat-mostrando');
    const sinResultados = $('#sin-resultados');

    let tipoActivo = '';

    // Sin tildes ni mayúsculas, igual que el campo data-busqueda que arma el servidor
    const normalizar = (texto) => (texto || '').normalize('NFD').replace(/\p{M}/gu, '').toLowerCase().trim();

    // PERRO -> Perro, GATO_DOMESTICO -> Gato domestico
    const bonito = (enumName) => {
        const t = (enumName || '').replace(/_/g, ' ').toLowerCase();
        return t.charAt(0).toUpperCase() + t.slice(1);
    };

    // Color estable del avatar según el nombre
    const hue = (texto) => {
        let h = 0;
        for (const c of texto || '') h = (h * 31 + c.charCodeAt(0)) % 360;
        return h;
    };

    function prepararFilas() {
        filas.forEach((fila) => {
            $$('.fmt-enum', fila).forEach((el) => { el.textContent = bonito(el.textContent.trim()); });
            const avatar = $('.avatar', fila);
            if (avatar) avatar.style.setProperty('--h', hue(avatar.dataset.seed));
        });
    }

    // Un chip por cada tipo de mascota que exista en la lista
    function crearChips() {
        const tipos = [...new Set(filas.map((f) => f.dataset.tipo).filter((t) => t && t !== '-'))].sort();
        tipos.forEach((tipo) => {
            const chip = document.createElement('button');
            chip.type = 'button';
            chip.className = 'chip';
            chip.dataset.tipo = tipo;
            chip.textContent = bonito(tipo);
            chipsBox.appendChild(chip);
        });
    }

    function filtrar() {
        const consulta = normalizar(buscador.value);
        let visibles = 0;

        filas.forEach((fila) => {
            const coincideTexto = !consulta || fila.dataset.busqueda.includes(consulta);
            const coincideTipo = !tipoActivo || fila.dataset.tipo === tipoActivo;
            const mostrar = coincideTexto && coincideTipo;
            fila.hidden = !mostrar;
            if (mostrar) visibles++;
        });

        resumen.textContent = visibles === 1 ? '1 mascota' : `${visibles} mascotas`;
        mostrando.textContent = visibles;
        sinResultados.hidden = visibles !== 0;
        btnLimpiar.hidden = !buscador.value;
    }

    function seleccionarTipo(tipo) {
        tipoActivo = tipo;
        $$('.chip', chipsBox).forEach((c) => c.classList.toggle('is-active', c.dataset.tipo === tipo));
        filtrar();
    }

    buscador.addEventListener('input', filtrar);

    btnLimpiar.addEventListener('click', () => {
        buscador.value = '';
        filtrar();
        buscador.focus();
    });

    buscador.addEventListener('keydown', (e) => {
        if (e.key === 'Escape') {
            buscador.value = '';
            filtrar();
        }
    });

    // Atajo: "/" lleva el cursor al buscador
    document.addEventListener('keydown', (e) => {
        const escribiendo = /^(INPUT|TEXTAREA|SELECT)$/.test(document.activeElement.tagName);
        if (e.key === '/' && !escribiendo) {
            e.preventDefault();
            buscador.focus();
        }
    });

    chipsBox.addEventListener('click', (e) => {
        const chip = e.target.closest('.chip');
        if (chip) seleccionarTipo(chip.dataset.tipo);
    });

    prepararFilas();
    crearChips();
    filtrar();
})();