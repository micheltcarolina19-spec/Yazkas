-- ============================================================
-- Datos de DEMO para el dashboard (PostgreSQL, base "yazkas")
-- Ejecutar UNA sola vez (si lo repites se duplican los datos).
-- Las fechas son relativas a hoy, así que siempre caen en el periodo actual.
-- ============================================================

-- ---------- Productos ----------
INSERT INTO productos (nombre, marca, categoria, descripcion, tipo, precio_base, precio_venta, foto_producto, stock) VALUES
                                                                                                                         ('Alimento Premium Adultos', 'Pro Plan',  'CONCENTRADOS', 'Alimento para perros adultos', 'PREMIUM',   90000, 120000, NULL, 40),
                                                                                                                         ('Pelota Interactiva',       'Kong',      'JUGUETES',     'Pelota resistente',            'CORRIENTE', 12000,  25000, NULL, 60),
                                                                                                                         ('Shampoo para Perros',      'Pet Clean', 'COSMETICOS',   'Shampoo hipoalergénico',       'CORRIENTE', 10000,  22000, NULL, 35),
                                                                                                                         ('Snack Dental',             'Pedigree',  'SNACKS',       'Snack para higiene dental',    'CORRIENTE',  4000,   9000, NULL, 80);

-- ---------- Ventas y sus detalles ----------
-- El total de cada venta = suma de los subtotales de sus detalles
DO $$
DECLARE
p_alimento BIGINT; p_pelota BIGINT; p_shampoo BIGINT; p_snack BIGINT;
    v1 BIGINT; v2 BIGINT; v3 BIGINT;
BEGIN
SELECT id INTO p_alimento FROM productos WHERE nombre = 'Alimento Premium Adultos' ORDER BY id DESC LIMIT 1;
SELECT id INTO p_pelota   FROM productos WHERE nombre = 'Pelota Interactiva'       ORDER BY id DESC LIMIT 1;
SELECT id INTO p_shampoo  FROM productos WHERE nombre = 'Shampoo para Perros'      ORDER BY id DESC LIMIT 1;
SELECT id INTO p_snack    FROM productos WHERE nombre = 'Snack Dental'             ORDER BY id DESC LIMIT 1;

-- Venta 1 (hoy): 2 alimentos + 1 shampoo = 262.000
INSERT INTO ventas (fecha_venta, total_venta) VALUES (CURRENT_DATE, 262000) RETURNING id INTO v1;
INSERT INTO detalle_venta (venta_id, producto_id, subtotal, cantidad) VALUES
                                                                          (v1, p_alimento, 240000, 2),
                                                                          (v1, p_shampoo,   22000, 1);

-- Venta 2 (ayer): 3 pelotas + 2 shampoo = 119.000
INSERT INTO ventas (fecha_venta, total_venta) VALUES (CURRENT_DATE - 1, 119000) RETURNING id INTO v2;
INSERT INTO detalle_venta (venta_id, producto_id, subtotal, cantidad) VALUES
                                                                          (v2, p_pelota,  75000, 3),
                                                                          (v2, p_shampoo, 44000, 2);

-- Venta 3 (hace 2 días): 1 alimento + 5 snacks + 2 pelotas = 215.000
INSERT INTO ventas (fecha_venta, total_venta) VALUES (CURRENT_DATE - 2, 215000) RETURNING id INTO v3;
INSERT INTO detalle_venta (venta_id, producto_id, subtotal, cantidad) VALUES
                                                                          (v3, p_alimento, 120000, 1),
                                                                          (v3, p_snack,     45000, 5),
                                                                          (v3, p_pelota,    50000, 2);
END $$;

-- ---------- Gastos ----------
INSERT INTO gastos (nombre, monto, fecha_gasto, categoria) VALUES
                                                               ('Compra de insumos de baño',  180000, CURRENT_DATE,     'INSUMOS'),
                                                               ('Recibo de agua y luz',        95000, CURRENT_DATE - 1, 'SERVICIOS_PUBLICOS'),
                                                               ('Arriendo del local',         400000, CURRENT_DATE - 2, 'ALQUILER_LOCAL'),
                                                               ('Mantenimiento de secador',    60000, CURRENT_DATE - 3, 'MANTENIMIENTO_EQUIPOS'),
                                                               ('Pago auxiliar',              150000, CURRENT_DATE - 3, 'PERSONAL');