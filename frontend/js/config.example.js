/* Configuracion Global del Frontend PUKIO
 * Copia este archivo a config.js y ajusta los valores.
 */

const CONFIG = {
    // URL base de la API del backend Java (Tomcat 10+)
    API_BASE_URL: 'http://localhost:8080/backend/api',

    // Si es true, la aplicacion no hara peticiones HTTP y simulara la BD localmente
    // usando localStorage como almacenamiento temporal.
    //
    // ⚠️  ADVERTENCIA DE PRODUCCION: Mantener MOCK_MODE en false para cualquier
    //     entorno real o de pruebas. Con MOCK_MODE=true, TODAS las operaciones
    //     (ventas, productos, clientes, etc.) se guardan solo en el navegador y
    //     NO se persisten en la base de datos Oracle. Los datos se pierden al
    //     limpiar el historial del navegador.
    MOCK_MODE: false,

    // Tiempo de retraso de red simulado (ms) en MOCK_MODE
    MOCK_DELAY: 400
};

export default CONFIG;
