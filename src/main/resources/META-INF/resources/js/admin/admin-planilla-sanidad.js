// Planilla de Sanidad — YAZKAS Admin
// Consume /api/planilla-sanidad (CRUD, opciones, cumplimiento y PDF)
(() => {
    'use strict';

    const API = '/api/planilla-sanidad';
    const DIAS = [null, 'LUNES', 'MARTES', 'MIERCOLES', 'JUEVES', 'VIERNES', null]; // índice = Date.getDay()

    const $ = (id) => document.getElementById(id);
    const el = {
        form: $('formPlanilla'),
        fecha: $('fecha'), dia: $('dia'), area: $('area'), herramienta: $('herramienta'),
        limpieza: $('limpieza'), desinfeccion: $('desinfeccion'), observaciones: $('observaciones'),
        textoLimpieza: $('textoLimpieza'), textoDesinfeccion: $('textoDesinfeccion'),
        errorFecha: $('errorFecha'), errorArea: $('errorArea'), errorHerramienta: $('errorHerramienta'),
        btnGuardar: $('btnGuardar'), textoGuardar: $('textoGuardar'), btnLimpiar: $('btnLimpiar'),
        barraEdicion: $('barraEdicion'), textoEdicion: $('textoEdicion'), btnCancelarEdicion: $('btnCancelarEdicion'),
        filtroDesde: $('filtroDesde'), filtroHasta: $('filtroHasta'), cumplimiento: $('cumplimiento'),
        cuerpoTabla: $('cuerpoTabla'), vacio: $('vacio'), resumen: $('resumenRegistros'),
        dlgPdf: $('dlgPdf'), formPdf: $('formPdf'), pdfDesde: $('pdfDesde'), pdfHasta: $('pdfHasta'),
        errorPdf: $('errorPdf'), btnAbrirPdf: $('btnAbrirPdf'), btnCerrarPdf: $('btnCerrarPdf'),
        btnDescargarPdf: $('btnDescargarPdf'),
        dlgEliminar: $('dlgEliminar'), textoEliminar: $('textoEliminar'),
        btnCancelarEliminar: $('btnCancelarEliminar'), btnConfirmarEliminar: $('btnConfirmarEliminar'),
        toasts: $('toasts'),
    };

    let registros = [];
    let editandoId = null;
    let idAEliminar = null;

    /* ---------- Utilidades ---------- */

    const pad = (n) => String(n).padStart(2, '0');
    const aIso = (d) => `${d.getFullYear()}-${pad(d.getMonth() + 1)}-${pad(d.getDate())}`;
    const hoyIso = () => aIso(new Date());
    const primerDiaMesIso = () => { const d = new Date(); return aIso(new Date(d.getFullYear(), d.getMonth(), 1)); };
    const desdeIso = (iso) => { const [y, m, d] = iso.split('-').map(Number); return new Date(y, m - 1, d); };
    const formatoFecha = (iso) => { const [y, m, d] = iso.split('-'); return `${d}/${m}/${y}`; };

    const escapar = (texto) => String(texto ?? '').replace(/[&<>"']/g, (c) => (
        { '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]
    ));

    function aviso(mensaje, tipo = 'info') {
        const toast = document.createElement('div');
        toast.className = `ps-toast ps-toast--${tipo}`;
        toast.textContent = mensaje;
        el.toasts.appendChild(toast);
        setTimeout(() => toast.remove(), tipo === 'error' ? 6000 : 3500);
    }

    async function leerError(resp) {
        try {
            const cuerpo = await resp.json();
            if (Array.isArray(cuerpo.violations) && cuerpo.violations.length) {
                return cuerpo.violations.map((v) => v.message).join('. ');
            }
            return cuerpo.mensaje || cuerpo.message || cuerpo.error || cuerpo.detail || cuerpo.title
                || `Error ${resp.status}`;
        } catch {
            return `Error ${resp.status}`;
        }
    }

    async function api(url, opciones = {}) {
        const resp = await fetch(url, {
            headers: { 'Content-Type': 'application/json', 'Accept': 'application/json' },
            ...opciones,
        });
        if (!resp.ok) {
            throw new Error(await leerError(resp));
        }
        return resp.status === 204 ? null : resp.json();
    }

    /* ---------- Formulario ---------- */

    function llenarSelect(select, opciones) {
        opciones.forEach((o) => {
            const option = document.createElement('option');
            option.value = o.valor;
            option.textContent = o.etiqueta;
            select.appendChild(option);
        });
    }

    async function cargarOpciones() {
        const opciones = await api(`${API}/opciones`);
        llenarSelect(el.area, opciones.areas);
        llenarSelect(el.herramienta, opciones.herramientas);
    }

    // El día lo calcula el backend; aquí solo se muestra y se avisa si es fin de semana
    function actualizarDia() {
        el.errorFecha.textContent = '';
        el.fecha.closest('.ps-input').classList.remove('is-invalid');
        if (!el.fecha.value) {
            el.dia.value = '';
            return true;
        }
        const dia = DIAS[desdeIso(el.fecha.value).getDay()];
        el.dia.value = dia || '';
        if (!dia) {
            el.errorFecha.textContent = 'La planilla de sanidad solo se diligencia de lunes a viernes.';
            el.fecha.closest('.ps-input').classList.add('is-invalid');
            return false;
        }
        if (el.fecha.value > hoyIso()) {
            el.errorFecha.textContent = 'No se puede registrar la planilla de una fecha futura.';
            el.fecha.closest('.ps-input').classList.add('is-invalid');
            return false;
        }
        return true;
    }

    // Sin limpieza no puede haber desinfección (regla del dominio)
    function sincronizarSwitches() {
        if (!el.limpieza.checked) {
            el.desinfeccion.checked = false;
        }
        el.desinfeccion.disabled = !el.limpieza.checked;
        el.textoLimpieza.textContent = el.limpieza.checked ? 'Sí' : 'No';
        el.textoDesinfeccion.textContent = el.desinfeccion.checked ? 'Sí' : 'No';
    }

    function validar() {
        let valido = actualizarDia();
        if (!el.fecha.value) {
            el.errorFecha.textContent = 'La fecha es obligatoria.';
            valido = false;
        }
        const campos = [[el.area, el.errorArea, 'Selecciona el área de limpieza.'],
            [el.herramienta, el.errorHerramienta, 'Selecciona la herramienta utilizada.']];
        campos.forEach(([campo, error, mensaje]) => {
            const vacio = !campo.value;
            error.textContent = vacio ? mensaje : '';
            campo.closest('.ps-input').classList.toggle('is-invalid', vacio);
            if (vacio) valido = false;
        });
        return valido;
    }

    function limpiarFormulario() {
        editandoId = null;
        el.form.reset();
        el.fecha.value = hoyIso();
        el.fecha.max = hoyIso();
        el.limpieza.checked = true;
        el.desinfeccion.checked = true;
        [el.errorArea, el.errorHerramienta].forEach((e) => { e.textContent = ''; });
        el.area.closest('.ps-input').classList.remove('is-invalid');
        el.herramienta.closest('.ps-input').classList.remove('is-invalid');
        el.barraEdicion.hidden = true;
        el.textoGuardar.textContent = 'Guardar registro';
        sincronizarSwitches();
        actualizarDia();
        marcarFilaEnEdicion();
    }

    async function guardar(evento) {
        evento.preventDefault();
        if (!validar()) return;

        const cuerpo = {
            fechaPlanilla: el.fecha.value,
            areaLimpieza: el.area.value,
            herramienta: el.herramienta.value,
            limpiezaRealizada: el.limpieza.checked,
            desinfeccion: el.desinfeccion.checked,
            observaciones: el.observaciones.value.trim() || null,
        };
        const edicion = editandoId !== null;

        el.btnGuardar.disabled = true;
        try {
            await api(edicion ? `${API}/${editandoId}` : API, {
                method: edicion ? 'PUT' : 'POST',
                body: JSON.stringify(cuerpo),
            });
            aviso(edicion ? 'Registro actualizado.' : 'Registro guardado.', 'ok');
            ampliarFiltroParaIncluir(cuerpo.fechaPlanilla);
            limpiarFormulario();
            await cargarRegistros();
        } catch (error) {
            aviso(error.message, 'error');
        } finally {
            el.btnGuardar.disabled = false;
        }
    }

    function editar(id) {
        const r = registros.find((x) => x.id === id);
        if (!r) return;
        editandoId = id;
        el.fecha.value = r.fechaPlanilla;
        el.area.value = r.areaLimpieza;
        el.herramienta.value = r.herramienta;
        el.limpieza.checked = !!r.limpiezaRealizada;
        el.desinfeccion.checked = !!r.desinfeccion;
        el.observaciones.value = r.observaciones || '';
        sincronizarSwitches();
        actualizarDia();
        el.textoEdicion.textContent = `Editando el registro del ${formatoFecha(r.fechaPlanilla)}: `
            + `${r.areaLimpiezaEtiqueta} / ${r.herramientaEtiqueta}`;
        el.barraEdicion.hidden = false;
        el.textoGuardar.textContent = 'Actualizar registro';
        marcarFilaEnEdicion();
        el.form.scrollIntoView({ behavior: 'smooth', block: 'start' });
        el.fecha.focus({ preventScroll: true });
    }

    /* ---------- Listado ---------- */

    function ampliarFiltroParaIncluir(iso) {
        if (iso < el.filtroDesde.value) el.filtroDesde.value = iso;
        if (iso > el.filtroHasta.value) el.filtroHasta.value = iso;
    }

    function pastilla(cumplido) {
        return cumplido
            ? '<span class="ps-pill ps-pill--ok"><svg class="ps-ic" aria-hidden="true"><use href="#i-check"/></svg>Sí</span>'
            : '<span class="ps-pill ps-pill--no"><svg class="ps-ic" aria-hidden="true"><use href="#i-x"/></svg>No</span>';
    }

    function pintarTabla() {
        el.cuerpoTabla.innerHTML = registros.map((r) => `
            <tr data-id="${r.id}">
                <td>${formatoFecha(r.fechaPlanilla)}</td>
                <td>${escapar(r.diaSanidadEtiqueta)}</td>
                <td>${escapar(r.areaLimpiezaEtiqueta)}</td>
                <td>${escapar(r.herramientaEtiqueta)}</td>
                <td>${pastilla(r.limpiezaRealizada)}</td>
                <td>${pastilla(r.desinfeccion)}</td>
                <td class="ps-left">${r.observaciones ? escapar(r.observaciones) : '—'}</td>
                <td>
                    <span class="ps-row-actions">
                        <button type="button" class="ps-icon-btn" data-accion="editar" data-id="${r.id}"
                                aria-label="Editar registro del ${formatoFecha(r.fechaPlanilla)}" title="Editar">
                            <svg class="ps-ic" aria-hidden="true"><use href="#i-edit"/></svg>
                        </button>
                        <button type="button" class="ps-icon-btn ps-icon-btn--danger" data-accion="eliminar" data-id="${r.id}"
                                aria-label="Eliminar registro del ${formatoFecha(r.fechaPlanilla)}" title="Eliminar">
                            <svg class="ps-ic" aria-hidden="true"><use href="#i-trash"/></svg>
                        </button>
                    </span>
                </td>
            </tr>`).join('');
        el.vacio.hidden = registros.length > 0;
        el.resumen.textContent = registros.length === 1 ? '1 registro en el rango' : `${registros.length} registros en el rango`;
        marcarFilaEnEdicion();
    }

    function marcarFilaEnEdicion() {
        el.cuerpoTabla.querySelectorAll('tr').forEach((fila) => {
            fila.classList.toggle('is-editing', editandoId !== null && Number(fila.dataset.id) === editandoId);
        });
    }

    async function cargarRegistros() {
        const inicio = el.filtroDesde.value;
        const fin = el.filtroHasta.value;
        if (!inicio || !fin) return;
        if (inicio > fin) {
            aviso('La fecha inicial no puede ser posterior a la final.', 'error');
            return;
        }
        const consulta = `inicio=${inicio}&fin=${fin}`;
        try {
            const [lista, porcentaje] = await Promise.all([
                api(`${API}?${consulta}`),
                api(`${API}/cumplimiento?${consulta}`),
            ]);
            registros = lista;
            el.cumplimiento.textContent = `${Number(porcentaje).toLocaleString('es-CO', { maximumFractionDigits: 1 })} %`;
            pintarTabla();
        } catch (error) {
            aviso(error.message, 'error');
        }
    }

    /* ---------- Eliminar ---------- */

    function pedirEliminar(id) {
        const r = registros.find((x) => x.id === id);
        if (!r) return;
        idAEliminar = id;
        el.textoEliminar.textContent = `Se eliminará el registro del ${formatoFecha(r.fechaPlanilla)} `
            + `(${r.areaLimpiezaEtiqueta} / ${r.herramientaEtiqueta}). Esta acción no se puede deshacer.`;
        el.dlgEliminar.showModal();
    }

    async function confirmarEliminar() {
        if (idAEliminar === null) return;
        el.btnConfirmarEliminar.disabled = true;
        try {
            await api(`${API}/${idAEliminar}`, { method: 'DELETE' });
            if (editandoId === idAEliminar) limpiarFormulario();
            aviso('Registro eliminado.', 'ok');
            el.dlgEliminar.close();
            await cargarRegistros();
        } catch (error) {
            aviso(error.message, 'error');
        } finally {
            idAEliminar = null;
            el.btnConfirmarEliminar.disabled = false;
        }
    }

    /* ---------- PDF ---------- */

    function abrirPdf() {
        el.pdfDesde.value = el.filtroDesde.value;
        el.pdfHasta.value = el.filtroHasta.value;
        el.errorPdf.textContent = '';
        el.dlgPdf.showModal();
    }

    async function descargarPdf(evento) {
        evento.preventDefault();
        const inicio = el.pdfDesde.value;
        const fin = el.pdfHasta.value;
        if (!inicio || !fin) {
            el.errorPdf.textContent = 'Elige la fecha inicial y la final.';
            return;
        }
        if (inicio > fin) {
            el.errorPdf.textContent = 'La fecha inicial no puede ser posterior a la final.';
            return;
        }
        el.errorPdf.textContent = '';
        el.btnDescargarPdf.disabled = true;
        try {
            const resp = await fetch(`${API}/pdf?inicio=${inicio}&fin=${fin}`, { headers: { 'Accept': 'application/pdf' } });
            if (!resp.ok) throw new Error(await leerError(resp));
            const url = URL.createObjectURL(await resp.blob());
            const enlace = document.createElement('a');
            enlace.href = url;
            enlace.download = `planilla-sanidad_${inicio}_${fin}.pdf`;
            document.body.appendChild(enlace);
            enlace.click();
            enlace.remove();
            URL.revokeObjectURL(url);
            el.dlgPdf.close();
            aviso('Planilla descargada.', 'ok');
        } catch (error) {
            el.errorPdf.textContent = error.message;
        } finally {
            el.btnDescargarPdf.disabled = false;
        }
    }

    /* ---------- Eventos e inicio ---------- */

    function enlazarEventos() {
        el.form.addEventListener('submit', guardar);
        el.fecha.addEventListener('change', actualizarDia);
        el.limpieza.addEventListener('change', sincronizarSwitches);
        el.desinfeccion.addEventListener('change', sincronizarSwitches);
        el.btnLimpiar.addEventListener('click', limpiarFormulario);
        el.btnCancelarEdicion.addEventListener('click', limpiarFormulario);

        el.filtroDesde.addEventListener('change', cargarRegistros);
        el.filtroHasta.addEventListener('change', cargarRegistros);
        el.cuerpoTabla.addEventListener('click', (e) => {
            const boton = e.target.closest('button[data-accion]');
            if (!boton) return;
            const id = Number(boton.dataset.id);
            if (boton.dataset.accion === 'editar') editar(id);
            else pedirEliminar(id);
        });

        el.btnAbrirPdf.addEventListener('click', abrirPdf);
        el.btnCerrarPdf.addEventListener('click', () => el.dlgPdf.close());
        el.formPdf.addEventListener('submit', descargarPdf);

        el.btnCancelarEliminar.addEventListener('click', () => el.dlgEliminar.close());
        el.btnConfirmarEliminar.addEventListener('click', confirmarEliminar);
    }

    async function iniciar() {
        enlazarEventos();
        el.filtroDesde.value = primerDiaMesIso();
        el.filtroHasta.value = hoyIso();
        limpiarFormulario();
        try {
            await cargarOpciones();
        } catch (error) {
            aviso(`No se pudieron cargar las áreas y herramientas: ${error.message}`, 'error');
        }
        await cargarRegistros();
    }

    document.addEventListener('DOMContentLoaded', iniciar);
})();