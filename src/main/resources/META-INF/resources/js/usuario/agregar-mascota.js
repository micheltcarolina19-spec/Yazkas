// Agregar mascota (cliente con sesión) — mismos pasos y validaciones de la mascota en /registro.
// Endpoints existentes:
//   POST /uploads/mascotas                            -> sube la foto y devuelve { url }
//   POST /api/usuarios/{usuarioId}/mascotas           -> crea la mascota
//   POST /api/mascotas/{mascotaId}/historia-clinica   -> historia clínica (solo si se llenó)
(() => {
    const $ = (s, r = document) => r.querySelector(s);
    const $$ = (s, r = document) => [...r.querySelectorAll(s)];
    const MAX = 5 * 1024 * 1024;
    const PRIMERO = 1, ULTIMO = 3;
    const S = { step: PRIMERO, usuarioId: ($('#form').dataset.usuarioId || '').trim(), foto: '', subiendo: false, nombres: [], ultimoId: null };

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
        $('#omitir').hidden = n !== ULTIMO;
        $('#siguiente').innerHTML = n === ULTIMO
            ? '<i class="bi bi-check2-circle"></i> Guardar mascota'
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
            fotoMascota: S.foto || null, temperamento: $$('#temp .chip.on').map(c => c.dataset.v).join(', ') || null,
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
            const r = await fetch(`/api/usuarios/${encodeURIComponent(S.usuarioId)}/mascotas`, {
                method: 'POST', credentials: 'same-origin',
                headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body)
            });
            if (!r.ok) throw new Error(await msgError(r));
            const data = await r.json().catch(() => ({}));

            S.aviso = '';
            if (hist && data.id != null) {   // la historia va por su propio endpoint
                try {
                    const h = await fetch(`/api/mascotas/${data.id}/historia-clinica`, {
                        method: 'POST', credentials: 'same-origin',
                        headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(hist)
                    });
                    if (!h.ok) throw 0;
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
        $('#subtitulo').textContent = `${nombre} ya hace parte de la familia Yazka’s`;
        $('#doneTexto').textContent = S.nombres.length > 1 ? 'Mascotas registradas en esta sesión:' : 'Tu mascota ya está registrada. ¿Tienes más?';
        if (S.aviso) $('#doneTexto').textContent += ' ' + S.aviso;
        $('#added').innerHTML = S.nombres.map(n => `<li><i class="bi bi-balloon-heart-fill"></i> ${n.replace(/</g, '&lt;')}</li>`).join('');
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

    ir(PRIMERO);
})();
