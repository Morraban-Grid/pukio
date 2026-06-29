import AuthService from '../services/authService.js';
import { showToast, toggleLoader } from '../app.js';
import API from '../services/api.js';

// ──────────────────────────────────────────────
// Guardia: solo ADMIN puede acceder a esta página
// ──────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', async () => {
    AuthService.checkGuard();

    const user = AuthService.getCurrentUser();
    if (!user || user.rol !== 'ADMIN') {
        showToast('Acceso denegado. Solo el rol ADMIN puede gestionar usuarios.', 'error');
        setTimeout(() => { window.location.href = 'dashboard.html'; }, 1500);
        return;
    }

    await cargarUsuarios();
    inicializarFormulario();
});

// ──────────────────────────────────────────────
// Estado local
// ──────────────────────────────────────────────
let modoEdicion = false;

// ──────────────────────────────────────────────
// Cargar y renderizar tabla de usuarios
// ──────────────────────────────────────────────
async function cargarUsuarios() {
    const tbody = document.querySelector('#usuarios-table tbody');
    try {
        toggleLoader(true);
        const lista = await API.get('usuarios');
        if (!lista || lista.length === 0) {
            tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--text-secondary);">No hay usuarios registrados.</td></tr>';
            return;
        }
        tbody.innerHTML = lista.map((u, idx) => `
            <tr>
                <td>${idx + 1}</td>
                <td><strong>${u.username}</strong></td>
                <td>${u.nombre}</td>
                <td>
                    <span class="badge ${u.rol === 'ADMIN' ? 'badge-warning' : 'badge-info'}">${u.rol}</span>
                </td>
                <td>
                    <span class="badge ${u.activo ? 'badge-success' : 'badge-danger'}">
                        ${u.activo ? 'Activo' : 'Inactivo'}
                    </span>
                </td>
                <td>
                    <button class="btn btn-ghost btn-sm btn-editar" data-id="${u.idUsuario}" title="Editar usuario">
                        <svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"></path><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"></path></svg>
                    </button>
                    <button class="btn btn-ghost btn-sm btn-toggle-activo" 
                            data-id="${u.idUsuario}" 
                            data-nombre="${u.nombre}" 
                            data-rol="${u.rol}"
                            data-activo="${u.activo}"
                            title="${u.activo ? 'Desactivar' : 'Activar'} usuario">
                        ${u.activo
                            ? '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 12H3m14 0-4 4m4-4-4-4"/><path d="M21 12a9 9 0 1 1-9-9c2.52 0 4.93 1 6.74 2.74L21 8"/></svg>'
                            : '<svg width="15" height="15" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M9 12l2 2 4-4"/><circle cx="12" cy="12" r="10"/></svg>'
                        }
                    </button>
                </td>
            </tr>
        `).join('');

        // Eventos de la tabla
        document.querySelectorAll('.btn-editar').forEach(btn => {
            btn.addEventListener('click', () => abrirEdicion(parseInt(btn.dataset.id), lista));
        });
        document.querySelectorAll('.btn-toggle-activo').forEach(btn => {
            btn.addEventListener('click', () => toggleActivo(btn));
        });
    } catch (err) {
        tbody.innerHTML = '<tr><td colspan="6" style="text-align:center; color:var(--danger);">Error al cargar usuarios.</td></tr>';
        showToast('Error al cargar la lista de usuarios.', 'error');
    } finally {
        toggleLoader(false);
    }
}

// ──────────────────────────────────────────────
// Inicializar eventos del formulario
// ──────────────────────────────────────────────
function inicializarFormulario() {
    document.getElementById('btn-nuevo-usuario').addEventListener('click', abrirNuevo);
    document.getElementById('btn-cancelar-usuario').addEventListener('click', resetFormulario);
    document.getElementById('btn-guardar-usuario').addEventListener('click', guardarUsuario);
    document.getElementById('btn-reset-password').addEventListener('click', restablecerPassword);
}

function abrirNuevo() {
    modoEdicion = false;
    resetFormulario();
    document.getElementById('form-titulo').textContent = 'Nuevo Usuario';
    document.getElementById('grupo-password').style.display = '';
    document.getElementById('grupo-activo').style.display = 'none';
    document.getElementById('seccion-reset-password').style.display = 'none';
    document.getElementById('input-id-usuario').value = '';
    document.getElementById('input-username').disabled = false;
}

function abrirEdicion(idUsuario, lista) {
    const u = lista.find(x => x.idUsuario === idUsuario);
    if (!u) return;

    modoEdicion = true;
    document.getElementById('form-titulo').textContent = 'Editar Usuario';
    document.getElementById('input-id-usuario').value  = u.idUsuario;
    document.getElementById('input-username').value    = u.username;
    document.getElementById('input-username').disabled = true; // username no editable
    document.getElementById('input-nombre').value      = u.nombre;
    document.getElementById('input-rol').value         = u.rol;

    const activoRadios = document.querySelectorAll('input[name="activo"]');
    activoRadios.forEach(r => { r.checked = (r.value === String(u.activo)); });

    document.getElementById('grupo-password').style.display        = 'none';
    document.getElementById('grupo-activo').style.display          = '';
    document.getElementById('seccion-reset-password').style.display = '';
    document.getElementById('input-nueva-password').value          = '';
}

function resetFormulario() {
    document.getElementById('form-titulo').textContent          = 'Nuevo Usuario';
    document.getElementById('input-id-usuario').value           = '';
    document.getElementById('input-username').value             = '';
    document.getElementById('input-username').disabled          = false;
    document.getElementById('input-nombre').value               = '';
    document.getElementById('input-rol').value                  = 'CAJERO';
    document.getElementById('input-password').value             = '';
    document.getElementById('input-nueva-password').value       = '';
    document.getElementById('grupo-password').style.display     = '';
    document.getElementById('grupo-activo').style.display       = 'none';
    document.getElementById('seccion-reset-password').style.display = 'none';
    modoEdicion = false;
}

// ──────────────────────────────────────────────
// Guardar (crear o actualizar) usuario
// ──────────────────────────────────────────────
async function guardarUsuario() {
    const nombre   = document.getElementById('input-nombre').value.trim();
    const rol      = document.getElementById('input-rol').value;
    const username = document.getElementById('input-username').value.trim();
    const password = document.getElementById('input-password').value;
    const idStr    = document.getElementById('input-id-usuario').value;

    if (!nombre) { showToast('El nombre completo es obligatorio.', 'warning'); return; }

    try {
        toggleLoader(true);
        if (modoEdicion) {
            const activoVal = document.querySelector('input[name="activo"]:checked')?.value === 'true';
            await API.put('usuarios', {
                idUsuario: parseInt(idStr),
                nombre,
                rol,
                activo: activoVal
            });
            showToast('Usuario actualizado correctamente.', 'success');
        } else {
            if (!username) { showToast('El nombre de usuario es obligatorio.', 'warning'); return; }
            if (!password || password.length < 6) { showToast('La contraseña debe tener al menos 6 caracteres.', 'warning'); return; }
            await API.post('usuarios', { username, password, nombre, rol });
            showToast('Usuario registrado correctamente.', 'success');
        }
        resetFormulario();
        await cargarUsuarios();
    } catch (err) {
        showToast(err.message || 'Error al guardar el usuario.', 'error');
    } finally {
        toggleLoader(false);
    }
}

// ──────────────────────────────────────────────
// Activar / desactivar usuario (RF-52)
// ──────────────────────────────────────────────
async function toggleActivo(btn) {
    const id      = parseInt(btn.dataset.id);
    const nombre  = btn.dataset.nombre;
    const rol     = btn.dataset.rol;
    const activo  = btn.dataset.activo === 'true';
    const accion  = activo ? 'desactivar' : 'activar';

    if (!confirm(`¿Está seguro de que desea ${accion} al usuario "${nombre}"?`)) return;

    try {
        toggleLoader(true);
        await API.put('usuarios', { idUsuario: id, nombre, rol, activo: !activo });
        showToast(`Usuario ${accion === 'desactivar' ? 'desactivado' : 'activado'} correctamente.`, 'success');
        await cargarUsuarios();
    } catch (err) {
        showToast(err.message || `Error al ${accion} el usuario.`, 'error');
    } finally {
        toggleLoader(false);
    }
}

// ──────────────────────────────────────────────
// Restablecer contraseña (RF-53)
// ──────────────────────────────────────────────
async function restablecerPassword() {
    const idStr        = document.getElementById('input-id-usuario').value;
    const nuevaPassword = document.getElementById('input-nueva-password').value;

    if (!idStr) { showToast('Seleccione un usuario para restablecer su contraseña.', 'warning'); return; }
    if (!nuevaPassword || nuevaPassword.length < 6) {
        showToast('La nueva contraseña debe tener al menos 6 caracteres.', 'warning');
        return;
    }

    try {
        toggleLoader(true);
        await API.put('usuarios/password', {
            idUsuario: parseInt(idStr),
            password:  nuevaPassword
        });
        document.getElementById('input-nueva-password').value = '';
        showToast('Contraseña actualizada correctamente.', 'success');
    } catch (err) {
        showToast(err.message || 'Error al restablecer la contraseña.', 'error');
    } finally {
        toggleLoader(false);
    }
}
