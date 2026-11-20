/* =====================================================================
   Yazka's · Nosotros · Encabezado "El spa de los consentidos"
   GSAP 3.13 + ScrollTrigger. Mejora progresiva:
   - Si GSAP no carga, el contenido queda visible y los números ya son los finales.
   - Solo se anima transform, opacity, clip-path y trazos SVG pequeños.
   - Movimiento reducido: sin animaciones de desplazamiento.
   - Todo vive dentro de .yz-about y se puede destruir/reiniciar.
   ===================================================================== */
(() => {
    'use strict';

    const SECTION_SELECTOR = '.yz-about';
    const html = document.documentElement;

    /* Quita el ocultamiento temporal del <head> (siempre seguro de llamar) */
    const revealContent = () => html.classList.remove('yz-anim');

    /* Curvas: ease-out fuerte para entradas; sin rebotes */
    const EASE = {
        enter: 'expo.out',
        mask: 'power4.out',
        draw: 'power2.out',
        wipe: 'power3.inOut'
    };

    let state = null; // { section, ctx, mm }

    /* ------------------------------------------------------------------
       Utilidades
       ------------------------------------------------------------------ */
    const toArray = (list) => Array.from(list || []).filter(Boolean);

    /** Vuelve a poner el valor final real en cada contador */
    function restoreCounters(section) {
        section.querySelectorAll('[data-yz-count]').forEach((el) => {
            el.textContent = el.dataset.yzCount;
        });
    }

    /* ------------------------------------------------------------------
       1 · Titular, texto, nota y banda de estadísticas (timeline de entrada)
       ------------------------------------------------------------------ */
    function initTitleReveal(section) {
        const q = (s) => section.querySelector(s);
        const qa = (s) => toArray(section.querySelectorAll(s));

        const photo      = q('.yz-about__photo');
        const eyebrow    = q('.yz-about__eyebrow');
        const eyebrowPaw = q('.yz-about__eyebrow .hm-ico');
        const lines      = qa('.yz-line__in');
        const script     = q('.yz-about__script');
        const underline  = q('.yz-about__underline');
        const underPath  = q('.yz-about__underline path');
        const rays       = q('.yz-about__rays');
        const rayPaths   = qa('.yz-about__rays path');
        const lead       = q('.yz-about__lead');
        const note       = q('.yz-about__note');
        const noteText   = q('.yz-about__note-text');
        const notePaths  = qa('.yz-about__arrow path, .yz-about__note-line path');
        const stats      = q('.yz-about__stats');
        const statItems  = qa('.yz-stat');
        const rules      = qa('.yz-stat__rule');

        const tl = gsap.timeline({ paused: true, defaults: { ease: EASE.enter } });

        /* La escena: opacidad + zoom-out muy sutil (1.04 → 1) */
        if (photo) {
            tl.fromTo(photo,
                { autoAlpha: 0, scale: 1.04 },
                { autoAlpha: 1, scale: 1, duration: 1.4, ease: 'power2.out' }, 0);
        }

        /* Etiqueta "Conócenos" */
        if (eyebrow) {
            tl.fromTo(eyebrow,
                { autoAlpha: 0, y: 12 },
                { autoAlpha: 1, y: 0, duration: .6 }, .2);
        }
        if (eyebrowPaw) {
            tl.fromTo(eyebrowPaw,
                { rotate: -35, scale: .7 },
                { rotate: 0, scale: 1, duration: .7 }, .25);
        }

        /* "No peluqueamos," sube desde su máscara */
        if (lines[0]) {
            tl.fromTo(lines[0],
                { autoAlpha: 1, yPercent: 105 },
                { yPercent: 0, duration: .8, ease: EASE.mask }, .3);
        }

        /* "embellecemos": máscara horizontal, como si se escribiera */
        if (script) {
            tl.fromTo(script,
                { autoAlpha: 1, clipPath: 'inset(-30% 100% -30% -6%)' },
                { clipPath: 'inset(-30% -6% -30% -6%)', duration: .85, ease: EASE.wipe }, .5);
        }

        /* Subrayado azul que se dibuja de izquierda a derecha */
        if (underline && underPath) {
            tl.fromTo(underline, { autoAlpha: 0 }, { autoAlpha: 1, duration: .01 }, 1.05)
                .fromTo(underPath,
                    { strokeDashoffset: 1 },
                    { strokeDashoffset: 0, duration: .6, ease: EASE.draw }, 1.05);
        }

        /* "a tu mascota." y los rayitos rosados */
        if (lines[1]) {
            tl.fromTo(lines[1],
                { autoAlpha: 1, yPercent: 105 },
                { yPercent: 0, duration: .8, ease: EASE.mask }, .72);
        }
        if (rays && rayPaths.length) {
            tl.fromTo(rays, { autoAlpha: 0 }, { autoAlpha: 1, duration: .01 }, 1.3)
                .fromTo(rayPaths,
                    { strokeDashoffset: 1 },
                    { strokeDashoffset: 0, duration: .35, stagger: .07, ease: EASE.draw }, 1.3);
        }

        /* Descripción */
        if (lead) {
            tl.fromTo(lead,
                { autoAlpha: 0, y: 18 },
                { autoAlpha: 1, y: 0, duration: .8 }, .85);
        }

        /* Nota escrita a mano + flecha hacia las mascotas */
        if (note) {
            tl.fromTo(note, { autoAlpha: 0 }, { autoAlpha: 1, duration: .01 }, 1.15);
            if (noteText) {
                tl.fromTo(noteText,
                    { clipPath: 'inset(-20% 100% -20% -4%)' },
                    { clipPath: 'inset(-20% -4% -20% -4%)', duration: .8, ease: EASE.wipe }, 1.15);
            }
            if (notePaths.length) {
                tl.fromTo(notePaths,
                    { strokeDashoffset: 1 },
                    { strokeDashoffset: 0, duration: .5, stagger: .12, ease: EASE.draw }, 1.55);
            }
        }

        /* Banda de estadísticas: aparece, luego los separadores y los números */
        if (stats) {
            tl.fromTo(stats,
                { autoAlpha: 0, y: 24 },
                { autoAlpha: 1, y: 0, duration: .8 }, 1);
        }
        if (statItems.length) {
            tl.fromTo(statItems,
                { autoAlpha: 0, y: 10 },
                { autoAlpha: 1, y: 0, duration: .6, stagger: (i) => [0, .07, .12][i] ?? i * .06 }, 1.1);
        }
        if (rules.length) {
            tl.fromTo(rules,
                { scaleY: 0 },
                { scaleY: 1, duration: .6, ease: EASE.draw, stagger: .08 }, 1.25);
        }

        return tl;
    }

    /* ------------------------------------------------------------------
       2 · Conteo de las estadísticas (valores reales del HTML)
       ------------------------------------------------------------------ */
    function initStatsAnimation(section, tl) {
        const counters = toArray(section.querySelectorAll('[data-yz-count]'));

        counters.forEach((el, i) => {
            const target = parseInt(el.dataset.yzCount, 10);
            if (!Number.isFinite(target) || target <= 0) return;

            const counter = { value: 0 };
            // Números grandes: conteo pausado; números pequeños: conteo corto
            const duration = target > 10 ? 1.6 : .7;

            // Arranca en 0 cuando empieza la entrada (la banda todavía está oculta)
            tl.call(() => { el.textContent = '0'; }, null, 0);
            tl.to(counter, {
                value: target,
                duration,
                ease: 'power2.out',
                onUpdate: () => { el.textContent = String(Math.round(counter.value)); },
                onComplete: () => { el.textContent = String(target); } // siempre termina en el valor exacto
            }, 1.15 + i * .1);
        });
    }

    /* ------------------------------------------------------------------
       3 · Revelación al llegar a la sección (ScrollTrigger)
       ------------------------------------------------------------------ */
    function initScrollAnimations(section, tl) {
        if (window.ScrollTrigger) {
            ScrollTrigger.create({
                trigger: section,
                start: 'top 80%',
                once: true,
                onEnter: () => tl.play()
            });
        } else {
            tl.play();
        }
    }

    /* ------------------------------------------------------------------
       4 · Profundidad de la fotografía al hacer scroll (una sola capa)
       ------------------------------------------------------------------ */
    function initImageParallax(section) {
        const media = section.querySelector('.yz-about__media');
        const stage = section.querySelector('.yz-about__stage');
        if (!media || !stage || !window.ScrollTrigger) return;

        gsap.fromTo(media,
            { yPercent: -3, scale: 1 },
            {
                yPercent: 3,
                scale: 1.03,
                ease: 'none',
                scrollTrigger: { trigger: stage, start: 'top bottom', end: 'bottom top', scrub: .8 }
            });
    }

    /* ------------------------------------------------------------------
       5 · Parallax con el cursor (solo mouse; quickTo, sin tweens nuevos por evento)
       ------------------------------------------------------------------ */
    function initPointerParallax(section, intensity) {
        const photo = section.querySelector('.yz-about__photo');
        const fx    = section.querySelector('.yz-about__fx');
        if (!photo) return () => {};

        const photoX = gsap.quickTo(photo, 'x', { duration: 1.4, ease: 'power3' });
        const photoY = gsap.quickTo(photo, 'y', { duration: 1.4, ease: 'power3' });
        const fxX = fx ? gsap.quickTo(fx, 'x', { duration: 1.6, ease: 'power3' }) : null;
        const fxY = fx ? gsap.quickTo(fx, 'y', { duration: 1.6, ease: 'power3' }) : null;

        let rect = section.getBoundingClientRect();
        const measure = () => { rect = section.getBoundingClientRect(); };

        const onMove = (e) => {
            const nx = (e.clientX - rect.left) / rect.width - .5;   // -0.5 … 0.5
            const ny = (e.clientY - rect.top) / rect.height - .5;
            photoX(nx * -intensity * 2);   // máx. ±intensity px
            photoY(ny * -intensity * 1.2);
            if (fxX) { fxX(nx * intensity * 2.4); fxY(ny * intensity * 1.6); }
        };
        const onLeave = () => {
            photoX(0); photoY(0);
            if (fxX) { fxX(0); fxY(0); }
        };

        section.addEventListener('pointerenter', measure);
        section.addEventListener('pointermove', onMove, { passive: true });
        section.addEventListener('pointerleave', onLeave);

        return () => {
            section.removeEventListener('pointerenter', measure);
            section.removeEventListener('pointermove', onMove);
            section.removeEventListener('pointerleave', onLeave);
        };
    }

    /* ------------------------------------------------------------------
       6 · Ambiente (máx. 3): reflejo, burbuja y destello. Se pausan fuera de pantalla
       ------------------------------------------------------------------ */
    function initAmbient(section, tl) {
        const glint   = section.querySelector('.yz-about__glint span');
        const bubble  = section.querySelector('.yz-about__bubble');
        const sparkle = section.querySelector('.yz-about__sparkle');
        const loops = [];

        // Estado inicial de la decoración (entra después del contenido)
        if (bubble)  tl.fromTo(bubble,  { autoAlpha: 0, scale: .85 }, { autoAlpha: 1, scale: 1, duration: 1 }, 1.4);
        if (sparkle) tl.fromTo(sparkle, { autoAlpha: 0, scale: .6, rotate: -30 }, { autoAlpha: .9, scale: 1, rotate: 0, duration: .8 }, 1.5);

        if (glint) {
            // Un reflejo al revelarse la escena y luego, de vez en cuando
            tl.fromTo(glint, { xPercent: -100 }, { xPercent: 100, duration: 1.6, ease: 'power2.inOut' }, .5);
            loops.push(gsap.fromTo(glint,
                { xPercent: -100 },
                { xPercent: 100, duration: 1.8, ease: 'power2.inOut', repeat: -1, repeatDelay: 12, delay: 9, paused: true }));
        }
        if (bubble) {
            loops.push(gsap.to(bubble, {
                y: -14, x: 6, duration: 4.5, ease: 'sine.inOut', repeat: -1, yoyo: true, paused: true
            }));
        }
        if (sparkle) {
            loops.push(gsap.timeline({ repeat: -1, repeatDelay: 4, paused: true })
                .to(sparkle, { scale: .55, autoAlpha: .3, rotate: 25, duration: .9, ease: 'sine.inOut' })
                .to(sparkle, { scale: 1, autoAlpha: .9, rotate: 0, duration: .9, ease: 'sine.inOut' }));
        }

        const play  = () => loops.forEach((l) => l.play());
        const pause = () => loops.forEach((l) => l.pause());

        tl.eventCallback('onComplete', play);

        if (window.ScrollTrigger) {
            ScrollTrigger.create({
                trigger: section,
                start: 'top bottom',
                end: 'bottom top',
                onToggle: (self) => {
                    if (tl.progress() < 1) return;
                    self.isActive ? play() : pause();
                }
            });
        }
    }

    /* ------------------------------------------------------------------
       7 · Animaciones según dispositivo y preferencias
       ------------------------------------------------------------------ */
    function initResponsiveAnimations(section) {
        const mm = gsap.matchMedia();

        mm.add({
            motion:  '(prefers-reduced-motion: no-preference)',
            desktop: '(min-width: 1100px)',
            tablet:  '(min-width: 768px) and (max-width: 1099px)',
            mouse:   '(hover: hover) and (pointer: fine)'
        }, (context) => {
            const { motion, desktop, tablet, mouse } = context.conditions;

            // Movimiento reducido: contenido final, sin animaciones
            if (!motion) {
                restoreCounters(section);
                revealContent();
                return;
            }

            const tl = initTitleReveal(section);
            initStatsAnimation(section, tl);
            initAmbient(section, tl);

            // Los estados iniciales ya están aplicados: se libera el ocultamiento del <head>
            revealContent();

            initScrollAnimations(section, tl);
            initImageParallax(section);

            // Cursor: escritorio 8 px, tablet con mouse 4 px, táctil/celular nada
            let removePointer = null;
            if (mouse && (desktop || tablet)) {
                removePointer = initPointerParallax(section, desktop ? 8 : 4);
            }

            return () => {
                if (removePointer) removePointer();
                restoreCounters(section);
            };
        });

        return mm;
    }

    /* ------------------------------------------------------------------
       Inicio y destrucción
       ------------------------------------------------------------------ */
    function initAboutSection() {
        const section = document.querySelector(SECTION_SELECTOR);

        if (!section) { revealContent(); return; }
        if (state && state.section === section) return; // evita inicializar dos veces

        if (!window.gsap) {
            console.warn('[Yazka\'s · Nosotros] GSAP no está disponible: se muestra el contenido sin animaciones.');
            restoreCounters(section);
            revealContent();
            return;
        }
        if (window.ScrollTrigger) gsap.registerPlugin(ScrollTrigger);

        try {
            let mm = null;
            const ctx = gsap.context(() => {
                mm = initResponsiveAnimations(section);
            }, section);
            state = { section, ctx, mm };
        } catch (error) {
            console.error('[Yazka\'s · Nosotros] No se pudieron iniciar las animaciones:', error);
            destroyAboutSection();
            restoreCounters(section);
            revealContent();
        }
    }

    function destroyAboutSection() {
        if (!state) return;
        if (state.mm) state.mm.revert();
        if (state.ctx) state.ctx.revert();
        restoreCounters(state.section);
        revealContent();
        state = null;
    }

    // API mínima por si la sección se recarga dinámicamente
    window.YazkasAbout = { init: initAboutSection, destroy: destroyAboutSection };

    if (document.readyState === 'loading') {
        document.addEventListener('DOMContentLoaded', initAboutSection, { once: true });
    } else {
        initAboutSection();
    }
})();