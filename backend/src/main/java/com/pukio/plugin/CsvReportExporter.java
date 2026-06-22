package com.pukio.plugin;

import com.pukio.model.Venta;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.List;

/**
 * Exportador de ventas en formato CSV.
 */
public class CsvReportExporter implements ReportExporter {

    private static final String SEPARATOR = ",";
    private static final SimpleDateFormat DATE_FMT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    @Override
    public String getFormato() {
        return "csv";
    }

    @Override
    public void exportar(List<Venta> ventas, OutputStream out) throws Exception {
        try (PrintWriter pw = new PrintWriter(new OutputStreamWriter(out, StandardCharsets.UTF_8))) {
            // Cabecera
            pw.println("id_venta,numero_comprobante,tipo_comprobante,fecha_venta," +
                       "nombre_cliente,nombre_cajero,metodo_pago,subtotal,igv,descuento,total,estado");

            for (Venta v : ventas) {
                pw.println(
                    v.getIdVenta() + SEPARATOR +
                    escape(v.getNumeroComprobante()) + SEPARATOR +
                    escape(v.getTipoComprobante()) + SEPARATOR +
                    (v.getFechaVenta() != null ? escape(DATE_FMT.format(v.getFechaVenta())) : "") + SEPARATOR +
                    escape(v.getNombreCliente()) + SEPARATOR +
                    escape(v.getNombreCajero()) + SEPARATOR +
                    escape(v.getMetodoPago()) + SEPARATOR +
                    v.getSubtotal() + SEPARATOR +
                    v.getIgv() + SEPARATOR +
                    v.getDescuento() + SEPARATOR +
                    v.getTotal() + SEPARATOR +
                    escape(v.getEstado())
                );
            }
            pw.flush();
        }
    }

    /** Envuelve el valor en comillas si contiene coma, comilla o salto de linea. */
    private String escape(String value) {
        if (value == null) return "";
        if (value.contains(SEPARATOR) || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
