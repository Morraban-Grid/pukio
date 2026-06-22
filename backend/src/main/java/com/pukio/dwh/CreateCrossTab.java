package com.pukio.dwh;

import com.pukio.service.DataWarehouseService;

/**
 * Servicio CreateCrossTab
 * Genera cubos OLAP (tablas cruzadas) de ventas por categoria y mes.
 * Equivalente a CreateCrossTab.EXE
 */
public class CreateCrossTab {

    public static void main(String[] args) {
        System.out.println("============================================");
        System.out.println("  PUKIO - Servicio CreateCrossTab");
        System.out.println("============================================");
        System.out.println("Generando tabla cruzada OLAP...");
        try {
            DataWarehouseService svc = new DataWarehouseService();
            int total = svc.generarDWH();
            System.out.println("Datos DWH actualizados: " + total + " registros.");
            int filas = svc.generarCrossTab();
            System.out.println("Registros CrossTab generados en CROSSTAB_VENTAS: " + filas);
            System.out.println("Cubo OLAP creado exitosamente.");
        } catch (Exception e) {
            System.err.println("ERROR: " + e.getMessage());
            e.printStackTrace();
            System.exit(1);
        }
        System.out.println("============================================");
    }
}
