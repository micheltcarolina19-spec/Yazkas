// Agregar / Editar mascota (cliente con sesión) — mismos pasos y validaciones de la mascota en /registro.
// La plantilla dice el modo en <form data-modo="agregar|editar" data-mascota-id="...">.
// Endpoints existentes:
//   POST /uploads/mascotas                            -> sube la foto y devuelve { url }
//   POST /api/usuarios/{usuarioId}/mascotas           -> crea la mascota            (agregar)
//   GET  /api/mascotas/{id}                           -> datos actuales             (editar)
//   PUT  /api/mascotas/{id}                           -> guarda los cambios         (editar)
//   GET|POST|PUT /api/mascotas/{id}/historia-clinica  -> historia clínica (404 en GET = aún no tiene)
(() => {
    const $ = (s, r = document) => r.querySelector(s);
    const $$ = (s, r = document) => [...r.querySelectorAll(s)];
    const MAX = 5 * 1024 * 1024;
    const PRIMERO = 1, ULTIMO = 3;
    const form = $('#form');
    const S = {
        step: PRIMERO, usuarioId: (form.dataset.usuarioId || '').trim(), foto: '', subiendo: false, nombres: [], ultimoId: null,
        editar: form.dataset.modo === 'editar', mascotaId: (form.dataset.mascotaId || '').trim(),
        tieneHistoria: false, tempExtra: []   // tempExtra: temperamentos guardados que no están en los chips
    };

    const RAZAS = {
        perro: ['Criollo', 'Golden Retriever', 'Labrador', 'Pastor Alemán', 'Bulldog Francés', 'Poodle', 'Schnauzer', 'Shih Tzu', 'Yorkshire Terrier', 'Chihuahua', 'Pug', 'Beagle', 'Husky Siberiano', 'Rottweiler', 'Pinscher', 'Cocker Spaniel', 'Maltés', 'Border Collie'],
        gato: ['Criollo', 'Siamés', 'Persa', 'Angora', 'Maine Coon', 'Bengalí', 'Ragdoll', 'Británico de pelo corto', 'Azul Ruso', 'Sphynx', 'Birmano']
    };

    // ---------- etiquetas legibles para los enums (PELO_CORTO -> Pelo corto)
    const bonito = t => { t = t.replace(/_/g, ' ').toLowerCase(); return t.charAt(0).toUpperCase() + t.slice(1); };
    $$('[data-pretty]').forEach(e => e.textContent = bonito(e.textContent.trim()));

    // ---------- tipo: iconos y razas dinámicas
    const esGato = v => /^gat/i.test(v);
    const tipoActual = () => $('input[name=tipo]:checked').value;
    function pintarTipo() {
        $$('#tipoSeg label').forEach(l => {
            const v = $('input', l).value;
            $('i', l).className = 'bi ' + (esGato(v) ? 'bi-emoji-sunglasses' : 'bi-emoji-smile');
        });
        $('#razas').innerHTML = RAZAS[esGato(tipoActual()) ? 'gato' : 'perro'].map(r => `<option value="${r}">`).join('');
    }
    $$('input[name=tipo]').forEach(r => r.addEventListener('change', () => { $('#mRaza').value = ''; pintarTipo(); }));
    pintarTipo();

    // Raza como combobox: al hacer clic se despliega la lista (también se puede escribir otra raza)
    const raza = $('#mRaza');
    raza.addEventListener('click', () => { try { raza.showPicker && raza.showPicker(); } catch { /* navegador sin soporte */ } });

    // La fecha de nacimiento no puede ser futura
    $('#mFecha').max = new Date(Date.now() - new Date().getTimezoneOffset() * 60000).toISOString().slice(0, 10);

    // ---------- chips y switches
    $$('.chips .chip').forEach(c => c.addEventListener('click', () => c.classList.toggle('on')));
    $$('.switch input').forEach(i => {
        const t = () => $('em', i.closest('.switch')).textContent = i.checked ? 'Sí' : 'No';
        i.addEventListener('change', t); t();
    });

    // ---------- validación (las mismas reglas de la mascota en el registro)
    const reglas = {
        mFecha: v => new Date(v) <= new Date() || 'La fecha no puede ser futura',
        mTamano: v => +v.replace(',', '.') > 0 || 'Debe ser mayor a 0',
        mPeso: v => +v.replace(',', '.') > 0 || 'Debe ser mayor a 0'
    };
    function validar(inp) {
        const f = inp.closest('.field'), v = inp.value.trim();
        let msg = '';
        if (!v) msg = 'Campo obligatorio';
        else if (reglas[inp.id]) { const r = reglas[inp.id](inp.value.trim()); if (r !== true) msg = r; }
        f.classList.toggle('error', !!msg); f.classList.toggle('ok', !msg);
        const s = $('small', f); if (s) s.textContent = msg;
        return !msg;
    }
    $$('[data-req]').forEach(i => {
        i.addEventListener('blur', () => i.value && validar(i));
        i.addEventListener('input', () => i.closest('.field').classList.contains('error') && validar(i));
    });
    const validarPaso = n => {
        const r = $$(`.step[data-step="${n}"] [data-req]`).map(validar);
        const primero = $(`.step[data-step="${n}"] .error input, .step[data-step="${n}"] .error select`);
        if (primero) primero.focus();
        return r.every(Boolean);
    };

    // ---------- navegación
    const alerta = m => { const a = $('#alert'); a.hidden = !m; if (m) $('span', a).textContent = m; };
    function ir(n) {
        S.step = n; alerta('');
        $$('.step').forEach(s => s.classList.toggle('active', +s.dataset.step === n));
        $$('#stepper li').forEach(li => { const k = +li.dataset.s; li.classList.toggle('active', k === n); li.classList.toggle('done', k < n); });
        $('#bar').style.width = ((n - PRIMERO) / (ULTIMO - PRIMERO) * 100) + '%';
        $('#atras').hidden = n === PRIMERO;
        $('#omitir').hidden = n !== ULTIMO || S.editar;
        $('#siguiente').innerHTML = n === ULTIMO
            ? '<i class="bi bi-check2-circle"></i> ' + (S.editar ? 'Guardar cambios' : 'Guardar mascota')
            : 'Continuar <i class="bi bi-arrow-right"></i>';
        $('.card').scrollIntoView({ behavior: 'smooth', block: 'start' });
    }

    $('#atras').addEventListener('click', () => ir(S.step - 1));
    $('#siguiente').addEventListener('click', () => {
        if (S.step < ULTIMO) { if (validarPaso(S.step)) ir(S.step + 1); }
        else enviar();
    });
    $('#omitir').addEventListener('click', () => enviar(true));
    $('#form').addEventListener('keydown', e => {
        if (e.key === 'Enter' && e.target.tagName !== 'TEXTAREA') { e.preventDefault(); $('#siguiente').click(); }
    });

    // ---------- foto
    const drop = $('#drop'), file = $('#foto');
    const estado = (t, c) => { const u = $('#upState'); u.textContent = t; u.className = 'up ' + (c || ''); };
    drop.addEventListener('click', e => { if (!e.target.closest('#quitarFoto')) file.click(); });
    drop.addEventListener('keydown', e => (e.key === 'Enter' || e.key === ' ') && (e.preventDefault(), file.click()));
    ['dragover', 'dragenter'].forEach(ev => drop.addEventListener(ev, e => { e.preventDefault(); drop.classList.add('over'); }));
    ['dragleave', 'drop'].forEach(ev => drop.addEventListener(ev, e => { e.preventDefault(); drop.classList.remove('over'); }));
    drop.addEventListener('drop', e => e.dataTransfer.files[0] && subir(e.dataTransfer.files[0]));
    file.addEventListener('change', () => file.files[0] && subir(file.files[0]));
    $('#quitarFoto').addEventListener('click', quitarFoto);
    function quitarFoto() { S.foto = ''; file.value = ''; $('#preview').hidden = true; $('#dropBody').hidden = false; }

    async function subir(f) {
        if (!/^image\/(jpeg|png|webp)$/.test(f.type)) return alerta('Formato no válido. Usa JPG, PNG o WebP');
        if (f.size > MAX) return alerta('La imagen no puede superar 5 MB');
        alerta('');
        $('#prevImg').src = URL.createObjectURL(f);
        $('#preview').hidden = false; $('#dropBody').hidden = true;
        estado('Subiendo…'); S.subiendo = true; S.foto = '';
        try {
            const fd = new FormData(); fd.append('imagen', f);
            const r = await fetch('/uploads/mascotas', { method: 'POST', body: fd, credentials: 'same-origin' });
            if (!r.ok) throw new Error(await msgError(r));
            S.foto = (await r.json()).url; estado('Foto lista ✓', 'ok');
        } catch (e) { estado(e.message, 'err'); S.foto = ''; }
        finally { S.subiendo = false; }
    }

    // ---------- historial clínico
    $$('input[name=hc]').forEach(r => r.addEventListener('change', () => $('#hcDetalle').hidden = $('input[name=hc]:checked').value !== 'si'));
    $$('.acc').forEach(a => a.addEventListener('input', () => {
        const n = $$('[data-h]', a).filter(i => i.value.trim()).length;
        $('.cnt', a).textContent = n ? n + (n > 1 ? ' campos' : ' campo') : '';
    }));
    const tri = n => ({ si: true, no: false })[$(`input[name=${n}]:checked`).value] ?? null;
    const historiaVacia = () => {
        const h = { esterilizacion: tri('esterilizacion'), presenciaParasitos: tri('presenciaParasitos') };
        $$('[data-h]').forEach(e => h[e.dataset.h] = null);
        return h;
    };
    function historia() {
        const h = { esterilizacion: tri('esterilizacion'), presenciaParasitos: tri('presenciaParasitos') };
        const det = $('input[name=hc]:checked').value === 'si';
        $$('[data-h]').forEach(e => h[e.dataset.h] = (det && e.value.trim()) || null);
        return Object.values(h).some(v => v !== null) ? h : null;   // null = sin historia
    }

    // ---------- envío
    async function msgError(r) {
        try {
            const j = await r.json();
            return j.message || j.mensaje || j.error || j.detail || j.title ||
                (j.violations && j.violations.map(v => v.message).join('. ')) || ('Error ' + r.status);
        } catch { return 'Error ' + r.status; }
    }
    const num = v => parseFloat(v.replace(',', '.'));
    function mascota() {
        const fobias = [...$$('#fobias .chip.on').map(c => c.dataset.v), $('#fobiaOtra').value.trim()].filter(Boolean).join(', ');
        const obs = [$('#mEspecial').checked ? 'Requiere cuidados especiales.' : '', $('#mObs').value.trim()].filter(Boolean).join(' ');
        return {
            nombre: $('#mNombre').value.trim(), fechaNacimiento: $('#mFecha').value, tipo: tipoActual(),
            raza: $('#mRaza').value.trim(), sexo: $('#mSexo').value, tamano: num($('#mTamano').value), peso: num($('#mPeso').value),
            color: $('#mColor').value.trim(), pelaje: $('#mPelaje').value, observaciones: obs || null,
            fotoMascota: S.foto || null, temperamento: [...$$('#temp .chip.on').map(c => c.dataset.v), ...S.tempExtra].join(', ') || null,
            nivelAgresividad: $('#mAgr').value, usaBozal: $('#mBozal').checked, fobias: fobias || null,
            sociable: $('#mSociable').checked, energia: $('#mEnergia').value
        };
    }
    async function enviar(omitir = false) {
        if (S.subiendo) return alerta('Espera a que termine de subir la foto');
        if (!S.usuarioId) return alerta('Tu sesión expiró. Vuelve a iniciar sesión.');
        if (!validarPaso(PRIMERO)) return ir(PRIMERO);      // por si se editó algo al volver atrás

        const btn = $('#siguiente'), om = $('#omitir'), html = btn.innerHTML;
        btn.disabled = om.disabled = true; btn.innerHTML = '<span class="spin"></span> Guardando…'; alerta('');
        const hist = omitir ? null : historia();
        try {
            const body = mascota();
            const r = S.editar
                ? await fetch(`/api/mascotas/${encodeURIComponent(S.mascotaId)}`, {
                    method: 'PUT', credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
                })
                : await fetch(`/api/usuarios/${encodeURIComponent(S.usuarioId)}/mascotas`, {
                    method: 'POST', credentials: 'same-origin',
                    headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
                });
            if (!r.ok) throw new Error(await msgError(r));
            const data = await r.json().catch(() => ({}));
            if (S.editar && data.id == null) data.id = S.mascotaId;

            // La historia va por su propio endpoint: POST si no tenía, PUT si ya existía.
            // Al editar una historia existente se envía aunque quede vacía (así se pueden borrar datos).
            S.aviso = '';
            const historiaAEnviar = hist || (S.editar && S.tieneHistoria ? historiaVacia() : null);
            if (historiaAEnviar && data.id != null) {
                try {
                    const h = await fetch(`/api/mascotas/${data.id}/historia-clinica`, {
                        method: S.editar && S.tieneHistoria ? 'PUT' : 'POST', credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(historiaAEnviar)
                    });
                    if (!h.ok) throw 0;
                    if (S.editar) S.tieneHistoria = true;
                } catch { S.aviso = 'No pudimos guardar el historial clínico; podrás completarlo después.'; }
            }
            S.ultimoId = data.id ?? null;
            S.nombres.push(body.nombre);
            exito(body.nombre);
        } catch (e) { alerta(e.message); }
        finally { btn.disabled = om.disabled = false; btn.innerHTML = html; }
    }

    // ---------- éxito y "agregar otra mascota"
    function exito(nombre) {
        $('#form').hidden = true; $('#stepper').hidden = true; $('#done').hidden = false;
        $('#titulo').textContent = '¡Listo!';
        if (S.editar) {
            $('#doneTitulo').textContent = '¡Cambios guardados!';
            $('#subtitulo').textContent = `Actualizamos la información de ${nombre}`;
            $('#doneTexto').textContent = 'La información de tu mascota quedó al día.' + (S.aviso ? ' ' + S.aviso : '');
            $('#added').innerHTML = '';
            $('#otra').hidden = true;
        } else {
            $('#subtitulo').textContent = `${nombre} ya hace parte de la familia Yazka’s`;
            $('#doneTexto').textContent = S.nombres.length > 1 ? 'Mascotas registradas en esta sesión:' : 'Tu mascota ya está registrada. ¿Tienes más?';
            if (S.aviso) $('#doneTexto').textContent += ' ' + S.aviso;
            $('#added').innerHTML = S.nombres.map(n => `<li><i class="bi bi-balloon-heart-fill"></i> ${n.replace(/</g, '&lt;')}</li>`).join('');
        }
        const ver = $('#verPerfil');
        ver.href = S.ultimoId != null ? `/mascotas/${encodeURIComponent(S.ultimoId)}` : '/mascotas';
        ver.innerHTML = `Ver perfil de ${nombre.replace(/</g, '&lt;')} <i class="bi bi-arrow-right"></i>`;
        window.scrollTo({ top: 0, behavior: 'smooth' });
    }
    $('#otra').addEventListener('click', () => {
        $$('.step input:not([type=radio]):not([type=checkbox]):not([type=file]), textarea').forEach(i => i.value = '');
        $$('#mSexo, #mPelaje, #mEnergia, #mAgr').forEach(s => s.selectedIndex = 0);
        $$('input[name=tipo]')[0].checked = true;
        $$('.chip.on').forEach(c => c.classList.remove('on'));
        $$('.field').forEach(f => f.classList.remove('ok', 'error'));
        $$('.field small').forEach(s => s.textContent = '');
        $('#mSociable').checked = true; $('#mBozal').checked = false; $('#mEspecial').checked = false;
        $$('input[name=esterilizacion][value=ns],input[name=presenciaParasitos][value=ns],input[name=hc][value=no]').forEach(r => r.checked = true);
        $('#hcDetalle').hidden = true; $$('.cnt').forEach(c => c.textContent = ''); $$('.acc').forEach((a, i) => a.open = i === 0);
        $$('.switch input').forEach(i => i.dispatchEvent(new Event('change')));
        quitarFoto(); pintarTipo();
        $('#done').hidden = true; $('#form').hidden = false; $('#stepper').hidden = false;
        $('#titulo').textContent = 'Agregar otra mascota'; $('#subtitulo').textContent = 'Cuéntanos sobre tu nuevo peludo';
        ir(PRIMERO);
    });

    // ---------- modo edición: cargar los datos actuales en el formulario
    const marcarRadio = (name, value) => { const r = $(`input[name="${name}"][value="${value}"]`); if (r) r.checked = true; };
    const triValor = b => (b === true ? 'si' : b === false ? 'no' : 'ns');
    const ponerSelect = (sel, v) => { if (v != null && [...$(sel).options].some(o => o.value === v)) $(sel).value = v; };
    const numTexto = v => (v == null ? '' : String(v).replace('.', ','));

    // Marca los chips que coinciden y devuelve lo que no está en los chips
    function marcarChips(contenedor, texto) {
        const valores = (texto || '').split(',').map(t => t.trim()).filter(Boolean);
        const resto = [];
        valores.forEach(v => {
            const chip = $$(`${contenedor} .chip`).find(c => c.dataset.v.toLowerCase() === v.toLowerCase());
            if (chip) chip.classList.add('on'); else resto.push(v);
        });
        return resto;
    }

    function llenarMascota(m) {
        $('#mNombre').value = m.nombre || '';
        $('#mFecha').value = m.fechaNacimiento || '';
        if (m.tipo) { marcarRadio('tipo', m.tipo); pintarTipo(); }
        $('#mRaza').value = m.raza || '';
        ponerSelect('#mSexo', m.sexo);
        $('#mColor').value = m.color || '';
        $('#mTamano').value = numTexto(m.tamano);
        $('#mPeso').value = numTexto(m.peso);
        ponerSelect('#mPelaje', m.pelaje);
        ponerSelect('#mEnergia', m.energia);
        ponerSelect('#mAgr', m.nivelAgresividad);

        S.tempExtra = marcarChips('#temp', m.temperamento);
        $('#fobiaOtra').value = marcarChips('#fobias', m.fobias).join(', ');

        $('#mSociable').checked = m.sociable !== false;
        $('#mBozal').checked = m.usaBozal === true;
        let obs = (m.observaciones || '').trim();
        const ESPECIAL = 'Requiere cuidados especiales.';
        $('#mEspecial').checked = obs.startsWith(ESPECIAL);
        if (obs.startsWith(ESPECIAL)) obs = obs.slice(ESPECIAL.length).trim();
        $('#mObs').value = obs;
        $$('.switch input').forEach(i => i.dispatchEvent(new Event('change')));

        if (m.fotoMascota) {
            S.foto = m.fotoMascota;
            $('#prevImg').src = m.fotoMascota;
            $('#preview').hidden = false; $('#dropBody').hidden = true;
            estado('Foto actual', 'ok');
        }
    }

    function llenarHistoria(h) {
        marcarRadio('esterilizacion', triValor(h.esterilizacion));
        marcarRadio('presenciaParasitos', triValor(h.presenciaParasitos));
        let hayTexto = false;
        $$('[data-h]').forEach(e => { e.value = h[e.dataset.h] || ''; if (e.value) hayTexto = true; });
        marcarRadio('hc', hayTexto ? 'si' : 'no');
        $('#hcDetalle').hidden = !hayTexto;
        $$('.acc').forEach(a => {
            a.dispatchEvent(new Event('input'));
            if ($$('[data-h]', a).some(i => i.value)) a.open = true;
        });
    }

    async function iniciarEdicion() {
        $('#titulo').textContent = 'Editar información';
        $('#subtitulo').textContent = 'Actualiza los datos de tu mascota';
        $('#volver').href = `/mascotas/${encodeURIComponent(S.mascotaId)}`;
        $('#volver').innerHTML = '<i class="bi bi-arrow-left"></i> Volver al perfil';
        $$('.step[data-step="1"] .step-head h2').forEach(h => h.textContent = 'Paso 1. Datos de tu mascota');
        $$('.step[data-step="1"] .step-head p').forEach(p => p.textContent = 'Revisa y corrige la información de tu mascota');
        $('#form').hidden = true; $('#cargando').hidden = false;
        try {
            const r = await fetch(`/api/mascotas/${encodeURIComponent(S.mascotaId)}`, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
            if (!r.ok) throw new Error(await msgError(r));
            const m = await r.json();
            llenarMascota(m);
            $('#titulo').textContent = `Editar a ${m.nombre}`;
            document.title = `Editar a ${m.nombre} · Yazkas Peluquería Canina`;

            const h = await fetch(`/api/mascotas/${encodeURIComponent(S.mascotaId)}/historia-clinica`, { credentials: 'same-origin', headers: { Accept: 'application/json' } });
            if (h.ok) { S.tieneHistoria = true; llenarHistoria(await h.json()); }
            $('#form').hidden = false;
        } catch (e) {
            alerta('No pudimos cargar la información de tu mascota. ' + e.message);
        } finally {
            $('#cargando').hidden = true;
        }
    }

    ir(PRIMERO);
    if (S.editar) iniciarEdicion();
})();
