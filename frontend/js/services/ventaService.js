import API from './api.js';

const VentaService = {
    // --- Servicios de Clientes ---
    async buscarClientes(q) {
        return await API.get('clientes/buscar', { q });
    },
    
    async listarClientes() {
        return await API.get('clientes');
    },
    
    async guardarCliente(cliente) {
        if (!cliente.nombre || cliente.nombre.trim() === '') throw new Error('El nombre es obligatorio.');
        if (!cliente.numeroDoc || cliente.numeroDoc.trim() === '') throw new Error('El número de documento es obligatorio.');
        
        // Validación del formato según longitud del documento
        const len = cliente.numeroDoc.trim().length;
        if (len !== 8 && len !== 11) {
            throw new Error('El documento de identidad debe ser DNI (8 dígitos) o RUC (11 dígitos).');
        }
        
        if (cliente.idCliente) {
            return await API.put('clientes', cliente);
        } else {
            return await API.post('clientes', cliente);
        }
    },
    
    async eliminarCliente(id) {
        return await API.delete('clientes', { id });
    },
    
    // --- Servicios de Ventas y POS ---
    async registrarVenta(venta) {
        if (!venta.idCliente) throw new Error('Debe seleccionar un cliente.');
        if (!venta.detalles || venta.detalles.length === 0) throw new Error('El carrito de compras está vacío.');
        
        return await API.post('ventas', venta);
    },
    
    async resumenHoy() {
        return await API.get('ventas/resumen-hoy');
    },
    
    // --- Reportes e Inteligencia de Negocios (DWH/OLAP) ---
    async reporteVentas(desde, hasta) {
        return await API.get('reportes/ventas', { desde, hasta });
    },
    
    async productosTop(limit = 5) {
        return await API.get('reportes/productos-top', { limit });
    },
    
    async obtenerCrossTab() {
        return await API.get('dwh/crosstab');
    },
    
    async procesarDWH() {
        return await API.post('dwh/procesar');
    },

    /**
     * Descarga el reporte de ventas en formato CSV para el rango de fechas indicado.
     * Usa la URL directa del endpoint GET /api/reportes/exportar?formato=csv
     * ya que la respuesta es un archivo binario (text/csv), no JSON.
     */
    exportarCsv(desde, hasta) {
        import('../config.js').then(({ default: CONFIG }) => {
            const base = CONFIG.API_BASE_URL;
            const url  = `${base}/reportes/exportar?formato=csv&desde=${desde}&hasta=${hasta}`;
            const a    = document.createElement('a');
            a.href     = url;
            a.download = `ventas_${desde}_${hasta}.csv`;
            document.body.appendChild(a);
            a.click();
            document.body.removeChild(a);
        });
    }
};

export default VentaService;
