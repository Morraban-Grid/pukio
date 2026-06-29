package com.pukio.dwh;

import com.pukio.dao.DataWarehouseDAO;
import com.pukio.util.FormatoUtil;

import java.util.List;

/**
 * ViewCrossTab - Visualiza el cubo OLAP de ventas por categoria y mes.
 * Uso: java -cp "out;lib/ojdbc11.jar" com.pukio.dwh.ViewCrossTab
 */
public class ViewCrossTab {

    public static void main(String[] args) {
        System.out.println("=============================================================");
        System.out.println("  PUKIO - ViewCrossTab: Cubo OLAP Ventas por Categoria/Mes  ");
        System.out.println("=============================================================");

        try {
            DataWarehouseDAO dao = new DataWarehouseDAO();
            List<Object[]> datos = dao.obtenerCrossTab();

            if (datos.isEmpty()) {
                System.out.println("\n[!] No hay datos en CROSSTAB_VENTAS.");
                System.out.println("    Ejecute primero GenerarDataWarehouse y luego CreateCrossTab.");
                return;
            }

            System.out.printf("\n%-6s %-5s %-25s %12s %16s %10s%n",
                    "ANO", "MES", "CATEGORIA", "UNIDADES", "INGRESOS (S/.)", "VENTAS");
            System.out.println("-".repeat(80));

            int anoActual = -1;
            double totalIngresosAno = 0;

            for (Object[] row : datos) {
                int    ano       = (int)    row[0];
                int    mes       = (int)    row[1];
                String cat       = (String) row[2];
                long   unidades  = (long)   row[3];
                double ingresos  = (double) row[4];
                int    numVentas = (int)    row[5];

                if (ano != anoActual) {
                    if (anoActual != -1) {
                        System.out.printf("%-37s %28s%n", "  Total Ano " + anoActual + ":",
                                FormatoUtil.moneda(totalIngresosAno));
                        System.out.println("-".repeat(80));
                        totalIngresosAno = 0;
                    }
                    anoActual = ano;
                }

                String mesNombre = nombreMes(mes);
                System.out.printf("%-6d %-5s %-25s %12d %16s %10d%n",
                        ano, mesNombre, cat, unidades, FormatoUtil.moneda(ingresos), numVentas);
                totalIngresosAno += ingresos;
            }

            System.out.println("-".repeat(80));
            System.out.printf("%-37s %28s%n", "  Total Ano " + anoActual + ":",
                    FormatoUtil.moneda(totalIngresosAno));
            System.out.println("=============================================================");
            System.out.println("Total filas: " + datos.size());

        } catch (Exception e) {
            System.err.println("[ERROR] " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static String nombreMes(int mes) {
        String[] meses = {"Ene","Feb","Mar","Abr","May","Jun","Jul","Ago","Sep","Oct","Nov","Dic"};
        return (mes >= 1 && mes <= 12) ? meses[mes - 1] : String.valueOf(mes);
    }
}
