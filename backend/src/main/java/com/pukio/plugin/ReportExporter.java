package com.pukio.plugin;

import com.pukio.model.Venta;
import java.io.OutputStream;
import java.util.List;

/**
 * Punto de extension para exportadores de reportes.
 * Para agregar un nuevo formato: implementa esta interfaz y registrala
 * en META-INF/services/com.pukio.plugin.ReportExporter.
 */
public interface ReportExporter {
    /** Identificador del formato, en minusculas (ej. "csv"). */
    String getFormato();

    /** Escribe las ventas al OutputStream en el formato correspondiente. */
    void exportar(List<Venta> ventas, OutputStream out) throws Exception;
}
