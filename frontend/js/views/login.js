import AuthService from '../services/authService.js';
import { showToast, toggleLoader } from '../app.js';

document.addEventListener('DOMContentLoaded', () => {
    // Verificar si ya tiene sesión activa (redirige automáticamente)
    AuthService.checkLoginGuard();
    
    const form = document.getElementById('login-form');
    if (!form) return;
    
    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        
        const usernameEl = document.getElementById('username');
        const passwordEl = document.getElementById('password');
        
        const username = usernameEl.value.trim();
        const password = passwordEl.value;
        
        try {
            toggleLoader(true);
            
            const user = await AuthService.login(username, password);
            
            showToast(`¡Bienvenido, ${user.nombre}!`, 'success');
            
            // Retrasar redirección brevemente para permitir la visualización del Toast de éxito
            setTimeout(() => {
                window.location.href = 'pages/dashboard.html';
            }, 600);
            
        } catch (error) {
            toggleLoader(false);
            showToast(error.message || 'Error al conectar con el servidor.', 'error');
            passwordEl.value = '';
            passwordEl.focus();
        }
    });
});
