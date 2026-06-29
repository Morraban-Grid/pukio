import AuthService from '../services/authService.js';
import VentaService from '../services/ventaService.js';
import ProductoService from '../services/productoService.js';
import { showToast, toggleLoader, formatCurrency } from '../app.js';

document.addEventListener('DOMContentLoaded', async () => {
    // Verificar sesión (protege ruta)
    AuthService.checkGuard();
    
    // Obtener información del usuario para la bienvenida
    const user = AuthService.getCurrentUser();
    if (user) {
        document.getElementById('welcome-username').textContent = user.nombre;
    }
    
    // Renderizar fecha de hoy en español
    renderCurrentDate();
    
    // Cargar métricas y alertas
    await cargarDatosDashboard();
});

function renderCurrentDate() {
    const dateEl = document.getElementById('current-date-el');
    if (!dateEl) return;
    
    const opciones = { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' };
    const hoy = new Date();
    // Capitalizar primera letra
    let fechaStr = hoy.toLocaleDateString('es-ES', opciones);
    fechaStr = fechaStr.charAt(0).toUpperCase() + fechaStr.slice(1);
    
    dateEl.textContent = fechaStr;
}

async function cargarDatosDashboard() {
    try {
        toggleLoader(true);
        
        // Ejecutar llamadas en paralelo para mejor rendimiento
        const [resumen, bajoStockList] = await Promise.all([
            VentaService.resumenHoy(),
            ProductoService.bajoStock()
        ]);
        
        // 1. Poblar Métricas Financieras
        document.getElementById('today-total').textContent = formatCurrency(resumen.total || 0);
        document.getElementById('today-count').textContent = resumen.cantidad || 0;
        
        // 2. Poblar Métricas de Alerta de Stock
        const lowStockCount = bajoStockList.length;
        const countEl = document.getElementById('low-stock-count');
        countEl.textContent = lowStockCount;
        
        const metricCard = document.getElementById('low-stock-metric-card');
        if (lowStockCount > 0) {
            metricCard.classList.add('metric-card-alert');
        } else {
            metricCard.classList.remove('metric-card-alert');
        }
        
        // 3. Renderizar Tabla de Productos con bajo stock
        renderLowStockTable(bajoStockList);
        
    } catch (error) {
        console.error('Error al cargar datos del dashboard:', error);
        showToast('Error al obtener la información en tiempo real.', 'error');
    } finally {
        toggleLoader(false);
    }
}

function renderLowStockTable(list) {
    const tbody = document.querySelector('#low-stock-table tbody');
    if (!tbody) return;
    
    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="5" style="text-align: center; color: var(--text-muted); padding: 30px;">
                    <svg width="24" height="24" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" style="margin-bottom:8px; display:block; margin-inline:auto; opacity:0.6;"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>
                    Excelente, todo el stock está óptimo.
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = list.map(p => {
        // Asignar nivel de gravedad según si el stock es cero o menor al stock mínimo
        const gravedadClase = p.stock === 0 ? 'row-danger' : 'row-warning';
        
        return `
            <tr class="${gravedadClase}">
                <td><code>${p.codigo}</code></td>
                <td style="font-weight:600;">${p.nombre}</td>
                <td>${p.categoriaNombre}</td>
                <td style="font-weight:700;">${p.stock}</td>
                <td style="color: var(--text-secondary);">${p.stockMinimo}</td>
            </tr>
        `;
    }).join('');
}
