import VentaService from '../services/ventaService.js';
import { showToast, toggleLoader } from '../app.js';

let listadoClientes = [];

document.addEventListener('DOMContentLoaded', async () => {
    // Cargar clientes iniciales
    await cargarClientes();
    
    // Evento de búsqueda instantánea
    const searchInput = document.getElementById('search-input');
    if (searchInput) {
        searchInput.addEventListener('input', debounce(async (e) => {
            const query = e.target.value.trim();
            await realizarBusqueda(query);
        }, 250));
    }
    
    // Configuración del modal
    const modalBackdrop = document.getElementById('cliente-modal-backdrop');
    const btnNuevo = document.getElementById('btn-nuevo-cliente');
    const btnCerrar = document.getElementById('btn-cerrar-modal');
    const btnCancelar = document.getElementById('btn-cancelar-modal');
    const form = document.getElementById('cliente-form');
    
    if (btnNuevo && modalBackdrop) {
        btnNuevo.addEventListener('click', () => {
            limpiarFormulario();
            document.getElementById('modal-title-text').textContent = 'Registrar Cliente';
            modalBackdrop.classList.add('active');
        });
    }
    
    const cerrarModal = () => {
        if (modalBackdrop) modalBackdrop.classList.remove('active');
    };
    
    if (btnCerrar) btnCerrar.addEventListener('click', cerrarModal);
    if (btnCancelar) btnCancelar.addEventListener('click', cerrarModal);
    
    // Validar longitud máxima dinámica del input de número de documento según Tipo Doc
    const tipoDocSelect = document.getElementById('cli-tipodoc');
    const numDocInput = document.getElementById('cli-numdoc');
    if (tipoDocSelect && numDocInput) {
        tipoDocSelect.addEventListener('change', (e) => {
            if (e.target.value === 'DNI') {
                numDocInput.placeholder = 'Ej. 12345678';
                numDocInput.maxLength = 8;
            } else {
                numDocInput.placeholder = 'Ej. 20555444332';
                numDocInput.maxLength = 11;
            }
        });
    }
    
    // Envío del Formulario (Crear/Editar)
    if (form) {
        form.addEventListener('submit', async (e) => {
            e.preventDefault();
            
            const idVal = document.getElementById('cli-id').value;
            const cliente = {
                tipoDoc: document.getElementById('cli-tipodoc').value,
                numeroDoc: numDocInput.value.trim(),
                nombre: document.getElementById('cli-nombre').value.trim(),
                telefono: document.getElementById('cli-telefono').value.trim(),
                correo: document.getElementById('cli-correo').value.trim(),
                direccion: document.getElementById('cli-direccion').value.trim()
            };
            
            if (idVal) {
                cliente.idCliente = parseInt(idVal);
            }
            
            try {
                toggleLoader(true);
                await VentaService.guardarCliente(cliente);
                showToast(`Cliente ${idVal ? 'actualizado' : 'registrado'} correctamente.`, 'success');
                cerrarModal();
                await cargarClientes();
            } catch (err) {
                showToast(err.message || 'Error al guardar el cliente.', 'error');
            } finally {
                toggleLoader(false);
            }
        });
    }
});

async function cargarClientes() {
    try {
        toggleLoader(true);
        listadoClientes = await VentaService.listarClientes();
        renderClientesTable(listadoClientes);
    } catch (err) {
        showToast('Error al cargar la lista de clientes.', 'error');
    } finally {
        toggleLoader(false);
    }
}

async function realizarBusqueda(q) {
    try {
        if (!q) {
            await cargarClientes();
            return;
        }
        const filtrados = await VentaService.buscarClientes(q);
        renderClientesTable(filtrados);
    } catch (err) {
        showToast('Error en la búsqueda.', 'error');
    }
}

function renderClientesTable(list) {
    const tbody = document.querySelector('#clientes-table tbody');
    if (!tbody) return;
    
    if (list.length === 0) {
        tbody.innerHTML = `
            <tr>
                <td colspan="7" style="text-align: center; color: var(--text-muted); padding: 40px;">
                    No se encontraron clientes en la base de datos.
                </td>
            </tr>
        `;
        return;
    }
    
    tbody.innerHTML = '';
    
    list.forEach(c => {
        const tr = document.createElement('tr');
        
        // El cliente genérico destaca o tiene restricciones de borrado
        const isGeneric = c.idCliente === 1;
        
        tr.innerHTML = `
            <td><span class="badge ${c.tipoDoc === 'DNI' ? 'badge-info' : 'badge-success'}">${c.tipoDoc}</span></td>
            <td><code>${c.numeroDoc}</code></td>
            <td style="font-weight: 600;">${c.nombre}</td>
            <td>${c.telefono || '-'}</td>
            <td>${c.correo || '-'}</td>
            <td style="font-size: 13px; color: var(--text-secondary); max-width: 250px; overflow: hidden; text-overflow: ellipsis; white-space: nowrap;">${c.direccion || '-'}</td>
            <td style="text-align: right;">
                <div style="display: flex; justify-content: flex-end; gap: 8px;">
                    <button class="btn btn-secondary btn-edit-cli" data-id="${c.idCliente}" style="padding: 6px 12px; font-size: 12px;">
                        Editar
                    </button>
                    <button class="btn btn-danger btn-delete-cli" data-id="${c.idCliente}" ${isGeneric ? 'disabled style="opacity: 0.4; cursor: not-allowed; padding: 6px 12px; font-size: 12px;"' : 'style="padding: 6px 12px; font-size: 12px;"'}>
                        Eliminar
                    </button>
                </div>
            </td>
        `;
        
        // Asociar eventos
        tr.querySelector('.btn-edit-cli').addEventListener('click', () => abrirEditar(c));
        if (!isGeneric) {
            tr.querySelector('.btn-delete-cli').addEventListener('click', () => eliminarCliente(c));
        }
        
        tbody.appendChild(tr);
    });
}

function abrirEditar(c) {
    limpiarFormulario();
    document.getElementById('cli-id').value = c.idCliente;
    document.getElementById('cli-tipodoc').value = c.tipoDoc;
    
    const numDocInput = document.getElementById('cli-numdoc');
    numDocInput.value = c.numeroDoc;
    numDocInput.maxLength = c.tipoDoc === 'DNI' ? 8 : 11;
    
    document.getElementById('cli-nombre').value = c.nombre;
    document.getElementById('cli-telefono').value = c.telefono || '';
    document.getElementById('cli-correo').value = c.correo || '';
    document.getElementById('cli-direccion').value = c.direccion || '';
    
    document.getElementById('modal-title-text').textContent = 'Editar Cliente';
    document.getElementById('cliente-modal-backdrop').classList.add('active');
}

async function eliminarCliente(c) {
    if (confirm(`¿Está seguro de que desea eliminar lógicamente al cliente "${c.nombre}"?`)) {
        try {
            toggleLoader(true);
            await VentaService.eliminarCliente(c.idCliente);
            showToast('Cliente eliminado correctamente.', 'success');
            await cargarClientes();
        } catch (err) {
            showToast(err.message || 'Error al eliminar el cliente.', 'error');
        } finally {
            toggleLoader(false);
        }
    }
}

function limpiarFormulario() {
    document.getElementById('cli-id').value = '';
    document.getElementById('cliente-form').reset();
    document.getElementById('cli-numdoc').maxLength = 8;
    document.getElementById('cli-numdoc').placeholder = 'Ej. 12345678';
}

function debounce(func, delay) {
    let timeoutId;
    return function (...args) {
        if (timeoutId) clearTimeout(timeoutId);
        timeoutId = setTimeout(() => {
            func.apply(this, args);
        }, delay);
    };
}
