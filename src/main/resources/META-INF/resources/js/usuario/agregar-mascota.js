// Vista "Agregar Mascota": vista previa de la foto, validación y envío al API existente.
// Endpoint usado (ya existente):  POST /api/usuarios/{usuarioId}/mascotas
(() => {
    const form = document.getElementById('am-form');
    if (!form) return;

    const $ = (sel, root = document) => root.querySelector(sel);
    const alertBox = $('#am-alert');
    const submitBtn = $('#am-submit');

    /* ======================= Usuario dueño de la mascota =======================
       Se toma del atributo data-usuario-id (la plantilla lo llena si recibe "usuarioId")
       o, si no viene, del parámetro ?usuarioId= de la URL. */
    const usuarioId = () => {
        const desdePlantilla = (form.dataset.usuarioId || '').trim();
        const desdeUrl = new URLSearchParams(location.search).get('usuarioId') || '';
        const id = desdePlantilla || desdeUrl.trim();
        return /^\d+$/.test(id) ? id : null;
    };

    /* ======================= Mensajes generales ======================= */
    const escapar = (t) => String(t).replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    const mostrarAlerta = (tipo, titulo, detalles = [], enlace = null) => {
        const body = $('.am-alert__body', alertBox);
        body.innerHTML = '<p>' + escapar(titulo) + '</p>' +
            (detalles.length ? '<ul>' + detalles.map((d) => '<li>' + escapar(d) + '</li>').join('') + '</ul>' : '') +
            (enlace ? '<p class="am-alert__link"><a href="' + escapar(enlace.href) + '">' + escapar(enlace.texto) + ' →</a></p>' : '');
        alertBox.classList.toggle('is-success', tipo === 'ok');
        $('use', alertBox).setAttribute('href', tipo === 'ok' ? '#am-check' : '#am-i-alert');
        alertBox.hidden = false;
        alertBox.scrollIntoView({ behavior: 'smooth', block: 'center' });
    };
    const ocultarAlerta = () => { alertBox.hidden = true; };
    $('.am-alert__close', alertBox).addEventListener('click', ocultarAlerta);

    /* ======================= Volver ======================= */
    const back = $('[data-am-back]');
    if (back) {
        back.addEventListener('click', (e) => {
            // Si se llegó desde otra página del sitio, regresa a ella
            if (document.referrer && new URL(document.referrer).origin === location.origin && history.length > 1) {
                e.preventDefault();
                history.back();
            }
        });
    }

    /* ======================= Icono de sexo ======================= */
    const sexo = $('#am-sexo');
    const sexoIco = $('#am-sexo-ico');
    const actualizarIconoSexo = () =>
        sexoIco.setAttribute('href', sexo.value === 'HEMBRA' ? '#am-female' : '#am-male');
    sexo.addEventListener('change', actualizarIconoSexo);

    /* ======================= Contadores (observaciones y fobias) ======================= */
    const areasConContador = [...form.querySelectorAll('textarea[data-contador]')];
    const contar = () => areasConContador.forEach((area) => {
        const salida = document.getElementById(area.dataset.contador);
        if (salida) salida.textContent = area.value.length;
    });
    areasConContador.forEach((area) => area.addEventListener('input', contar));

    /* ======================= Fecha de nacimiento: no puede ser futura ======================= */
    const hoyIso = () => {
        const d = new Date();
        d.setMinutes(d.getMinutes() - d.getTimezoneOffset());
        return d.toISOString().slice(0, 10);
    };
    const fecha = $('#am-fecha');
    fecha.max = hoyIso();

    /* ======================= Foto (vista previa) =======================
       El API actual solo recibe "fotoMascota" como URL de texto (máx. 255) y no tiene un
       endpoint para subir fotos de mascotas, por eso aquí la imagen se muestra como vista
       previa y no se envía. Cuando exista ese endpoint, basta con subir "archivoFoto" y
       poner la URL devuelta en el campo fotoMascota del cuerpo. */
    const TIPOS = ['image/jpeg', 'image/png', 'image/webp'];
    const MAX_BYTES = 2 * 1024 * 1024;
    const drop = $('#am-drop');
    const fileInput = $('#am-foto');
    const preview = $('#am-preview');
    const fotoError = $('#am-foto-error');
    const fotoLabel = $('#am-foto-label');
    const quitarFoto = $('#am-foto-quitar');
    let archivoFoto = null;
    let urlPreview = null;

    const errorFoto = (msg) => {
        fotoError.textContent = msg || '';
        fotoError.hidden = !msg;
        drop.classList.toggle('is-invalid', Boolean(msg));
    };

    const limpiarFoto = () => {
        if (urlPreview) URL.revokeObjectURL(urlPreview);
        urlPreview = null;
        archivoFoto = null;
        fileInput.value = '';
        preview.removeAttribute('src');
        preview.hidden = true;
        drop.classList.remove('has-image');
        fotoLabel.textContent = 'Seleccionar imagen';
        quitarFoto.hidden = true;
        errorFoto('');
    };

    const cargarFoto = (file) => {
        if (!file) return;
        if (!TIPOS.includes(file.type)) { errorFoto('Formato no válido. Usa una imagen JPG, PNG o WebP.'); return; }
        if (file.size > MAX_BYTES) { errorFoto('La imagen no puede superar 2 MB.'); return; }
        limpiarFoto();
        archivoFoto = file;
        urlPreview = URL.createObjectURL(file);
        preview.src = urlPreview;
        preview.hidden = false;
        drop.classList.add('has-image');
        fotoLabel.textContent = 'Cambiar imagen';
        quitarFoto.hidden = false;
    };

    fileInput.addEventListener('change', () => cargarFoto(fileInput.files[0]));
    quitarFoto.addEventListener('click', limpiarFoto);

    ['dragenter', 'dragover'].forEach((ev) => drop.addEventListener(ev, (e) => {
        e.preventDefault();
        drop.classList.add('is-dragover');
    }));
    ['dragleave', 'dragend', 'drop'].forEach((ev) => drop.addEventListener(ev, (e) => {
        e.preventDefault();
        drop.classList.remove('is-dragover');
    }));
    drop.addEventListener('drop', (e) => cargarFoto(e.dataTransfer && e.dataTransfer.files[0]));

    /* ======================= Validación ======================= */
    const campoDe = (el) => el.closest('.am-field');

    const marcar = (field, msg) => {
        const err = $('.am-error', field);
        field.classList.toggle('is-invalid', Boolean(msg));
        if (err) { err.textContent = msg || ''; err.hidden = !msg; }
        field.querySelectorAll('input, select, textarea').forEach((el) => {
            if (msg) el.setAttribute('aria-invalid', 'true'); else el.removeAttribute('aria-invalid');
        });
    };

    const validarCampo = (el) => {
        const field = campoDe(el);
        if (!field) return true;
        let msg = '';
        if (el.type === 'radio') {
            if (!form.querySelector(`input[name="${el.name}"]:checked`)) {
                const conMensaje = form.querySelector(`input[name="${el.name}"][data-msg]`);
                msg = (conMensaje && conMensaje.dataset.msg) || 'Selecciona una opción';
            }
        } else if (el.type === 'date') {
            if (el.validity.badInput) msg = 'Escribe una fecha válida';
            else if (el.required && !el.value) msg = el.dataset.msg || 'Este campo es obligatorio';
            else if (el.value && el.value > hoyIso()) msg = 'La fecha de nacimiento no puede ser futura';
            else if (el.value && el.min && el.value < el.min) msg = 'Revisa la fecha de nacimiento';
        } else if (el.type === 'number' && el.validity.badInput) {
            msg = 'Escribe un número válido';
        } else if (el.type === 'number' && el.value !== '') {
            const n = Number(el.value);
            if (!(n > 0)) msg = 'El peso debe ser mayor a 0';
            else if (el.max && n > Number(el.max)) msg = `Revisa el peso (máximo ${el.max} kg)`;
        } else if (el.required && !el.value.trim()) {
            msg = el.dataset.msg || 'Este campo es obligatorio';
        }
        marcar(field, msg);
        return !msg;
    };

    const validarTodo = () => {
        const requeridos = [...form.querySelectorAll('[required]')];
        const vistos = new Set();
        let primero = null;
        requeridos.forEach((el) => {
            if (el.type === 'radio') { if (vistos.has(el.name)) return; vistos.add(el.name); }
            if (!validarCampo(el) && !primero) primero = el;
        });
        if (primero) primero.focus({ preventScroll: false });
        return !primero;
    };

    // Se valida al salir del campo y se limpia el error mientras se corrige
    form.addEventListener('focusout', (e) => {
        const el = e.target;
        if (el.matches('input[type="text"][required], input[type="number"], input[type="date"]')
            && (el.value !== '' || el.validity.badInput)) validarCampo(el);
    });
    form.addEventListener('input', (e) => {
        const field = campoDe(e.target);
        if (field && field.classList.contains('is-invalid')) validarCampo(e.target);
    });
    form.addEventListener('change', (e) => {
        if (e.target.matches('select, input[type="radio"]')) validarCampo(e.target);
    });

    /* ======================= Limpiar ======================= */
    form.addEventListener('reset', () => {
        // Espera a que el navegador restablezca los valores
        setTimeout(() => {
            form.querySelectorAll('.am-field.is-invalid').forEach((f) => marcar(f, ''));
            limpiarFoto();
            actualizarIconoSexo();
            contar();
            ocultarAlerta();
            $('#am-nombre').focus();
        });
    });

    /* ======================= Envío ======================= */
    const texto = (name) => {
        const v = (form.elements[name].value || '').trim();
        return v === '' ? null : v;
    };

    // Radios Sí / No -> true / false (null si no se eligió)
    const siNo = (name) => {
        const marcado = form.querySelector(`input[name="${name}"]:checked`);
        return marcado ? marcado.value === 'true' : null;
    };

    // Mismos campos que DesparasitacionResponse.MascotaRequest
    const construirCuerpo = () => ({
        nombre: texto('nombre'),
        fechaNacimiento: texto('fechaNacimiento'),
        tipo: form.querySelector('input[name="tipo"]:checked').value,
        raza: texto('raza'),
        sexo: texto('sexo'),
        tamano: texto('tamano') ? Number(texto('tamano')) : null,
        peso: texto('peso') ? Number(texto('peso')) : null,
        color: texto('color'),
        pelaje: texto('pelaje'),
        observaciones: texto('observaciones'),
        fotoMascota: null,
        temperamento: texto('temperamento'),
        nivelAgresividad: texto('nivelAgresividad'),
        usaBozal: siNo('usaBozal'),
        fobias: texto('fobias'),
        sociable: siNo('sociable'),
        energia: texto('energia')
    });

    const ocupado = (si) => {
        submitBtn.disabled = si;
        submitBtn.classList.toggle('is-loading', si);
        $('use', submitBtn).setAttribute('href', si ? '#am-refresh' : '#am-check');
        $('span', submitBtn).textContent = si ? 'Guardando...' : 'Guardar Mascota';
    };

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        ocultarAlerta();
        if (!validarTodo()) return;

        const id = usuarioId();
        if (!id) {
            mostrarAlerta('error', 'No pudimos identificar tu cuenta.',
                ['Vuelve a ingresar a la plataforma e inténtalo de nuevo.']);
            return;
        }

        ocupado(true);
        try {
            const res = await fetch(`/api/usuarios/${encodeURIComponent(id)}/mascotas`, {
                method: 'POST',
                headers: { 'Content-Type': 'application/json', Accept: 'application/json' },
                body: JSON.stringify(construirCuerpo())
            });

            if (res.ok) {
                const mascota = await res.json().catch(() => ({}));
                const nombre = (mascota && mascota.nombre) || texto('nombre') || 'Tu mascota';
                form.reset();
                // Después del restablecimiento (que limpia mensajes) se muestra el éxito
                const perfil = mascota && mascota.id != null
                    ? { href: `/mascotas/perfil?id=${encodeURIComponent(mascota.id)}`, texto: `Ver perfil de ${nombre}` }
                    : null;
                setTimeout(() => mostrarAlerta('ok', `¡${nombre} quedó registrada en Yazkas!`,
                    ['Ya puedes agendarle una cita o registrar otra mascota.'], perfil));
                return;
            }

            const error = await res.json().catch(() => null);
            const titulo = (error && error.mensaje) || 'No se pudo registrar la mascota.';
            const detalles = (error && Array.isArray(error.detalles))
                ? error.detalles.map((d) => String(d).replace(/^[\w.]+:\s*/, ''))
                : [];
            mostrarAlerta('error', titulo, detalles);
        } catch (_) {
            mostrarAlerta('error', 'No hay conexión con el servidor.', ['Revisa tu conexión e inténtalo de nuevo.']);
        } finally {
            ocupado(false);
        }
    });
})();
