import ProductoService from '../services/productoService.js';
import VentaService from '../services/ventaService.js';
import AuthService from '../services/authService.js';
import { showToast, toggleLoader, formatCurrency } from '../app.js';

let cart = [];
let selectedCliente = { idCliente: 1, tipoDoc: 'DNI', numeroDoc: '00000000', nombre: 'Clientes Varios' };
let allProductsCache = [];

document.addEventListener('DOMContentLoaded', async () => {
    // Proteger ruta
    AuthService.checkGuard();
    
    // Inicializar datos del POS
    await inicializarPOS();
    
    // Autocompletado de Clientes
    configurarAutocompleteCliente();
    
    // Autocompletado de Productos y Escáner de Barras
    configurarAutocompleteProducto();
    
    // Modales de Cliente Rápido
    configurarClienteRapido();
    
    // Configurar Modales de Comprobante
    configurarComprobanteModal();
    
    // Evento de Pagar / Procesar Transacción
    const btnPagar = document.getElementById('pos-btn-pagar');
    if (btnPagar) {
        btnPagar.addEventListener('click', procesarPago);
    }
});

async function inicializarPOS() {
    try {
        toggleLoader(true);
        // Cargar productos del catálogo
        allProductsCache = await ProductoService.listar();
        renderQuickProducts(allProductsCache);
        renderCart();
    } catch (err) {
        showToast('Error al inicializar los productos en la terminal.', 'error');
    } finally {
        toggleLoader(false);
    }
}

// --- AUTOCOMPLETADO CLIENTE ---
function configurarAutocompleteCliente() {
    const input = document.getElementById('pos-search-cliente');
    const resultsDiv = document.getElementById('client-autocomplete-results');
    
    if (!input || !resultsDiv) return;
    
    input.addEventListener('input', async (e) => {
        const val = e.target.value.trim();
        if (val.length < 2) {
            resultsDiv.classList.remove('active');
            return;
        }
        
        try {
            const list = await VentaService.buscarClientes(val);
            if (list.length === 0) {
                resultsDiv.innerHTML = '<div class="autocomplete-item" style="color:var(--text-muted); cursor:default;">No se encontraron clientes</div>';
            } else {
                resultsDiv.innerHTML = list.map(c => `
                    <div class="autocomplete-item" data-id="${c.idCliente}">
                        <strong>${c.nombre}</strong> <span style="color:var(--accent-orange); font-size:11px; margin-left:8px;">${c.tipoDoc}: ${c.numeroDoc}</span>
                    </div>
                `).join('');
                
                // Clic en item
                resultsDiv.querySelectorAll('.autocomplete-item').forEach(item => {
                    item.addEventListener('click', (e) => {
                        const id = parseInt(item.getAttribute('data-id'));
                        const c = list.find(x => x.idCliente === id);
                        if (c) selectCliente(c);
                        resultsDiv.classList.remove('active');
                        input.value = '';
                    });
                });
            }
            resultsDiv.classList.add('active');
        } catch (err) {
            console.error(err);
        }
    });
    
    // Cerrar autocomplete al hacer clic fuera
    document.addEventListener('click', (e) => {
        if (!input.contains(e.target) && !resultsDiv.contains(e.target)) {
            resultsDiv.classList.remove('active');
        }
    });
}

function selectCliente(cliente) {
    selectedCliente = cliente;
    document.getElementById('selected-client-name').textContent = cliente.nombre;
    document.getElementById('selected-client-doc').textContent = `${cliente.tipoDoc}: ${cliente.numeroDoc}`;
}

// --- AUTOCOMPLETADO PRODUCTO Y ENTER (ESCÁNER) ---
function configurarAutocompleteProducto() {
    const input = document.getElementById('pos-search-producto');
    const resultsDiv = document.getElementById('product-autocomplete-results');
    
    if (!input || !resultsDiv) return;
    
    input.addEventListener('input', async (e) => {
        const val = e.target.value.trim();
        if (val.length < 2) {
            resultsDiv.classList.remove('active');
            return;
        }
        
        try {
            const list = await ProductoService.buscar(val);
            if (list.length === 0) {
                resultsDiv.innerHTML = '<div class="autocomplete-item" style="color:var(--text-muted); cursor:default;">No se encontraron productos</div>';
            } else {
                resultsDiv.innerHTML = list.map(p => `
                    <div class="autocomplete-item" data-id="${p.idProducto}">
                        <strong>${p.nombre}</strong> - ${formatCurrency(p.precioVenta)} 
                        <span style="font-size:11px; color:${p.stock <= p.stockMinimo ? 'var(--danger)' : 'var(--success)'}; margin-left:8px;">
                            (Stock: ${p.stock})
                        </span>
                    </div>
                `).join('');
                
                resultsDiv.querySelectorAll('.autocomplete-item').forEach(item => {
                    item.addEventListener('click', () => {
                        const id = parseInt(item.getAttribute('data-id'));
                        const p = list.find(x => x.idProducto === id);
                        if (p) agregarAlCarrito(p);
                        resultsDiv.classList.remove('active');
                        input.value = '';
                        input.focus();
                    });
                });
            }
            resultsDiv.classList.add('active');
        } catch (err) {
            console.error(err);
        }
    });
    
    // Emulación de lector de código de barras al presionar ENTER
    input.addEventListener('keydown', async (e) => {
        if (e.key === 'Enter') {
            e.preventDefault();
            const val = input.value.trim();
            if (!val) return;
            
            try {
                toggleLoader(true);
                // Intentar buscar exactamente por código
                const p = await ProductoService.buscarPorCodigo(val);
                agregarAlCarrito(p);
                input.value = '';
                resultsDiv.classList.remove('active');
            } catch (err) {
                showToast(`No se encontró producto con el código: ${val}`, 'warning');
            } finally {
                toggleLoader(false);
            }
        }
    });
    
    document.addEventListener('click', (e) => {
        if (!input.contains(e.target) && !resultsDiv.contains(e.target)) {
            resultsDiv.classList.remove('active');
        }
    });
}

// --- GRID PRODUCTOS RÁPIDOS ---
function renderQuickProducts(products) {
    const grid = document.getElementById('quick-products-grid');
    if (!grid) return;
    
    // Tomar los primeros 12 productos activos
    const showProds = products.slice(0, 12);
    
    grid.innerHTML = showProds.map(p => {
        let tileClass = 'product-tile';
        if (p.stock === 0) tileClass += ' tile-no-stock';
        else if (p.stock <= p.stockMinimo) tileClass += ' tile-low-stock';
        
        return `
            <div class="${tileClass}" data-id="${p.idProducto}">
                <div class="product-tile-name">${p.nombre}</div>
                <div style="display:flex; justify-content:space-between; align-items:flex-end;">
                    <div class="product-tile-price">${formatCurrency(p.precioVenta)}</div>
                    <div class="product-tile-stock">S: ${p.stock}</div>
                </div>
            </div>
        `;
    }).join('');
    
    grid.querySelectorAll('.product-tile').forEach(tile => {
        tile.addEventListener('click', () => {
            const id = parseInt(tile.getAttribute('data-id'));
            const p = allProductsCache.find(x => x.idProducto === id);
            if (p) {
                if (p.stock === 0) {
                    showToast('El producto no tiene stock disponible.', 'error');
                } else {
                    agregarAlCarrito(p);
                }
            }
        });
    });
}

// --- ACCIONES DEL CARRITO ---
function agregarAlCarrito(prod) {
    if (prod.stock <= 0) {
        showToast(`El producto "${prod.nombre}" no tiene stock disponible.`, 'error');
        return;
    }
    
    const existing = cart.find(item => item.idProducto === prod.idProducto);
    if (existing) {
        if (existing.cantidad >= prod.stock) {
            showToast(`No puedes agregar más unidades. Stock máximo disponible: ${prod.stock}`, 'warning');
            return;
        }
        existing.cantidad++;
        existing.subtotal = parseFloat(((existing.cantidad * existing.precioUnit) - existing.descuento).toFixed(2));
    } else {
        cart.push({
            idProducto: prod.idProducto,
            codigo: prod.codigo,
            nombreProducto: prod.nombre,
            idCategoria: prod.idCategoria,
            cantidad: 1,
            precioUnit: prod.precioVenta,
            descuento: 0,
            subtotal: prod.precioVenta,
            stock: prod.stock
        });
    }
    
    showToast(`Agregado: ${prod.nombre}`, 'success');
    renderCart();
}

function renderCart() {
    const tbody = document.querySelector('#pos-cart-table tbody');
    const countEl = document.getElementById('cart-item-count');
    
    if (!tbody) return;
    
    countEl.textContent = `${cart.reduce((acc, item) => acc + item.cantidad, 0)} Items`;
    
    if (cart.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="6" style="text-align: center; color: var(--text-muted); padding: 40px;">
                    El carrito está vacío. Agregue productos de la izquierda.
                </td>
            </tr>
        `;
        actualizarTotales(0, 0, 0);
        return;
    }
    
    tbody.innerHTML = '';
    
    cart.forEach(item => {
        const tr = document.createElement('tr');
        tr.innerHTML = `
            <td style="font-weight: 500; font-size:13.5px;">
                ${item.nombreProducto}<br>
                <code style="font-size:10.5px; color:var(--text-muted);">${item.codigo}</code>
            </td>
            <td>
                <div class="cart-qty-ctrl">
                    <button class="btn-qty btn-minus" data-id="${item.idProducto}">&minus;</button>
                    <input type="number" class="input-qty" value="${item.cantidad}" readonly>
                    <button class="btn-qty btn-plus" data-id="${item.idProducto}">&plus;</button>
                </div>
            </td>
            <td>${formatCurrency(item.precioUnit)}</td>
            <td>${formatCurrency(item.cantidad * item.precioUnit)}</td>
            <td>
                <input type="number" class="cart-discount-input" data-id="${item.idProducto}" value="${item.descuento}" step="0.10" min="0" placeholder="0.00">
            </td>
            <td style="font-weight: 600;">${formatCurrency(item.subtotal)}</td>
            <td style="text-align: right;">
                <button class="btn-remove-item" data-id="${item.idProducto}">
                    <svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><polyline points="3 6 5 6 21 6"></polyline><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path><line x1="10" y1="11" x2="10" y2="17"></line><line x1="14" y1="11" x2="14" y2="17"></line></svg>
                </button>
            </td>
        `;
        
        // Eventos de cantidad
        tr.querySelector('.btn-minus').addEventListener('click', () => modificarCantidad(item.idProducto, -1));
        tr.querySelector('.btn-plus').addEventListener('click', () => modificarCantidad(item.idProducto, 1));
        
        // Evento de descuento por item
        const descInput = tr.querySelector('.cart-discount-input');
        descInput.addEventListener('change', (e) => {
            const val = parseFloat(e.target.value) || 0;
            modificarDescuento(item.idProducto, val);
        });
        
        // Eliminar item
        tr.querySelector('.btn-remove-item').addEventListener('click', () => eliminarDelCarrito(item.idProducto));
        
        tbody.appendChild(tr);
    });
    
    calcularYMostrarTotales();
}

function modificarCantidad(id, cambio) {
    const item = cart.find(x => x.idProducto === id);
    if (!item) return;
    
    const nuevaCant = item.cantidad + cambio;
    if (nuevaCant <= 0) {
        eliminarDelCarrito(id);
        return;
    }
    
    if (nuevaCant > item.stock) {
        showToast(`Stock insuficiente. Disponible: ${item.stock}`, 'warning');
        return;
    }
    
    item.cantidad = nuevaCant;
    item.subtotal = parseFloat(((item.cantidad * item.precioUnit) - item.descuento).toFixed(2));
    renderCart();
}

function modificarDescuento(id, descVal) {
    const item = cart.find(x => x.idProducto === id);
    if (!item) return;
    
    const subBruto = item.cantidad * item.precioUnit;
    if (descVal < 0) {
        showToast('El descuento no puede ser negativo.', 'error');
        renderCart();
        return;
    }
    if (descVal > subBruto) {
        showToast('El descuento no puede superar el monto bruto del ítem.', 'error');
        renderCart();
        return;
    }
    
    item.descuento = descVal;
    item.subtotal = parseFloat((subBruto - descVal).toFixed(2));
    renderCart();
}

function eliminarDelCarrito(id) {
    cart = cart.filter(item => item.idProducto !== id);
    showToast('Producto quitado del carrito.', 'info');
    renderCart();
}

function calcularYMostrarTotales() {
    // Subtotal total (neto acumulado de los items)
    const subtotalBruto = cart.reduce((acc, item) => acc + (item.cantidad * item.precioUnit), 0);
    const descuentoTotal = cart.reduce((acc, item) => acc + item.descuento, 0);
    
    const subtotalNeto = subtotalBruto - descuentoTotal;
    
    // IGV y Total
    const igv = parseFloat((subtotalNeto * 0.18).toFixed(2));
    const total = parseFloat((subtotalNeto + igv).toFixed(2));
    
    actualizarTotales(subtotalNeto, igv, total, descuentoTotal);
}

function actualizarTotales(subtotal, igv, total, desc = 0) {
    document.getElementById('pos-subtotal').textContent = formatCurrency(subtotal);
    document.getElementById('pos-igv').textContent = formatCurrency(igv);
    document.getElementById('pos-descuento').textContent = formatCurrency(desc);
    document.getElementById('pos-total').textContent = formatCurrency(total);
}

// --- PROCESAR PAGO ---
async function procesarPago() {
    if (cart.length === 0) {
        showToast('El carrito de compras está vacío.', 'warning');
        return;
    }
    
    const user = AuthService.getCurrentUser();
    const subtotalBruto = cart.reduce((acc, item) => acc + (item.cantidad * item.precioUnit), 0);
    const descuento = cart.reduce((acc, item) => acc + item.descuento, 0);
    const subtotalNeto = subtotalBruto - descuento;
    const igv = parseFloat((subtotalNeto * 0.18).toFixed(2));
    const total = parseFloat((subtotalNeto + igv).toFixed(2));
    
    const venta = {
        idCliente: selectedCliente.idCliente,
        nombreCliente: selectedCliente.nombre,
        idUsuario: user ? user.idUsuario : 1,
        nombreCajero: user ? user.nombre : 'Cajero Local',
        tipoComprobante: document.getElementById('pos-comprobante-tipo').value,
        metodoPago: document.getElementById('pos-pago-metodo').value,
        subtotal: parseFloat(subtotalNeto.toFixed(2)),
        igv: igv,
        descuento: parseFloat(descuento.toFixed(2)),
        total: total,
        detalles: cart.map(item => ({
            idProducto: item.idProducto,
            nombreProducto: item.nombreProducto,
            idCategoria: item.idCategoria,
            cantidad: item.cantidad,
            precioUnit: item.precioUnit,
            descuento: item.descuento,
            subtotal: item.subtotal
        }))
    };
    
    try {
        toggleLoader(true);
        const result = await VentaService.registrarVenta(venta);
        
        showToast('¡Venta realizada con éxito!', 'success');
        
        // Abrir y poblar el modal de ticket de venta emitido
        abrirComprobanteEmitido(result, venta);
        
        // Limpiar Carrito y recargar productos para actualizar stock
        cart = [];
        await inicializarPOS();
        
    } catch (err) {
        showToast(err.message || 'Error al procesar la venta.', 'error');
    } finally {
        toggleLoader(false);
    }
}

// --- TICKET DE COMPROBANTE EMITIDO ---
function configurarComprobanteModal() {
    const modal = document.getElementById('comprobante-modal-backdrop');
    const closeBtn = document.getElementById('btn-cerrar-comprobante');
    const closeFooterBtn = document.getElementById('btn-cerrar-comprobante-footer');
    const printBtn = document.getElementById('btn-imprimir-comprobante');
    
    const cerrar = () => modal.classList.remove('active');
    if (closeBtn) closeBtn.addEventListener('click', cerrar);
    if (closeFooterBtn) closeFooterBtn.addEventListener('click', cerrar);
    
    if (printBtn) {
        printBtn.addEventListener('click', () => {
            // Emular impresión física
            showToast('Enviando ticket a la ticketera térmica...', 'success');
            cerrar();
        });
    }
}

function abrirComprobanteEmitido(ventaResult, ventaDetails) {
    document.getElementById('receipt-title').textContent = ventaDetails.tipoComprobante;
    document.getElementById('receipt-number').textContent = ventaResult.numeroComprobante;
    
    // Fecha y hora formateada
    const fecha = new Date(ventaResult.fechaVenta);
    document.getElementById('receipt-date').textContent = `${fecha.toLocaleDateString()} ${fecha.toLocaleTimeString([], {hour: '2-digit', minute:'2-digit'})}`;
    
    document.getElementById('receipt-client-name').textContent = ventaDetails.nombreCliente;
    
    // Buscar documento del cliente
    let docStr = '00000000';
    if (selectedCliente && selectedCliente.idCliente === ventaDetails.idCliente) {
        docStr = `${selectedCliente.tipoDoc}: ${selectedCliente.numeroDoc}`;
    }
    document.getElementById('receipt-client-doc').textContent = docStr;
    document.getElementById('receipt-cajero').textContent = ventaDetails.nombreCajero;
    
    // Render de items en el comprobante
    const itemsBody = document.getElementById('receipt-items-body');
    itemsBody.innerHTML = ventaDetails.detalles.map(item => `
        <tr>
            <td>${item.nombreProducto}</td>
            <td style="text-align: center;">${item.cantidad}</td>
            <td style="text-align: right;">${item.precioUnit.toFixed(2)}</td>
            <td style="text-align: right;">${item.subtotal.toFixed(2)}</td>
        </tr>
    `).join('');
    
    document.getElementById('receipt-subtotal').textContent = formatCurrency(ventaDetails.subtotal);
    document.getElementById('receipt-igv').textContent = formatCurrency(ventaDetails.igv);
    document.getElementById('receipt-desc').textContent = formatCurrency(ventaDetails.descuento);
    document.getElementById('receipt-total').textContent = formatCurrency(ventaDetails.total);
    document.getElementById('receipt-method').textContent = ventaDetails.metodoPago;
    
    // Mostrar modal
    document.getElementById('comprobante-modal-backdrop').classList.add('active');
    
    // Resetear cliente a genérico por defecto para la siguiente venta
    selectCliente({ idCliente: 1, tipoDoc: 'DNI', numeroDoc: '00000000', nombre: 'Clientes Varios' });
}

// --- CREAR CLIENTE RÁPIDO DESDE EL POS ---
function configurarClienteRapido() {
    const modal = document.getElementById('cliente-rapido-backdrop');
    const openBtn = document.getElementById('pos-btn-nuevo-cliente');
    const closeBtn = document.getElementById('btn-cerrar-cli-rapido');
    const cancelBtn = document.getElementById('btn-cancelar-cli-rapido');
    const form = document.getElementById('cliente-rapido-form');
    
    if (openBtn && modal) {
        openBtn.addEventListener('click', () => {
            form.reset();
            modal.classList.add('active');
        });
    }
    
    const cerrar = () => modal.classList.remove('active');
    if (closeBtn) closeBtn.addEventListener('click', cerrar);
    if (cancelBtn) cancelBtn.addEventListener('click', cerrar);
    
    // Adaptar dinámicamente longitud del input de doc según tipo
    const tipoSel = document.getElementById('cli-rap-tipo');
    const docInput = document.getElementById('cli-rap-doc');
    if (tipoSel && docInput) {
        tipoSel.addEventListener('change', (e) => {
            if (e.target.value === 'DNI') {
                docInput.placeholder = '12345678';
                docInput.maxLength = 8;
            } else {
                docInput.placeholder = '20555444332';
                docInput.maxLength = 11;
            }
        });
    }
    
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const client = {
                tipoDoc: tipoSel.value,
                numeroDoc: docInput.value.trim(),
                nombre: document.getElementById('cli-rap-nombre').value.trim(),
                telefono: '',
                correo: '',
                direccion: ''
            };
            
            try {
                toggleLoader(true);
                const nuevoCli = await VentaService.guardarCliente(client);
                showToast('Cliente registrado y seleccionado.', 'success');
                selectCliente(nuevoCli);
                cerrar();
            } catch (err) {
                showToast(err.message || 'Error al registrar cliente.', 'error');
            } finally {
                toggleLoader(false);
            }
        });
    }
}
