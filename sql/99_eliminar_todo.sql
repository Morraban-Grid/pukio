-- ============================================================
-- PUKIO - Sistema POS
-- Script 99: Limpieza total (Drop Tables y Sequences)
-- Base de Datos: Oracle 21c XE
-- IMPORTANTE: Ejecutar este script en la misma cuenta o cambiar el
--             esquema al cual pertenecen los objetos a eliminar.
-- ============================================================

ALTER SESSION SET CURRENT_SCHEMA = PUKIO_DB;

-- Eliminar tablas con llaves foraneas usando CASCADE CONSTRAINTS
DROP TABLE DETALLE_VENTA CASCADE CONSTRAINTS;
DROP TABLE VENTAS CASCADE CONSTRAINTS;
DROP TABLE CLIENTES CASCADE CONSTRAINTS;
DROP TABLE PRODUCTOS CASCADE CONSTRAINTS;
DROP TABLE PROVEEDORES CASCADE CONSTRAINTS;
DROP TABLE CATEGORIAS CASCADE CONSTRAINTS;
DROP TABLE USUARIOS CASCADE CONSTRAINTS;

-- Eliminar secuencia
DROP SEQUENCE SEQ_COMPROBANTE;

-- Eliminar tablas del DWH (Si se requiere limpiar DWH cambiar esquema antes)
-- ALTER SESSION SET CURRENT_SCHEMA = PUKIO_DWH;
-- DROP TABLE DWH_VENTAS CASCADE CONSTRAINTS;
-- DROP TABLE CROSSTAB_VENTAS CASCADE CONSTRAINTS;

COMMIT;
