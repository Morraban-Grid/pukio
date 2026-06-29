import ProductoService from '../services/productoService.js';
import { showToast, toggleLoader, formatCurrency } from '../app.js';

let listadoProductos = [];

document.addEventListener('DOMContentLoaded', async () => {
    // Cargar listas y poblar selector de categorías/proveedores en el modal
    await inicializarVista();
    
    // Evento de búsqueda instantánea
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(async (e) => {
            const query = e.target.value.trim();
            await realizarBusqueda(query);
        }, 250));
    }
    
    // Configuración del modal
    const modalBackdrop = document.getElementById('producto-modal-backdrop');
    const btnNuevo = document.getElementById('btn-nuevo-producto');
    const btnCerrar = document.getElementById('btn-cerrar-modal');
    const btnCancelar = document.getElementById('btn-cancelar-modal');
    const form = document.getElementById('producto-form');
    
    if (btnNuevo && modalBackdrop) {
        btnNuevo.addEventListener('click', () => {
            limpiarFormulario();
            document.getElementById('modal-title-text').textContent = 'Registrar Producto';
            modalBackdrop.classList.add('active');
        });
    }
    
    const cerrarModal = () => {
        if (modalBackdrop) modalBackdrop.classList.remove('active');
    };
    
    if (btnCerrar) btnCerrar.addEventListener('click', cerrarModal);
    if (btnCancelar) btnCancelar.addEventListener('click', cerrarModal);
    
    // Envío del Formulario (Crear/Editar)
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const idVal = document.getElementById('prod-id').value;
            const producto = {
                codigo: document.getElementById('prod-codigo').value.trim(),
                nombre: document.getElementById('prod-nombre').value.trim(),
                descripcion: document.getElementById('prod-desc').value.trim(),
                idCategoria: parseInt(document.getElementById('prod-categoria').value),
                idProveedor: parseInt(document.getElementById('prod-proveedor').value),
                precioCompra: parseFloat(document.getElementById('prod-pcompra').value),
                precioVenta: parseFloat(document.getElementById('prod-pventa').value),
                stock: parseInt(document.getElementById('prod-stock').value),
                stockMinimo: parseInt(document.getElementById('prod-sminimo').value)
            };
            
            if (idVal) {
                producto.idProducto = parseInt(idVal);
            }
            
            try {
                toggleLoader(true);
                await ProductoService.guardar(producto);
                showToast(`Producto ${idVal ? 'actualizado' : 'registrado'} correctamente.`, 'success');
                cerrarModal();
                await cargarProductos();
            } catch (err) {
                showToast(err.message || 'Error al guardar el producto.', 'error');
            } finally {
                toggleLoader(false);
            }
        });
    }
});

async function inicializarVista() {
    try {
        toggleLoader(true);
        // Cargar combos del modal en paralelo junto a los productos
        const [categorias, proveedores] = await Promise.all([
            ProductoService.listarCategorias(),
            ProductoService.listarProveedores()
        ]);
        
        poblarCombos(categorias, proveedores);
        await cargarProductos();
        
    } catch (err) {
        showToast('Error al inicializar la vista de productos.', 'error');
    } finally {
        toggleLoader(false);
    }
}

function poblarCombos(categorias, proveedores) {
    const catSelect = document.getElementById('prod-categoria');
    const provSelect = document.getElementById('prod-proveedor');
    
    if (catSelect) {
        catSelect.innerHTML = '<option value="">Seleccione...</option>' + 
            categorias.map(c => `<option value="${c.idCategoria}">${c.nombre}</option>`).join('');
    }
    
    if (provSelect) {
        provSelect.innerHTML = '<option value="">Seleccione...</option>' + 
            proveedores.map(p => `<option value="${p.idProveedor}">${p.nombre}</option>`).join('');
    }
}

async function cargarProductos() {
    try {
        listadoProductos = await ProductoService.listar();
        renderProductosTable(listadoProductos);
    } catch (err) {
        showToast('Error al cargar la lista de productos.', 'error');
    }
}

async function realizarBusqueda(q) {
    try {
        if (!q) {
            await cargarProductos();
            return;
        }
        const filtrados = await ProductoService.buscar(q);
        renderProductosTable(filtrados);
    } catch (err) {
        showToast('Error en la búsqueda.', 'error');
    }
}

function renderProductosTable(list) {
    const tbody = document.querySelector('#productos-table tbody');
    if (!tbody) return;
    
    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="10" style="text-align: center; color: var(--text-muted); padding: 40px;">
                    No se encontraron productos en el catálogo.
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = '';
    
    list.forEach(p => {
        const tr = document.createElement('tr');
        
        // Alertas visuales según stock
        if (p.stock === 0) {
            tr.className = 'row-danger';
        } else if (p.stock <= p.stockMinimo) {
            tr.className = 'row-warning';
        }
        
        tr.innerHTML = `
            <td><code>${p.codigo}</code></td>
            <td style="font-weight: 600;">${p.nombre}</td>
            <td style="font-size: 13px; color: var(--text-secondary); max-width: 200px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${p.descripcion || '-'}</td>
            <td>${formatCurrency(p.precioCompra)}</td>
            <td style="font-weight: 600; color: var(--text-primary);">${formatCurrency(p.precioVenta)}</td>
            <td style="font-weight: 700;">${p.stock}</td>
            <td style="color: var(--text-secondary);">${p.stockMinimo}</td>
            <td><span class="badge badge-info">${p.categoriaNombre || 'Sin cat.'}</span></td>
            <td style="font-size: 13px; color: var(--text-secondary);">${p.proveedorNombre || 'Sin prov.'}</td>
            <td style="text-align: right;">
                <div style="display: flex; justify-content: flex-end; gap: 8px;">
                    <button class="btn btn-secondary btn-edit-prod" data-id="${p.idProducto}" style="padding: 6px 12px; font-size: 12px;">
                        Editar
                    </button>
                    <button class="btn btn-danger btn-delete-prod" data-id="${p.idProducto}" style="padding: 6px 12px; font-size: 12px;">
                        Desactivar
                    </button>
                </div>
            </td>
        `;
        
        // Asociar eventos
        tr.querySelector('.btn-edit-prod').addEventListener('click', () => abrirEditar(p));
        tr.querySelector('.btn-delete-prod').addEventListener('click', () => desactivarProducto(p));
        
        tbody.appendChild(tr);
    });
}

function abrirEditar(p) {
    limpiarFormulario();
    document.getElementById('prod-id').value = p.idProducto;
    document.getElementById('prod-codigo').value = p.codigo;
    document.getElementById('prod-nombre').value = p.nombre;
    document.getElementById('prod-desc').value = p.descripcion || '';
    document.getElementById('prod-categoria').value = p.idCategoria;
    document.getElementById('prod-proveedor').value = p.idProveedor;
    document.getElementById('prod-pcompra').value = p.precioCompra;
    document.getElementById('prod-pventa').value = p.precioVenta;
    document.getElementById('prod-stock').value = p.stock;
    document.getElementById('prod-sminimo').value = p.stockMinimo;
    
    document.getElementById('modal-title-text').textContent = 'Editar Producto';
    document.getElementById('producto-modal-backdrop').classList.add('active');
}

async function desactivarProducto(p) {
    if (confirm(`¿Está seguro de que desea desactivar el producto "${p.nombre}"?`)) {
        try {
            toggleLoader(true);
            await ProductoService.eliminar(p.idProducto);
            showToast('Producto desactivado correctamente.', 'success');
            await cargarProductos();
        } catch (err) {
            showToast(err.message || 'Error al desactivar el producto.', 'error');
        } finally {
            toggleLoader(false);
        }
    }
}

function limpiarFormulario() {
    document.getElementById('prod-id').value = '';
    document.getElementById('producto-form').reset();
}

// Helper para evitar múltiples llamadas en la búsqueda rápida
function debounce(func, delay) {
    let timeoutId;
    return function (...args) {
        if (timeoutId) clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            func.apply(this, args);
        }, delay);
    };
}
