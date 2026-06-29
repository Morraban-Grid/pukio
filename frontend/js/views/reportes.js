import VentaService from '../services/ventaService.js';
import AuthService from '../services/authService.js';
import { showToast, toggleLoader, formatCurrency } from '../app.js';

document.addEventListener('DOMContentLoaded', async () => {
    // Proteger ruta
    AuthService.checkGuard();
    
    // Configurar fechas por defecto (Desde hace 30 días hasta hoy)
    establecerFechasPorDefecto();
    
    // Inicializar reporte
    await cargarTodosLosReportes();
    
    // Evento de filtro por fechas
    const filterForm = document.getElementById('filter-date-form');
    if (filterForm) {
        filterForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            await filtrarVentas();
        });
    }
    
    // Evento de límite de Top Productos
    const topLimitSelect = document.getElementById('top-limit-select');
    if (topLimitSelect) {
        topLimitSelect.addEventListener('change', async (e) => {
            const limit = parseInt(e.target.value) || 5;
            await cargarTopProductos(limit);
        });
    }
    
    // Evento de procesar DWH
    const btnProcesarDwh = document.getElementById('btn-procesar-dwh');
    if (btnProcesarDwh) {
        btnProcesarDwh.addEventListener('click', async () => {
            try {
                toggleLoader(true);
                const res = await VentaService.procesarDWH();
                showToast(res.message || 'DataWarehouse actualizado.', 'success');
                await cargarTodosLosReportes();
            } catch (err) {
                showToast('Error al procesar el ETL del DataWarehouse.', 'error');
            } finally {
                toggleLoader(false);
            }
        });
    }

    // Exportar CSV del rango de fechas actual (RF-48)
    const btnExportarCsv = document.getElementById('btn-exportar-csv');
    if (btnExportarCsv) {
        btnExportarCsv.addEventListener('click', () => {
            const desde = document.getElementById('filter-desde').value;
            const hasta = document.getElementById('filter-hasta').value;
            if (!desde || !hasta) {
                showToast('Seleccione un rango de fechas antes de exportar.', 'warning');
                return;
            }
            VentaService.exportarCsv(desde, hasta);
        });
    }
});

function establecerFechasPorDefecto() {
    const hastaInput = document.getElementById('filter-hasta');
    const desdeInput = document.getElementById('filter-desde');
    
    if (hastaInput && desdeInput) {
        const hoy = new Date();
        const hace30dias = new Date();
        hace30dias.setDate(hoy.getDate() - 30);
        
        // Formato YYYY-MM-DD
        hastaInput.value = hoy.toISOString().split('T')[0];
        desdeInput.value = hace30dias.toISOString().split('T')[0];
    }
}

async function cargarTodosLosReportes() {
    try {
        toggleLoader(true);
        const desde = document.getElementById('filter-desde').value;
        const hasta = document.getElementById('filter-hasta').value;
        const topLimit = parseInt(document.getElementById('top-limit-select').value) || 5;
        
        const [ventas, topProds, crosstabData] = await Promise.all([
            VentaService.reporteVentas(desde, hasta),
            VentaService.productosTop(topLimit),
            VentaService.obtenerCrossTab()
        ]);
        
        renderSalesReportTable(ventas);
        renderTopProductsList(topProds);
        renderOlapCrosstabTable(crosstabData);
        
    } catch (err) {
        console.error(err);
        showToast('Error al cargar la información analítica de reportes.', 'error');
    } finally {
        toggleLoader(false);
    }
}

async function filtrarVentas() {
    try {
        toggleLoader(true);
        const desde = document.getElementById('filter-desde').value;
        const hasta = document.getElementById('filter-hasta').value;
        
        const ventas = await VentaService.reporteVentas(desde, hasta);
        renderSalesReportTable(ventas);
        showToast('Filtro de ventas aplicado.', 'success');
    } catch (err) {
        showToast('Error al filtrar ventas.', 'error');
    } finally {
        toggleLoader(false);
    }
}

async function cargarTopProductos(limit) {
    try {
        toggleLoader(true);
        const topProds = await VentaService.productosTop(limit);
        renderTopProductsList(topProds);
    } catch (err) {
        showToast('Error al recargar productos top.', 'error');
    } finally {
        toggleLoader(false);
    }
}

// RENDER: Historial de Ventas
function renderSalesReportTable(list) {
    const tbody = document.querySelector('#sales-report-table tbody');
    if (!tbody) return;
    
    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 40px;">
                    No hay ventas registradas en el período seleccionado.
                </td>
            </tr>
        `;
        return;
    }
    
    let totalAcumulado = 0;
    let descAcumulado = 0;
    
    tbody.innerHTML = list.map(v => {
        totalAcumulado += v.total;
        descAcumulado += v.descuento;
        
        const fecha = new Date(v.fechaVenta);
        const fechaFormateada = `${fecha.toLocaleDateString()} ${fecha.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
        
        return `
            <tr>
                <td><strong><code>${v.numeroComprobante}</code></strong></td>
                <td style="font-size:13px; color:var(--text-secondary);">${fechaFormateada}</td>
                <td style="font-weight:600;">${v.nombreCliente}</td>
                <td style="color:var(--text-muted);">${formatCurrency(v.descuento)}</td>
                <td style="font-weight:700; color:var(--text-primary);">${formatCurrency(v.total)}</td>
                <td><span class="badge badge-info">${v.metodoPago}</span></td>
                <td style="font-size:13px; color:var(--text-secondary);">${v.nombreCajero}</td>
            </tr>
        `;
    }).join('');
    
    // Agregar fila de resumen al final
    const summaryTr = document.createElement('tr');
    summaryTr.style.background = 'var(--bg-tertiary)';
    summaryTr.style.fontWeight = '700';
    summaryTr.innerHTML = `
        <td colspan="3" style="text-align: right; color:var(--text-primary);">TOTAL PERÍODO:</td>
        <td style="color:var(--text-muted);">${formatCurrency(descAcumulado)}</td>
        <td style="color:var(--success); font-size:15px;">${formatCurrency(totalAcumulado)}</td>
        <td colspan="2" style="font-size:12px; color:var(--text-secondary);">${list.length} Ventas</td>
    `;
    tbody.appendChild(summaryTr);
}

// RENDER: Top Productos
function renderTopProductsList(list) {
    const container = document.getElementById('top-products-container');
    if (!container) return;
    
    if (list.length === 0) {
        container.innerHTML = `
            <div style="text-align: center; color: var(--text-muted); padding: 40px;">
                No se registran ventas para este ranking.
            </div>
        `;
        return;
    }
    
    container.innerHTML = list.map((item, index) => {
        const rank = index + 1;
        let rankClass = `top-product-rank`;
        if (rank <= 3) rankClass += ` rank-${rank}`;
        
        return `
            <div class="top-product-item">
                <div class="${rankClass}">${rank}</div>
                <div class="top-product-details">
                    <div class="top-product-name">${item.nombre}</div>
                    <div class="top-product-stats">${item.cantidadTotal} unidades vendidas</div>
                </div>
                <div class="top-product-total">${formatCurrency(item.totalIngresos)}</div>
            </div>
        `;
    }).join('');
}

// RENDER: Cubo OLAP Pivot Crosstab Table
function renderOlapCrosstabTable(crosstabData) {
    const table = document.getElementById('olap-crosstab-table');
    if (!table) return;
    
    if (crosstabData.length === 0) {
        table.innerHTML = `<tbody><tr><td style="text-align: center; color: var(--text-muted); padding: 40px;">No hay datos agregados en el DWH. Asegúrate de procesar ventas e iniciar los servicios DWH.</td></tr></tbody>`;
        return;
    }
    
    // 1. Obtener todos los períodos únicos en orden cronológico (clave: YYYY-MM)
    const periodosMap = {}; // Clave: YYYY-MM, Valor: { anio, mes, label }
    const categoriasSet = new Set();
    
    crosstabData.forEach(item => {
        const key = `${item.anio}-${String(item.mes).padStart(2, '0')}`;
        if (!periodosMap[key]) {
            periodosMap[key] = {
                anio: item.anio,
                mes: item.mes,
                label: `${nombreMes(item.mes)} ${item.anio}`
            };
        }
        categoriasSet.add(item.categoria);
    });
    
    const sortedKeys = Object.keys(periodosMap).sort(); // Ordenar ascendente
    const sortedCategorias = Array.from(categoriasSet).sort();
    
    // 2. Mapear datos agregados en una estructura Category -> PeriodKey -> { total, uds }
    const matrix = {};
    sortedCategorias.forEach(cat => {
        matrix[cat] = {};
        sortedKeys.forEach(key => {
            matrix[cat][key] = { ingresos: 0, unidades: 0 };
        });
    });
    
    crosstabData.forEach(item => {
        const key = `${item.anio}-${String(item.mes).padStart(2, '0')}`;
        if (matrix[item.categoria] && matrix[item.categoria][key] !== undefined) {
            matrix[item.categoria][key] = {
                ingresos: item.totalIngresos,
                unidades: item.totalUnidades
            };
        }
    });
    
    // 3. Renderizar Header (THEAD)
    let theadHtml = `
        <tr>
            <th>Categoría</th>
    `;
    sortedKeys.forEach(key => {
        theadHtml += `<th>${periodosMap[key].label}</th>`;
    });
    theadHtml += `
            <th>Total General</th>
        </tr>
    `;
    table.querySelector('thead').innerHTML = theadHtml;
    
    // 4. Renderizar Filas (TBODY)
    let tbodyHtml = '';
    
    // Inicializar sumatorias para fila de totales por columna
    const columnasTotales = {};
    sortedKeys.forEach(key => {
        columnasTotales[key] = { ingresos: 0, unidades: 0 };
    });
    let granTotalIngresos = 0;
    
    sortedCategorias.forEach(cat => {
        let filaHtml = `<tr><td>${cat}</td>`;
        let totalFilaIngresos = 0;
        
        sortedKeys.forEach(key => {
            const cell = matrix[cat][key];
            totalFilaIngresos += cell.ingresos;
            
            // Sumar a totales de columna
            columnasTotales[key].ingresos += cell.ingresos;
            columnasTotales[key].unidades += cell.unidades;
            
            if (cell.ingresos > 0) {
                filaHtml += `
                    <td>
                        <span class="olap-cell-value">${formatCurrency(cell.ingresos)}</span>
                        <span class="olap-cell-sub">${cell.unidades} uds</span>
                    </td>
                `;
            } else {
                filaHtml += `
                    <td style="color:var(--text-muted); opacity: 0.5;">
                        <span>S/. 0.00</span>
                        <span class="olap-cell-sub">0 uds</span>
                    </td>
                `;
            }
        });
        
        granTotalIngresos += totalFilaIngresos;
        filaHtml += `
            <td style="background: rgba(67, 97, 238, 0.03); font-weight:700;">
                <span class="olap-cell-value" style="color:var(--accent-orange);">${formatCurrency(totalFilaIngresos)}</span>
            </td>
        </tr>`;
        
        tbodyHtml += filaHtml;
    });
    
    // 5. Añadir Fila de Totales de Columnas al final
    let totalFilaHtml = `
        <tr style="background:var(--bg-tertiary); font-weight:700; border-top:2px solid var(--border-color);">
            <td>TOTAL GENERAL</td>
    `;
    sortedKeys.forEach(key => {
        const totalCol = columnasTotales[key];
        totalFilaHtml += `
            <td>
                <span class="olap-cell-value" style="color:var(--success);">${formatCurrency(totalCol.ingresos)}</span>
                <span class="olap-cell-sub" style="color:var(--text-secondary);">${totalCol.unidades} uds</span>
            </td>
        `;
    });
    totalFilaHtml += `
            <td style="color:var(--success); font-size:14px; background: rgba(76, 175, 80, 0.08);">${formatCurrency(granTotalIngresos)}</td>
        </tr>
    `;
    
    tbodyHtml += totalFilaHtml;
    
    table.querySelector('tbody').innerHTML = tbodyHtml;
}

function nombreMes(numMes) {
    const meses = ['Ene', 'Feb', 'Mar', 'Abr', 'May', 'Jun', 'Jul', 'Ago', 'Sep', 'Oct', 'Nov', 'Dic'];
    return (numMes >= 1 && numMes <= 12) ? meses[numMes - 1] : String(numMes);
}
