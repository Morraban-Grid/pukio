# Bitácora de Tareas de Desarrollo — PUKIO

> Reconstrucción ordenada de las tareas realizadas durante el desarrollo de PUKIO, inferida a partir de la arquitectura final, los comentarios `@Deprecated` encontrados en el código, las notas de migración del `README.md` y la organización de los scripts SQL numerados. Las tareas están agrupadas por fases siguiendo un ciclo de vida típico de un proyecto POS construido de forma incremental.

---

## Fase 0 — Definición y Planeamiento

- [x] **T0.1** Definir el alcance del producto: sistema POS web para pequeños negocios (bodegas/minimarkets) con módulos de Productos, Clientes, Ventas, Reportes y Data Warehouse analítico.
- [x] **T0.2** Seleccionar el stack tecnológico: backend Java 21 + Jakarta Servlets (sin framework como Spring) desplegado en Tomcat 10.1.x; frontend JavaScript vanilla sin build tools; base de datos Oracle 21c XE.
- [x] **T0.3** Definir la separación de esquemas de base de datos: `PUKIO_DB` (OLTP/transaccional) y `PUKIO_DWH` (OLAP/analítico), anticipando la necesidad de reportes de negocio desacoplados del modelo transaccional.
- [x] **T0.4** Definir convención de nombres de base de datos en español y mayúsculas (`PRODUCTOS`, `ID_PRODUCTO`, etc.), consistente en todo el proyecto.

## Fase 1 — Infraestructura de Base de Datos

- [x] **T1.1** Crear script `00_crear_usuario.sql`: aprovisionar los usuarios/esquemas `PUKIO_DB` y `PUKIO_DWH` en Oracle, con lógica de limpieza idempotente (`DROP USER ... CASCADE` si ya existe) para permitir recreaciones limpias durante el desarrollo.
- [x] **T1.2** Otorgar privilegios cruzados (`SELECT/INSERT/UPDATE/DELETE ANY TABLE`) entre ambos esquemas para habilitar el proceso ETL desde `PUKIO_DWH` hacia `PUKIO_DB` (lectura) sin depender de sinónimos.
- [x] **T1.3** Crear script `01_crear_tablas.sql` con el modelo transaccional completo: `USUARIOS`, `CATEGORIAS`, `PROVEEDORES`, `PRODUCTOS`, `CLIENTES`, `VENTAS`, `DETALLE_VENTA`.
- [x] **T1.4** Definir restricciones de integridad: claves primarias `IDENTITY`, `UNIQUE` en campos de negocio (`USERNAME`, `CODIGO`, `RUC`, `NUMERO_DOC`, `NUMERO_COMPROBANTE`), `CHECK` en `ROL` (`ADMIN`/`CAJERO`), y claves foráneas entre `PRODUCTOS`→`CATEGORIAS`/`PROVEEDORES` y `VENTAS`→`CLIENTES`/`USUARIOS`.
- [x] **T1.5** Crear la secuencia `SEQ_COMPROBANTE` para la numeración correlativa de comprobantes de venta.
- [x] **T1.6** Crear índices de soporte a búsquedas frecuentes: `IDX_PROD_CODIGO`, `IDX_PROD_NOMBRE`, `IDX_CLI_NUMDOC`, `IDX_VENTA_FECHA`, `IDX_DET_VENTA`.
- [x] **T1.7** Crear script `02_insertar_datos.sql` con datos semilla: usuarios `admin`/`cajero`, 7 categorías típicas de bodega, 2 proveedores, cliente genérico y 8 productos de ejemplo.
- [x] **T1.8** Crear script `99_eliminar_todo.sql` para desmontar el esquema transaccional completo (`DROP TABLE ... CASCADE CONSTRAINTS` + `DROP SEQUENCE`) y soportar ciclos rápidos de recreación durante el desarrollo.

## Fase 2 — Lógica de Negocio en Base de Datos (PL/SQL)

- [x] **T2.1** Crear script `04_crear_sp.sql` con el procedimiento almacenado `SP_REGISTRAR_VENTA`, encargado de generar el número de comprobante correlativo (`BOL-######` / `FAC-######`) e insertar la cabecera de venta en una sola operación atómica.
- [x] **T2.2** Implementar manejo de excepciones en el procedimiento (`RAISE_APPLICATION_ERROR(-20002, ...)`) para envolver cualquier error inesperado al registrar la cabecera.
- [x] **T2.3** Crear el trigger `TRG_DETALLE_VENTA_STOCK` (`BEFORE INSERT ON DETALLE_VENTA`) para validar stock disponible y descontarlo automáticamente, garantizando la integridad del inventario a nivel de motor de base de datos (no solo en la capa de aplicación).
- [x] **T2.4** Definir el código de error de negocio `-20001` ("Stock insuficiente") como contrato entre la capa PL/SQL y la capa Java (capturado explícitamente en `VentaDAO.insertar`).

## Fase 3 — Backend: Fundaciones y Configuración

- [x] **T3.1** Configurar el proyecto Maven (`pom.xml`): empaquetado `war`, Java 21, dependencias iniciales (`jakarta.servlet-api`, `gson`, `ojdbc11`, `jbcrypt`).
- [x] **T3.2** Crear `DatabaseConfig` para cargar la configuración de conexión (`host`, `port`, `service`, `user`, `password`, `cors.allowed.origin`) desde `config.properties` en el classpath, evitando credenciales embebidas en el código fuente.
- [x] **T3.3** Crear `config.properties.example` como plantilla versionable, dejando `config.properties` real fuera del control de versiones.
- [x] **T3.4** Crear `AppConfig` con constantes de la aplicación: nombre, versión, datos de la empresa emisora, tasa de IGV (18%) y paleta de colores corporativa.
- [x] **T3.5** Configurar `web.xml`: tiempo de expiración de sesión (30 minutos) y cookies de sesión `HttpOnly`.

## Fase 4 — Backend: Capa de Acceso a Datos (DAO) — Versión Inicial

- [x] **T4.1** Implementar `ConexionDB` en su versión inicial como **Singleton clásico** (`getInstance().getConexion()`), manejando manualmente `commit()`/`rollback()`/`cerrar()` desde cada DAO.
- [x] **T4.2** Implementar `UsuarioDAO`, `ProductoDAO`, `ClienteDAO`, `ProveedorDAO`, `CategoriaDAO` y `VentaDAO` usando JDBC puro (`PreparedStatement`/`CallableStatement`), con `try-with-resources` para garantizar el cierre de recursos.
- [x] **T4.3** Implementar en cada DAO transaccional el patrón `setAutoCommit(false)` → operación → `commit()` / `rollback()` en caso de excepción.
- [x] **T4.4** Implementar mapeo manual de `ResultSet` a modelos de dominio (métodos `mapear()` privados en cada DAO) en lugar de un ORM.

## Fase 5 — Backend: Refactor de Capa de Conexión (HikariCP)

> Esta fase se infiere de los métodos `@Deprecated` conservados en `ConexionDB.java`, que documentan explícitamente la migración desde el patrón Singleton manual hacia un pool de conexiones administrado.

- [x] **T5.1** Incorporar la dependencia `HikariCP` al `pom.xml` para introducir un pool de conexiones JDBC real en lugar de una única conexión compartida.
- [x] **T5.2** Reescribir `ConexionDB` para inicializar un `HikariDataSource` estático (tamaño máximo de pool: 10, mínimo idle: 2, timeout de conexión: 30s, vida máxima: 30 min) configurado a partir de `DatabaseConfig`.
- [x] **T5.3** Exponer el nuevo método estático `ConexionDB.getConnection()` como API recomendada, pensado para usarse con `try-with-resources`.
- [x] **T5.4** Mantener los métodos antiguos (`getInstance()`, `getConexion()`, `commit()`, `rollback()`, `cerrar()`) marcados con `@Deprecated` como *no-ops* o *wrappers*, para no romper compatibilidad con código DAO existente durante la transición.
- [x] **T5.5** Verificar que todos los DAOs migraran a usar `ConexionDB.getConnection()` directamente dentro de bloques `try-with-resources`.

## Fase 6 — Backend: Seguridad y Autenticación

- [x] **T6.1** Implementar `Usuario` (modelo) con campo `passwordHash` y rol restringido a `ADMIN`/`CAJERO`.
- [x] **T6.2** Implementar autenticación inicial de contraseñas (versión histórica, según nota de migración del README): hashing con **SHA-256**.
- [x] **T6.3** Detectar la insuficiencia de SHA-256 sin sal/factor de costo configurable para almacenamiento de contraseñas y planificar la migración a un algoritmo de hashing adaptativo.
- [x] **T6.4** Incorporar la dependencia `jbcrypt` y crear `SecurityUtil` con los métodos `hash(plain)` (factor de costo 12) y `verificar(plain, hash)`.
- [x] **T6.5** Migrar la columna `PASSWORD_HASH` y los datos semilla (`02_insertar_datos.sql`) para almacenar hashes BCrypt en lugar de SHA-256.
- [x] **T6.6** Documentar en el `README.md` el procedimiento manual de remediación para usuarios preexistentes con hash SHA-256 (regenerar con `SecurityUtil.hash()` e insertar manualmente).
- [x] **T6.7** Implementar `AuthService.login()` con validaciones de campos vacíos, existencia de usuario, estado activo y verificación de contraseña vía BCrypt.
- [x] **T6.8** Implementar `AuthServlet` con los endpoints `POST /api/auth/login`, `POST /api/auth/logout` y `GET /api/auth/session`, gestionando la sesión vía `HttpSession`.
- [x] **T6.9** Implementar `SecurityFilter` para interceptar todas las rutas `/api/*`, exceptuando login y verificación de sesión, devolviendo `401` si no hay sesión activa.
- [x] **T6.10** Implementar `CorsFilter` con whitelist de origen único leído desde configuración, denegando cabeceras CORS a orígenes no autorizados.

## Fase 7 — Backend: Módulos de Catálogo (Productos, Categorías, Proveedores)

- [x] **T7.1** Implementar `Producto` (modelo) incluyendo el método de dominio `isStockBajo()`.
- [x] **T7.2** Implementar `ProductoService` con reglas de validación (código y nombre obligatorios, precio de venta > 0, stock ≥ 0) antes de delegar al DAO.
- [x] **T7.3** Implementar `ProductoServlet` con soporte para listar, buscar por nombre/código, listar bajo stock, crear, actualizar y eliminar (lógicamente) productos.
- [x] **T7.4** Implementar `Categoria` y `CategoriaDAO`/`CategoriaServlet` para el catálogo de categorías (listar activas, alta y edición vía DAO).
- [x] **T7.5** Implementar `Proveedor` y `ProveedorDAO`/`ProveedorServlet` para el catálogo de proveedores (listar activos, alta, edición, búsqueda por RUC).
- [x] **T7.6** Definir la baja lógica (`ACTIVO=0`) como estrategia de borrado consistente en todos los catálogos, preservando el historial de ventas asociado.

## Fase 8 — Backend: Módulo de Clientes

- [x] **T8.1** Implementar `Cliente` (modelo) con tipo de documento (`DNI`/`RUC`) y datos de contacto.
- [x] **T8.2** Implementar `ClienteDAO` con búsqueda por número de documento y por nombre (parcial, `LIKE` case-insensitive).
- [x] **T8.3** Implementar `ClienteServlet` con el endpoint compuesto `/api/clientes/buscar` que prioriza la coincidencia exacta por documento y, si no la encuentra, recurre a la búsqueda por nombre.
- [x] **T8.4** Insertar el cliente genérico ("CLIENTE GENERICO", documento `00000000`) en los datos semilla para soportar ventas sin cliente identificado.

## Fase 9 — Backend: Módulo de Ventas (POS)

- [x] **T9.1** Implementar `Venta` y `DetalleVenta` (modelos), con cálculo de subtotal por línea (`precioUnit * cantidad - descuento`) y de totales de cabecera (`calcularTotales()`: subtotal neto, IGV al 18%, total).
- [x] **T9.2** Implementar `VentaService.registrar()` con verificación preventiva de stock (capa aplicación) antes de delegar la persistencia, y validación de que exista un usuario autenticado asignado a la venta.
- [x] **T9.3** Implementar `VentaDAO.insertar()` integrando la llamada al procedimiento `SP_REGISTRAR_VENTA` (`CallableStatement` con parámetros `OUT`) seguida de la inserción en lote (`addBatch`/`executeBatch`) del detalle de venta, todo dentro de una única transacción.
- [x] **T9.4** Capturar específicamente el código de error `-20001` lanzado por el trigger de stock y traducirlo a un mensaje de negocio ("Error de inventario: ...") antes de propagar la excepción.
- [x] **T9.5** Implementar `VentaServlet` con `POST /api/ventas` (requiere sesión, asigna el cajero desde la sesión del servidor, no desde el payload del cliente) y `GET /api/ventas/resumen-hoy` (total e cantidad de ventas del día).
- [x] **T9.6** Implementar en `VentaDAO` las consultas de soporte al dashboard y reportes: `totalVentasHoy()`, `cantidadVentasHoy()`, `listarPorFecha()`, `productosMasVendidos()`.

## Fase 10 — Backend: Reportes, Exportación y Extensibilidad

- [x] **T10.1** Implementar `ReporteService` como fachada sobre `VentaDAO`/`ProductoDAO` para los reportes operativos (ventas por fecha, productos más vendidos).
- [x] **T10.2** Diseñar la interfaz `ReportExporter` (`getFormato()`, `exportar(ventas, OutputStream)`) como punto de extensión para nuevos formatos de exportación.
- [x] **T10.3** Implementar `CsvReportExporter` como primera implementación concreta de `ReportExporter`, incluyendo el escape correcto de campos CSV (comillas, comas, saltos de línea).
- [x] **T10.4** Registrar `CsvReportExporter` en `META-INF/services/com.pukio.plugin.ReportExporter` para habilitar el descubrimiento dinámico vía `ServiceLoader`.
- [x] **T10.5** Implementar en `ReporteServlet` el endpoint `GET /api/reportes/exportar?formato=csv`, resolviendo el exportador correspondiente en tiempo de ejecución y soportando rango de fechas opcional (por defecto, últimos 30 días).
- [x] **T10.6** Implementar `GET /api/reportes/productos-top?limit=` como ranking configurable de productos más vendidos.

## Fase 11 — Backend: Data Warehouse y Cubo OLAP

- [x] **T11.1** Crear script `03_crear_dwh.sql`: tabla de hechos `DWH_VENTAS` (desnormalizada, con año/mes/día/trimestre precalculados) y tabla agregada `CROSSTAB_VENTAS` (única por año+mes+categoría).
- [x] **T11.2** Crear la vista cross-schema `VW_VENTAS_DETALLADA` en `PUKIO_DWH`, que integra `VENTAS`, `DETALLE_VENTA`, `PRODUCTOS`, `USUARIOS`, `CLIENTES` y `CATEGORIAS` desde `PUKIO_DB`, calculando las dimensiones de tiempo con `EXTRACT()` y `CEIL()`.
- [x] **T11.3** Implementar `DataWarehouseDAO` con `cargarDWH()` (proceso tipo ELT: `DELETE` + `INSERT ... SELECT` desde la vista) y `generarCrossTab()` (agregación `GROUP BY` sobre `DWH_VENTAS`).
- [x] **T11.4** Implementar `DataWarehouseService` como capa de negocio sobre el DAO del DWH.
- [x] **T11.5** Exponer el proceso ETL vía API: `POST /api/dwh/procesar` (ejecuta carga + agregación) y `GET /api/dwh/crosstab` (consulta el cubo materializado), ambos en `ReporteServlet`.
- [x] **T11.6** Implementar utilitarios de línea de comandos independientes del ciclo web, pensados para automatización por tareas programadas:
  - `GenerarDataWarehouse` (equivalente conceptual a un job de carga).
  - `CreateCrossTab` (orquesta carga + agregación en un solo proceso).
  - `ViewCrossTab` (reporte de consola formateado, con totales por año).

## Fase 12 — Frontend: Fundaciones

- [x] **T12.1** Definir la estructura de carpetas sin build tools: `frontend/{css,js,pages}`, con módulos ES nativos.
- [x] **T12.2** Crear `config.example.js` con `API_BASE_URL`, bandera `MOCK_MODE` y `MOCK_DELAY`, replicando la estrategia de configuración por plantilla usada en el backend.
- [x] **T12.3** Implementar `api.js` como cliente HTTP único (`get`/`post`/`put`/`delete`) con manejo centralizado de errores HTTP y redirección automática al login ante `401`/`403`.
- [x] **T12.4** Implementar el enrutador *mock* (`handleMockRequest`) dentro de `api.js`, con un dataset inicial completo (usuarios, categorías, proveedores, productos, clientes) y generación sintética de historial de ventas para poblar dashboards y reportes en modo demo.
- [x] **T12.5** Implementar `app.js` con utilidades transversales de UI: sistema de notificaciones *toast*, loader/spinner global, formateo de moneda (`S/.`), guardas de sesión a nivel de DOM, sidebar colapsable persistido en `localStorage`, resaltado de enlace activo y ocultamiento de "Reportes" para el rol `CAJERO`.
- [x] **T12.6** Definir la hoja de estilos base (`variables.css`, `main.css`, `layout.css`, `components.css`) y una hoja específica por vista (`css/views/*.css`).

## Fase 13 — Frontend: Autenticación y Dashboard

- [x] **T13.1** Construir `index.html` (login) con diseño *glassmorphism* y formulario controlado por `login.js`.
- [x] **T13.2** Implementar `authService.js`: `login()`, `logout()`, `getCurrentUser()`, `isAuthenticated()`, `checkGuard()`, `checkLoginGuard()`, persistiendo la sesión del usuario en `localStorage` (`pukio_sesion`).
- [x] **T13.3** Construir `pages/dashboard.html` y `dashboard.js`: fecha actual localizada en español, tarjetas de métricas (total e cantidad de ventas de hoy), tarjeta de alerta de bajo stock y tabla de productos críticos con severidad visual (stock cero vs. stock bajo).

## Fase 14 — Frontend: Módulos de Catálogo

- [x] **T14.1** Construir `pages/productos.html` y `productos.js`: listado, búsqueda con `debounce`, formulario modal de alta/edición con combos de categoría/proveedor, y desactivación con confirmación.
- [x] **T14.2** Construir `pages/clientes.html` y `clientes.js`: listado, búsqueda con `debounce`, formulario modal con validación de longitud de documento según tipo (DNI/RUC), y desactivación con confirmación.

## Fase 15 — Frontend: Punto de Venta (POS)

- [x] **T15.1** Construir `pages/pos.html` con la disposición de catálogo rápido, carrito, selector de cliente y panel de totales.
- [x] **T15.2** Implementar en `pos.js` la cuadrícula de "productos rápidos" (primeros 12 productos activos) con indicadores visuales de stock (normal/bajo/agotado).
- [x] **T15.3** Implementar autocompletado de búsqueda de cliente (mínimo 2 caracteres) con selección por click y cierre al hacer click fuera del panel.
- [x] **T15.4** Implementar autocompletado de búsqueda de producto y **flujo de escaneo de código de barras**: al presionar `Enter` en el campo de búsqueda, se interpreta el valor como código exacto y se agrega directamente al carrito.
- [x] **T15.5** Implementar la gestión del carrito en memoria (`cart`): agregar, modificar cantidad, eliminar línea, y recálculo reactivo de subtotal/IGV/total ante cada cambio.
- [x] **T15.6** Implementar el modal de "Cliente Rápido" para registrar un cliente nuevo sin abandonar el flujo de venta, adaptando dinámicamente el `maxLength` del campo de documento según el tipo seleccionado.
- [x] **T15.7** Implementar `procesarPago()`: construcción del payload de venta (incluyendo el cajero desde la sesión local), envío a `VentaService.registrarVenta()`, manejo de errores de stock devueltos por el backend, y reinicio del estado del POS tras una venta exitosa.
- [x] **T15.8** Implementar el modal de comprobante emitido (ticket), incluyendo una acción simulada de "Enviar a impresora térmica" y el reseteo automático del cliente seleccionado a "Clientes Varios" al cerrar la venta.

## Fase 16 — Frontend: Servicios de Dominio

- [x] **T16.1** Implementar `productoService.js`: validaciones de cliente espejo a las del backend (código/nombre obligatorios, precio > 0, stock ≥ 0) antes de invocar la API, más accesos a categorías y proveedores para poblar combos.
- [x] **T16.2** Implementar `ventaService.js`: operaciones de clientes (buscar, listar, guardar con validación de documento, eliminar), operaciones de venta (registrar, resumen de hoy) y operaciones de *Business Intelligence* (reporte de ventas por fecha, top productos, consulta y disparo del proceso DWH/OLAP).

## Fase 17 — Frontend: Reportes y Business Intelligence

- [x] **T17.1** Construir `pages/reportes.html` y `reportes.js`: filtro de ventas por rango de fechas con valores por defecto, selector de "top N" productos más vendidos, y botón para disparar manualmente el proceso ETL del DWH.
- [x] **T17.2** Implementar el renderizado de la tabla cruzada (cross-tab) de ventas por año/mes/categoría en el frontend, con traducción de número de mes a nombre abreviado en español.
- [x] **T17.3** *(Pendiente / no completada)* Exponer en la UI de Reportes un control para invocar `GET /api/reportes/exportar?formato=csv`, funcionalidad que ya existe a nivel de backend (`CsvReportExporter`) pero que no tiene disparador visible en `reportes.html`.

## Fase 18 — Empaquetado, Despliegue y Documentación

- [x] **T18.1** Configurar el plugin `maven-war-plugin` (`failOnMissingWebXml=false`) para generar `backend.war` con nombre final fijo (`backend`).
- [x] **T18.2** Redactar el `README.md` con la guía paso a paso de instalación: requisitos previos (Docker, JDK 21, Maven, Tomcat, DBeaver), configuración de archivos `*.example`, levantamiento de Oracle XE en Docker, orden estricto de ejecución de los scripts SQL, compilación y despliegue del WAR, y arranque del frontend estático.
- [x] **T18.3** Documentar las credenciales de acceso por defecto (`admin`/`admin123`, `cajero`/`cajero123`) y la política de hashing (BCrypt, factor 12).
- [x] **T18.4** Redactar la sección de *Troubleshooting* del README cubriendo: conflicto de puerto 1521, verificación de readiness del contenedor Oracle, error de `config.properties` faltante, fallo de descarga del driver `ojdbc11`, y el procedimiento de remediación para contraseñas heredadas en SHA-256.

---

## Backlog / Trabajo Identificado como Pendiente

> Inferido por comparación entre las capacidades del backend y lo realmente expuesto en el frontend, y por la ausencia de ciertos artefactos esperados en un proyecto de este tipo.

- [ ] **B1** Exponer en `reportes.html` un control de UI para la exportación CSV (`/api/reportes/exportar`), ya implementada en el backend.
- [ ] **B2** Construir una vista de administración de usuarios (`UsuarioDAO` y modelo `Usuario` existen, pero no hay `UsuarioServlet` ni página/vista de frontend dedicada).
- [ ] **B3** Construir vistas de frontend dedicadas para Categorías y Proveedores como pantallas independientes (actualmente solo se consumen como combos dentro del formulario de Productos).
- [ ] **B4** Automatizar la ejecución periódica de `GenerarDataWarehouse`/`CreateCrossTab` (por ejemplo, mediante un job programado nocturno) en lugar de depender exclusivamente del disparo manual desde la UI de Reportes.
- [ ] **B5** Agregar pruebas automatizadas (unitarias/integración); no se identificaron carpetas `src/test` ni frameworks de testing en el `pom.xml`.