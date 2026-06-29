# PUKIO — Sistema POS Web

PUKIO es un sistema de punto de venta (POS) web diseñado para pequeños y medianos negocios del sector retail. Incluye módulos de productos, categorías, clientes, proveedores, ventas, reportes analíticos con Data Warehouse y administración de usuarios. El backend es Java 21 + Jakarta Servlets desplegado en Tomcat; el frontend es JavaScript vanilla + HTML + CSS sin herramientas de build.

---

## Tabla de Contenidos

1. [Requisitos Previos](#1-requisitos-previos)
2. [Paso 1 — Configuración](#2-paso-1--configuración)
3. [Paso 2 — Levantar Oracle XE en Docker](#3-paso-2--levantar-oracle-xe-en-docker)
4. [Paso 3 — Crear Esquemas e Inicializar la Base de Datos](#4-paso-3--crear-esquemas-e-inicializar-la-base-de-datos)
5. [Paso 4 — Compilar y Desplegar el Backend](#5-paso-4--compilar-y-desplegar-el-backend)
6. [Paso 5 — Levantar el Frontend](#6-paso-5--levantar-el-frontend)
7. [Credenciales de Acceso por Defecto](#7-credenciales-de-acceso-por-defecto)
8. [Roles y Permisos](#8-roles-y-permisos)
9. [Módulos del Sistema — Guía Operativa](#9-módulos-del-sistema--guía-operativa)
10. [Gestión de Usuarios (solo ADMIN)](#10-gestión-de-usuarios-solo-admin)
11. [Mensajes de Error Frecuentes](#11-mensajes-de-error-frecuentes)
12. [Troubleshooting de Instalación](#12-troubleshooting-de-instalación)
13. [Glosario](#13-glosario)

---

## 1. Requisitos Previos

Puedes instalar la mayoría de requisitos en Windows desde PowerShell con `winget`:

| Requisito | Versión mínima | Comando winget |
|-----------|---------------|----------------|
| Docker Desktop | Cualquier versión reciente | `winget install Docker.DockerDesktop` |
| JDK 21 | 21 | `winget install Oracle.JDK.21` |
| Apache Maven | 3.8+ | `winget install Apache.Maven` |
| Apache Tomcat | 10.1.x | [Descargar manualmente](https://tomcat.apache.org/download-10.cgi) |
| DBeaver Community (u otro cliente Oracle) | — | `winget install dbeaver.dbeaver` |
| Python o Node.js | Cualquier versión reciente | `winget install Python.Python.3` |

---

## 2. Paso 1 — Configuración

Antes de compilar o levantar nada, crea los archivos de configuración local:

**Backend:**
```bash
cp backend/src/main/resources/config.properties.example \
   backend/src/main/resources/config.properties
```
Abre `config.properties` y completa los valores:
```properties
db.host=localhost
db.port=1521
db.service=XEPDB1
db.user=PUKIO_DB
db.password=TU_PASSWORD_AQUI        # la misma que usarás en Docker (Paso 2)
cors.allowed.origin=http://localhost:8000
```

**Frontend:**
```bash
cp frontend/js/config.example.js frontend/js/config.js
```

> ⚠️ **ADVERTENCIA — MOCK_MODE:** El archivo `config.js` tiene la opción `MOCK_MODE`.
> Asegúrate de que esté en `false` para cualquier entorno real o de pruebas:
> ```js
> MOCK_MODE: false   // ← NUNCA cambiar a true en producción
> ```
> Con `MOCK_MODE: true`, **todas** las operaciones (ventas, productos, clientes, etc.)
> se guardan solo en el navegador (localStorage) y **no se persisten en la base de datos**.
> Los datos se pierden al limpiar el historial del navegador.

---

## 3. Paso 2 — Levantar Oracle XE en Docker

### Primera vez (instalación limpia):
```bash
docker run -d \
  --name oracle-pukio \
  -p 1521:1521 \
  -e ORACLE_PASSWORD=TU_PASSWORD_AQUI \
  gvenzl/oracle-xe:21-slim
```
> Usa la misma contraseña que pusiste en `config.properties`.

Verifica que el contenedor esté listo (puede tardar 2–5 minutos):
```bash
docker logs -f oracle-pukio
```
Cuando aparezca `DATABASE IS READY TO USE!` puedes continuar.

### Inicios posteriores (contenedor ya creado):
```bash
docker start oracle-pukio
docker ps   # verifica que esté corriendo
```

---

## 4. Paso 3 — Crear Esquemas e Inicializar la Base de Datos

Conéctate con tu cliente Oracle (DBeaver u otro) usando estas credenciales de administrador:

| Campo | Valor |
|-------|-------|
| Host | `localhost` |
| Puerto | `1521` |
| Servicio | `XEPDB1` |
| Usuario | `SYSTEM` (o `SYS AS SYSDBA`) |
| Contraseña | La que usaste en Docker |

Ejecuta los scripts en `sql/` **en este orden estricto**:

1. **`00_crear_usuario.sql`** *(conectado como SYSTEM)*: crea los esquemas `PUKIO_DB` y `PUKIO_DWH`.
2. **`01_crear_tablas.sql`**: crea las tablas transaccionales.
3. **`02_insertar_datos.sql`**: inserta datos semilla (usuarios, productos, categorías).
4. **`03_crear_dwh.sql`**: crea las tablas del Data Warehouse en `PUKIO_DWH`.
5. **`04_crear_sp.sql`**: registra el procedimiento almacenado `SP_REGISTRAR_VENTA` y el trigger `TRG_DETALLE_VENTA_STOCK`.

---

## 5. Paso 4 — Compilar y Desplegar el Backend

Desde la raíz del repositorio:
```bash
cd backend
mvn clean package
```
Esto genera `backend/target/backend.war`.

Despliega en Tomcat:
- **Opción A (copiar WAR):** copia `backend/target/backend.war` a la carpeta `webapps/` de tu instalación de Tomcat 10.1.x.
- **Opción B (Manager):** sube el WAR desde `http://localhost:8080/manager/html`.

Inicia Tomcat. El backend quedará disponible en:
```
http://localhost:8080/backend/api
```

---

## 6. Paso 5 — Levantar el Frontend

Desde la raíz del repositorio, sirve los archivos estáticos con Python:
```bash
python -m http.server 8000
```
O con Node.js:
```bash
npx serve . -p 8000
```

Abre en el navegador:
```
http://localhost:8000/frontend/index.html
```

---

## 7. Credenciales de Acceso por Defecto

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Administrador | `admin` | `admin123` |
| Cajero | `cajero` | `cajero123` |

Las contraseñas están almacenadas con hash BCrypt (factor de costo 12) en la columna `PASSWORD_HASH` de la tabla `USUARIOS`.

> **Importante:** Cambia las contraseñas por defecto antes de poner el sistema en producción. Puedes hacerlo desde **Usuarios → Restablecer Contraseña** (requiere rol ADMIN).

---

## 8. Roles y Permisos

PUKIO diferencia dos roles operativos con distintos niveles de acceso:

### ADMIN (Administrador)
Tiene acceso completo a todos los módulos del sistema:

| Módulo | Acceso |
|--------|--------|
| Dashboard | ✅ Lectura |
| Punto de Venta (POS) | ✅ Completo |
| Productos | ✅ Completo (CRUD) |
| Clientes | ✅ Completo (CRUD) |
| Proveedores | ✅ Completo (CRUD) |
| Categorías | ✅ Completo (CRUD) |
| Reportes y OLAP | ✅ Completo (incluye ETL y exportación CSV) |
| Administración de Usuarios | ✅ Completo (CRUD, activar/desactivar, restablecer contraseña) |

### CAJERO
Tiene acceso operativo al sistema pero **sin** acceso a reportes ni gestión de usuarios:

| Módulo | Acceso |
|--------|--------|
| Dashboard | ✅ Lectura |
| Punto de Venta (POS) | ✅ Completo |
| Productos | ✅ Lectura y consulta |
| Clientes | ✅ Consulta y registro rápido desde POS |
| Proveedores | ⛔ Sin acceso |
| Reportes y OLAP | ⛔ Sin acceso (oculto en menú y bloqueado en backend) |
| Administración de Usuarios | ⛔ Sin acceso (oculto en menú y bloqueado en backend) |

> La restricción de acceso está implementada en dos capas:
> - **Frontend:** el menú lateral oculta las secciones no disponibles para el rol `CAJERO` (`app.js`).
> - **Backend:** `SecurityFilter` bloquea con HTTP 403 cualquier petición a `/api/reportes/*`, `/api/dwh/*` y `/api/usuarios/*` que provenga de una sesión con rol `CAJERO`.

---

## 9. Módulos del Sistema — Guía Operativa

### 9.1 Iniciar Sesión
1. Abre `http://localhost:8000/frontend/index.html`.
2. Ingresa tu usuario y contraseña.
3. Haz clic en **Iniciar Sesión**.
4. Serás redirigido automáticamente al **Dashboard**.

> La sesión expira automáticamente después de **30 minutos** de inactividad. Al expirar, el sistema redirige al login y debes ingresar nuevamente tus credenciales.

### 9.2 Dashboard
El Dashboard muestra los indicadores del día al iniciar sesión:
- **Ventas del día:** número de transacciones realizadas hoy.
- **Ingresos del día:** monto total facturado hoy.
- **Productos con bajo stock:** productos cuyo stock actual está por debajo del stock mínimo definido.
- **Accesos rápidos** a los módulos más utilizados.

### 9.3 Punto de Venta (POS)
El módulo de mayor uso operacional. Permite registrar ventas completas.

**Flujo de una venta:**
1. **Buscar producto:** escribe el nombre o código de barras en el campo de búsqueda. Haz clic en el producto o presiona Enter para agregarlo al carrito.
2. **Ajustar cantidades:** en el carrito, modifica la cantidad de cada ítem con los botones `+` / `−` o editando el campo directamente.
3. **Seleccionar cliente:** busca al cliente por nombre o número de documento. Si el cliente no está registrado, puedes registrarlo rápidamente desde el modal que aparece al hacer clic en **Nuevo Cliente**. Para ventas al público en general, selecciona **Clientes Varios**.
4. **Elegir tipo de comprobante:** Boleta (BOL-) o Factura (FAC-). El número de comprobante se genera automáticamente en orden correlativo.
5. **Elegir método de pago:** Efectivo o Tarjeta.
6. **Confirmar venta:** haz clic en **Cobrar / Registrar Venta**. El sistema valida el stock de cada ítem antes de confirmar.

**Estados posibles de una venta:**
- **Carrito:** venta en proceso, aún no confirmada.
- **Validando:** el sistema está verificando el stock y procesando la transacción.
- **Completada:** venta registrada con éxito. Se emite el comprobante correlativo.
- **Rechazada:** la venta no pudo completarse (generalmente por stock insuficiente). Corrige las cantidades y vuelve a intentarlo.

> El cálculo del IGV (18%) se realiza automáticamente sobre el subtotal. El precio de venta de cada producto ya incluye IGV; el sistema lo desglosa en el resumen antes de confirmar.

### 9.4 Gestión de Productos
Permite al ADMIN crear, editar y dar de baja productos del catálogo.

- **Registrar producto:** haz clic en **Nuevo Producto**, completa el formulario (código de barras, nombre, precios, stock actual, stock mínimo, categoría y proveedor) y guarda.
- **Editar producto:** haz clic en el ícono de edición en la fila del producto.
- **Dar de baja:** usa el botón de desactivar. El producto no aparecerá en el POS pero se conserva el historial de ventas.
- **Alertas de bajo stock:** los productos con stock por debajo del mínimo aparecen resaltados en el listado y en el Dashboard.

> El código de barras (SKU) debe ser único. Si intentas registrar un código duplicado recibirás el error **"Código de producto ya registrado"**.

### 9.5 Gestión de Clientes
- **Buscar cliente:** escribe nombre o número de documento en el campo de búsqueda.
- **Registrar cliente:** completa nombre, tipo de documento (DNI u RUC) y número. El sistema valida automáticamente la longitud: DNI = 8 dígitos, RUC = 11 dígitos.
- **Editar / Eliminar:** disponible desde la tabla de clientes.

> El cliente **"Clientes Varios"** (DNI `00000000`) es el cliente genérico para ventas al público. No puede eliminarse.

### 9.6 Gestión de Proveedores y Categorías
Accesible para el ADMIN desde el menú o desde los formularios de Productos:
- **Proveedores:** CRUD completo. El RUC del proveedor debe ser único (11 dígitos). Si intentas registrar un RUC duplicado recibirás el error **"RUC de proveedor ya registrado"**.
- **Categorías:** CRUD completo. Se usan para clasificar productos y para el análisis OLAP.

### 9.7 Reportes y Business Intelligence *(solo ADMIN)*
Accesible únicamente para el rol ADMIN.

**Historial de Ventas:**
1. Selecciona el rango de fechas en los campos **Desde** y **Hasta**.
2. Haz clic en **Filtrar** para actualizar la tabla de ventas.
3. La tabla muestra: comprobante, fecha, cliente, descuento, total, método de pago y cajero responsable.

**Exportar a CSV:**
- Después de filtrar, haz clic en **Exportar CSV** para descargar el reporte del rango actual en formato CSV. El archivo se descargará automáticamente como `ventas_YYYY-MM-DD_YYYY-MM-DD.csv`.

**Top Productos:**
- Muestra el ranking de los productos más vendidos por cantidad e ingresos. Cambia el número de productos mostrados con el selector (5, 10, 20).

**Cubo OLAP (CrossTab):**
- Tabla de doble entrada que muestra ventas por año/mes y categoría. Permite identificar tendencias de ventas por período y línea de producto.

**Proceso ETL (Actualizar Data Warehouse):**
- Haz clic en **Procesar DWH (ETL)** para actualizar el Data Warehouse con las ventas más recientes. El proceso ejecuta `SP_REGISTRAR_VENTA` y reconstruye la vista `CROSSTAB_VENTAS`.
- Se recomienda ejecutar el ETL al inicio de la jornada o antes de generar reportes importantes.

---

## 10. Gestión de Usuarios *(solo ADMIN)*

La gestión completa de usuarios está disponible desde el menú **Usuarios**, accesible únicamente para el rol ADMIN.

### Registrar nuevo usuario (RF-51)
1. Haz clic en **Nuevo Usuario**.
2. Completa: nombre de usuario, nombre completo, rol (ADMIN o CAJERO) y contraseña.
3. Haz clic en **Guardar**.

### Activar / Desactivar usuario (RF-52)
- En la tabla de usuarios, haz clic en el ícono de activar/desactivar en la columna de acciones.
- Los usuarios inactivos no pueden iniciar sesión.

### Restablecer contraseña (RF-53)
1. Haz clic en el ícono de editar del usuario.
2. En la sección **Restablecer Contraseña** al final del formulario, ingresa la nueva contraseña.
3. Haz clic en **Actualizar Contraseña**.

> Si necesitas gestionar usuarios directamente en la base de datos (por ejemplo, para recuperar acceso de administrador), usa el siguiente procedimiento SQL conectándote como SYSTEM:
> ```sql
> -- Generar hash BCrypt en Java con SecurityUtil.hash('nueva_password')
> -- Luego actualizar directamente:
> UPDATE PUKIO_DB.USUARIOS
> SET PASSWORD_HASH = '<hash_bcrypt_generado>'
> WHERE USERNAME = 'admin';
> COMMIT;
> ```

---

## 11. Mensajes de Error Frecuentes

| Error mostrado | Causa | Acción correctiva |
|---------------|-------|-------------------|
| **"Stock insuficiente para [producto]"** | La cantidad solicitada en el carrito supera el stock disponible. El trigger `TRG_DETALLE_VENTA_STOCK` rechaza la operación y hace ROLLBACK automático. | Reduce la cantidad del producto en el carrito o retíralo de la venta. |
| **"La sesión ha expirado. Inicie sesión nuevamente."** | La sesión del servidor expiró después de 30 minutos de inactividad (RF-03). | Vuelve a la pantalla de login e ingresa tus credenciales. |
| **"El documento de identidad debe ser DNI (8 dígitos) o RUC (11 dígitos)."** | El número de documento ingresado para un cliente no tiene la longitud correcta (RF-24). | Verifica el número e ingrésalo sin espacios ni guiones. |
| **"Código de producto ya registrado."** | Intentas guardar un producto con un código de barras que ya existe en el sistema (RF-15). | Usa un código diferente o edita el producto existente. |
| **"RUC de proveedor ya registrado."** | Intentas registrar un proveedor con un RUC que ya existe (RF-22). | Busca el proveedor existente y edítalo si necesitas actualizar datos. |
| **"El carrito está vacío."** | Intentas confirmar una venta sin ningún producto en el carrito. | Agrega al menos un producto antes de cobrar. |
| **"Debe seleccionar un cliente."** | Intentas confirmar la venta sin asignar un cliente. | Selecciona un cliente o usa "Clientes Varios" para ventas al público. |
| **"Acceso denegado. Se requiere rol ADMIN."** | Un usuario con rol CAJERO intentó acceder a reportes, usuarios u otra sección restringida. | Inicia sesión con una cuenta ADMIN para acceder a esa sección. |

---

## 12. Troubleshooting de Instalación

**El puerto 1521 ya está en uso:**
```bash
# Detén cualquier servicio Oracle local, o cambia el puerto del contenedor:
docker run -d --name oracle-pukio -p 1522:1521 -e ORACLE_PASSWORD=... gvenzl/oracle-xe:21-slim
# Y actualiza db.port=1522 en config.properties
```

**Cómo verificar que el contenedor esté listo:**
```bash
docker logs oracle-pukio | grep "DATABASE IS READY"
```

**Error `config.properties` no encontrado al arrancar Tomcat:**
Asegúrate de haber copiado `config.properties.example` a `config.properties` y de haber recompilado con `mvn clean package` antes de desplegar.

**`mvn clean package` falla con error de ojdbc11:**
El driver Oracle se descarga automáticamente desde Maven Central. Asegúrate de tener conexión a internet al compilar por primera vez.

**La sesión expira o el login falla tras la migración:**
Si tenías usuarios creados con SHA-256 (antes del refactor), sus contraseñas deben rehacerse con BCrypt. Conéctate a la base de datos y ejecuta:
```sql
UPDATE PUKIO_DB.USUARIOS
SET PASSWORD_HASH = '<nuevo_hash_bcrypt>'
WHERE USERNAME = '<usuario>';
COMMIT;
```
Para generar el hash BCrypt, usa `SecurityUtil.hash("nueva_password")` desde el código Java.

---

## 13. Glosario

| Término | Significado |
|---------|-------------|
| **POS** | *Point of Sale* — Punto de Venta. Terminal donde se registran las ventas. |
| **IGV** | Impuesto General a las Ventas (18%). Equivalente al IVA en otros países. Se aplica sobre el subtotal de la venta. |
| **Boleta (BOL-)** | Comprobante de pago para personas naturales. Numeración correlativa automática. |
| **Factura (FAC-)** | Comprobante de pago para empresas (requiere RUC). Numeración correlativa automática. |
| **Stock mínimo** | Umbral de cantidad mínima de un producto. Al caer por debajo, el sistema genera una alerta de bajo stock en el Dashboard. |
| **Clientes Varios** | Cliente genérico (DNI 00000000) para ventas al público sin identificación. |
| **BCrypt** | Algoritmo de cifrado de contraseñas. PUKIO usa factor de costo 12 para almacenar contraseñas de forma segura. |
| **ETL** | *Extract, Transform, Load* — proceso que copia datos transaccionales al Data Warehouse para análisis. |
| **Data Warehouse (DWH)** | Base de datos analítica (`PUKIO_DWH`) que almacena datos históricos de ventas para reportes. |
| **OLAP / CrossTab** | Cubo analítico que muestra ventas cruzadas por período (año/mes) y categoría de producto. |
| **WAR** | *Web Application Archive* — archivo empaquetado del backend Java para desplegar en Tomcat. |
| **Maven** | Herramienta de construcción del proyecto Java. Se usa con `mvn clean package`. |
| **HikariCP** | Pool de conexiones de base de datos de alto rendimiento usado internamente por PUKIO. |
| **Jakarta Servlets** | Especificación Java para aplicaciones web. Base del backend de PUKIO (requiere Tomcat 10+). |
| **Docker** | Plataforma de contenedores usada para ejecutar Oracle XE sin instalación directa. |
| **MOCK_MODE** | Modo de simulación del frontend. Con `true`, los datos se guardan en el navegador, no en Oracle. Solo para desarrollo sin backend. |
