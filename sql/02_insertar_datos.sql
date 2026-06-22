-- ============================================================
-- PUKIO - Sistema POS
-- Script 02: Datos Iniciales
-- Contrasena admin: admin123  (BCrypt, factor de costo 12)
-- Contrasena cajero: cajero123 (BCrypt, factor de costo 12)
-- ============================================================

ALTER SESSION SET CURRENT_SCHEMA = PUKIO_DB;

-- Usuario Administrador
INSERT INTO USUARIOS (USERNAME, PASSWORD_HASH, NOMBRE, ROL)
VALUES ('admin',
        '$2a$12$4kM.L65sblkFeF9FY4bLGOCDxY43JWen3u61jbG3E6KZNwGsPFJGi',
        'Administrador Principal', 'ADMIN');

-- Usuario Cajero
INSERT INTO USUARIOS (USERNAME, PASSWORD_HASH, NOMBRE, ROL)
VALUES ('cajero',
        '$2a$12$Ef9CctqgDGzTPFJaoD4Zi.1fW3i5NHAyoJal1d98aPMaf5UN785K.',
        'Cajero General', 'CAJERO');

-- Categorias
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Bebidas',    'Gaseosas, jugos, aguas y bebidas en general');
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Lacteos',    'Leches, yogures, quesos y derivados');
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Snacks',     'Papas, galletas, chocolates y golosinas');
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Limpieza',   'Detergentes, desinfectantes y articulos de limpieza');
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Higiene',    'Jabon, champu, pasta dental y cuidado personal');
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Panaderia',  'Pan, pasteles y productos de panaderia');
INSERT INTO CATEGORIAS (NOMBRE, DESCRIPCION) VALUES ('Abarrotes',  'Arroz, azucar, aceite, fideos y otros');

-- Proveedor de ejemplo
INSERT INTO PROVEEDORES (RUC, NOMBRE, CONTACTO, TELEFONO, CORREO)
VALUES ('20100000001', 'Distribuidora El Sol S.A.C.', 'Juan Perez', '999888777', 'ventas@elsol.com');

INSERT INTO PROVEEDORES (RUC, NOMBRE, CONTACTO, TELEFONO, CORREO)
VALUES ('20100000002', 'Alicorp Peru S.A.A.', 'Maria Lopez', '998877665', 'contacto@alicorp.com');

-- Cliente Generico (para ventas sin cliente registrado)
INSERT INTO CLIENTES (TIPO_DOC, NUMERO_DOC, NOMBRE)
VALUES ('DNI', '00000000', 'CLIENTE GENERICO');

-- Productos de ejemplo
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('BEB001', 'Inca Kola 600ml',      1.80, 3.00, 100, 10, 1);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('BEB002', 'Coca Cola 1.5L',       3.20, 5.50, 80,  10, 1);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('LAC001', 'Leche Gloria 1L',      2.90, 4.50, 60,  10, 2);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('SNK001', 'Papas Lays 45g',       1.20, 2.00, 120, 20, 3);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('SNK002', 'Chocolate Sublime',    0.90, 1.50, 200, 30, 3);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('ABA001', 'Arroz Costeno 1kg',   2.50, 4.00,  50,  5, 7);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('ABA002', 'Aceite Primor 1L',    5.20, 8.00,  40,  5, 7);
INSERT INTO PRODUCTOS (CODIGO, NOMBRE, PRECIO_COMPRA, PRECIO_VENTA, STOCK, STOCK_MINIMO, ID_CATEGORIA)
VALUES ('HIG001', 'Jabon Palmolive 90g', 1.00, 1.80,  90, 15, 5);

COMMIT;
