package com.pukio.appserver;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Aplicación principal del servidor de aplicaciones Pukio.
 * Entregable 2: Arquitectura Cliente/Servidor.
 * 
 * Este servidor centraliza toda la lógica de negocio del sistema POS:
 * - Procesamiento de ventas
 * - Gestión de inventario
 * - Aplicación de promociones
 * - Arqueo de caja
 * - Auditoría de operaciones
 */
@SpringBootApplication
public class AppServerApplication {

    public static void main(String[] args) {
        SpringApplication.run(AppServerApplication.class, args);
    }

}
