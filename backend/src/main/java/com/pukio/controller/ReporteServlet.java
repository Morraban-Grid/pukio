package com.pukio.controller;

import com.pukio.model.Venta;
import com.pukio.plugin.ReportExporter;
import com.pukio.service.DataWarehouseService;
import com.pukio.service.ReporteService;
import com.pukio.util.JsonUtil;
import jakarta.servlet.ServletException;
import jakarta.servlet.annotation.WebServlet;
import jakarta.servlet.http.*;
import java.io.IOException;
import java.sql.SQLException;
import java.text.SimpleDateFormat;
import java.util.*;

@WebServlet(
    name = "ReporteServlet",
    urlPatterns = {
        "/api/reportes/ventas",
        "/api/reportes/productos-top",
        "/api/reportes/exportar",
        "/api/dwh/crosstab",
        "/api/dwh/procesar"
    }
)
public class ReporteServlet extends HttpServlet {

    private final ReporteService       reporteService = new ReporteService();
    private final DataWarehouseService dwhService     = new DataWarehouseService();
    private final SimpleDateFormat     dateFormat     = new SimpleDateFormat("yyyy-MM-dd");

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI();

        try {
            if (path.endsWith("/reportes/ventas")) {
                response.setContentType("application/json");
                handleVentasPorFecha(request, response);
            } else if (path.endsWith("/reportes/productos-top")) {
                response.setContentType("application/json");
                handleProductosTop(request, response);
            } else if (path.endsWith("/reportes/exportar")) {
                handleExportar(request, response);
            } else if (path.endsWith("/dwh/crosstab")) {
                response.setContentType("application/json");
                handleCrossTab(request, response);
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Error al procesar reporte: " + e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }

    private void handleVentasPorFecha(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String desdeStr = request.getParameter("desde");
        String hastaStr = request.getParameter("hasta");

        if (desdeStr == null || hastaStr == null) {
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Parametros 'desde' y 'hasta' son requeridos.\"}");
            return;
        }

        Date desde = dateFormat.parse(desdeStr);
        Date hasta = dateFormat.parse(hastaStr);

        var ventas = reporteService.ventasPorFecha(desde, hasta);
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.toJson(ventas));
    }

    private void handleProductosTop(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        String limitStr = request.getParameter("limit");
        int limit = 5;
        if (limitStr != null && !limitStr.isBlank()) {
            limit = Integer.parseInt(limitStr);
        }

        List<Object[]> rawList = reporteService.productosMasVendidos(limit);
        List<Map<String, Object>> mappedList = new ArrayList<>();
        for (Object[] row : rawList) {
            Map<String, Object> map = new HashMap<>();
            map.put("nombre",         row[0]);
            map.put("cantidadTotal",  row[1]);
            map.put("totalIngresos",  row[2]);
            mappedList.add(map);
        }

        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.toJson(mappedList));
    }

    /**
     * GET /api/reportes/exportar?formato=csv
     * Usa ServiceLoader para encontrar el exportador del formato pedido.
     */
    private void handleExportar(HttpServletRequest request, HttpServletResponse response)
            throws Exception {
        String formato = request.getParameter("formato");
        if (formato == null || formato.isBlank()) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Parametro 'formato' es requerido.\"}");
            return;
        }

        // Buscar exportador via ServiceLoader
        ReportExporter exporter = null;
        for (ReportExporter e : ServiceLoader.load(ReportExporter.class)) {
            if (e.getFormato().equalsIgnoreCase(formato)) {
                exporter = e;
                break;
            }
        }

        if (exporter == null) {
            response.setContentType("application/json");
            response.setStatus(HttpServletResponse.SC_BAD_REQUEST);
            response.getWriter().write("{\"error\": \"Formato no soportado: " + formato + "\"}");
            return;
        }

        // Obtener ventas de los ultimos 30 dias si no se especifica rango
        String desdeStr = request.getParameter("desde");
        String hastaStr = request.getParameter("hasta");
        Date hasta = new Date();
        Date desde = new Date(hasta.getTime() - 30L * 24 * 60 * 60 * 1000);
        if (desdeStr != null && !desdeStr.isBlank()) desde = dateFormat.parse(desdeStr);
        if (hastaStr != null && !hastaStr.isBlank()) hasta = dateFormat.parse(hastaStr);

        List<Venta> ventas = reporteService.ventasPorFecha(desde, hasta);

        response.setContentType("text/csv; charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename=\"ventas_" + formato + ".csv\"");
        response.setStatus(HttpServletResponse.SC_OK);
        exporter.exportar(ventas, response.getOutputStream());
    }

    private void handleCrossTab(HttpServletRequest request, HttpServletResponse response)
            throws SQLException, IOException {
        List<Object[]> rawList = dwhService.verCrossTab();
        List<Map<String, Object>> mappedList = new ArrayList<>();
        for (Object[] row : rawList) {
            Map<String, Object> map = new HashMap<>();
            map.put("anio",          row[0]);
            map.put("mes",           row[1]);
            map.put("categoria",     row[2]);
            map.put("totalUnidades", row[3]);
            map.put("totalIngresos", row[4]);
            map.put("totalVentas",   row[5]);
            mappedList.add(map);
        }
        response.setStatus(HttpServletResponse.SC_OK);
        response.getWriter().write(JsonUtil.toJson(mappedList));
    }

    @Override
    protected void doPost(HttpServletRequest request, HttpServletResponse response)
            throws ServletException, IOException {

        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        String path = request.getRequestURI();

        try {
            if (path.endsWith("/dwh/procesar")) {
                int rowsDwh  = dwhService.generarDWH();
                int rowsOlap = dwhService.generarCrossTab();

                Map<String, Object> res = new HashMap<>();
                res.put("success",  true);
                res.put("rowsDwh",  rowsDwh);
                res.put("rowsOlap", rowsOlap);
                res.put("message",  "DataWarehouse y Cubo OLAP actualizados con exito.");

                response.setStatus(HttpServletResponse.SC_OK);
                response.getWriter().write(JsonUtil.toJson(res));
            } else {
                response.setStatus(HttpServletResponse.SC_NOT_FOUND);
            }
        } catch (Exception e) {
            response.setStatus(HttpServletResponse.SC_INTERNAL_SERVER_ERROR);
            Map<String, String> err = new HashMap<>();
            err.put("error", "Error al procesar ETL DWH: " + e.getMessage());
            response.getWriter().write(JsonUtil.toJson(err));
        }
    }
}
