# PUKIO — Sistema POS Web

PUKIO es un sistema de punto de venta (POS) web diseñado para pequeños negocios. Incluye módulos de productos, categorías, clientes, proveedores, ventas, reportes y un data warehouse analítico simple. El backend es Java 21 + Jakarta Servlets desplegado en Tomcat; el frontend es JavaScript vanilla + HTML + CSS sin herramientas de build.

---

## Requisitos Previos

Puedes instalar la mayoría de requisitos en Windows desde PowerShell con `winget`:

| Requisito | Versión mínima | winget |
|-----------|---------------|--------|
| Docker Desktop | Cualquier versión reciente | `winget install Docker.DockerDesktop` |
| JDK 21 | 21 | `winget install Oracle.JDK.21` |
| Apache Maven | 3.8+ | `winget install Apache.Maven` |
| Apache Tomcat | 10.1.x | [Descargar manualmente](https://tomcat.apache.org/download-10.cgi) |
| DBeaver Community (u otro cliente Oracle) | — | `winget install dbeaver.dbeaver` |
| Python o Node.js | Cualquier versión reciente | `winget install Python.Python.3` |

---

## Paso 1 — Configuración

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
El valor por defecto (`http://localhost:8080/backend/api`) funciona sin cambios para desarrollo local.

---

## Paso 2 — Levantar Oracle XE en Docker

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

## Paso 3 — Crear Esquemas e Inicializar la Base de Datos

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
5. **`04_crear_sp.sql`**: registra el procedimiento almacenado y el trigger de stock.

---

## Paso 4 — Compilar y Desplegar el Backend

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

## Paso 5 — Levantar el Frontend

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

## Credenciales de Acceso por Defecto

| Rol | Usuario | Contraseña |
|-----|---------|------------|
| Administrador | `admin` | `admin123` |
| Cajero | `cajero` | `cajero123` |

Las contraseñas están almacenadas con hash BCrypt (factor de costo 12) en la base de datos.

---

## Troubleshooting

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
Si tenías usuarios creados con SHA-256 (antes del refactor), sus contraseñas deben rehacerse con BCrypt. Usa `SecurityUtil.hash("nueva_password")` para generar el nuevo hash e insértalo manualmente en la columna `PASSWORD_HASH`.
