# Pukio — Sistema POS para Tiendas Minoristas

Sistema de Punto de Venta con evolución arquitectónica progresiva en 4 entregables.
Desarrollado con Java 21, Spring Boot 3.3.5 y Maven multi-módulo.

---

## Módulos — Entregable 1

| Módulo | Descripción |
|---|---|
| pukio-common | Modelos compartidos y API de archivos indexados |
| pukio-maintenance | Gestión de productos e inventario |
| pukio-pos-client | Terminal de ventas local |
| pukio-send-service | Envío de datos al servidor central vía TCP |
| pukio-update-service | Recepción y sincronización con base de datos central |

---

## Requisitos previos

- Java 21
- Maven 3.9+
- PostgreSQL 18.1

---

## Configuración local — Primeros pasos

1. Clonar el repositorio.

2. En cada módulo, copiar el archivo template al archivo real de secretos:

```bash
cp pukio-update-service/src/main/resources/application-secrets.properties.template \
   pukio-update-service/src/main/resources/application-secrets.properties

cp pukio-send-service/src/main/resources/application-secrets.properties.template \
   pukio-send-service/src/main/resources/application-secrets.properties

cp pukio-maintenance/src/main/resources/application-secrets.properties.template \
   pukio-maintenance/src/main/resources/application-secrets.properties

cp pukio-pos-client/src/main/resources/application-secrets.properties.template \
   pukio-pos-client/src/main/resources/application-secrets.properties
```

3. Rellenar los valores reales en cada `application-secrets.properties` creado.

4. Nunca subir los archivos `application-secrets.properties` a Git.

---

## Compilar el proyecto

```bash
mvn clean install
```

---

## ⚠️ Seguridad — Archivos que NUNCA deben subirse a GitHub

| Archivo | Motivo |
|---|---|
| `application-secrets.properties` | Contiene contraseñas y credenciales reales |
| `.env` | Contiene credenciales para Docker |
| `.env.*` (excepto `.env.template`) | Variantes de entorno con valores reales |
| `*.jks` | Java KeyStore con claves privadas |
| `*.p12` | Certificados con clave privada |
| `*.pem` | Claves privadas SSL |
| `*.key` | Claves privadas |
| `k8s/**/secret.yaml` | Secretos de Kubernetes con valores reales |
| `ssl/` | Directorio de certificados SSL |
| `secrets/` | Directorio de secretos locales |

Estos archivos están excluidos en el `.gitignore` del proyecto.

**Si accidentalmente subes uno, consulta `docs/security.md` para el protocolo de emergencia.**

---

## Guía de Instalación y Ejecución — Entregable 1

### Prerequisitos

- Java 21
- Maven 3.9+
- Docker (para PostgreSQL)
- Git

### Paso 1 — Clonar el repositorio

```bash
git clone git@github.com:Morraban-Grid/pukio.git
cd pukio
```

### Paso 2 — Levantar PostgreSQL 18.1 con Docker

```bash
docker run -d \
  --name pukio-postgres \
  -e POSTGRES_DB=pukio_central \
  -e POSTGRES_USER=pukio_user \
  -e POSTGRES_PASSWORD=pukio_pass \
  -p 5432:5432 \
  postgres:18.1
```

Verificar que está corriendo:

```bash
docker ps
```

### Paso 3 — Crear las tablas en la base de datos

```bash
docker exec -i pukio-postgres psql \
  -U pukio_user \
  -d pukio_central \
  < pukio-update-service/src/main/resources/schema-e1.sql
```

### Paso 4 — Configurar secretos locales

Copiar los templates y rellenar los valores en cada módulo:

**pukio-update-service:**

```bash
cp pukio-update-service/src/main/resources/application-secrets.properties.template \
   pukio-update-service/src/main/resources/application-secrets.properties
```

Editar el archivo y rellenar:

```properties
DB_URL=jdbc:postgresql://localhost:5432/pukio_central
DB_USERNAME=pukio_user
DB_PASSWORD=pukio_pass
SERVER_PORT=9090
STORE_ID=STORE-001
```

**pukio-send-service:**

```bash
cp pukio-send-service/src/main/resources/application-secrets.properties.template \
   pukio-send-service/src/main/resources/application-secrets.properties
```

Editar el archivo y rellenar:

```properties
SERVER_HOST=localhost
SERVER_PORT=9090
PUKIO_FILES_PRODUCTS=/tmp/pukio/data/products
PUKIO_FILES_INVENTORY=/tmp/pukio/data/inventory
```

**pukio-maintenance y pukio-pos-client:**

```bash
cp pukio-maintenance/src/main/resources/application-secrets.properties.template \
   pukio-maintenance/src/main/resources/application-secrets.properties

cp pukio-pos-client/src/main/resources/application-secrets.properties.template \
   pukio-pos-client/src/main/resources/application-secrets.properties
```

Editar cada archivo y rellenar:

```properties
PUKIO_FILES_PRODUCTS=/tmp/pukio/data/products
PUKIO_FILES_INVENTORY=/tmp/pukio/data/inventory
PUKIO_FILES_SALES=/tmp/pukio/data/sales
```

### Paso 5 — Crear los directorios de datos locales

```bash
mkdir -p /tmp/pukio/data
```

### Paso 6 — Compilar el proyecto completo

```bash
mvn clean install
```

### Paso 7 — Ejecutar los módulos

Abrir una terminal por cada módulo en el siguiente orden:

**Terminal 1 — Update Service (servidor TCP + sync a BD):**

```bash
cd pukio-update-service
mvn spring-boot:run
```

**Terminal 2 — Maintenance (gestión de productos e inventario):**

```bash
cd pukio-maintenance
mvn spring-boot:run
```

**Terminal 3 — POS Client (terminal de ventas):**

```bash
cd pukio-pos-client
mvn spring-boot:run
```

**Terminal 4 — Send Service (envío de datos al servidor):**

```bash
cd pukio-send-service
mvn spring-boot:run
```

### Paso 8 — Detener PostgreSQL cuando termines

```bash
docker stop pukio-postgres
```

Para volver a levantarlo:

```bash
docker start pukio-postgres
```

---

---

## Guía de Instalación y Ejecución — Entregable 2

### Descripción General

El Entregable 2 implementa una arquitectura Cliente/Servidor donde:
- **POS_Client** es un cliente ligero (solo UI) que delega toda la lógica al servidor
- **Application_Server** (Spring Boot) centraliza la lógica de negocio y acceso a datos
- **Data_Server** (PostgreSQL) almacena datos transaccionales
- **Analytics_Server** (PostgreSQL DW) almacena datos analíticos en esquema estrella
- **Apache Superset** (opcional) proporciona dashboards y visualización de datos

### Requisitos previos

- Java 21
- Maven 3.9.9
- PostgreSQL 18.1 (dos instancias: `pukio_db` y `pukio_analytics`)
- Apache Superset 6.1.0 (opcional para dashboards)
- Python 3.9+ (si se usa Superset)

### Paso 1 — Clonar el repositorio

```bash
git clone git@github.com:Morraban-Grid/pukio.git
cd pukio
```

### Paso 2 — Levantar las bases de datos PostgreSQL

**Base de datos transaccional:**

```bash
docker run -d \
  --name pukio-db \
  -e POSTGRES_DB=pukio_db \
  -e POSTGRES_USER=pukio_user \
  -e POSTGRES_PASSWORD=pukio_pass \
  -p 5432:5432 \
  postgres:18.1
```

**Base de datos de Analytics (Data Warehouse):**

```bash
docker run -d \
  --name pukio-analytics \
  -e POSTGRES_DB=pukio_analytics \
  -e POSTGRES_USER=pukio_analytics_user \
  -e POSTGRES_PASSWORD=analytics_pass \
  -p 5433:5432 \
  postgres:18.1
```

Verificar que ambas están corriendo:

```bash
docker ps
```

### Paso 3 — Crear las tablas en ambas bases de datos

**Base de datos transaccional:**

```bash
docker exec -i pukio-db psql \
  -U pukio_user \
  -d pukio_db \
  < pukio-app-server/src/main/resources/db/schema-e2.sql
```

**Base de datos Analytics:**

```bash
docker exec -i pukio-analytics psql \
  -U pukio_analytics_user \
  -d pukio_analytics \
  < pukio-analytics/src/main/resources/db/schema-dw.sql
```

### Paso 4 — Configurar secretos locales

**pukio-app-server:**

```bash
cp pukio-app-server/src/main/resources/application-secrets.properties.template \
   pukio-app-server/src/main/resources/application-secrets.properties
```

Editar `pukio-app-server/src/main/resources/application-secrets.properties`:

```properties
# Base de datos transaccional
DB_USERNAME=pukio_user
DB_PASSWORD=pukio_pass
DB_URL=jdbc:postgresql://localhost:5432/pukio_db

# Base de datos Analytics (Data Warehouse)
ANALYTICS_DB_URL=jdbc:postgresql://localhost:5433/pukio_analytics
ANALYTICS_DB_USERNAME=pukio_analytics_user
ANALYTICS_DB_PASSWORD=analytics_pass

# Pool de conexiones HikariCP (opcional, ya tiene defaults en application-dev.properties)
# HIKARI_MAX_POOL_SIZE=20
```

**pukio-pos-client:**

```bash
cp pukio-pos-client/src/main/resources/application-secrets.properties.template \
   pukio-pos-client/src/main/resources/application-secrets.properties
```

Editar `pukio-pos-client/src/main/resources/application-secrets.properties`:

```properties
# URL del Application Server
APP_SERVER_URL=http://localhost:8080
```

**pukio-analytics:**

```bash
cp pukio-analytics/src/main/resources/application-secrets.properties.template \
   pukio-analytics/src/main/resources/application-secrets.properties
```

Editar `pukio-analytics/src/main/resources/application-secrets.properties`:

```properties
ANALYTICS_DB_URL=jdbc:postgresql://localhost:5433/pukio_analytics
ANALYTICS_DB_USERNAME=pukio_analytics_user
ANALYTICS_DB_PASSWORD=analytics_pass
```

### Paso 5 — Compilar el proyecto completo

```bash
mvn clean install
```

### Paso 6 — Ejecutar los módulos

Abrir una terminal por cada módulo:

**Terminal 1 — Application Server (servidor Spring Boot con toda la lógica):**

```bash
cd pukio-app-server
mvn spring-boot:run
```

El servidor estará disponible en `http://localhost:8080`

**Terminal 2 — POS Client (cliente ligero con UI Swing):**

```bash
cd pukio-pos-client
mvn spring-boot:run
```

La interfaz gráfica se abrirá automáticamente.

**Terminal 3 (opcional) — Analytics Service (poblar dimensiones del DW):**

```bash
cd pukio-analytics
mvn spring-boot:run
```

Este servicio pobla la tabla `dim_time` al arrancar y queda en ejecución para posibles ETLs adicionales.

### Paso 7 (opcional) — Configurar Apache Superset para dashboards

Si deseas visualizar dashboards analíticos:

1. Instalar Superset:

```bash
pip install apache-superset==6.1.0
superset db upgrade
superset fab create-admin
superset init
```

2. Arrancar Superset:

```bash
superset run -p 8088 --with-threads --reload --debugger
```

3. Acceder a `http://localhost:8088` y seguir la guía completa en:

```
docs/superset-setup.md
```

### Paso 8 — Probar el sistema

1. **Login en POS_Client:** Ingresar usuario y contraseña (autenticación pendiente de implementar en E2, por ahora puede usar credenciales de prueba)

2. **Procesar una venta:**
   - En el panel de Venta Activa, ingresar SKU de un producto
   - Añadir cantidad
   - Seleccionar método de pago
   - Click en "Cobrar"
   - Verificar que aparece el recibo

3. **Verificar en la base de datos:**

```bash
docker exec -it pukio-db psql -U pukio_user -d pukio_db -c "SELECT * FROM sales ORDER BY sale_date DESC LIMIT 5;"
```

4. **Verificar en el Data Warehouse:**

```bash
docker exec -it pukio-analytics psql -U pukio_analytics_user -d pukio_analytics -c "SELECT * FROM fact_sales ORDER BY sale_id DESC LIMIT 5;"
```

### Paso 9 — Detener los servicios

**Detener PostgreSQL:**

```bash
docker stop pukio-db pukio-analytics
```

**Detener Superset:**

```bash
# Ctrl+C en la terminal donde está corriendo
```

Para volver a levantar las bases de datos:

```bash
docker start pukio-db pukio-analytics
```

---

## Perfiles de ejecución (dev / prod)

El Application Server soporta dos perfiles:

**Desarrollo (por defecto):**

```bash
cd pukio-app-server
mvn spring-boot:run -Dspring-boot.run.profiles=dev
```

- Logging verbose (DEBUG level)
- Pool de conexiones pequeño (5 conexiones transaccional, 2 analytics)
- Sin caché

**Producción:**

```bash
cd pukio-app-server
mvn spring-boot:run -Dspring-boot.run.profiles=prod
```

- Logging INFO level
- Pool de conexiones optimizado (20 conexiones transaccional, 5 analytics)
- Caché habilitado

---

## Estado del Proyecto por Entregable

| Entregable | Arquitectura | Estado |
|---|---|---|
| Entregable 1 | Unitaria con servidor de datos | ✅ Completo |
| Entregable 2 | Cliente/Servidor | 🚧 En progreso (Fase 4B) |
| Entregable 3 | N-Capas con alta disponibilidad | 🔜 Pendiente |
| Entregable 4 | Cloud Computing con contenedores | 🔜 Pendiente |
