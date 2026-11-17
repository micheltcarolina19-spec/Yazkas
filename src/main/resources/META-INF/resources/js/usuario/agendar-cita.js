// "Agenda tu cita" del cliente.
//   GET  /citas/disponibilidad?fecha=YYYY-MM-DD&mascotas=1&mascotas=2
//        -> duración estimada por la IA (sin precio) y horarios con cupo
//   POST /citas  { mascotaIds:[..], fecha:"YYYY-MM-DD", hora:"HH:mm" }
// Reglas del backend: máximo 2 mascotas al mismo tiempo (de uno o varios dueños), atención 8:30 a 17:30.
(() => {
    const main = document.getElementById('contenido');
    if (!main) return;
    const $ = (s, r = document) => r.querySelector(s);
    const $$ = (s, r = document) => [...r.querySelectorAll(s)];

    const MAXIMO = Number(main.dataset.maximo || 2);
    const HOY = main.dataset.hoy;                     // fecha de Colombia que manda el servidor
    const MESES = ['Enero', 'Febrero', 'Marzo', 'Abril', 'Mayo', 'Junio', 'Julio', 'Agosto', 'Septiembre', 'Octubre', 'Noviembre', 'Diciembre'];
    const DIAS = ['Domingo', 'Lunes', 'Martes', 'Miércoles', 'Jueves', 'Viernes', 'Sábado'];

    const S = {
        mascotas: [],          // ids seleccionados (en orden)
        fecha: null,           // 'YYYY-MM-DD'
        hora: null,            // 'HH:mm'
        horaTexto: null,
        mes: null,             // Date del primer día del mes visible
        consulta: 0,           // para ignorar respuestas viejas
        duraciones: {}         // mascotaId -> texto de duración
    };

    const iso = d => `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')}`;
    const desdeIso = t => { const [y, m, d] = t.split('-').map(Number); return new Date(y, m - 1, d); };
    const escapar = t => String(t).replace(/[&<>"']/g, c => ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
    const hoy = desdeIso(HOY);

    const alerta = (msg) => {
        const a = $('#ac-alert');
        a.hidden = !msg;
        if (msg) { $('span', a).textContent = msg; a.scrollIntoView({ behavior: 'smooth', block: 'center' }); }
    };

    async function json(url, opciones = {}) {
        const r = await fetch(url, { credentials: 'same-origin', headers: { Accept: 'application/json', ...(opciones.headers || {}) }, ...opciones });
        const body = await r.json().catch(() => ({}));
        if (r.status === 401) { location.href = '/login'; throw new Error('Tu sesión expiró'); }
        if (!r.ok) throw new Error(body.mensaje || body.message || body.error || `Error ${r.status}`);
        return body;
    }

    /* ======================= Calendario ======================= */
    function pintarCalendario() {
        const mes = S.mes;
        $('#ac-mes').textContent = `${MESES[mes.getMonth()]} ${mes.getFullYear()}`;
        // No se puede ir a meses anteriores al actual
        $('#ac-mes-ant').disabled = mes.getFullYear() === hoy.getFullYear() && mes.getMonth() === hoy.getMonth();

        const primerDia = (mes.getDay() + 6) % 7;                 // lunes = 0
        const diasMes = new Date(mes.getFullYear(), mes.getMonth() + 1, 0).getDate();
        let html = '';
        for (let i = 0; i < primerDia; i++) html += '<span class="ac-dia ac-dia--vacio"></span>';
        for (let d = 1; d <= diasMes; d++) {
            const fecha = new Date(mes.getFullYear(), mes.getMonth(), d);
            const f = iso(fecha);
            const pasado = fecha < hoy;
            const clases = ['ac-dia'];
            if (f === HOY) clases.push('is-hoy');
            if (f === S.fecha) clases.push('is-sel');
            html += `<button type="button" class="${clases.join(' ')}" data-fecha="${f}" ${pasado ? 'disabled' : ''}
                        aria-label="${DIAS[fecha.getDay()]} ${d} de ${MESES[fecha.getMonth()]}" ${f === S.fecha ? 'aria-pressed="true"' : ''}>${d}</button>`;
        }
        $('#ac-dias').innerHTML = html;
    }

    $('#ac-dias').addEventListener('click', e => {
        const b = e.target.closest('.ac-dia[data-fecha]');
        if (!b || b.disabled) return;
        elegirFecha(b.dataset.fecha);
    });
    $('#ac-mes-ant').addEventListener('click', () => { S.mes = new Date(S.mes.getFullYear(), S.mes.getMonth() - 1, 1); pintarCalendario(); });
    $('#ac-mes-sig').addEventListener('click', () => { S.mes = new Date(S.mes.getFullYear(), S.mes.getMonth() + 1, 1); pintarCalendario(); });
    $('#ac-hoy').addEventListener('click', () => { S.mes = new Date(hoy.getFullYear(), hoy.getMonth(), 1); elegirFecha(HOY); });

    function textoFecha(f) {
        const d = desdeIso(f);
        return `${DIAS[d.getDay()]}, ${d.getDate()} de ${MESES[d.getMonth()]}`;
    }

    function elegirFecha(f) {
        S.fecha = f;
        S.hora = null; S.horaTexto = null;
        const d = desdeIso(f);
        S.mes = new Date(d.getFullYear(), d.getMonth(), 1);
        pintarCalendario();
        $('#ac-fecha-sel span').innerHTML = `Seleccionado: <b>${textoFecha(f)}</b>`;
        $('#ac-hor-dia').textContent = textoFecha(f);
        actualizar();
        cargarHorarios();
    }

    /* ======================= Mascotas ======================= */
    $$('.ac-mascota[data-id]').forEach(btn => btn.addEventListener('click', () => {
        const id = Number(btn.dataset.id);
        const i = S.mascotas.indexOf(id);
        if (i >= 0) {
            S.mascotas.splice(i, 1);
        } else {
            if (S.mascotas.length >= MAXIMO) {
                alerta(`Puedes agendar máximo ${MAXIMO} mascotas en el mismo horario. Quita una para elegir otra.`);
                return;
            }
            S.mascotas.push(id);
        }
        alerta('');
        $$('.ac-mascota[data-id]').forEach(b => {
            const on = S.mascotas.includes(Number(b.dataset.id));
            b.classList.toggle('is-sel', on);
            b.setAttribute('aria-pressed', String(on));
            if (!on) $('[data-dur]', b).textContent = '';
        });
        S.hora = null; S.horaTexto = null;
        actualizar();
        cargarHorarios();
    }));

    /* ======================= Horarios + IA ======================= */
    async function cargarHorarios() {
        const caja = $('#ac-horarios');
        const ia = $('#ac-ia');
        if (!S.mascotas.length || !S.fecha) {
            caja.innerHTML = `<p class="ac-horarios__vacio">${!S.mascotas.length ? 'Elige una mascota' : 'Elige un día en el calendario'} para ver los horarios.</p>`;
            ia.hidden = true;
            return;
        }
        const n = ++S.consulta;
        caja.innerHTML = Array.from({ length: 9 }, () => '<span class="ac-hora ac-hora--cargando"></span>').join('');
        ia.hidden = false;
        $('span', ia).textContent = 'Calculando la duración del servicio según la raza y el pelaje…';
        ia.classList.add('is-cargando');

        try {
            const q = new URLSearchParams({ fecha: S.fecha });
            S.mascotas.forEach(id => q.append('mascotas', id));
            const data = await json(`/citas/disponibilidad?${q}`);
            if (n !== S.consulta) return;    // llegó una respuesta más nueva

            // Duración estimada (sin precio)
            S.duraciones = {};
            (data.duraciones || []).forEach(d => {
                S.duraciones[d.mascotaId] = d.texto;
                const card = $(`.ac-mascota[data-id="${d.mascotaId}"]`);
                if (card) $('[data-dur]', card).textContent = d.texto.split(' · ')[0];
            });
            ia.classList.remove('is-cargando');
            $('span', ia).innerHTML = (data.duraciones || []).map(d => {
                const nombre = $(`.ac-mascota[data-id="${d.mascotaId}"]`)?.dataset.nombre || 'Tu mascota';
                return `<b>${escapar(nombre)}</b>: ${escapar(d.texto)}`;
            }).join(' &nbsp;·&nbsp; ') + '<small>Duración estimada con IA según la raza y el pelaje.</small>';

            const horarios = data.horarios || [];
            const libres = horarios.filter(h => h.disponible).length;
            caja.innerHTML = horarios.map(h => `
                <button type="button" class="ac-hora ${h.disponible ? '' : 'is-off'}" data-hora="${h.hora}" data-etiqueta="${escapar(h.etiqueta)}"
                        ${h.disponible ? '' : `disabled title="${escapar(h.motivo || 'No disponible')}"`}>
                    <span>${escapar(h.etiqueta)}</span>
                    ${h.disponible ? '<i class="ac-check" aria-hidden="true"><svg><use href="#ac-check"/></svg></i>' : `<small>${escapar(h.motivo || '')}</small>`}
                </button>`).join('')
                + (libres ? '' : '<p class="ac-horarios__vacio ac-horarios__vacio--full">No hay horarios disponibles este día. Prueba con otro día.</p>');
        } catch (e) {
            if (n !== S.consulta) return;
            ia.hidden = true;
            caja.innerHTML = `<p class="ac-horarios__vacio">No pudimos cargar los horarios. ${escapar(e.message)}</p>`;
        }
        actualizar();
    }

    $('#ac-horarios').addEventListener('click', e => {
        const b = e.target.closest('.ac-hora[data-hora]');
        if (!b || b.disabled) return;
        S.hora = b.dataset.hora;
        S.horaTexto = b.dataset.etiqueta;
        $$('.ac-hora').forEach(x => x.classList.toggle('is-sel', x === b));
        actualizar();
    });

    /* ======================= Resumen y pasos ======================= */
    function actualizar() {
        const cards = S.mascotas.map(id => $(`.ac-mascota[data-id="${id}"]`)).filter(Boolean);
        $('#ac-res-masc').textContent = cards.length ? cards.map(c => c.dataset.nombre).join(' y ') : '—';
        $('#ac-res-fotos').innerHTML = cards.map(c => `<span>${$('.ac-mascota__foto', c).innerHTML}</span>`).join('');
        $('#ac-res-fecha').textContent = S.fecha
            ? `${textoFecha(S.fecha)}${S.horaTexto ? ' · ' + S.horaTexto : ''}`
            : '—';

        const paso = !S.mascotas.length ? 1 : (!S.fecha || !S.hora) ? 2 : 3;
        $$('#ac-steps li').forEach(li => {
            const k = Number(li.dataset.paso);
            li.classList.toggle('is-active', k === paso);
            li.classList.toggle('is-done', k < paso);
        });
        $('#ac-confirmar').disabled = paso !== 3;
    }

    /* ======================= Confirmar ======================= */
    $('#ac-confirmar').addEventListener('click', async () => {
        const btn = $('#ac-confirmar');
        if (btn.disabled) return;
        btn.disabled = true;
        btn.classList.add('is-cargando');
        $('span', btn).textContent = 'Agendando…';
        alerta('');
        try {
            const data = await json('/citas', {
                method: 'POST',
                headers: { 'Content-Type': 'application/json' },
                body: JSON.stringify({ mascotaIds: S.mascotas, fecha: S.fecha, hora: S.hora })
            });
            $('#ac-exito-texto').innerHTML = `Para el <b>${escapar(data.fechaTexto)}</b> a las <b>${escapar(data.hora)}</b>.`;
            $('#ac-exito-lista').innerHTML = (data.citas || []).map(c =>
                `<li><svg aria-hidden="true"><use href="#i-paw-solid"/></svg><b>${escapar(c.mascota)}</b><small>Baño y Peluquería · ${escapar(c.duracion)}</small></li>`).join('');
            $('#ac-exito').hidden = false;
            document.body.style.overflow = 'hidden';
            $('#ac-exito-listo').focus();
        } catch (e) {
            // Puede que otra persona haya tomado el cupo: se recargan los horarios
            alerta(e.message);
            S.hora = null; S.horaTexto = null;
            cargarHorarios();
        } finally {
            btn.classList.remove('is-cargando');
            $('span', btn).textContent = 'Confirmar cita';
            actualizar();
        }
    });
    // Al cerrar, se recarga para ver la cita en "Tus citas programadas"
    $('#ac-exito-listo').addEventListener('click', () => location.reload());

    /* ======================= Inicio ======================= */
    S.mes = new Date(hoy.getFullYear(), hoy.getMonth(), 1);
    pintarCalendario();
    // Si solo tiene una mascota, se selecciona sola
    const unicas = $$('.ac-mascota[data-id]');
    if (unicas.length === 1) unicas[0].click();
    actualizar();
})();
