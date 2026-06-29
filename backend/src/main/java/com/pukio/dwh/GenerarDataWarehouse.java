package com.pukio.dwh;

import com.pukio.service.DataWarehouseService;

/**
 * Servicio GenerarDataWarehouse
 * Transfiere datos de ventas al servidor DataWarehouse (DWH_VENTAS).
 * Equivalente a GenerarDataWarehouse.EXE
 */
public class GenerarDataWarehouse {

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  PUKIO - Servicio GenerarDataWarehouse");
        System.out.println("============================================");
        System.out.println("Iniciando transferencia de datos...");
        try {
            DataWarehouseService svc = new DataWarehouseService();
            int filas = svc.generarDWH();
            System.out.println("Registros cargados en DWH_VENTAS: " + filas);
            System.out.println("Proceso completado exitosamente.");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("============================================");
    }
}
