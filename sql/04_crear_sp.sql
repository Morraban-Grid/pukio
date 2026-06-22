-- ============================================================
-- PUKIO - Sistema POS
-- Script 04: Creacion de Procedimientos Almacenados y Triggers (PL/SQL)
-- Base de Datos: Oracle 21c XE
-- ============================================================

ALTER SESSION SET CURRENT_SCHEMA = PUKIO_DB;

-- 1. PROCEDIMIENTO ALMACENADO PARA REGISTRAR CABECERA DE VENTA
-- Genera el correlativo y registra la venta en la tabla VENTAS.
-- Retorna el ID de la venta generada y el numero de comprobante.
CREATE OR REPLACE PROCEDURE SP_REGISTRAR_VENTA (
    p_tipo_comprobante IN VARCHAR2,
    p_id_cliente IN NUMBER,
    p_id_usuario IN NUMBER,
    p_subtotal IN NUMBER,
    p_igv IN NUMBER,
    p_descuento IN NUMBER,
    p_total IN NUMBER,
    p_metodo_pago IN VARCHAR2,
    p_id_venta OUT NUMBER,
    p_num_comprobante OUT VARCHAR2
) AS
    v_seq_val NUMBER;
BEGIN
    -- Generar el numero de comprobante de manera correlativa
    SELECT SEQ_COMPROBANTE.NEXTVAL INTO v_seq_val FROM DUAL;
    
    IF p_tipo_comprobante = 'FACTURA' THEN
        p_num_comprobante := 'FAC-' || LPAD(v_seq_val, 6, '0');
    ELSE
        p_num_comprobante := 'BOL-' || LPAD(v_seq_val, 6, '0');
    END IF;

    -- Insertar el registro de cabecera
    INSERT INTO VENTAS (
        NUMERO_COMPROBANTE,
        TIPO_COMPROBANTE,
        ID_CLIENTE,
        ID_USUARIO,
        FECHA_VENTA,
        SUBTOTAL,
        IGV,
        DESCUENTO,
        TOTAL,
        METODO_PAGO,
        ESTADO
    ) VALUES (
        p_num_comprobante,
        p_tipo_comprobante,
        p_id_cliente,
        p_id_usuario,
        SYSDATE,
        p_subtotal,
        p_igv,
        p_descuento,
        p_total,
        p_metodo_pago,
        'COMPLETADA'
    ) RETURNING ID_VENTA INTO p_id_venta;
    
EXCEPTION
    WHEN OTHERS THEN
        RAISE_APPLICATION_ERROR(-20002, 'Error al registrar la cabecera de la venta: ' || SQLERRM);
END;
/

-- 2. TRIGGER PARA VALIDACIÓN DE STOCK Y ACTUALIZACIÓN AUTOMÁTICA
-- Se ejecuta antes de insertar cada linea de detalle de venta.
-- Lanza excepcion personalizada si no hay stock suficiente.
CREATE OR REPLACE TRIGGER TRG_DETALLE_VENTA_STOCK
BEFORE INSERT ON DETALLE_VENTA
FOR EACH ROW
DECLARE
    v_stock_actual NUMBER;
    v_nombre_prod VARCHAR2(150);
BEGIN
    -- Obtener stock actual y nombre del producto
    SELECT STOCK, NOMBRE INTO v_stock_actual, v_nombre_prod
    FROM PRODUCTOS 
    WHERE ID_PRODUCTO = :NEW.ID_PRODUCTO;
    
    -- Validar si hay stock suficiente
    IF v_stock_actual < :NEW.CANTIDAD THEN
        RAISE_APPLICATION_ERROR(-20001, 'Stock insuficiente para el producto: ' || v_nombre_prod || '. Solicitado: ' || :NEW.CANTIDAD || ', Disponible: ' || v_stock_actual);
    END IF;
    
    -- Actualizar el stock disminuyendo la cantidad vendida
    UPDATE PRODUCTOS 
    SET STOCK = STOCK - :NEW.CANTIDAD 
    WHERE ID_PRODUCTO = :NEW.ID_PRODUCTO;
END;
/

COMMIT;
