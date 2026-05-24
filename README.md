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

## Estado del Proyecto por Entregable

| Entregable | Arquitectura | Estado |
|---|---|---|
| Entregable 1 | Unitaria con servidor de datos | ✅ Completo |
| Entregable 2 | Cliente/Servidor | 🔜 Pendiente |
| Entregable 3 | N-Capas con alta disponibilidad | 🔜 Pendiente |
| Entregable 4 | Cloud Computing con contenedores | 🔜 Pendiente |
