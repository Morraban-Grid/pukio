import AuthService from './services/authService.js';

// --- UTILIDADES GLOBALES DE INTERFAZ (Exportadas para uso en vistas) ---

// Mostrar Notificaciones Toast
export function showToast(message, type = 'info') {
    let container = document.getElementById('toast-container');
    if (!container) {
        container = document.createElement('div');
        container.id = 'toast-container';
        document.body.appendChild(container);
    }
    
    const toast = document.createElement('div');
    toast.className = `toast toast-${type}`;
    
    // Icono SVG según tipo
    let iconSvg = '';
    if (type === 'success') {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M22 11.08V12a10 10 0 1 1-5.93-9.14"></path><polyline points="22 4 12 14.01 9 11.01"></polyline></svg>`;
    } else if (type === 'error') {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="15" y1="9" x2="9" y2="15"></line><line x1="9" y1="9" x2="15" y2="15"></line></svg>`;
    } else if (type === 'warning') {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
    } else {
        iconSvg = `<svg width="18" height="18" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
    }
    
    toast.innerHTML = `
        <span style="display:flex; color: inherit;">${iconSvg}</span>
        <div class="toast-message">${message}</div>
        <button class="toast-close">&times;</button>
    `;
    
    container.appendChild(toast);
    
    // Auto-eliminar después de 4 segundos
    const removeTimeout = setTimeout(() => {
        toast.style.animation = 'fadeIn 0.2s reverse ease-in forwards';
        setTimeout(() => toast.remove(), 200);
    }, 4000);
    
    // Botón cerrar manual
    toast.querySelector('.toast-close').addEventListener('click', () => {
        clearTimeout(removeTimeout);
        toast.remove();
    });
}

// Control del Loader / Spinner Global
export function toggleLoader(show = true) {
    let loader = document.querySelector('.app-loader-backdrop');
    if (!loader) {
        loader = document.createElement('div');
        loader.className = 'app-loader-backdrop';
        loader.innerHTML = '<div class="spinner"></div>';
        document.body.appendChild(loader);
    }
    
    if (show) {
        loader.classList.add('active');
    } else {
        loader.classList.remove('active');
    }
}

// Formatear Moneda a Soles (S/.)
export function formatCurrency(amount) {
    return `S/. ${parseFloat(amount).toFixed(2)}`;
}

// --- LÓGICA DE INICIALIZACIÓN DE DISEÑO GLOBAL ---

document.addEventListener('DOMContentLoaded', () => {
    // Si la página actual no es el index (login), verificar guardias de sesión
    const isLoginPage = window.location.pathname.endsWith('index.html') || window.location.pathname === '/' || window.location.pathname.endsWith('frontend/');
    
    if (isLoginPage) {
        AuthService.checkLoginGuard();
        return; // No cargar barra superior ni sidebar si estamos en Login
    }
    
    // Ejecutar Guardia de Seguridad
    AuthService.checkGuard();
    
    const user = AuthService.getCurrentUser();
    
    // Inyectar Sidebar en la página si no existe estructuralmente en el HTML rígido
    // Para simplificar y mantener código limpio, el HTML de cada página incluye el layout base. 
    // Aquí solo inicializaremos sus eventos y lógica dinámica.
    
    // Rellenar información del usuario
    const userNameEl = document.querySelector('.user-name');
    const userRoleEl = document.querySelector('.user-role');
    if (userNameEl && userRoleEl && user) {
        userNameEl.textContent = user.nombre;
        userRoleEl.textContent = user.rol;
    }
    
    // Cierre de Sesión
    const logoutBtn = document.getElementById('logout-btn');
    if (logoutBtn) {
        logoutBtn.addEventListener('click', (e) => {
            e.preventDefault();
            AuthService.logout();
        });
    }
    
    // Sidebar Colapsable Toggle
    const sidebarToggle = document.getElementById('sidebar-toggle-btn');
    if (sidebarToggle) {
        // Cargar estado colapsado anterior
        if (localStorage.getItem('pukio_sidebar_collapsed') === 'true') {
            document.body.classList.add('sidebar-collapsed');
        }
        
        sidebarToggle.addEventListener('click', () => {
            document.body.classList.toggle('sidebar-collapsed');
            localStorage.setItem('pukio_sidebar_collapsed', document.body.classList.contains('sidebar-collapsed'));
        });
    }
    
    // Resaltar Link Activo en el Sidebar
    const path = window.location.pathname;
    const menuLinks = document.querySelectorAll('.sidebar-menu .menu-item');
    
    menuLinks.forEach(item => {
        const link = item.querySelector('a');
        if (link) {
            const href = link.getAttribute('href');
            if (path.includes(href)) {
                item.classList.add('active');
            } else {
                item.classList.remove('active');
            }
        }
    });

    // Control de Permisos por Rol
    if (user && user.rol === 'CAJERO') {
        // Ocultar Reportes y otros módulos restringidos para Cajeros si corresponde
        // En PUKIO, reportes.html es restringido o limitado a ADMIN según requerimientos generales
        const reportMenuItem = document.querySelector('.menu-item-reportes');
        if (reportMenuItem) {
            reportMenuItem.style.display = 'none';
        }
    }
});
