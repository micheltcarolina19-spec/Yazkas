/* ==========================================================
   Yazka's · Contáctanos · Animaciones (GSAP)
   - Entradas con ease-out fuerte: arrancan rápido y frenan suave.
   - Nada aparece desde escala 0: todo parte de una escala visible + opacidad.
   - Las tarjetas de contacto (lo importante) están listas en menos de 1.5 s.
   - Movimiento reducido: solo fundidos de opacidad, sin desplazamientos.
   - Los bucles decorativos se pausan cuando el encabezado no se ve.
   ========================================================== */
(() => {
    const root = document.documentElement;
    const hero = document.querySelector('[data-hero-root]');
    const mostrar = () => root.classList.remove('ct-anim');

    if (!hero || !window.gsap) { mostrar(); return; }
    if (window.ScrollTrigger) gsap.registerPlugin(ScrollTrigger);

    const $  = (s) => hero.querySelector(s);
    const $$ = (s) => Array.from(hero.querySelectorAll(s));

    const el = {
        bg:        $('.ct-hero__bg'),
        img:       $('[data-hero-bg]'),
        glint:     $('.ct-hero__glint'),
        fx:        $('.ct-hero__fx'),
        sparks:    $$('[data-hero-spark]'),
        paw:       $('[data-hero-paw]'),
        eyebrow:   $('.ct-eyebrow'),
        eyePaw:    $('.ct-eyebrow .hm-ico'),
        lines:     $$('.ct-line__in'),
        script:    $('.ct-hero__script'),
        swoosh:    $('.ct-hero__swoosh'),
        swooshP:   $('.ct-hero__swoosh path'),
        doodle:    $('.ct-hero__doodle'),
        doodleP:   $$('.ct-hero__doodle path'),
        lead:      $('.ct-hero__lead'),
        cards:     $$('.ct-quick > li'),
        waIcon:    $('.ct-quick__card--wa .ct-quick__icon'),
        badge:     $('.ct-hero__badge'),
        badgeLove: $('.ct-hero__badge-heart')
    };

    /* Bloques de más abajo (equipo y mapa) que aparecen al hacer scroll */
    const bloques = Array.from(document.querySelectorAll('.ct-admin, .ct-place'));

    const mm = gsap.matchMedia();

    mm.add({
        motion: '(prefers-reduced-motion: no-preference)',
        reduce: '(prefers-reduced-motion: reduce)',
        desktop: '(min-width: 861px) and (hover: hover) and (pointer: fine)'
    }, (ctx) => {
        const { motion, desktop } = ctx.conditions;

        /* ======================================================
           MOVIMIENTO REDUCIDO: menos animación, no cero.
           Solo un fundido de opacidad corto, sin desplazamientos.
           ====================================================== */
        if (!motion) {
            gsap.set([el.swooshP, ...el.doodleP].filter(Boolean), { strokeDashoffset: 0 });
            gsap.fromTo(
                [el.img, el.eyebrow, ...el.lines, el.script, el.swoosh, el.doodle, el.lead, ...el.cards, el.badge, el.paw, ...el.sparks].filter(Boolean),
                { autoAlpha: 0 },
                { autoAlpha: (i, t) => (t === el.paw ? .7 : 1), duration: .4, ease: 'power1.out', stagger: .02, onStart: mostrar }
            );
            return;
        }

        /* ======================================================
           EFECTOS EN BUCLE (decorativos, se crean pausados)
           ====================================================== */
        const loops = [];

        // Destello que cruza el espejo de vez en cuando (no constante)
        loops.push(gsap.fromTo(el.glint,
            { xPercent: -100 },
            { xPercent: 100, duration: 1.6, ease: 'power2.inOut', repeat: -1, repeatDelay: 10, delay: 8, paused: true }));

        // Brillos que titilan suavemente
        const twinkle = gsap.timeline({ paused: true, repeat: -1, yoyo: true });
        el.sparks.forEach((s, i) => {
            twinkle.to(s, {
                scale: gsap.utils.random(.6, .8),
                opacity: .45,
                rotate: i % 2 ? 30 : -30,
                duration: gsap.utils.random(1.4, 2.1),
                ease: 'sine.inOut'
            }, i * .35);
        });
        loops.push(twinkle);

        // Corazón de la etiqueta: doble latido y pausa larga
        if (el.badgeLove) {
            loops.push(gsap.timeline({ paused: true, repeat: -1, repeatDelay: 2.2 })
                .to(el.badgeLove, { scale: 1.18, duration: .14, ease: 'power2.out' })
                .to(el.badgeLove, { scale: 1, duration: .2, ease: 'power2.out' })
                .to(el.badgeLove, { scale: 1.1, duration: .12, ease: 'power2.out' })
                .to(el.badgeLove, { scale: 1, duration: .3, ease: 'power2.out' }));
        }

        // Etiqueta y corazón dibujado flotan muy suave
        if (el.badge)  loops.push(gsap.to(el.badge,  { y: -6, duration: 2.8, ease: 'sine.inOut', repeat: -1, yoyo: true, paused: true }));
        if (el.doodle) loops.push(gsap.to(el.doodle, { rotate: 6, y: -2, duration: 2, ease: 'sine.inOut', repeat: -1, yoyo: true, paused: true }));

        const playLoops  = () => loops.forEach((l) => l.play());
        const pauseLoops = () => loops.forEach((l) => l.pause());

        /* ======================================================
           ENTRADA: ~1.7 s en total, tarjetas listas a ~1.4 s
           ====================================================== */
        const tl = gsap.timeline({ defaults: { ease: 'expo.out' } });

        // Foto: zoom suave hacia atrás + destello inicial
        tl.fromTo(el.img,
            { autoAlpha: 0, scale: 1.1 },
            { autoAlpha: 1, scale: 1.04, duration: 1.2, ease: 'power2.out' }, 0)
            .fromTo(el.glint,
                { xPercent: -100 },
                { xPercent: 100, duration: 1.4, ease: 'power2.inOut' }, .3)

            // Etiqueta "Contáctanos" y su huellita
            .fromTo(el.eyebrow,
                { autoAlpha: 0, y: 10, scale: .92 },
                { autoAlpha: 1, y: 0, scale: 1, duration: .5 }, .15)
            .fromTo(el.eyePaw,
                { rotate: -40, scale: .6 },
                { rotate: 0, scale: 1, duration: .6, ease: 'back.out(2)' }, .2)

            // Título: líneas que suben desde su máscara
            .fromTo(el.lines[0],
                { autoAlpha: 1, yPercent: 110, rotate: 2 },
                { yPercent: 0, rotate: 0, duration: .75, ease: 'power4.out' }, .25)
            .fromTo(el.script,
                { autoAlpha: 1, clipPath: 'inset(-30% 100% -30% -5%)' },
                { clipPath: 'inset(-30% -5% -30% -5%)', duration: .7, ease: 'power3.inOut' }, .42)
            .fromTo(el.lines[1],
                { autoAlpha: 1, yPercent: 110, rotate: 2 },
                { yPercent: 0, rotate: 0, duration: .75, ease: 'power4.out' }, .55)
            .set(el.swoosh, { autoAlpha: 1 }, .85)
            .fromTo(el.swooshP,
                { strokeDashoffset: 1 },
                { strokeDashoffset: 0, duration: .5, ease: 'power2.out' }, .85)

            // Corazón dibujado a mano con un pequeño rebote
            .set(el.doodle, { autoAlpha: 1 }, .95)
            .fromTo(el.doodleP,
                { strokeDashoffset: 1 },
                { strokeDashoffset: 0, duration: .45, stagger: .15, ease: 'power2.out' }, .95)
            .fromTo(el.doodle,
                { scale: .8 },
                { scale: 1, duration: .7, ease: 'elastic.out(1, .5)' }, 1.15)

            // Texto y tarjetas: retrasos cortos y no uniformes
            .fromTo(el.lead,
                { autoAlpha: 0, y: 16 },
                { autoAlpha: 1, y: 0, duration: .6 }, .6)
            .fromTo(el.cards,
                { autoAlpha: 0, y: 24, scale: .96 },
                { autoAlpha: 1, y: 0, scale: 1, duration: .6, ease: 'back.out(1.2)', stagger: (i) => [0, .06, .1][i] || i * .05 }, .75)

            // WhatsApp "suena" una sola vez
            .to(el.waIcon,
                { keyframes: { rotate: [0, -14, 12, -8, 5, 0] }, duration: .6, ease: 'none', clearProps: 'transform' }, 1.3)

            // Decoración: llega después del contenido, nunca lo bloquea
            .fromTo(el.sparks,
                { autoAlpha: 0, scale: .5, rotate: -45 },
                { autoAlpha: 1, scale: 1, rotate: 0, duration: .6, stagger: .08, ease: 'back.out(1.6)' }, .7)
            .fromTo(el.badge,
                { autoAlpha: 0, x: 40, scale: .92 },
                { autoAlpha: 1, x: 0, scale: 1, duration: .8, ease: 'back.out(1.4)' }, .9)
            .fromTo(el.paw,
                { autoAlpha: 0, scale: 1.25, rotate: -40 },
                { autoAlpha: .7, scale: 1, rotate: -18, duration: .45, ease: 'power3.out' }, 1.2)

            .add(() => { mostrar(); playLoops(); });

        /* ======================================================
           SCROLL: parallax de la foto, pausa de bucles
           y aparición del equipo y el mapa
           ====================================================== */
        if (window.ScrollTrigger) {
            gsap.to(el.bg, {
                yPercent: 10,
                ease: 'none',
                scrollTrigger: { trigger: hero, start: 'top top', end: 'bottom top', scrub: .6 }
            });

            ScrollTrigger.create({
                trigger: hero,
                start: 'top bottom',
                end: 'bottom top',
                onToggle: (self) => {
                    if (tl.progress() < 1) return;
                    self.isActive ? playLoops() : pauseLoops();
                }
            });

            // Se anima "desde" el estado oculto: si el JS falla, todo queda visible
            ScrollTrigger.batch(bloques, {
                start: 'top 88%',
                once: true,
                onEnter: (lote) => gsap.from(lote, {
                    autoAlpha: 0,
                    y: 24,
                    duration: .6,
                    ease: 'expo.out',
                    stagger: (i) => [0, .07, .12][i] || i * .05
                })
            });
        }

        /* ======================================================
           MOUSE: parallax suave con inercia (solo escritorio)
           ====================================================== */
        if (!desktop) return;

        const to = (t, p, d) => gsap.quickTo(t, p, { duration: d, ease: 'power3' });
        const imgX = to(el.img, 'x', 1.2), imgY = to(el.img, 'y', 1.2);
        const fxX  = to(el.fx,  'x', 1.4), fxY  = to(el.fx,  'y', 1.4);

        let rect = hero.getBoundingClientRect();
        const medir = () => { rect = hero.getBoundingClientRect(); };
        const mover = (e) => {
            const nx = (e.clientX - rect.left) / rect.width - .5;
            const ny = (e.clientY - rect.top) / rect.height - .5;
            imgX(nx * -14); imgY(ny * -8);
            fxX(nx * 20);   fxY(ny * 14);
        };
        const soltar = () => { imgX(0); imgY(0); fxX(0); fxY(0); };

        hero.addEventListener('pointerenter', medir);
        hero.addEventListener('pointermove', mover, { passive: true });
        hero.addEventListener('pointerleave', soltar);
        window.addEventListener('resize', medir, { passive: true });

        return () => {
            hero.removeEventListener('pointerenter', medir);
            hero.removeEventListener('pointermove', mover);
            hero.removeEventListener('pointerleave', soltar);
            window.removeEventListener('resize', medir);
        };
    });
})();