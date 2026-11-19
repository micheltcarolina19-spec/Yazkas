// Yazka's · Vista "Servicios": año del pie + animaciones del encabezado (GSAP)
document.addEventListener('DOMContentLoaded', () => {
    const anio = document.getElementById('anio');
    if (anio) anio.textContent = new Date().getFullYear();

    const hero = document.querySelector('.svh');
    if (!hero) return;
    const mostrar = () => hero.classList.remove('is-loading');

    // Sin GSAP: se muestra todo sin animación
    if (!window.gsap) { mostrar(); return; }
    const conScroll = !!window.ScrollTrigger;
    if (conScroll) gsap.registerPlugin(ScrollTrigger);

    const q = gsap.utils.selector(hero);

    /* ---------- Preparación del DOM ---------- */
    // Partir "YAZKA'S" en letras
    const marca = hero.querySelector('.svh__brand-name');
    if (marca && !marca.dataset.split) {
        marca.innerHTML = [...marca.textContent.trim()]
            .map(c => `<span class="svh__char">${c}</span>`).join('');
        marca.dataset.split = '1';
    }

    // Partículas (pocas en móvil)
    const contPart = hero.querySelector('.svh__particles');
    if (contPart && !contPart.children.length) {
        const total = window.innerWidth < 768 ? 6 : 12;
        const frag = document.createDocumentFragment();
        for (let i = 0; i < total; i++) {
            const p = document.createElement('span');
            const t = gsap.utils.random(3, 7, 1);
            p.className = 'svh__dot';
            p.style.cssText = `left:${gsap.utils.random(2, 98)}%;top:${gsap.utils.random(15, 95)}%;width:${t}px;height:${t}px;`;
            frag.appendChild(p);
        }
        contPart.appendChild(frag);
    }

    const mm = gsap.matchMedia();

    /* ---------- Movimiento reducido: sin animaciones ---------- */
    mm.add('(prefers-reduced-motion: reduce)', () => { mostrar(); });

    /* ---------- Entrada + animaciones en bucle ---------- */
    mm.add('(prefers-reduced-motion: no-preference)', (ctx) => {
        const bucles = [];
        let observador;

        const tl = gsap.timeline({ defaults: { ease: 'power3.out', duration: .9 } });

        tl.from(q('.svh__glow'),         { autoAlpha: 0, scale: .6, duration: 1.6, stagger: .2 }, 0)
            .from(q('.svh__panel'),        { xPercent: 100, duration: 1.3, ease: 'expo.out' }, .1)
            .from(q('.svh__char'),         { autoAlpha: 0, x: 50, stagger: .06, duration: .8, ease: 'back.out(1.6)' }, .55)
            .from(q('.svh__brand-tag'),    { autoAlpha: 0, y: 40 }, .9)
            .from(q('.svh__eyebrow-bar'),  { scaleY: 0, transformOrigin: '50% 0%', duration: .6 }, .25)
            .from(q('.svh__eyebrow-txt'),  { autoAlpha: 0, x: -24 }, .35)
            .from(q('.svh__w > span'),     { yPercent: 115, stagger: .08, duration: 1, ease: 'expo.out' }, .4)
            .from(q('.svh__script'),       { autoAlpha: 0, scale: .3, rotation: -14, duration: 1.1, ease: 'elastic.out(1, .55)' }, .9)
            .from(q('.svh__underline path'), { strokeDashoffset: 1, duration: .9, ease: 'power2.inOut' }, 1.25)
            .from(q('.svh__lead'),         { autoAlpha: 0, y: 24 }, 1)
            .from(q('.svh__btn'),          { autoAlpha: 0, y: 24, stagger: .12 }, 1.1)
            .from(q('.svh-card'),          {
                autoAlpha: 0, y: 50, rotationX: -35, transformPerspective: 800,
                transformOrigin: '50% 100%', stagger: .1, duration: 1
            }, 1.25)
            .from(q('.svh__halo'),         { autoAlpha: 0, scale: .5, duration: 1.4 }, .3)
            .from(q('.svh__ring'),         { autoAlpha: 0, scale: .8, duration: 1.4 }, .5)
            .from(q('.svh__pet-img'),      { autoAlpha: 0, y: 90, scale: 1.08, duration: 1.4, ease: 'expo.out' }, .45)
            .from(q('.svh__badge'),        { autoAlpha: 0, scale: .6, y: 20, stagger: .2, ease: 'back.out(1.8)' }, 1.4)
            .from(q('.svh__float'),        { autoAlpha: 0, scale: 0, stagger: .08, ease: 'back.out(2)' }, 1.2);

        // Los estados iniciales ya están puestos: se puede quitar el ocultamiento
        mostrar();

        const iniciarBucles = () => {
            // Mascota flotando
            bucles.push(gsap.to(q('.svh__pet-img'), { y: -14, duration: 3, ease: 'sine.inOut', yoyo: true, repeat: -1 }));
            // Anillo girando
            bucles.push(gsap.to(q('.svh__ring'), { rotation: 360, duration: 40, ease: 'none', repeat: -1 }));
            // Brillos respirando
            q('.svh__glow').forEach((g, i) => bucles.push(
                gsap.to(g, { scale: 1.12, duration: 6 + i * 1.5, ease: 'sine.inOut', yoyo: true, repeat: -1 })
            ));
            // Insignias
            q('.svh__badge').forEach((b, i) => bucles.push(
                gsap.to(b, { y: -10, duration: 2.6 + i * .7, ease: 'sine.inOut', yoyo: true, repeat: -1 })
            ));
            // Corazón latiendo
            bucles.push(gsap.to(q('.svh__badge-ico--pink .hm-ico'), {
                scale: 1.25, duration: .25, ease: 'power1.inOut', yoyo: true, repeat: -1, repeatDelay: 1.6
            }));
            // Iconos del fondo
            q('.svh__float').forEach(f => bucles.push(
                gsap.to(f, {
                    y: gsap.utils.random(-22, -10), rotation: gsap.utils.random(-15, 15),
                    duration: gsap.utils.random(3, 5), ease: 'sine.inOut', yoyo: true, repeat: -1
                })
            ));
            // Partículas subiendo
            q('.svh__dot').forEach(d => bucles.push(
                gsap.to(d, {
                    keyframes: {
                        '0%':   { y: 20, autoAlpha: 0 },
                        '30%':  { autoAlpha: .8 },
                        '100%': { y: gsap.utils.random(-160, -80), autoAlpha: 0 }
                    },
                    duration: gsap.utils.random(5, 9), ease: 'none', repeat: -1, delay: gsap.utils.random(0, 4)
                })
            ));
            // Destello en el botón principal
            const brillo = q('.svh__shine');
            gsap.set(brillo, { skewX: -20 });
            bucles.push(gsap.fromTo(brillo, { xPercent: -130 }, {
                xPercent: 280, duration: 1.1, ease: 'power2.inOut', repeat: -1, repeatDelay: 3.5
            }));

            // Pausar los bucles cuando el encabezado no se ve (ahorra CPU/GPU)
            observador = new IntersectionObserver(([e]) => {
                bucles.forEach(t => (e.isIntersecting ? t.resume() : t.pause()));
            });
            observador.observe(hero);
        };

        tl.call(() => ctx.add(iniciarBucles), null, '-=0.5');

        return () => { if (observador) observador.disconnect(); };
    });

    /* ---------- Solo escritorio con mouse: parallax, tilt, botones magnéticos y scroll ---------- */
    mm.add('(prefers-reduced-motion: no-preference) and (min-width: 992px) and (pointer: fine)', () => {
        // Parallax con el mouse (solo transform, con quickTo)
        const capas = q('[data-depth]').map(el => ({
            d: parseFloat(el.dataset.depth) || .2,
            x: gsap.quickTo(el, 'x', { duration: .9, ease: 'power3' }),
            y: gsap.quickTo(el, 'y', { duration: .9, ease: 'power3' })
        }));
        const mover = (e) => {
            const nx = e.clientX / window.innerWidth - .5;
            const ny = e.clientY / window.innerHeight - .5;
            capas.forEach(c => { c.x(nx * c.d * -70); c.y(ny * c.d * -70); });
        };
        const salir = () => capas.forEach(c => { c.x(0); c.y(0); });
        hero.addEventListener('pointermove', mover);
        hero.addEventListener('pointerleave', salir);

        // Tarjetas con inclinación 3D
        const limpiezas = [];
        q('.svh-card').forEach(card => {
            gsap.set(card, { transformPerspective: 700 });
            const rx = gsap.quickTo(card, 'rotationX', { duration: .5, ease: 'power3' });
            const ry = gsap.quickTo(card, 'rotationY', { duration: .5, ease: 'power3' });
            const img = card.querySelector('.svh-card__ico');            const ix = gsap.quickTo(img, 'x', { duration: .5, ease: 'power3' });
            let r;
            const entrar = () => { r = card.getBoundingClientRect(); };
            const moverC = (e) => {
                if (!r) entrar();
                const px = (e.clientX - r.left) / r.width - .5;
                const py = (e.clientY - r.top) / r.height - .5;
                rx(-py * 16); ry(px * 16); ix(px * 10);
            };
            const salirC = () => { rx(0); ry(0); ix(0); r = null; };
            card.addEventListener('pointerenter', entrar);
            card.addEventListener('pointermove', moverC);
            card.addEventListener('pointerleave', salirC);
            limpiezas.push(() => {
                card.removeEventListener('pointerenter', entrar);
                card.removeEventListener('pointermove', moverC);
                card.removeEventListener('pointerleave', salirC);
            });
        });

        // Botones magnéticos
        q('.svh__btn').forEach(btn => {
            const bx = gsap.quickTo(btn, 'x', { duration: .4, ease: 'power3' });
            const by = gsap.quickTo(btn, 'y', { duration: .4, ease: 'power3' });
            let r;
            const entrar = () => { r = btn.getBoundingClientRect(); };
            const moverB = (e) => {
                if (!r) entrar();
                bx((e.clientX - (r.left + r.width / 2)) * .25);
                by((e.clientY - (r.top + r.height / 2)) * .35);
            };
            const salirB = () => { bx(0); by(0); r = null; };
            btn.addEventListener('pointerenter', entrar);
            btn.addEventListener('pointermove', moverB);
            btn.addEventListener('pointerleave', salirB);
            limpiezas.push(() => {
                btn.removeEventListener('pointerenter', entrar);
                btn.removeEventListener('pointermove', moverB);
                btn.removeEventListener('pointerleave', salirB);
            });
        });

        // Parallax al hacer scroll
        if (conScroll) {
            gsap.timeline({
                scrollTrigger: { trigger: hero, start: 'top top', end: 'bottom top', scrub: .6 }
            })
                .to(q('.svh__media'), { yPercent: 12, ease: 'none' }, 0)
                .to(q('.svh__text'),  { y: -60, autoAlpha: .25, ease: 'none' }, 0)
                .to(q('.svh__brand'), { yPercent: -18, ease: 'none' }, 0);
        }

        return () => {
            hero.removeEventListener('pointermove', mover);
            hero.removeEventListener('pointerleave', salir);
            limpiezas.forEach(fn => fn());
        };
    });
});