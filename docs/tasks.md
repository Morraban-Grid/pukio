# Tasks Document — Sistema POS Pukio

## Convenciones

| Símbolo | Significado |
|---------|-------------|
| `[ ]`   | Tarea pendiente |
| `[x]`   | Tarea completada 
| **TASK-E1-XX** | Entregable 1, tarea número XX |
| **TASK-E2-XX** | Entregable 2, tarea número XX |
| **TASK-E3-XX** | Entregable 3, tarea número XX |
| **TASK-E4-XX** | Entregable 4, tarea número XX |
| **TASK-SEC-XX** | Tarea transversal de seguridad de credenciales (aplica a todos los entregables) |

Cada tarea incluye: identificador, descripción, capa/componente afectado y criterio de aceptación asociado del `requirements.md`.

---

## Entregable 1 — Arquitectura Unitaria con Servidor de Datos

### E1-G1: Configuración Inicial del Proyecto

- [x] **TASK-E1-01** — Crear el repositorio Git del proyecto `pukio` con `.gitignore` para Java/Maven.
- [x] **TASK-E1-02** — Crear el Maven Parent POM (`pukio/pom.xml`) con Java 21, Spring Boot 3.3.5 y declaración de todos los módulos hijos.
- [x] **TASK-E1-03** — Crear los módulos Maven vacíos: `pukio-common`, `pukio-maintenance`, `pukio-pos-client`, `pukio-send-service`, `pukio-update-service`.
- [x] **TASK-E1-04** — Configurar dependencias comunes en el Parent POM: `spring-boot-starter`, `spring-boot-starter-test`, `lombok`, `slf4j`.
- [x] **TASK-E1-05** — Crear el archivo `README.md` raíz con descripción del proyecto y guía de ejecución del Entregable 1.

---

---

### E1-G1-SEC: Seguridad de Credenciales — Configuración Base del Repositorio

> Estas tareas deben completarse **antes** de escribir cualquier línea de código de negocio. Establecen las bases que evitan subir información sensible a GitHub en todos los entregables.

- [x] **TASK-E1-SEC-01** — Ampliar el `.gitignore` creado en TASK-E1-01 con el bloque de exclusiones de seguridad mínimo para el proyecto. El bloque debe incluir como mínimo: *(REQ 7.1, 7.5)*
  ```
  # ============================================================
  # SEGURIDAD — Archivos que NUNCA deben subirse a GitHub
  # ============================================================
  application-secrets.properties
  .env
  .env.*
  !.env.template
  *.jks
  *.p12
  *.pem
  *.key
  k8s/**/secret.yaml
  ssl/
  secrets/
  ```
- [x] **TASK-E1-SEC-02** — Crear `application-secrets.properties.template` (commiteado) en cada módulo Spring Boot con todas las variables requeridas listadas sin valores, por ejemplo: *(REQ 7.1)*
  ```properties
  # Copiar este archivo a application-secrets.properties y rellenar los valores
  DB_USERNAME=
  DB_PASSWORD=
  SERVER_HOST=
  SERVER_PORT=
  ```
- [x] **TASK-E1-SEC-03** — Verificar que el `application.properties` de cada módulo usa **exclusivamente placeholders** `${VAR}` para host, puerto y credenciales de base de datos, y que no contiene ningún valor hardcodeado. *(REQ 7.2)*
- [x] **TASK-E1-SEC-04** — Añadir al `README.md` raíz una sección visible **"⚠️ Seguridad — Archivos que NUNCA deben subirse a GitHub"** con la tabla de archivos excluidos y las instrucciones de configuración local para nuevos desarrolladores. *(REQ 7.5)*
- [x] **TASK-E1-SEC-05** — Crear el archivo `docs/security.md` con la estrategia completa de gestión de secretos: configuración local (`application-secrets.properties`), procedimiento de onboarding, y el protocolo de emergencia si un secreto es commiteado accidentalmente (rotar credencial → `git filter-repo` → force-push). *(REQ 7.5)*
- [x] **TASK-E1-SEC-06** — Realizar una auditoría manual del primer commit: revisar que ningún archivo commiteado contiene valores reales de contraseñas, IPs internas, puertos o usuarios de base de datos. *(REQ 7.1, 7.2)*

---

### E1-G2: Módulo `pukio-common` — API de Archivos Indexados

- [x] **TASK-E1-06** — Crear el modelo `ProductRecord.java`: campos `sku`, `name`, `price`, `category`, `description`, `deleted`, con serialización binaria (`Serializable`). *(REQ 1.1)*
- [x] **TASK-E1-07** — Crear el modelo `InventoryRecord.java`: campos `sku`, `quantity`, `outOfStock`, `lastUpdated`, con serialización binaria. *(REQ 1.2)*
- [x] **TASK-E1-08** — Crear el modelo `SaleRecord.java`: campos `transactionId`, `timestamp`, `items` (lista de `LineItem`), `total`, `paymentMethod`. *(REQ 1.6)*
- [x] **TASK-E1-09** — Crear el enum `PaymentMethod.java`: `CASH`, `CARD`, `TRANSFER`.
- [x] **TASK-E1-10** — Crear la interfaz `IndexedFileStore<K, V>` con métodos: `insert(K key, V record)`, `update(K key, V record)`, `delete(K key)`, `findByKey(K key)`, `readAll()`, `close()`. *(REQ 1.1)*
- [x] **TASK-E1-11** — Implementar `BTreeIndexedFileStore<K, V>` que persiste registros en archivo binario usando índice B-tree en memoria serializado a disco. *(REQ 1.1 — O(log n))*
- [x] **TASK-E1-12** — Implementar `HashIndexedFileStore<K, V>` alternativa con índice hash para búsquedas O(1). *(REQ 1.1)*
- [x] **TASK-E1-13** — Implementar mecanismo de **file locking** (`FileLock` de NIO) para prevenir escrituras concurrentes. *(REQ 1.2)*
- [x] **TASK-E1-14** — Implementar soporte de **lecturas concurrentes** con `ReadWriteLock`. *(REQ 1.2)*
- [x] **TASK-E1-15** — Implementar **soft-delete**: el método `delete()` marca `deleted=true` sin borrar físicamente el registro. *(REQ 1.1)*
- [x] **TASK-E1-16** — Escribir tests unitarios para `BTreeIndexedFileStore`: insert, update, delete, findByKey, concurrencia. *(REQ 1.1, 1.2)*

---

### E1-G3: Módulo `pukio-maintenance` — Sistema de Mantenimiento

- [x] **TASK-E1-17** — Crear la clase principal `MaintenanceApplication.java` con menú de consola (y UI básica Swing).
- [x] **TASK-E1-18** — Implementar `ProductService.java`: métodos `createProduct()`, `updateProduct()`, `deleteProduct()`, `findBySku()`, `listAll()`. Usa `IndexedFileStore<String, ProductRecord>`. *(REQ 1.1)*
- [x] **TASK-E1-19** — Implementar `InventoryService.java`: métodos `updateStock()`, `decrementStock()`, `flagOutOfStock()`, `getStock()`. Usa `IndexedFileStore<String, InventoryRecord>`. *(REQ 1.2)*
- [x] **TASK-E1-20** — Implementar validación de SKU único al crear producto (búsqueda en índice antes de insertar). *(REQ 1.1)*
- [x] **TASK-E1-21** — Implementar lógica de `outOfStock`: cuando `quantity == 0`, actualizar flag en el registro. *(REQ 1.2)*
- [x] **TASK-E1-22** — Configurar rutas de archivos indexados via `application.properties` (`pukio.files.products`, `pukio.files.inventory`).
- [x] **TASK-E1-23** — Escribir tests de integración para `ProductService` e `InventoryService`. *(REQ 1.1, 1.2)*

---

### E1-G4: Módulo `pukio-pos-client` — Terminal de Ventas Local

- [x] **TASK-E1-24** — Crear la clase principal `PosClientApplication.java` con menú de consola para operaciones de venta.
- [x] **TASK-E1-25** — Implementar `SaleService.java`: método `processSale(sku, quantity, paymentMethod)`. *(REQ 1.6)*
  - Leer precio desde `IndexedFileStore<String, ProductRecord>` usando índice (SKU).
  - Verificar stock en `IndexedFileStore<String, InventoryRecord>`.
  - Calcular total de todos los ítems de la venta.
  - Registrar pago (método de pago).
  - Escribir `SaleRecord` en archivo indexado de ventas.
  - Decrementar inventario en archivo indexado.
- [x] **TASK-E1-26** — Implementar generación de recibo de venta en consola con: ID transacción, timestamp, ítems, total, método de pago. *(REQ 1.6)*
- [x] **TASK-E1-27** — Escribir tests unitarios para `SaleService` con mocks del `IndexedFileStore`. *(REQ 1.6)*

---

### E1-G5: Módulo `pukio-send-service` — Servicio de Envío TCP

- [x] **TASK-E1-28** — Crear la clase principal `SendServiceApplication.java` como programa independiente ejecutable (JAR con main). *(REQ 1.3)*
- [x] **TASK-E1-29** — Implementar `FileSender.java`: leer completamente los archivos indexados locales (productos, inventario). *(REQ 1.3)*
- [x] **TASK-E1-30** — Implementar conexión TCP a `Data_Server` via `java.net.Socket` con host y puerto configurables (`application.properties`). *(REQ 1.3)*
- [x] **TASK-E1-31** — Implementar transmisión del contenido del archivo via `OutputStream` del socket. *(REQ 1.3)*
- [x] **TASK-E1-32** — Implementar recepción del ACK/NACK desde `Update_Service` via `InputStream`. *(REQ 1.3)*
- [x] **TASK-E1-33** — Implementar lógica de **retry x3 con exponential backoff** ante fallos de conexión: intentos a 1s, 2s, 4s. *(REQ 1.3)*
- [x] **TASK-E1-34** — Implementar logging de cada intento: timestamp, host destino, estado (éxito/error), registros enviados. *(REQ 1.3)*
- [x] **TASK-E1-35** — Escribir tests unitarios para `FileSender` con socket mockeado. *(REQ 1.3)*

---

### E1-G6: Módulo `pukio-update-service` — Servicio de Recepción y Actualización

- [x] **TASK-E1-36** — Crear la clase principal `UpdateServiceApplication.java` como servidor TCP independiente (`ServerSocket`). *(REQ 1.4)*
- [x] **TASK-E1-37** — Implementar `FileReceiver.java`: escuchar en puerto TCP configurado, aceptar conexiones entrantes. *(REQ 1.4)*
- [x] **TASK-E1-38** — Implementar `IndexedFileParser.java`: parsear la estructura binaria del archivo recibido y extraer `ProductRecord` e `InventoryRecord`. *(REQ 1.4)*
- [x] **TASK-E1-39** — Configurar conexión JDBC a PostgreSQL 18.1 (`spring-boot-starter-data-jpa` + `postgresql` driver). *(REQ 1.5)*
- [x] **TASK-E1-40** — Crear script SQL `schema-e1.sql`: tablas `products` y `inventory` con índices en `sku` y `store_id`. *(REQ 1.5)*
- [x] **TASK-E1-41** — Implementar `ProductSyncRepository.java`: operaciones `upsert` (INSERT … ON CONFLICT DO UPDATE) para tabla `products`. *(REQ 1.4)*
- [x] **TASK-E1-42** — Implementar `InventorySyncRepository.java`: operaciones `upsert` para tabla `inventory`. *(REQ 1.4)*
- [x] **TASK-E1-43** — Envolver todas las operaciones de base de datos dentro de una **única transacción** con `@Transactional`. *(REQ 1.4)*
- [x] **TASK-E1-44** — Implementar **rollback completo** si cualquier operación de la transacción falla. *(REQ 1.4)*
- [x] **TASK-E1-45** — Enviar ACK (éxito + conteo de registros procesados) o NACK (mensaje de error) al `Send_Service`. *(REQ 1.4)*
- [x] **TASK-E1-46** — Implementar logging de cada archivo recibido: origen (store_id), timestamp, conteo de registros. *(REQ 1.4)*
- [x] **TASK-E1-47** — Escribir tests de integración para `UpdateService` con base de datos H2 embebida (solo para testing). *(REQ 1.4, 1.5)*

---

### E1-G7: Base de Datos y Configuración

- [x] **TASK-E1-48** — Configurar instancia local PostgreSQL 18.1 con base de datos `pukio_central`.
- [x] **TASK-E1-49** — Crear usuario PostgreSQL `pukio_user` con contraseña y permisos sobre `pukio_central`.
- [x] **TASK-E1-50** — Ejecutar `schema-e1.sql`: crear tablas `products`, `inventory` con restricciones e índices. *(REQ 1.5)*
- [x] **TASK-E1-51** — Documentar procedimiento de instalación y ejecución del Entregable 1 en `README.md`.

---

### E1-G8-SEC: Seguridad de Credenciales — Verificación Final Entregable 1

- [x] **TASK-E1-SEC-07** — Confirmar que los archivos `application-secrets.properties` de todos los módulos están en `.gitignore` y que `git status` no los muestra como tracked. *(REQ 7.1)*
- [x] **TASK-E1-SEC-08** — Confirmar que `application.properties` de `pukio-update-service` (el módulo con conexión JDBC) usa `${DB_USERNAME}` y `${DB_PASSWORD}` y no contiene credenciales en texto plano. *(REQ 7.2)*
- [x] **TASK-E1-SEC-09** — Confirmar que `application.properties` de `pukio-send-service` usa `${SERVER_HOST}` y `${SERVER_PORT}` y que no hay IPs ni puertos hardcodeados en el código Java. *(REQ 7.2)*

---

## Entregable 2 — Arquitectura Cliente/Servidor

### E2-G1: Refactorización y Nuevos Módulos

- [x] **TASK-E2-01** — Crear módulo Maven `pukio-app-server` (Application_Server — Spring Boot 3.3.5).
- [x] **TASK-E2-02** — Refactorizar `pukio-pos-client`: eliminar toda lógica de negocio local, toda referencia a `IndexedFileStore`. El cliente solo debe contener UI y cliente HTTP. *(REQ 2.1)*
- [x] **TASK-E2-03** — Crear módulo Maven `pukio-analytics` (Analytics_Server / Data Warehouse).
- [x] **TASK-E2-04** — Agregar dependencias al `pukio-app-server`: `spring-boot-starter-web`, `spring-boot-starter-data-jpa`, `spring-boot-starter-validation`, `postgresql`.

---

### E2-G2: Application_Server — Capa de Persistencia

- [x] **TASK-E2-05** — Crear script SQL `schema-e2.sql`: tablas `products`, `inventory`, `stores`, `sales`, `sale_items`, `payments`, `promotions`, `arqueo` con claves foráneas e índices. *(REQ 2.6)*
- [x] **TASK-E2-06** — Crear entidades JPA: `Product.java`, `Inventory.java`, `Store.java`, `Sale.java`, `SaleItem.java`, `Payment.java`, `Promotion.java`, `Arqueo.java`, `AuditLog.java`. *(REQ 2.6)*
- [x] **TASK-E2-07** — Crear repositorios Spring Data JPA: `ProductRepository`, `InventoryRepository`, `SaleRepository`, `PromotionRepository`, `ArqueoRepository`, `AuditLogRepository`. *(REQ 2.6)*
- [x] **TASK-E2-08** — Configurar **HikariCP connection pool** en `application.properties` (max pool size, timeout, etc.). *(REQ 2.6)*
- [x] **TASK-E2-09** — Configurar **índices adicionales** en el script SQL: `(sku)`, `(transaction_date)`, `(store_id)`. *(REQ 2.6)*

---

### E2-G3: Application_Server — Lógica de Negocio

- [x] **TASK-E2-10** — Implementar `ProductService.java` (server-side): `createProduct()`, `updateProduct()`, `deactivateProduct()`, `findBySku()`, búsqueda por nombre/categoría, paginación (50 por página). *(REQ 5.1)*
- [x] **TASK-E2-11** — Implementar `InventoryService.java`: `checkStock()`, `decrementStock()` con **lock pesimista** (`SELECT FOR UPDATE`), `adjustInventory()`, `transferStock()`. *(REQ 2.3, 5.2)*
- [x] **TASK-E2-12** — Implementar `PromotionService.java`: `evaluatePromotions(saleItems)` — evalúa todas las promociones activas, aplica la más beneficiosa, registra promoción aplicada. *(REQ 2.4, 5.4)*
  - Soportar tipos: porcentaje, monto fijo, compra X lleva Y.
  - Validar vigencia (fecha inicio/fin) y monto mínimo.
- [x] **TASK-E2-13** — Implementar `SaleService.java` (server-side): orquesta validación de SKU, verificación de stock, aplicación de promociones, cálculo de total con impuestos, persistencia atómica de venta + ítems + pagos + actualización de inventario. *(REQ 2.2, 2.3, 5.3)*
- [x] **TASK-E2-14** — Implementar soporte de **pago dividido** (split payment): validar que suma de métodos de pago == total de venta. *(REQ 5.3)*
- [x] **TASK-E2-15** — Implementar `ArqueoService.java`: calcular montos esperados por método de pago por turno, comparar con montos declarados, calcular varianza, persistir resultado, flag si varianza > umbral. *(REQ 2.5, 5.5)*
- [x] **TASK-E2-16** — Escribir tests unitarios para `SaleService`, `PromotionService`, `ArqueoService` con Mockito. *(REQ 2.2, 2.4, 2.5)*
- [x] **TASK-E2-17** — Escribir tests de integración con `@SpringBootTest` + Testcontainers (PostgreSQL). *(REQ 2.6)*

---

### E2-G4: Application_Server — Capa REST

- [x] **TASK-E2-18** — Implementar `ProductController.java`: endpoints `GET /api/products`, `GET /api/products/{sku}`, `POST /api/products`, `PUT /api/products/{sku}`, `DELETE /api/products/{sku}`. *(REQ 5.1)*
- [x] **TASK-E2-19** — Implementar `SaleController.java`: endpoint `POST /api/sales/process` (recibe SKU, cantidad, método de pago; retorna confirmación con transactionId). *(REQ 2.2)*
- [x] **TASK-E2-20** — Implementar `InventoryController.java`: endpoints para consultar stock, ajuste manual, transferencia entre tiendas. *(REQ 5.2)*
- [x] **TASK-E2-21** — Implementar `PromotionController.java`: CRUD de promociones, listado de activas. *(REQ 5.4)*
- [x] **TASK-E2-22** — Implementar `ArqueoController.java`: `POST /api/arqueo/start`, `POST /api/arqueo/close`. *(REQ 2.5)*
- [x] **TASK-E2-23** — Implementar manejo global de excepciones con `@ControllerAdvice`: errores de validación, stock insuficiente, SKU duplicado, errores de BD. *(REQ 5.9)*
- [x] **TASK-E2-24** — Añadir validación de entrada con Bean Validation (`@NotBlank`, `@Positive`, `@Valid`) en todos los DTOs. *(REQ 2.2)*

---

### E2-G5: POS_Client — Cliente Ligero

- [x] **TASK-E2-25** — Implementar `AppServerClient.java`: cliente HTTP (`RestTemplate` o `WebClient`) que apunta al `Application_Server`. *(REQ 2.1)*
- [x] **TASK-E2-26** — Implementar UI de venta refactorizada: capturar SKU + cantidad + método de pago → delegar al `AppServerClient` → mostrar respuesta. *(REQ 2.1)*
- [x] **TASK-E2-26b** — Crear `MainFrame.java` (extiende `JFrame`): ventana principal 1024×768, `BorderLayout`, con `HeaderPanel` al norte, `JMenuBar` como barra de navegación, `CardLayout` en el centro y `StatusBar` al sur. Configurar `setDefaultCloseOperation(EXIT_ON_CLOSE)` y restaurar tamaño/posición desde `java.util.prefs.Preferences`. *(REQ 2.1-UI)*
- [x] **TASK-E2-26c** — Crear `HeaderPanel.java`: barra superior con `JLabel` para nombre de tienda, cajero, turno, y un `javax.swing.Timer` de 1 segundo que actualiza la hora en pantalla. *(REQ 2.1-UI)*
- [x] **TASK-E2-26d** — Crear `StatusBar.java`: barra inferior con indicador de conexión (JLabel con ícono verde/rojo), etiqueta de último resultado de operación, y etiqueta de rol del usuario activo. *(REQ 2.1-UI)*
- [x] **TASK-E2-26e** — Crear `LoginPanel.java`: formulario centrado con `JTextField` (usuario), `JPasswordField` (contraseña) y botón "Iniciar Sesión". Al confirmar, invocar `AppServerClient.login()` en un `SwingWorker`; en éxito, cargar el panel de venta; en fallo, mostrar mensaje de error inline en rojo debajo del formulario sin cerrar el panel. *(REQ 2.1-UI)*
- [x] **TASK-E2-26f** — Crear `SalePanel.java`: panel dividido en dos columnas (60/40) con `JSplitPane`. Columna izquierda: `JTextField` de SKU (auto-focus al cargar), botón "Agregar", y `JTable` respaldado por `SaleItemTableModel`. Columna derecha: etiquetas de subtotal, descuento, IGV 18%, total (fuente bold 16pt); sección de cobro con `JComboBox` de métodos de pago, `JTextField` de monto tendered con listener que actualiza el vuelto en tiempo real, botón "Pago Dividido" y botón "Cobrar". Botón "Cancelar Venta" con diálogo de confirmación. *(REQ 2.1-UI)*
- [x] **TASK-E2-26g** — Crear `SaleItemTableModel.java`: extiende `AbstractTableModel` con columnas SKU, Nombre, Cantidad (editable), Precio Unit., Descuento, Subtotal, y columna de botón "Eliminar" renderizada con `ButtonRenderer`/`ButtonEditor`. Sobreescribir `isCellEditable()` para permitir solo edición de Cantidad. *(REQ 2.1-UI)*
- [x] **TASK-E2-26h** — Crear `SplitPaymentDialog.java`: `JDialog` modal con una fila por método de pago (Efectivo, Tarjeta Crédito, Tarjeta Débito, Transferencia, Billetera Digital), cada fila con `JCheckBox` de habilitación y `JTextField` de monto. Mostrar suma total en tiempo real y deshabilitar "Aceptar" mientras la suma no iguale el total de la venta. *(REQ 2.1-UI)*
- [x] **TASK-E2-26i** — Crear `ReceiptPanel.java`: panel con `JTextArea` no editable de fuente monoespaciada mostrando todos los campos del recibo (tienda, cajero, fecha, ID transacción, ítems, subtotal, descuento, IGV, total, métodos de pago, vuelto). Botón "Imprimir" usando `java.awt.print.PrinterJob`. Botón "Nueva Venta" que limpia el `SaleItemTableModel` y muestra el `SalePanel`. *(REQ 2.1-UI)*
- [x] **TASK-E2-26j** — Crear `ProductPanel.java`: barra de búsqueda con `JTextField` de texto libre y `JComboBox` de categoría, botón "Buscar". `JTable` paginada (50 filas) con columnas SKU, Nombre, Categoría, Precio, Stock, Estado. Botones de paginación "Anterior" / "Siguiente" con `JLabel` "Página X de Y". Botones "Nuevo Producto", "Editar" (activo solo con fila seleccionada), "Desactivar" (con diálogo de confirmación). Visible solo para roles Manager y Administrator. *(REQ 2.1-UI, REQ 5.1)*
- [x] **TASK-E2-26k** — Crear `ProductFormDialog.java`: `JDialog` modal con campos `JTextField` para Nombre, `JTextArea` para Descripción, `JTextField` validado para Precio (solo decimales positivos), `JComboBox` para Categoría, `JTextField` no editable para SKU (habilitado solo en creación). Botón "Seleccionar Imagen" que abre `JFileChooser` filtrado para PNG/JPG y muestra miniatura 80×80 px. Botones "Guardar" (llama al servidor vía `SwingWorker`) y "Cancelar". *(REQ 2.1-UI, REQ 5.1)*
- [x] **TASK-E2-26l** — Crear `InventoryPanel.java`: `JTable` con columnas SKU, Nombre, Stock Actual, Punto de Reorden, Tienda, Última Actualización. Aplicar `DefaultTableCellRenderer` personalizado que colorea filas: amarillo cuando `stock == reorder_point`, rojo cuando `stock == 0`. Botón "Ajuste Manual" que abre `InventoryAdjustmentDialog`. Visible para roles Supervisor, Manager y Administrator. *(REQ 2.1-UI, REQ 5.2)*
- [x] **TASK-E2-26m** — Crear `InventoryAdjustmentDialog.java`: `JDialog` modal con `JTextField` de SKU, `JSpinner` de cantidad (permite negativos), `JComboBox` de motivo (Corrección, Merma, Robo, Devolución) y botones "Confirmar" / "Cancelar". Llamar a `AppServerClient.adjustInventory()` en `SwingWorker`. *(REQ 2.1-UI, REQ 5.2)*
- [x] **TASK-E2-26n** — Crear `ArqueoPanel.java`: tabla de solo lectura con montos esperados por método de pago (cargados del servidor al abrir el panel), campos `JTextField` editables para montos declarados por método, y `JLabel` de varianza por método que se recalcula en tiempo real con `DocumentListener` (color rojo si excede umbral). Botón "Cerrar Turno" que llama a `AppServerClient.submitArqueo()` en `SwingWorker` y muestra resultado. *(REQ 2.1-UI, REQ 5.5)*
- [x] **TASK-E2-26o** — Crear `PromotionPanel.java`: `JTable` con columnas Nombre, Tipo, Valor, Vigencia, Alcance, Activa. Botones "Nueva Promoción" y "Editar" que abren `PromotionFormDialog`. Visible solo para roles Manager y Administrator. *(REQ 2.1-UI, REQ 5.4)*
- [x] **TASK-E2-26p** — Crear `PromotionFormDialog.java`: `JDialog` modal con `JTextField` para Nombre, `JComboBox` de Tipo (Porcentaje, Monto Fijo, Compra X lleva Y), `JTextField` para Valor, `JTextField` para Monto Mínimo, dos `JSpinner` de tipo `SpinnerDateModel` para Fecha Inicio y Fecha Fin, `JComboBox` de Tienda (con opción "Todas"), `JCheckBox` Activa. Botones "Guardar" y "Cancelar". *(REQ 2.1-UI, REQ 5.4)*
- [x] **TASK-E2-26q** — Crear `SwingWorkerTask.java`: clase genérica utilitaria que acepta un `Supplier<T>` (tarea de fondo), un `Consumer<T>` (callback éxito) y un `Consumer<Exception>` (callback error). En `execute()`: deshabilitar el botón pasado como parámetro, mostrar `JProgressBar` indeterminate, y restaurar ambos en `done()`. Usada por todos los paneles para llamadas al servidor. *(REQ 2.1-UI)*
- [x] **TASK-E2-26r** — Definir atajos de teclado globales en `MainFrame` con `KeyboardFocusManager`: F2 → foco en campo SKU, F4 → abrir ArqueoPanel, F5 → recargar panel activo, Escape → cerrar dialog modal activo, Enter → activar botón de acción primaria del panel activo. *(REQ 2.1-UI)*
- [x] **TASK-E2-27** — Verificar que el cliente NO realiza cálculos ni validaciones locales. *(REQ 2.1)*
- [x] **TASK-E2-28** — Implementar visualización del recibo recibido desde el servidor. *(REQ 2.2)*

---

### E2-G6: Analytics_Server — Data Warehouse

- [x] **TASK-E2-29** — Crear script SQL `schema-dw.sql`: tablas `fact_sales`, `dim_time`, `dim_product`, `dim_store`, `dim_payment` (esquema estrella). *(REQ 2.7)*
- [x] **TASK-E2-30** — Implementar `DimTimePopulator.java`: poblar `dim_time` con registros para un rango de años. *(REQ 2.7)*
- [x] **TASK-E2-31** — Implementar `FactSalesEtlService.java`: al confirmar una venta en `Data_Server`, insertar registro en `fact_sales` del `Analytics_Server` de forma asíncrona (`@Async`). *(REQ 2.7)*
- [x] **TASK-E2-32** — Configurar segunda fuente de datos (`DataSource`) en Spring Boot apuntando al `Analytics_Server`. *(REQ 2.7)*
- [x] **TASK-E2-33** — Configurar **Apache Superset 6.1.0**: conexión a `Analytics_Server`, crear datasets sobre `fact_sales` y dimensiones. *(REQ 2.8)*
- [x] **TASK-E2-34** — Crear dashboards en Superset: tendencia de ventas, top productos, distribución por método de pago, ventas por tienda. *(REQ 2.8)*
- [x] **TASK-E2-35** — Implementar queries cross-tab (tabla cruzada) para análisis multidimensional en Superset. *(REQ 2.8)*

---

### E2-G7: Configuración y Documentación

- [x] **TASK-E2-36** — Crear perfiles Spring (`application-dev.properties`, `application-prod.properties`) con configuración de BD, puertos y timeouts.
- [x] **TASK-E2-37** — Actualizar `README.md` con guía de ejecución del Entregable 2.

---

### E2-G8-SEC: Seguridad de Credenciales — Entregable 2

> En este entregable se incorporan nuevas credenciales: la segunda fuente de datos (Analytics_Server) y el pool de conexiones HikariCP. Todas deben gestionarse como secretos.

- [x] **TASK-E2-SEC-01** — Ampliar `application-secrets.properties.template` con las nuevas variables del Entregable 2: `ANALYTICS_DB_URL`, `ANALYTICS_DB_USERNAME`, `ANALYTICS_DB_PASSWORD`, `HIKARI_MAX_POOL_SIZE` (si es sensible al entorno). *(REQ 7.1, 7.6)*
- [x] **TASK-E2-SEC-02** — Verificar que `application-dev.properties` y `application-prod.properties` (commiteados) no contienen contraseñas ni credenciales; solo referencias `${VAR}` y valores de configuración no sensibles como timeouts o tamaños de página. *(REQ 7.2)*
- [x] **TASK-E2-SEC-03** — Verificar que la segunda fuente de datos (`DataSource`) del `Analytics_Server` configurada en TASK-E2-32 usa `${ANALYTICS_DB_PASSWORD}` y no tiene la contraseña hardcodeada en el código Java ni en archivos commiteados. *(REQ 7.2)*
- [x] **TASK-E2-SEC-04** — Verificar que la configuración de Apache Superset (conexión a `Analytics_Server`) no almacena la cadena de conexión con contraseña en ningún archivo commiteado; documentar en `docs/security.md` cómo configurar la conexión de Superset localmente. *(REQ 7.1)*
- [x] **TASK-E2-SEC-05** — Realizar auditoría de seguridad del Entregable 2: ejecutar `git log --all -- "**/*.properties"` y revisar que ningún commit histórico contiene valores sensibles. *(REQ 7.5)*

---

## Entregable 3 — Arquitectura N-Capas

### E3-G1: Separación Estricta de Capas

- [ ] **TASK-E3-01** — Reorganizar el código de `pukio-app-server` en paquetes estrictos: `presentation` (controllers), `business` (services), `dataaccess` (repositories). *(REQ 3.1)*
- [ ] **TASK-E3-02** — Aplicar `@Service`, `@Repository` y `@RestController` correctamente y verificar que ningún controller accede directamente a repositorios. *(REQ 3.1)*
- [ ] **TASK-E3-03** — Refactorizar `Data Access Layer`: implementar **patrón Repository** explícito con interfaz + implementación para cada entidad. Los services solo llaman al repositorio, nunca a JDBC directamente. *(REQ 3.4)*
- [ ] **TASK-E3-04** — Implementar traducción de excepciones de BD a excepciones de aplicación (`DataAccessException` → excepciones de dominio) en la capa de acceso a datos. *(REQ 3.4)*
- [ ] **TASK-E3-05** — Verificar y documentar que cada capa es **deployable independientemente**. *(REQ 3.1)*

---

### E3-G2: Balanceo de Carga con Nginx

- [ ] **TASK-E3-06** — Instalar y configurar Nginx 1.28.3 como reverse proxy y load balancer. *(REQ 3.2)*
- [ ] **TASK-E3-07** — Crear `nginx.conf` con bloque `upstream pukio_app` que incluya las instancias del `Application_Server` (e.g., `localhost:8081`, `localhost:8082`). *(REQ 3.2)*
- [ ] **TASK-E3-08** — Configurar algoritmo de balanceo `least_conn` (least connections). *(REQ 3.2)*
- [ ] **TASK-E3-09** — Configurar **health checks** en Nginx: `health_check interval=10s` con remoción automática de instancias fallidas. *(REQ 3.2)*
- [ ] **TASK-E3-10** — Configurar **session affinity** (sticky sessions via `ip_hash`) para operaciones de arqueo. *(REQ 3.2)*
- [ ] **TASK-E3-11** — Verificar que el `POS_Client` apunta al Nginx (no directamente al `Application_Server`).

---

### E3-G3: Múltiples Instancias del Application_Server

- [ ] **TASK-E3-12** — Configurar el `Application_Server` para ser **stateless** (sin estado local en memoria entre requests). *(REQ 3.3)*
- [ ] **TASK-E3-13** — Implementar **distributed locks** via PostgreSQL (`pg_try_advisory_lock`) para operaciones críticas (venta + actualización de inventario) que requieren coordinación entre instancias. *(REQ 3.3)*
- [ ] **TASK-E3-14** — Crear script de arranque `start-cluster.sh` que levante 2 instancias del `Application_Server` en puertos distintos. *(REQ 3.3)*
- [ ] **TASK-E3-15** — Verificar con pruebas de carga (Apache JMeter o similar) que ambas instancias procesan requests independientemente. *(REQ 3.3)*

---

### E3-G4: FTP Server — Imágenes y Auditoría

- [ ] **TASK-E3-16** — Instalar y configurar vsftpd 3.0.5 (o ProFTPD 1.3.8b). *(REQ 3.5)*
- [ ] **TASK-E3-17** — Crear la estructura de directorios FTP: `/images/{SKU}/`, `/backups/{yyyy}/{MM}/{dd}/`, `/audit-logs/{yyyy}/{MM}/{dd}/`. *(REQ 3.5, 3.6, 3.7)*
- [ ] **TASK-E3-18** — Configurar autenticación de usuario FTP para el `Application_Server` (usuario `pukio_ftp` con contraseña). *(REQ 3.5)*
- [ ] **TASK-E3-19** — Implementar `FtpClientService.java` usando Apache Commons Net: métodos `uploadImage(sku, bytes)`, `downloadImage(sku)`, `uploadArqueoBackup(storeId, json)`, `uploadAuditLog(storeId, xml)`. *(REQ 3.5, 3.6, 3.7)*
- [ ] **TASK-E3-20** — Integrar `FtpClientService` en `ProductService`: al crear producto, subir imagen al FTP. *(REQ 3.5)*
- [ ] **TASK-E3-21** — Integrar `FtpClientService` en `ArqueoService`: al cerrar arqueo, subir backup JSON al FTP y verificar éxito antes de confirmar. *(REQ 3.6)*
- [ ] **TASK-E3-22** — Implementar `AuditLogService.java`: al completar venta, generar log XML/JSON (transactionId, timestamp, SKU, cantidad, precio, impuesto, total, método de pago) y subirlo al FTP inmediatamente. *(REQ 3.7)*
- [ ] **TASK-E3-23** — Configurar **inmutabilidad** de los logs de auditoría (solo append, sin modificación). *(REQ 3.7)*
- [ ] **TASK-E3-24** — Implementar búsqueda de logs de auditoría por rango de fechas y `transaction_id`. *(REQ 3.7)*
- [ ] **TASK-E3-25** — Escribir tests unitarios para `FtpClientService` con servidor FTP embebido (MockFtpServer). *(REQ 3.5)*

---

### E3-G5: Mirror Server — Replicación y Failover

- [ ] **TASK-E3-26** — Configurar PostgreSQL 18.1 **Streaming Replication**: `Data_Server` (PRIMARY) → `Mirror_Server` (STANDBY). *(REQ 3.8)*
- [ ] **TASK-E3-27** — Configurar `postgresql.conf` en PRIMARY: `wal_level=replica`, `max_wal_senders`, `wal_keep_size`. *(REQ 3.8)*
- [ ] **TASK-E3-28** — Configurar `recovery.conf` / `standby.signal` en STANDBY: `primary_conninfo`, `restore_command`. *(REQ 3.8)*
- [ ] **TASK-E3-29** — Verificar lag de replicación < 1 segundo con query a `pg_stat_replication`. *(REQ 3.8)*
- [ ] **TASK-E3-30** — Implementar `DatabaseFailoverManager.java`: detectar fallo del PRIMARY (ping cada 5s via JDBC), activar reconexión automática al STANDBY. *(REQ 3.8, 3.9)*
- [ ] **TASK-E3-31** — Configurar datasource secundario en Spring Boot apuntando al `Mirror_Server` como fallback. *(REQ 3.8)*
- [ ] **TASK-E3-32** — Implementar failover transparente: el `POS_Client` no debe recibir errores durante el switchover. *(REQ 3.9)*
- [ ] **TASK-E3-33** — Implementar procedimiento de **re-sincronización y restauración del PRIMARY** cuando vuelve en línea. *(REQ 3.8)*
- [ ] **TASK-E3-34** — Escribir test de integración de failover: detener el PRIMARY, verificar que operaciones continúan en STANDBY. *(REQ 3.8, 3.9)*

---

### E3-G6: Monitoreo de Salud

- [ ] **TASK-E3-35** — Implementar `HealthMonitorService.java` con tareas `@Scheduled`: ping Data_Server cada 5s, check lag de replicación cada 10s, check disk FTP cada 60s. *(REQ 3.10)*
- [ ] **TASK-E3-36** — Implementar sistema de alertas: loggear alertas y exponer estado via endpoint `GET /api/health/status`. *(REQ 3.10)*
- [ ] **TASK-E3-37** — Crear dashboard de estado básico (endpoint JSON) con estado de todos los componentes: `DATA_SERVER`, `MIRROR_SERVER`, `FTP_SERVER`, `APP_SERVER_1`, `APP_SERVER_2`. *(REQ 3.10)*

---

### E3-G7: Configuración y Documentación

- [ ] **TASK-E3-38** — Crear `nginx.conf` definitivo y documentar su configuración.
- [ ] **TASK-E3-39** — Crear `vsftpd.conf` (o `proftpd.conf`) y documentar su configuración.
- [ ] **TASK-E3-40** — Documentar procedimiento de configuración de Streaming Replication en `docs/replication-setup.md`.
- [ ] **TASK-E3-41** — Actualizar `README.md` con guía de ejecución del Entregable 3.

---

### E3-G8-SEC: Seguridad de Credenciales — Entregable 3

> En este entregable se incorporan nuevas credenciales críticas: usuario FTP (`FTP_USER`, `FTP_PASSWORD`) y la replicación PostgreSQL (usuario de replicación con contraseña). Deben gestionarse como secretos desde el primer momento.

- [ ] **TASK-E3-SEC-01** — Ampliar `application-secrets.properties.template` con las nuevas variables del Entregable 3: `FTP_HOST`, `FTP_USER`, `FTP_PASSWORD`, `FTP_PORT`, `MIRROR_DB_HOST`, `MIRROR_DB_PORT`, `REPLICATION_USER`, `REPLICATION_PASSWORD`. *(REQ 7.1, 7.6)*
- [ ] **TASK-E3-SEC-02** — Verificar que `FtpClientService.java` (TASK-E3-19) lee `FTP_USER` y `FTP_PASSWORD` desde `${FTP_USER}` / `${FTP_PASSWORD}` inyectados por Spring (`@Value`), y que ningún valor de credencial FTP aparece hardcodeado en código Java o en archivos commiteados. *(REQ 7.2)*
- [ ] **TASK-E3-SEC-03** — Verificar que los archivos de configuración de vsftpd (`vsftpd.conf`) y ProFTPD (`proftpd.conf`) commiteados en `ftp/` no contienen contraseñas reales; documentar en `docs/security.md` cómo configurar el usuario FTP localmente con `ftpasswd` o `htpasswd`. *(REQ 7.1)*
- [ ] **TASK-E3-SEC-04** — Verificar que `postgresql.conf` y `recovery.conf` / `standby.signal` (usados en TASK-E3-27 y TASK-E3-28) no contienen la contraseña del usuario de replicación en texto plano dentro del repositorio; la contraseña debe estar en el `pg_hba.conf` local o en un `.pgpass` excluido del Git. *(REQ 7.1)*
- [ ] **TASK-E3-SEC-05** — Actualizar `nginx.conf` (commiteado) para asegurarse de que no contiene rutas absolutas con nombres de usuario del sistema operativo ni tokens de autenticación básica (`auth_basic_user_file` debe apuntar a una ruta que no esté en el repositorio). *(REQ 7.1)*
- [ ] **TASK-E3-SEC-06** — Actualizar `docs/security.md` con la sección de Entregable 3: lista de nuevos secretos, cómo configurarlos localmente, y cómo se inyectan en los servicios FTP y Mirror. *(REQ 7.5)*
- [ ] **TASK-E3-SEC-07** — Realizar auditoría de seguridad del Entregable 3: buscar en el repositorio con `grep -r "password\|passwd\|secret\|token" --include="*.properties" --include="*.conf" --include="*.yaml"` y verificar que los resultados solo muestran placeholders `${VAR}` o comentarios, nunca valores reales. *(REQ 7.5)*

---

## Entregable 4 — Cloud Computing

### E4-G1: Contenedorización Docker

- [ ] **TASK-E4-01** — Crear `Dockerfile` multi-stage para `pukio-app-server`: stage `build` (Maven 3.9.9 + JDK 21), stage `runtime` (JRE 21 mínimo). *(REQ 4.1)*
- [ ] **TASK-E4-02** — Crear `Dockerfile` para `pukio-analytics` (Superset). *(REQ 4.1)*
- [ ] **TASK-E4-03** — Crear `docker-compose.yml` que orqueste: `pukio-app`, `postgres-primary`, `postgres-replica`, `postgres-dw`, `minio`, `keycloak`, `prometheus`, `grafana`, `elasticsearch`, `logstash`, `kibana`. *(REQ 4.1)*
- [ ] **TASK-E4-04** — Configurar **volumes** Docker para persistencia: `pgdata-primary`, `pgdata-replica`, `pgdata-dw`, `minio-data`. *(REQ 4.1)*
- [ ] **TASK-E4-05** — Configurar **networks** Docker para aislamiento: `app-net`, `data-net`, `observability-net`. *(REQ 4.1)*
- [ ] **TASK-E4-06** — Configurar variables de entorno en `docker-compose.yml`: `DB_URL`, `DB_PASSWORD`, `KEYCLOAK_URL`, `MINIO_ENDPOINT`. *(REQ 4.1)*
- [ ] **TASK-E4-07** — Verificar que cada imagen Docker contenga **solo dependencias necesarias** (principio de mínima superficie). *(REQ 4.1)*
- [ ] **TASK-E4-08** — Publicar imagen `pukio-app` en Docker Hub (o registry local). *(REQ 4.1)*

---

### E4-G2: Orquestación Kubernetes

- [ ] **TASK-E4-09** — Instalar y configurar Minikube v1.38.1 con recursos suficientes (CPU, RAM). *(REQ 4.2)*
- [ ] **TASK-E4-10** — Crear namespaces Kubernetes: `app`, `data`, `auth`, `observability`. *(REQ 4.2)*
- [ ] **TASK-E4-11** — Crear `Deployment` Kubernetes para `pukio-app` (namespace `app`, replicas: 2). *(REQ 4.2)*
- [ ] **TASK-E4-12** — Crear `Service` Kubernetes tipo `ClusterIP` para `pukio-app` (load balancing interno). *(REQ 4.2)*
- [ ] **TASK-E4-13** — Crear `StatefulSet` para `postgres-primary` con `PersistentVolumeClaim` de 50Gi. *(REQ 4.2)*
- [ ] **TASK-E4-14** — Crear `StatefulSet` para `postgres-replica` con `PersistentVolumeClaim` de 50Gi. *(REQ 4.2)*
- [ ] **TASK-E4-15** — Crear `StatefulSet` para `postgres-dw` con `PersistentVolumeClaim` de 100Gi. *(REQ 4.2)*
- [ ] **TASK-E4-16** — Crear `StatefulSet` para MinIO con `PersistentVolumeClaim` de 200Gi. *(REQ 4.6)*
- [ ] **TASK-E4-17** — Crear `ConfigMap` `pukio-config` con variables de configuración no sensibles. *(REQ 4.2)*
- [ ] **TASK-E4-18** — Crear `Secret` `pukio-secrets` con credenciales de BD, JWT secret, MinIO credentials. *(REQ 4.2)*
- [ ] **TASK-E4-19** — Configurar `Ingress` con Nginx Ingress Controller para exponer servicios externamente. *(REQ 4.2)*
- [ ] **TASK-E4-20** — Crear `NetworkPolicy` para restringir comunicación pod-a-pod: solo los flujos autorizados (app→data, app→auth, observability→app). *(REQ 4.8)*
- [ ] **TASK-E4-21** — Escribir todos los manifiestos YAML en `k8s/` con subdirectorios por namespace. *(REQ 4.2)*

---

### E4-G3: Auto-Escalado Horizontal (HPA)

- [ ] **TASK-E4-22** — Crear `HorizontalPodAutoscaler` para `pukio-app`: `minReplicas: 2`, `maxReplicas: 10`, `targetCPUUtilizationPercentage: 70`. *(REQ 4.3)*
- [ ] **TASK-E4-23** — Configurar reducción de pods cuando CPU < 30%. *(REQ 4.3)*
- [ ] **TASK-E4-24** — Verificar que el HPA evalúa métricas cada 15 segundos. *(REQ 4.3)*
- [ ] **TASK-E4-25** — Crear `CronJob` Kubernetes `pre-scale-blackfriday`: escala a 10 pods un día antes de eventos especiales configurados. *(REQ 4.4)*
- [ ] **TASK-E4-26** — Crear `CronJob` Kubernetes `scale-down-post-event`: reduce a mínimo 2 horas después del pico. *(REQ 4.4)*
- [ ] **TASK-E4-27** — Verificar que el sistema mantiene `minReplicas` durante periodos de baja carga. *(REQ 4.3)*

---

### E4-G4: Base de Datos Multi-AZ en Kubernetes

- [ ] **TASK-E4-28** — Configurar `postgres-primary` y `postgres-replica` como StatefulSets en distintos nodos Minikube (simulando AZs separadas). *(REQ 4.5)*
- [ ] **TASK-E4-29** — Configurar Streaming Replication entre `postgres-primary` y `postgres-replica` dentro del cluster Kubernetes. *(REQ 4.5)*
- [ ] **TASK-E4-30** — Implementar **read replica routing**: queries de lectura a `postgres-replica`, escrituras solo a `postgres-primary`. *(REQ 4.5)*
- [ ] **TASK-E4-31** — Configurar alerta de replication lag > 5 segundos en Prometheus. *(REQ 4.5)*
- [ ] **TASK-E4-32** — Implementar `CronJob` de failover automático: si PRIMARY falla, promover REPLICA a PRIMARY. *(REQ 4.5)*

---

### E4-G5: MinIO — Object Storage

- [ ] **TASK-E4-33** — Desplegar MinIO como StatefulSet en Kubernetes con bucket inicial `product-images`, `audit-logs`, `backups`. *(REQ 4.6)*
- [ ] **TASK-E4-34** — Implementar `MinioStorageService.java`: reemplaza `FtpClientService`; métodos `uploadProductImage()`, `getPreSignedUrl()`, `uploadAuditLog()`, `uploadBackup()`. *(REQ 4.6)*
  - Usa AWS SDK v2 para Java (compatible con MinIO S3 API).
- [ ] **TASK-E4-35** — Configurar **bucket policies**: `product-images` con acceso público (pre-signed URLs), `audit-logs` y `backups` con acceso autenticado. *(REQ 4.6)*
- [ ] **TASK-E4-36** — Habilitar **versionado** en el bucket `audit-logs`. *(REQ 4.6)*
- [ ] **TASK-E4-37** — Integrar `MinioStorageService` en `ProductService` y `AuditLogService`: reemplazar llamadas FTP. *(REQ 4.6)*
- [ ] **TASK-E4-38** — Actualizar `POS_Client`: obtener imágenes de productos via **pre-signed URLs** de MinIO. *(REQ 4.6)*
- [ ] **TASK-E4-39** — Implementar `CronJob` `daily-backup`: pg_dump → comprimir → cifrar → subir a bucket `backups`. *(REQ 4.13)*
- [ ] **TASK-E4-40** — Implementar `CronJob` `weekly-full-backup` y `daily-incremental-backup`. *(REQ 4.13)*
- [ ] **TASK-E4-41** — Implementar `CronJob` `verify-backup`: verificar integridad del backup tras la subida. *(REQ 4.13)*
- [ ] **TASK-E4-42** — Implementar `CronJob` `monthly-restore-test`: restaurar backup en entorno de prueba y verificar. *(REQ 4.13)*
- [ ] **TASK-E4-43** — Configurar política de retención: diario 7 días, semanal 4 semanas, mensual 12 meses. *(REQ 4.13)*
- [ ] **TASK-E4-44** — Escribir tests unitarios para `MinioStorageService` con MinIO Test Container. *(REQ 4.6)*

---

### E4-G6: Keycloak — Gestión de Identidad

- [ ] **TASK-E4-45** — Desplegar Keycloak 26.5.6 como Deployment en namespace `auth`. *(REQ 4.7)*
- [ ] **TASK-E4-46** — Crear realm `pukio` en Keycloak. *(REQ 4.7)*
- [ ] **TASK-E4-47** — Crear roles en Keycloak: `cashier`, `supervisor`, `manager`, `administrator`, `auditor`. *(REQ 4.7, 5.8)*
- [ ] **TASK-E4-48** — Crear client `pukio-app` en Keycloak con `Authorization Code Flow` + `client_credentials`. *(REQ 4.7)*
- [ ] **TASK-E4-49** — Configurar política de contraseñas: longitud mínima 8, complejidad, expiración 90 días. *(REQ 4.7)*
- [ ] **TASK-E4-50** — Habilitar **MFA (TOTP)** obligatorio para el rol `administrator`. *(REQ 4.7)*
- [ ] **TASK-E4-51** — Integrar `pukio-app-server` con Keycloak: configurar `spring-boot-starter-oauth2-resource-server`, validar JWT en cada request. *(REQ 4.7)*
- [ ] **TASK-E4-52** — Implementar `@PreAuthorize` con roles Keycloak en cada endpoint REST. *(REQ 4.7, 5.8)*
  - `cashier`: processar ventas, ver productos, realizar arqueo.
  - `supervisor`: + aprobar arqueo, ajustar inventario.
  - `manager`: + gestionar productos, ver reportes, configurar promociones.
  - `administrator`: + gestionar usuarios, configurar sistema.
  - `auditor`: solo lectura en todos los endpoints.
- [ ] **TASK-E4-53** — Actualizar `POS_Client`: implementar flujo de login via Keycloak OIDC, almacenar JWT, refrescar token automáticamente. *(REQ 4.7)*
- [ ] **TASK-E4-54** — Configurar logging de Keycloak para todos los intentos de autenticación y decisiones de autorización. *(REQ 4.7)*
- [ ] **TASK-E4-55** — Escribir tests de seguridad: verificar que endpoints protegidos rechazan requests sin JWT válido. *(REQ 4.7)*

---

### E4-G7: Seguridad de Red y SSL

- [ ] **TASK-E4-56** — Configurar **iptables / firewalld**: permitir solo puertos 80, 443 y 5432 (solo red interna). *(REQ 4.8)*
- [ ] **TASK-E4-57** — Configurar regla **deny all** por defecto y abrir solo puertos requeridos. *(REQ 4.8)*
- [ ] **TASK-E4-58** — Configurar **rate limiting** en iptables para mitigar DDoS. *(REQ 4.8)*
- [ ] **TASK-E4-59** — Obtener certificado SSL con **Let's Encrypt Certbot 3.3.0** para el dominio del sistema. *(REQ 4.9)*
- [ ] **TASK-E4-60** — Configurar Nginx Ingress con TLS: redirigir HTTP → HTTPS. *(REQ 4.9)*
- [ ] **TASK-E4-61** — Configurar **auto-renovación** de certificados (cron de Certbot). *(REQ 4.9)*
- [ ] **TASK-E4-62** — Deshabilitar cipher suites débiles en Nginx: permitir solo TLS 1.2 y TLS 1.3. *(REQ 4.9)*
- [ ] **TASK-E4-63** — Verificar que `POS_Client` valida certificado del servidor antes de conectar. *(REQ 4.9)*

---

### E4-G8: Observabilidad — Prometheus y Grafana

- [ ] **TASK-E4-64** — Agregar dependencia `micrometer-registry-prometheus` a `pukio-app-server`. *(REQ 4.10)*
- [ ] **TASK-E4-65** — Exponer endpoint `/actuator/prometheus` en `pukio-app-server`. *(REQ 4.10)*
- [ ] **TASK-E4-66** — Desplegar **Prometheus 3.4.0** en namespace `observability` con `ServiceMonitor` que haga scrape cada 15s. *(REQ 4.10)*
- [ ] **TASK-E4-67** — Configurar scrape targets: pods de `pukio-app`, `postgres-exporter`, `kube-state-metrics`, `node-exporter`. *(REQ 4.10)*
- [ ] **TASK-E4-68** — Desplegar **Grafana OSS 12.0.1** con datasource Prometheus configurado. *(REQ 4.10)*
- [ ] **TASK-E4-69** — Crear dashboard Grafana **System Overview**: CPU, RAM, red por pod. *(REQ 4.10)*
- [ ] **TASK-E4-70** — Crear dashboard Grafana **App Performance**: request rate, response time (p95), error rate. *(REQ 4.10)*
- [ ] **TASK-E4-71** — Crear dashboard Grafana **DB Performance**: conexiones activas, query latency, replication lag. *(REQ 4.10)*
- [ ] **TASK-E4-72** — Configurar **reglas de alerta** en Prometheus (`rules.yaml`): *(REQ 4.11)*
  - CPU > 70% por más de 5 minutos.
  - Error rate > 5%.
  - Response time > 2 segundos.
  - Pod caído > 1 minuto.
  - Replication lag > 10 segundos.
- [ ] **TASK-E4-73** — Configurar **Alertmanager**: enviar alertas a canal configurado (email o webhook). *(REQ 4.11)*
- [ ] **TASK-E4-74** — Configurar **agrupación de alertas** para evitar spam. *(REQ 4.11)*

---

### E4-G9: Gestión de Logs — ELK Stack

- [ ] **TASK-E4-75** — Configurar `pukio-app-server` para emitir logs en **formato JSON estructurado** con `logstash-logback-encoder`. *(REQ 4.12)*
- [ ] **TASK-E4-76** — Desplegar **Elasticsearch 8.17.0** en namespace `observability`. *(REQ 4.12)*
- [ ] **TASK-E4-77** — Desplegar **Logstash 8.17.0** con pipeline que colecta stdout de pods, parsea JSON, enriquece con metadatos (pod, namespace) e indexa en Elasticsearch. *(REQ 4.12)*
- [ ] **TASK-E4-78** — Desplegar **Kibana 8.17.0** con index pattern para los logs de `pukio`. *(REQ 4.12)*
- [ ] **TASK-E4-79** — Crear **index lifecycle policy** en Elasticsearch: retención mínima 30 días. *(REQ 4.12)*
- [ ] **TASK-E4-80** — Implementar **correlation IDs**: generar `X-Correlation-ID` en cada request y propagarlo a todos los logs y al `AuditLogService`. *(REQ 4.12)*
- [ ] **TASK-E4-81** — Crear dashboard Kibana: búsqueda de logs por nivel, pod, store, mensaje. *(REQ 4.12)*

---

### E4-G10: Disaster Recovery

- [ ] **TASK-E4-82** — Documentar procedimientos de DR para cada componente en `docs/disaster-recovery.md`. *(REQ 4.14)*
- [ ] **TASK-E4-83** — Definir y documentar **RTO = 1 hora** y **RPO = 15 minutos**. *(REQ 4.14)*
- [ ] **TASK-E4-84** — Configurar entorno de standby en zona secundaria (segundo namespace de Minikube o segundo cluster simulado). *(REQ 4.14)*
- [ ] **TASK-E4-85** — Implementar procedimiento de failover documentado y probado. *(REQ 4.14)*
- [ ] **TASK-E4-86** — Crear `CronJob` `quarterly-dr-test`: ejecutar prueba de DR y generar reporte. *(REQ 4.14)*

---

### E4-G11: Documentación Final

- [ ] **TASK-E4-87** — Actualizar `README.md` raíz con guía completa de ejecución del Entregable 4 (Minikube, kubectl apply, etc.).
- [ ] **TASK-E4-88** — Crear `docs/runbook.md` con procedimientos operativos: cómo escalar manualmente, cómo rotar certificados, cómo restaurar un backup.
- [ ] **TASK-E4-89** — Crear `docs/security.md` documentando la arquitectura de seguridad completa (firewall, TLS, Keycloak, NetworkPolicy).

---

### E4-G12-SEC: Seguridad de Credenciales — Entregable 4 (Docker, Kubernetes y Cloud)

> Este entregable introduce el mayor volumen de secretos del proyecto: credenciales de Keycloak, MinIO, JWT, certificados SSL y manifiestos Kubernetes. La gestión incorrecta de cualquiera de estos en Git representa una vulnerabilidad crítica.

#### Docker y Docker Compose

- [ ] **TASK-E4-SEC-01** — Crear `.env.template` (commiteado) en la raíz del proyecto con todas las variables requeridas por `docker-compose.yml` listadas sin valores: `DB_PASSWORD`, `ANALYTICS_DB_PASSWORD`, `MINIO_ROOT_USER`, `MINIO_ROOT_PASSWORD`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `KEYCLOAK_ADMIN_PASSWORD`, `KEYCLOAK_CLIENT_SECRET`, `JWT_SECRET`. *(REQ 7.3, 7.6)*
- [ ] **TASK-E4-SEC-02** — Verificar que `docker-compose.yml` (TASK-E4-03, commiteado) usa exclusivamente `${VAR}` para todos los campos sensibles y que no contiene ningún valor hardcodeado de contraseña, token o clave. *(REQ 7.3)*
- [ ] **TASK-E4-SEC-03** — Confirmar que `.env` (con valores reales) está en `.gitignore` y que `git status` nunca lo muestra como untracked ni staged. *(REQ 7.3)*
- [ ] **TASK-E4-SEC-04** — Añadir instrucción en `README.md`: "Antes de ejecutar `docker compose up`, copiar `.env.template` a `.env` y rellenar todos los valores requeridos." *(REQ 7.3)*

#### Kubernetes Secrets

- [ ] **TASK-E4-SEC-05** — Crear `k8s/app/secret.yaml.template` (commiteado) listando todas las keys del Secret de Kubernetes sin valores base64: `db-password`, `analytics-db-password`, `minio-access-key`, `minio-secret-key`, `keycloak-client-secret`, `jwt-secret`. *(REQ 7.4)*
- [ ] **TASK-E4-SEC-06** — Confirmar que `k8s/app/secret.yaml` (con valores base64 reales) está excluido por `.gitignore` mediante la regla `k8s/**/secret.yaml` y que nunca aparece en ningún commit. *(REQ 7.4)*
- [ ] **TASK-E4-SEC-07** — Documentar en `README.md` y en `docs/security.md` el comando para crear el Secret de Kubernetes antes del primer despliegue: `kubectl create secret generic pukio-secrets --from-env-file=.env -n app`. *(REQ 7.4)*
- [ ] **TASK-E4-SEC-08** — Verificar que `k8s/app/configmap.yaml` (TASK-E4-17, commiteado) contiene únicamente variables no sensibles: `DB_URL`, `APP_PORT`, `MINIO_ENDPOINT`, `KEYCLOAK_URL`, `FTP_HOST`; y que ninguna contraseña aparece en el ConfigMap. *(REQ 7.4)*
- [ ] **TASK-E4-SEC-09** — Verificar que el `Deployment` de `pukio-app` (TASK-E4-11) inyecta los secretos en los pods exclusivamente via `envFrom.secretRef` apuntando al Secret `pukio-secrets`, y no tiene valores sensibles escritos directamente en el manifiesto YAML. *(REQ 7.4)*

#### Certificados SSL

- [ ] **TASK-E4-SEC-10** — Confirmar que el directorio `nginx/ssl/` está excluido por `.gitignore` (regla `ssl/`) y que los archivos generados por Certbot (`.pem`, `.key`, `.crt`) no aparecen en ningún commit. *(REQ 7.1, 7.5)*
- [ ] **TASK-E4-SEC-11** — Verificar que el `ingress.yaml` de Kubernetes referencia los certificados SSL mediante un Kubernetes Secret (creado fuera del repositorio con `kubectl create secret tls`) y no embebe el contenido de los certificados directamente en el YAML commiteado. *(REQ 7.4)*

#### Keycloak y MinIO

- [ ] **TASK-E4-SEC-12** — Verificar que la contraseña del admin de Keycloak (`KEYCLOAK_ADMIN_PASSWORD`) y el `client-secret` (`KEYCLOAK_CLIENT_SECRET`) se leen exclusivamente desde variables de entorno inyectadas por el Secret de Kubernetes, y no están presentes en `keycloak-deployment.yaml` ni en ningún archivo commiteado. *(REQ 7.2, 7.4)*
- [ ] **TASK-E4-SEC-13** — Verificar que las credenciales de MinIO (`MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `MINIO_ROOT_PASSWORD`) se leen desde el Secret de Kubernetes en `minio-statefulset.yaml` y que `MinioStorageService.java` (TASK-E4-34) las obtiene via `@Value("${minio.access-key}")` / `@Value("${minio.secret-key}")` con los placeholders correctos. *(REQ 7.2, 7.4)*

#### Auditoría Final y Escaneo Automático

- [ ] **TASK-E4-SEC-14** — Actualizar `docs/security.md` con la sección completa del Entregable 4: lista de todos los secretos del sistema (Docker, Kubernetes, SSL, Keycloak, MinIO), cómo se crean localmente, cómo se inyectan en cada entorno (dev, staging, prod), y el protocolo de rotación de credenciales. *(REQ 7.5)*
- [ ] **TASK-E4-SEC-15** — Instalar y ejecutar **`gitleaks`** sobre el historial completo del repositorio (`gitleaks detect --source . --log-opts="--all"`) y verificar que el reporte no detecta ningún secreto filtrado. *(REQ 7.5)*
- [ ] **TASK-E4-SEC-16** — (Opcional recomendado) Agregar un hook de Git pre-commit con `gitleaks protect --staged` que bloquee automáticamente cualquier commit que contenga patrones de secretos (contraseñas, tokens, claves privadas). *(REQ 7.5)*
- [ ] **TASK-E4-SEC-17** — Realizar la auditoría final de seguridad de credenciales sobre todo el repositorio: ejecutar `grep -rn "password\|passwd\|secret\|api.key\|token\|access.key" --include="*.java" --include="*.properties" --include="*.yaml" --include="*.yml" --include="*.conf"` y confirmar que los únicos resultados son referencias a placeholders `${VAR}`, anotaciones `@Value`, o comentarios de documentación; nunca valores reales. *(REQ 7.1, 7.2, 7.5)*

---

## Estructura del Proyecto

```
pukio/
│
├── pom.xml                          ← Maven Parent POM (Java 21, Spring Boot 3.3.5)
├── README.md                        ← Guía general del proyecto
│
├── docs/                            ← Documentación técnica
│   ├── requirements.md
│   ├── design.md
│   ├── tasks.md
│   ├── replication-setup.md         ← Guía de Streaming Replication PostgreSQL
│   ├── disaster-recovery.md         ← Plan DR (RTO/RPO, procedimientos)
│   ├── runbook.md                   ← Procedimientos operativos
│   └── security.md                  ← Arquitectura de seguridad
│
├── pukio-common/                    ← Modelos y API de archivos indexados (E1)
│   ├── pom.xml
│   └── src/
│       ├── main/java/com/pukio/common/
│       │   ├── model/
│       │   │   ├── ProductRecord.java
│       │   │   ├── InventoryRecord.java
│       │   │   ├── SaleRecord.java
│       │   │   ├── LineItem.java
│       │   │   └── PaymentMethod.java
│       │   └── indexedfile/
│       │       ├── IndexedFileStore.java          ← Interfaz genérica
│       │       ├── BTreeIndexedFileStore.java     ← Implementación B-tree
│       │       └── HashIndexedFileStore.java      ← Implementación Hash
│       └── test/java/com/pukio/common/
│           └── indexedfile/
│               ├── BTreeIndexedFileStoreTest.java
│               └── HashIndexedFileStoreTest.java
│
├── pukio-maintenance/               ← Sistema de mantenimiento local (E1)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pukio/maintenance/
│       │   │   ├── MaintenanceApplication.java
│       │   │   ├── service/
│       │   │   │   ├── ProductService.java
│       │   │   │   └── InventoryService.java
│       │   │   └── ui/
│       │   │       └── MaintenanceMenu.java
│       │   └── resources/
│       │       └── application.properties         ← pukio.files.products, pukio.files.inventory
│       └── test/java/com/pukio/maintenance/
│           ├── ProductServiceTest.java
│           └── InventoryServiceTest.java
│
├── pukio-pos-client/                ← Terminal POS (E1: con lógica local; E2+: cliente ligero)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pukio/posclient/
│       │   │   ├── PosClientApplication.java
│       │   │   ├── service/
│       │   │   │   └── SaleService.java           ← E1: lógica local; E2+: delega a AppServerClient
│       │   │   ├── client/
│       │   │   │   └── AppServerClient.java       ← E2+: cliente REST/HTTP
│       │   │   └── ui/
│       │   │       ├── SaleMenu.java
│       │   │       └── ReceiptPrinter.java
│       │   └── resources/
│       │       └── application.properties
│       └── test/java/com/pukio/posclient/
│           └── SaleServiceTest.java
│
├── pukio-send-service/              ← Servicio de envío TCP (E1)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pukio/sendservice/
│       │   │   ├── SendServiceApplication.java
│       │   │   └── FileSender.java                ← TCP socket, retry x3, backoff
│       │   └── resources/
│       │       └── application.properties         ← server.host, server.port
│       └── test/java/com/pukio/sendservice/
│           └── FileSenderTest.java
│
├── pukio-update-service/            ← Servicio de recepción y actualización (E1)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pukio/updateservice/
│       │   │   ├── UpdateServiceApplication.java
│       │   │   ├── FileReceiver.java              ← ServerSocket TCP
│       │   │   ├── IndexedFileParser.java
│       │   │   └── repository/
│       │   │       ├── ProductSyncRepository.java
│       │   │       └── InventorySyncRepository.java
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/
│       │           └── schema-e1.sql
│       └── test/java/com/pukio/updateservice/
│           └── UpdateServiceIntegrationTest.java
│
├── pukio-app-server/                ← Application Server Spring Boot (E2, E3, E4)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pukio/appserver/
│       │   │   ├── AppServerApplication.java
│       │   │   │
│       │   │   ├── presentation/               ← Capa de Presentación (E3: separación estricta)
│       │   │   │   ├── ProductController.java
│       │   │   │   ├── SaleController.java
│       │   │   │   ├── InventoryController.java
│       │   │   │   ├── PromotionController.java
│       │   │   │   ├── ArqueoController.java
│       │   │   │   ├── HealthController.java
│       │   │   │   └── GlobalExceptionHandler.java
│       │   │   │
│       │   │   ├── business/                   ← Capa de Negocio
│       │   │   │   ├── ProductService.java
│       │   │   │   ├── SaleService.java
│       │   │   │   ├── InventoryService.java
│       │   │   │   ├── PromotionService.java
│       │   │   │   ├── ArqueoService.java
│       │   │   │   ├── AuditLogService.java
│       │   │   │   ├── HealthMonitorService.java
│       │   │   │   └── etl/
│       │   │   │       └── FactSalesEtlService.java
│       │   │   │
│       │   │   ├── dataaccess/                 ← Capa de Acceso a Datos
│       │   │   │   ├── ProductRepository.java
│       │   │   │   ├── SaleRepository.java
│       │   │   │   ├── InventoryRepository.java
│       │   │   │   ├── PromotionRepository.java
│       │   │   │   ├── ArqueoRepository.java
│       │   │   │   └── DatabaseFailoverManager.java
│       │   │   │
│       │   │   ├── domain/                     ← Entidades JPA y DTOs
│       │   │   │   ├── entity/
│       │   │   │   │   ├── Product.java
│       │   │   │   │   ├── Inventory.java
│       │   │   │   │   ├── Store.java
│       │   │   │   │   ├── Sale.java
│       │   │   │   │   ├── SaleItem.java
│       │   │   │   │   ├── Payment.java
│       │   │   │   │   ├── Promotion.java
│       │   │   │   │   ├── Arqueo.java
│       │   │   │   │   └── AuditLog.java
│       │   │   │   └── dto/
│       │   │   │       ├── SaleRequestDto.java
│       │   │   │       ├── SaleResponseDto.java
│       │   │   │       ├── ProductDto.java
│       │   │   │       ├── ArqueoRequestDto.java
│       │   │   │       └── ArqueoResponseDto.java
│       │   │   │
│       │   │   ├── infrastructure/             ← Integraciones externas
│       │   │   │   ├── ftp/
│       │   │   │   │   └── FtpClientService.java       ← E3: vsftpd/ProFTPD
│       │   │   │   └── storage/
│       │   │   │       └── MinioStorageService.java    ← E4: MinIO S3 API
│       │   │   │
│       │   │   └── security/                   ← E4: Keycloak + JWT
│       │   │       └── SecurityConfig.java
│       │   │
│       │   └── resources/
│       │       ├── application.properties
│       │       ├── application-dev.properties
│       │       ├── application-prod.properties
│       │       ├── logback-spring.xml           ← E4: JSON logs para ELK
│       │       └── db/
│       │           ├── schema-e2.sql
│       │           └── schema-dw.sql
│       │
│       └── test/java/com/pukio/appserver/
│           ├── business/
│           │   ├── SaleServiceTest.java
│           │   ├── PromotionServiceTest.java
│           │   └── ArqueoServiceTest.java
│           ├── presentation/
│           │   └── SaleControllerTest.java
│           ├── integration/
│           │   └── SaleIntegrationTest.java    ← Testcontainers
│           └── security/
│               └── SecurityTest.java
│
├── pukio-analytics/                 ← Analytics Server / Data Warehouse (E2, E3, E4)
│   ├── pom.xml
│   └── src/
│       ├── main/
│       │   ├── java/com/pukio/analytics/
│       │   │   ├── AnalyticsApplication.java
│       │   │   └── dim/
│       │   │       └── DimTimePopulator.java
│       │   └── resources/
│       │       ├── application.properties
│       │       └── db/
│       │           └── schema-dw.sql
│       └── test/
│
├── nginx/                           ← Configuración Nginx (E3, E4)
│   ├── nginx.conf                   ← Upstream, least_conn, health checks, sticky sessions
│   └── ssl/                         ← Certificados Let's Encrypt (E4)
│       └── .gitkeep
│
├── ftp/                             ← Configuración FTP (E3)
│   ├── vsftpd.conf
│   └── proftpd.conf
│
├── k8s/                             ← Manifiestos Kubernetes (E4)
│   ├── namespace/
│   │   └── namespaces.yaml          ← app, data, auth, observability
│   ├── app/
│   │   ├── deployment.yaml          ← pukio-app Deployment
│   │   ├── service.yaml             ← ClusterIP Service
│   │   ├── hpa.yaml                 ← HorizontalPodAutoscaler (min:2, max:10, cpu:70%)
│   │   ├── configmap.yaml
│   │   ├── secret.yaml
│   │   ├── networkpolicy.yaml
│   │   └── cronjobs/
│   │       ├── pre-scale-events.yaml
│   │       └── scale-down-post-event.yaml
│   ├── data/
│   │   ├── postgres-primary-statefulset.yaml
│   │   ├── postgres-replica-statefulset.yaml
│   │   ├── postgres-dw-statefulset.yaml
│   │   ├── minio-statefulset.yaml
│   │   └── cronjobs/
│   │       ├── daily-backup.yaml
│   │       ├── weekly-full-backup.yaml
│   │       ├── verify-backup.yaml
│   │       └── monthly-restore-test.yaml
│   ├── auth/
│   │   └── keycloak-deployment.yaml
│   ├── observability/
│   │   ├── prometheus-deployment.yaml
│   │   ├── prometheus-rules.yaml    ← Reglas de alerta
│   │   ├── alertmanager.yaml
│   │   ├── grafana-deployment.yaml
│   │   ├── elasticsearch-statefulset.yaml
│   │   ├── logstash-deployment.yaml
│   │   └── kibana-deployment.yaml
│   └── ingress/
│       └── ingress.yaml             ← Nginx Ingress + TLS
│
├── docker/                          ← Dockerfiles y Docker Compose
│   ├── docker-compose.yml           ← Orquestación local completa
│   ├── Dockerfile.app               ← Multi-stage para pukio-app-server
│   └── Dockerfile.analytics         ← Para pukio-analytics
│
└── scripts/                         ← Scripts de utilidad
    ├── start-cluster.sh             ← Levanta 2 instancias AppServer (E3)
    ├── init-db.sh                   ← Inicializar PostgreSQL con schemas
    ├── setup-replication.sh         ← Configurar Streaming Replication (E3)
    └── dr-test.sh                   ← Ejecutar prueba de Disaster Recovery (E4)
```

---

---

## Tareas Transversales de Seguridad de Credenciales

Estas tareas no pertenecen a un entregable específico sino al proyecto completo. Deben revisarse periódicamente durante todo el desarrollo.

### SEC-G1: Documentación y Convenciones de Seguridad

- [ ] **TASK-SEC-01** — Crear y mantener la tabla de clasificación de archivos en `README.md` distinguiendo **"✅ Se sube a Git"** vs **"🚫 NUNCA se sube a Git"** para cada entregable, actualizada conforme crecen los secretos del sistema. *(REQ 7.6)*

  | Archivo | ¿Se sube a Git? | Motivo |
  |---|---|---|
  | `application.properties` | ✅ Sí | Solo placeholders `${VAR}` |
  | `application-dev.properties` | ✅ Sí | Config de dev sin passwords |
  | `application-secrets.properties` | 🚫 NUNCA | Contiene valores reales |
  | `application-secrets.properties.template` | ✅ Sí | Solo listado de variables |
  | `.env` | 🚫 NUNCA | Valores reales para Docker |
  | `.env.template` | ✅ Sí | Solo listado de variables |
  | `docker-compose.yml` | ✅ Sí | Solo referencias `${VAR}` |
  | `k8s/app/configmap.yaml` | ✅ Sí | Config no sensible |
  | `k8s/app/secret.yaml` | 🚫 NUNCA | Valores base64 reales |
  | `k8s/app/secret.yaml.template` | ✅ Sí | Solo listado de keys |
  | `nginx/ssl/*.pem`, `*.key`, `*.crt` | 🚫 NUNCA | Claves privadas SSL |
  | `*.jks`, `*.p12` | 🚫 NUNCA | Keystores con claves |
  | `vsftpd.conf`, `proftpd.conf` | ✅ Sí | Sin contraseñas reales |

- [ ] **TASK-SEC-02** — Asegurarse de que `docs/security.md` está actualizado al final de cada entregable con la lista completa de secretos vigentes, instrucciones de configuración local, y el protocolo de emergencia. *(REQ 7.5)*

### SEC-G2: Protocolo de Emergencia ante Filtración de Secretos

- [ ] **TASK-SEC-03** — Documentar en `docs/security.md` el protocolo paso a paso a seguir si un secreto es accidentalmente commiteado: *(REQ 7.5)*
  1. **Rotar inmediatamente** la credencial comprometida (cambiar contraseña de BD, revocar token, regenerar API key).
  2. Ejecutar `git filter-repo --path <archivo-con-secreto> --invert-paths` para purgar el archivo del historial completo.
  3. Ejecutar `git push --force --all` y `git push --force --tags` para sobrescribir el historial remoto.
  4. Notificar a todos los colaboradores para que hagan `git fetch` y rebasen sobre el historial limpio.
  5. Verificar con `gitleaks detect --source . --log-opts="--all"` que el secreto ya no aparece en ningún commit.

---

*Documento generado para el proyecto **Pukio** — Sistema POS Minorista*
*Total de tareas: 89 originales + 38 de seguridad = 127 tareas (E1: 57 · E2: 42 · E3: 48 · E4: 106 · Transversales: 3)*

