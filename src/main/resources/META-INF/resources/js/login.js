// Ruta sugerida: src/main/resources/META-INF/resources/js/usuarios/login.js
(function () {
    'use strict';

    var form = document.getElementById('login-form');
    var submit = document.getElementById('login-submit');
    var password = document.getElementById('contrasena');
    var toggle = document.getElementById('toggle-password');

    // Mostrar / ocultar contraseña
    if (toggle && password) {
        toggle.addEventListener('click', function () {
            var visible = password.type === 'text';
            password.type = visible ? 'password' : 'text';
            toggle.setAttribute('aria-pressed', String(!visible));
            toggle.setAttribute('aria-label', visible ? 'Mostrar contraseña' : 'Ocultar contraseña');
            password.focus();
        });
    }

    // Estado de carga al enviar (evita doble clic)
    if (form && submit) {
        form.addEventListener('submit', function () {
            if (form.checkValidity()) {
                submit.classList.add('is-loading');
                submit.setAttribute('aria-busy', 'true');
            }
        });

        // Si el usuario vuelve con el botón "atrás", restablece el botón
        window.addEventListener('pageshow', function () {
            submit.classList.remove('is-loading');
            submit.removeAttribute('aria-busy');
        });
    }

    // Al escribir de nuevo, quita el marcado de error
    ['correo', 'contrasena'].forEach(function (id) {
        var el = document.getElementById(id);
        if (el) {
            el.addEventListener('input', function () {
                el.removeAttribute('aria-invalid');
            });
        }
    });
})();