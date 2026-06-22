import API from './api.js';

const AuthService = {
    async login(username, password) {
        if (!username || !password) {
            throw new Error('Debe completar todos los campos.');
        }
        
        const user = await API.post('auth/login', { username, password });
        localStorage.setItem('pukio_sesion', JSON.stringify(user));
        return user;
    },
    
    logout() {
        localStorage.removeItem('pukio_sesion');
        window.location.href = '/frontend/index.html';
    },
    
    getCurrentUser() {
        try {
            return JSON.parse(localStorage.getItem('pukio_sesion')) || null;
        } catch (e) {
            return null;
        }
    },
    
    isAuthenticated() {
        return this.getCurrentUser() !== null;
    },
    
    // Guardia de seguridad para redirigir si no hay sesión activa
    checkGuard() {
        if (!this.isAuthenticated()) {
            window.location.href = '/frontend/index.html';
        }
    },
    
    // Guardia para redirigir fuera del login si ya tiene sesión activa
    checkLoginGuard() {
        if (this.isAuthenticated()) {
            window.location.href = '/frontend/pages/dashboard.html';
        }
    }
};

export default AuthService;
