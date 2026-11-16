// Vista "Perfil de Mascota": consulta la mascota y su dueño con los endpoints existentes
//   GET /api/mascotas/{id}
//   GET /api/usuarios/{usuarioId}
//   GET /api/mascotas/{id}/historia-clinica   (botón "Descargar Historia Clínica")
(() => {
    const main = document.getElementById('contenido');
    if (!main) return;

    const $ = (sel, root = document) => root.querySelector(sel);
    const $$ = (sel, root = document) => [...root.querySelectorAll(sel)];

    const status = $('#pm-status');
    const layout = $('#pm-layout');
    const alertBox = $('#pm-alert');

    /* ======================= Utilidades ======================= */
    const escapar = (t) => String(t).replace(/[&<>"']/g, (c) =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));

    // "GOLDEN_RETRIEVER" / "MACHO" -> "Golden retriever" / "Macho"
    const capitalizar = (v) => {
        if (v == null || v === '') return '';
        const s = String(v).replace(/_/g, ' ').toLowerCase();
        return s.charAt(0).toUpperCase() + s.slice(1);
    };
    const oGuion = (v) => (v == null || String(v).trim() === '' ? '—' : String(v));

    // El backend guarda el tamaño en cm (aprox.). Mismos rangos que el formulario de registro:
    // Pequeño = 25, Mediano = 45, Grande = 60, Gigante = 75
    const tamanoTexto = (cm) => {
        if (cm == null) return '—';
        if (cm < 35) return 'Pequeño';
        if (cm < 53) return 'Mediano';
        if (cm < 68) return 'Grande';
        return 'Gigante';
    };
    const pesoTexto = (kg) => (kg == null ? '—' : `${Number(kg).toLocaleString('es-CO', { maximumFractionDigits: 1 })} kg`);
    const siNo = (b) => (b == null ? '—' : b ? 'Sí' : 'No');

    // "2022-03-15" -> fecha local (sin desfase por zona horaria)
    const aFecha = (iso) => {
        const p = /^(\d{4})-(\d{2})-(\d{2})/.exec(iso || '');
        return p ? new Date(Number(p[1]), Number(p[2]) - 1, Number(p[3])) : null;
    };
    const edadTexto = (nac) => {
        const hoy = new Date();
        let meses = (hoy.getFullYear() - nac.getFullYear()) * 12 + (hoy.getMonth() - nac.getMonth());
        if (hoy.getDate() < nac.getDate()) meses -= 1;
        if (meses < 0) return '';
        const anios = Math.floor(meses / 12);
        const resto = meses % 12;
        const partes = [];
        if (anios) partes.push(`${anios} ${anios === 1 ? 'año' : 'años'}`);
        if (resto) partes.push(`${resto} ${resto === 1 ? 'mes' : 'meses'}`);
        return partes.length ? partes.join(' y ') : 'menos de 1 mes';
    };
    // "15 de marzo de 2022 (2 años y 8 meses)"
    const nacimientoTexto = (iso) => {
        const nac = aFecha(iso);
        if (!nac) return '—';
        const fecha = nac.toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
        const edad = edadTexto(nac);
        return edad ? `${fecha} (${edad})` : fecha;
    };

    const getJson = async (url) => {
        const res = await fetch(url, { headers: { Accept: 'application/json' } });
        const body = await res.json().catch(() => null);
        if (!res.ok) {
            const err = new Error((body && body.mensaje) || `Error ${res.status}`);
            err.status = res.status;
            throw err;
        }
        return body;
    };

    /* ======================= Mensajes ======================= */
    const mostrarAlerta = (tipo, titulo, detalle) => {
        $('.am-alert__body', alertBox).innerHTML =
            '<p>' + escapar(titulo) + '</p>' + (detalle ? '<ul><li>' + escapar(detalle) + '</li></ul>' : '');
        alertBox.classList.toggle('is-success', tipo === 'ok');
        alertBox.classList.toggle('is-info', tipo === 'info');
        $('use', alertBox).setAttribute('href', tipo === 'ok' ? '#am-check' : '#am-alert');
        alertBox.hidden = false;
        alertBox.scrollIntoView({ behavior: 'smooth', block: 'center' });
    };
    $('.am-alert__close', alertBox).addEventListener('click', () => { alertBox.hidden = true; });

    const mostrarError = (texto) => {
        status.classList.add('is-error');
        $('.pm-status__text', status).textContent = texto;
        $('#pm-status-btn').hidden = false;
        status.hidden = false;
        layout.hidden = true;
    };

    /* ======================= Pintar datos ======================= */
    const ponerCampo = (campo, valor) =>
        $$(`[data-campo="${campo}"]`).forEach((el) => { el.textContent = valor; });
    const ponerDueno = (campo, valor) =>
        $$(`[data-dueno="${campo}"]`).forEach((el) => { el.textContent = valor; });

    const pintarMascota = (m) => {
        const hembra = m.sexo === 'HEMBRA';
        ponerCampo('nombre', oGuion(m.nombre));
        ponerCampo('raza', oGuion(m.raza));
        ponerCampo('color', oGuion(capitalizar(m.color)));
        ponerCampo('sexo', oGuion(capitalizar(m.sexo)));
        ponerCampo('tamano', tamanoTexto(m.tamano));
        ponerCampo('peso', pesoTexto(m.peso));
        ponerCampo('tipo', oGuion(capitalizar(m.tipo)));
        ponerCampo('pelaje', oGuion(capitalizar(m.pelaje)));
        ponerCampo('fechaNacimiento', nacimientoTexto(m.fechaNacimiento));
        ponerCampo('temperamento', oGuion(m.temperamento));
        ponerCampo('nivelAgresividad', oGuion(capitalizar(m.nivelAgresividad)));
        ponerCampo('energia', oGuion(capitalizar(m.energia)));
        ponerCampo('usaBozal', siNo(m.usaBozal));
        ponerCampo('sociable', siNo(m.sociable));
        ponerCampo('fobias', oGuion(m.fobias));
        ponerCampo('observaciones', oGuion(m.observaciones));

        const iconoSexo = hembra ? '#pm-female' : '#pm-male';
        $('#pm-sexo-ico').setAttribute('href', iconoSexo);
        $('use', $('#pm-nombre-sexo')).setAttribute('href', iconoSexo);
        $('#pm-nombre-sexo').setAttribute('aria-label', hembra ? 'Hembra' : 'Macho');

        // Foto: si la URL no carga, se deja la huella por defecto
        const img = $('#pm-foto');
        const vacia = $('#pm-foto-vacia');
        if (m.fotoMascota) {
            img.alt = `Foto de ${m.nombre}`;
            img.onload = () => { img.hidden = false; vacia.hidden = true; };
            img.onerror = () => { img.hidden = true; vacia.hidden = false; };
            img.src = m.fotoMascota;
        }

        if (m.usuarioId != null) {
            $('#pm-agregar').href = `/mascotas/agregar?usuarioId=${encodeURIComponent(m.usuarioId)}`;
        }
        document.title = `${m.nombre} · Perfil de Mascota · Yazkas`;
    };

    const pintarDueno = (u, respaldoNombre) => {
        const nombre = u ? [u.nombre, u.apellido].filter(Boolean).join(' ') : respaldoNombre;
        ponerDueno('nombre', oGuion(nombre));
        ponerDueno('celular', oGuion(u && u.celular));
        // El correo puede partirse justo después de la @ (y no a mitad de palabra)
        const correo = oGuion(u && u.correo);
        $$('[data-dueno="correo"]').forEach((el) => {
            el.innerHTML = escapar(correo).replace('@', '@<wbr>');
        });
        // Nombre del cliente en el navbar
        const navNombre = $('.pc-user__name');
        if (navNombre && u && u.nombre) navNombre.textContent = u.nombre;
    };

    /* ======================= Historia clínica (imprimir / guardar PDF) ======================= */
    let mascota = null;

    const filas = (pares) => pares
        .map(([k, v]) => `<tr><th>${escapar(k)}</th><td>${escapar(oGuion(v))}</td></tr>`).join('');

    const construirImprimible = (m, h) => {
        const hoy = new Date().toLocaleDateString('es-CO', { day: 'numeric', month: 'long', year: 'numeric' });
        $('#pm-print').innerHTML = `
            <h1>Historia Clínica · ${escapar(m.nombre)}</h1>
            <p class="pm-print__sub">Yazkas Peluquería Canina · Generada el ${escapar(hoy)}</p>

            <h2>Datos de la mascota</h2>
            <table>${filas([
                ['Nombre', m.nombre], ['Tipo', capitalizar(m.tipo)], ['Raza', m.raza],
                ['Sexo', capitalizar(m.sexo)], ['Fecha de nacimiento', nacimientoTexto(m.fechaNacimiento)],
                ['Color', capitalizar(m.color)], ['Tamaño', tamanoTexto(m.tamano)], ['Peso', pesoTexto(m.peso)],
                ['Pelaje', capitalizar(m.pelaje)], ['Propietario', m.nombrePropietario]
            ])}</table>

            <h2>Comportamiento</h2>
            <table>${filas([
                ['Temperamento', m.temperamento], ['Nivel de agresividad', capitalizar(m.nivelAgresividad)],
                ['Nivel de energía', capitalizar(m.energia)], ['Usa bozal', siNo(m.usaBozal)],
                ['Sociable', siNo(m.sociable)], ['Fobias', m.fobias],
                ['Observaciones del dueño', m.observaciones]
            ])}</table>

            <h2>Alergias</h2>
            <table>${filas([
                ['A medicamentos', h.alergiasMedicamentos], ['Cosméticas', h.alergiasCosmeticas],
                ['Alimenticias', h.alergiasAlimenticias]
            ])}</table>

            <h2>Condiciones de salud</h2>
            <table>${filas([
                ['Enfermedades crónicas', h.enfermedadesCronicas], ['Condiciones físicas', h.condicionesFisicas],
                ['Problemas cardíacos', h.problemasCardiacos], ['Problemas ortopédicos', h.problemasOrtopedicos],
                ['Diagnóstico dermatológico', h.diagnosticoDermatologico],
                ['Presencia de parásitos', siNo(h.presenciaParasitos)],
                ['Cirugías recientes', h.cirugiasRecientes], ['Lesiones', h.lesiones],
                ['Esterilización', siNo(h.esterilizacion)]
            ])}</table>

            <h2>Observaciones</h2>
            <table>${filas([['Observaciones', h.observaciones]])}</table>

            <p class="pm-print__foot">Documento generado desde el portal de clientes de Yazkas.</p>`;
    };

    const btnDescargar = $('#pm-descargar');
    btnDescargar.addEventListener('click', async () => {
        if (!mascota) return;
        btnDescargar.disabled = true;
        btnDescargar.classList.add('is-loading');
        try {
            const historia = await getJson(`/api/mascotas/${encodeURIComponent(mascota.id)}/historia-clinica`);
            construirImprimible(mascota, historia);
            // Abre el diálogo de impresión: desde ahí se puede "Guardar como PDF"
            window.print();
        } catch (e) {
            if (e.status === 404) {
                mostrarAlerta('info', `${mascota.nombre} aún no tiene una historia clínica registrada.`,
                    'Cuando la peluquería la registre, podrás descargarla desde aquí.');
            } else {
                mostrarAlerta('error', 'No se pudo obtener la historia clínica.', e.message);
            }
        } finally {
            btnDescargar.disabled = false;
            btnDescargar.classList.remove('is-loading');
        }
    });

    /* ======================= Editar ======================= */
    // Aún no existe una vista de edición; el API ya tiene PUT /api/mascotas/{id} para cuando se construya.
    $$('[data-pm-editar]').forEach((btn) => btn.addEventListener('click', () => {
        mostrarAlerta('info', 'La edición de la información estará disponible muy pronto.',
            'Mientras tanto, si necesitas corregir algún dato, comunícate con la peluquería.');
    }));

    /* ======================= Carga inicial ======================= */
    const cargar = async () => {
        const desdePlantilla = (main.dataset.mascotaId || '').trim();
        const desdeUrl = (new URLSearchParams(location.search).get('id') || '').trim();
        const id = desdePlantilla || desdeUrl;

        if (!/^\d+$/.test(id)) {
            mostrarError('No se indicó qué mascota mostrar.');
            return;
        }

        try {
            mascota = await getJson(`/api/mascotas/${id}`);
        } catch (e) {
            mostrarError(e.status === 404
                ? 'No encontramos esta mascota. Es posible que haya sido eliminada.'
                : 'No se pudo cargar la información de la mascota. Inténtalo de nuevo más tarde.');
            return;
        }

        pintarMascota(mascota);

        // Si falla la consulta del dueño, se muestra al menos el nombre que trae la mascota
        let dueno = null;
        if (mascota.usuarioId != null) {
            try { dueno = await getJson(`/api/usuarios/${encodeURIComponent(mascota.usuarioId)}`); } catch (_) { /* respaldo */ }
        }
        pintarDueno(dueno, mascota.nombrePropietario);

        status.hidden = true;
        layout.hidden = false;
    };

    cargar();
})();
