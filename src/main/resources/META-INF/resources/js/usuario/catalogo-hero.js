/* ==========================================================
   Yazka's · Catálogo · Animaciones del hero con GSAP
   - Solo anima transform y opacity (no recalcula el layout).
   - Respeta "reducir movimiento" del sistema.
   - Los brillos se pausan cuando el hero no se ve.
   - El efecto con el mouse solo se activa en escritorio.
   ========================================================== */
(() => {
    const root = document.documentElement;
    const hero = document.querySelector('[data-hero-root]');
    const mostrar = () => root.classList.remove('ct-anim');
    const reduceMotion = matchMedia('(prefers-reduced-motion: reduce)').matches;

    if (!hero) { mostrar(); return; }

    /* ---------- Accesos rápidos: activan el filtro y bajan al catálogo ---------- */
    hero.querySelectorAll('[data-ir-grupo]').forEach((btn) => {
        btn.addEventListener('click', () => {
            document.querySelector(`.ct-pill[data-grupo="${btn.dataset.irGrupo}"]`)?.click();
            document.querySelector('.ct-filters')?.scrollIntoView({
                behavior: reduceMotion ? 'auto' : 'smooth',
                block: 'start'
            });
        });
    });

    /* Sin GSAP (CDN caído, etc.): se muestra todo tal cual */
    if (!window.gsap) { mostrar(); return; }
    if (window.ScrollTrigger) gsap.registerPlugin(ScrollTrigger);

    const q = gsap.utils.selector(hero);
    const bg      = q('.ct-hero__bg')[0];
    const img     = q('[data-hero-bg]')[0];
    const fx      = q('.ct-hero__fx')[0];
    const sparks  = q('[data-hero-spark]');
    const lines   = q('.ct-line__in');
    const swoosh  = q('.ct-hero__swoosh')[0];
    const path    = q('.ct-hero__swoosh path')[0];
    const sub     = q('.ct-hero__subtitle')[0];
    const script  = q('.ct-hero__script')[0];
    const lead    = q('.ct-hero__lead')[0];
    const search  = q('.ct-search')[0];
    const quick   = q('.ct-quick')[0];
    const items   = q('.ct-quick__item');

    const mm = gsap.matchMedia();

    mm.add({
        motion: '(prefers-reduced-motion: no-preference)',
        desktop: '(min-width: 861px) and (hover: hover) and (pointer: fine)'
    }, (ctx) => {
        const { motion, desktop } = ctx.conditions;

        if (!motion) { mostrar(); return; }

        /* ---------- 1. Entrada ---------- */
        const tl = gsap.timeline({ defaults: { ease: 'power3.out' } });

        tl.fromTo(img,
            { autoAlpha: 0, scale: 1.14 },
            { autoAlpha: 1, scale: 1.04, duration: 1.8, ease: 'power2.out' }, 0)

            .fromTo(lines,
                { autoAlpha: 1, yPercent: 115, rotate: 3 },
                { yPercent: 0, rotate: 0, duration: 1.05, stagger: .12, ease: 'power4.out' }, .25)

            .fromTo(swoosh, { autoAlpha: 1 }, { autoAlpha: 1, duration: 0 }, .75)
            .fromTo(path,
                { strokeDashoffset: 1 },
                { strokeDashoffset: 0, duration: 1.1, ease: 'power2.inOut' }, .75)

            .fromTo([sub, lead],
                { autoAlpha: 0, y: 24 },
                { autoAlpha: 1, y: 0, duration: .9, stagger: .12 }, .85)

            .fromTo(script,
                { scale: .6, rotate: -8 },
                { scale: 1, rotate: 0, duration: .9, ease: 'back.out(2.2)' }, 1.05)

            .fromTo(search,
                { autoAlpha: 0, y: 22, scale: .97 },
                { autoAlpha: 1, y: 0, scale: 1, duration: .9 }, 1.15)

            .fromTo(quick,
                { autoAlpha: 0, y: 30 },
                { autoAlpha: 1, y: 0, duration: .9 }, 1.3)
            .fromTo(items,
                { opacity: 0, y: 12 },
                { opacity: 1, y: 0, duration: .6, stagger: .08 }, 1.45)

            .fromTo(sparks,
                { autoAlpha: 0, scale: 0, rotate: -90 },
                { autoAlpha: 1, scale: 1, rotate: 0, duration: .8, stagger: .12, ease: 'back.out(2.5)' }, 1.1)

            .add(() => { mostrar(); ambient.play(); });

        /* ---------- 2. Brillos en bucle (suaves y baratos) ---------- */
        const ambient = gsap.timeline({ paused: true, repeat: -1, yoyo: true });
        sparks.forEach((s, i) => {
            ambient.to(s, {
                scale: gsap.utils.random(.55, .75),
                opacity: .45,
                rotate: i % 2 ? 45 : -45,
                y: gsap.utils.random(-8, 8),
                duration: gsap.utils.random(1.4, 2.2),
                ease: 'sine.inOut'
            }, i * .35);
        });

        /* ---------- 3. Parallax al hacer scroll + pausa fuera de pantalla ---------- */
        if (window.ScrollTrigger) {
            gsap.to(bg, {
                yPercent: 12,
                ease: 'none',
                scrollTrigger: { trigger: hero, start: 'top top', end: 'bottom top', scrub: .6 }
            });

            ScrollTrigger.create({
                trigger: hero,
                start: 'top bottom',
                end: 'bottom top',
                onToggle: (self) => {
                    if (tl.progress() < 1) return;
                    self.isActive ? ambient.play() : ambient.pause();
                }
            });
        }

        /* ---------- 4. Movimiento sutil con el mouse (solo escritorio) ---------- */
        if (!desktop) return;

        const imgX = gsap.quickTo(img, 'x', { duration: 1.2, ease: 'power3' });
        const imgY = gsap.quickTo(img, 'y', { duration: 1.2, ease: 'power3' });
        const fxX  = gsap.quickTo(fx,  'x', { duration: 1.4, ease: 'power3' });
        const fxY  = gsap.quickTo(fx,  'y', { duration: 1.4, ease: 'power3' });

        let rect = hero.getBoundingClientRect();
        const medir = () => { rect = hero.getBoundingClientRect(); };

        const mover = (e) => {
            const nx = (e.clientX - rect.left) / rect.width - .5;
            const ny = (e.clientY - rect.top) / rect.height - .5;
            imgX(nx * -18); imgY(ny * -12);
            fxX(nx * 26);   fxY(ny * 18);
        };
        const soltar = () => { imgX(0); imgY(0); fxX(0); fxY(0); };

        hero.addEventListener('pointerenter', medir);
        hero.addEventListener('pointermove', mover, { passive: true });
        hero.addEventListener('pointerleave', soltar);
        window.addEventListener('resize', medir, { passive: true });

        /* Limpieza cuando cambia la media query */
        return () => {
            hero.removeEventListener('pointerenter', medir);
            hero.removeEventListener('pointermove', mover);
            hero.removeEventListener('pointerleave', soltar);
            window.removeEventListener('resize', medir);
        };
    });
})();