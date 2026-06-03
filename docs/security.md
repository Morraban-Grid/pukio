# Pukio — Estrategia de Gestión de Secretos y Seguridad de Credenciales

Este documento describe cómo se gestionan las credenciales y secretos sensibles
en el proyecto Pukio para que NUNCA sean expuestos en el repositorio Git.

---

## Regla fundamental

Ningún archivo con valores reales de contraseñas, tokens, claves API,
claves privadas SSL, IPs internas o puertos sensibles debe aparecer
jamás en el historial de Git.

---

## Archivos que NUNCA se suben a Git

| Archivo | Por qué es sensible |
|---|---|
| `application-secrets.properties` | Contraseñas de BD, tokens, claves |
| `.env` | Credenciales para Docker Compose |
| `k8s/**/secret.yaml` | Secretos de Kubernetes en base64 |
| `*.pem / *.key / *.jks / *.p12` | Claves privadas y certificados SSL |

Todos están excluidos en el `.gitignore` raíz del proyecto.

---

## Estructura de configuración por entorno

### Desarrollo local (Entregable 1)

```
src/main/resources/
├── application.properties              ← SÍ se sube (solo placeholders ${VAR})
├── application-secrets.properties.template  ← SÍ se sube (variables sin valores)
└── application-secrets.properties      ← NUNCA se sube (valores reales locales)
```

### Docker (Entregable 4)

```
.env.template    ← SÍ se sube (variables sin valores)
.env             ← NUNCA se sube (valores reales)
```

### Kubernetes (Entregable 4)

```
k8s/app/secret.yaml.template   ← SÍ se sube (keys sin valores)
k8s/app/secret.yaml            ← NUNCA se sube (valores base64 reales)
```

---

## Procedimiento de onboarding para nuevos desarrolladores

1. Clonar el repositorio.

2. Por cada módulo Spring Boot, copiar el template al archivo real:
   ```bash
   cp application-secrets.properties.template application-secrets.properties
   ```

3. Rellenar cada variable con el valor real proporcionado por el líder técnico
   (nunca compartir estos valores por canales públicos como Slack o email).

4. Verificar que `git status` no muestra ningún `application-secrets.properties`
   como archivo nuevo o modificado antes de hacer cualquier commit.

---

## Protocolo de emergencia — Si un secreto fue commiteado accidentalmente

Si detectas que un archivo con credenciales reales fue subido a GitHub,
sigue estos pasos INMEDIATAMENTE:

### Paso 1 — Rotar la credencial comprometida

Cambia inmediatamente la contraseña, revoca el token o regenera la API key
expuesta. Hazlo ANTES de limpiar el historial Git, porque el daño ya ocurrió.

### Paso 2 — Eliminar el archivo del historial completo de Git

```bash
git filter-repo --path ruta/al/archivo/secreto --invert-paths
```

### Paso 3 — Forzar la actualización del repositorio remoto

```bash
git push --force --all
git push --force --tags
```

### Paso 4 — Notificar a todos los colaboradores

Todos deben ejecutar:
```bash
git fetch origin
```

Y rebasar su trabajo local sobre el historial limpio.

### Paso 5 — Verificar que el secreto fue eliminado

Instalar `gitleaks` y ejecutar:
```bash
gitleaks detect --source . --log-opts="--all"
```

Confirmar que el reporte no detecta ningún secreto en el historial.

---

## Variables de entorno por módulo — Entregable 1

### pukio-update-service

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC completa de PostgreSQL |
| `DB_USERNAME` | Usuario de la base de datos |
| `DB_PASSWORD` | Contraseña de la base de datos |
| `SERVER_PORT` | Puerto TCP donde escucha el servidor |

### pukio-send-service

| Variable | Descripción |
|---|---|
| `SERVER_HOST` | IP o hostname del servidor central |
| `SERVER_PORT` | Puerto TCP del servidor central |

### pukio-maintenance y pukio-pos-client

| Variable | Descripción |
|---|---|
| `PUKIO_FILES_PRODUCTS` | Ruta absoluta al archivo indexado de productos |
| `PUKIO_FILES_INVENTORY` | Ruta absoluta al archivo indexado de inventario |
| `PUKIO_FILES_SALES` | Ruta absoluta al archivo indexado de ventas (solo pos-client) |

---

## Variables de entorno por módulo — Entregable 2

### pukio-app-server (Application Server)

| Variable | Descripción |
|---|---|
| `DB_URL` | URL JDBC completa de PostgreSQL 18.1 (Data Server) |
| `DB_USERNAME` | Usuario de la base de datos transaccional |
| `DB_PASSWORD` | Contraseña de la base de datos transaccional |
| `SERVER_PORT` | Puerto HTTP donde escucha el Application Server (ej: 8080) |
| `ANALYTICS_DB_URL` | URL JDBC del Data Warehouse (Analytics Server) |
| `ANALYTICS_DB_USERNAME` | Usuario del Data Warehouse |
| `ANALYTICS_DB_PASSWORD` | Contraseña del Data Warehouse |

### Configuración local — Entregable 2

Para desarrolladores que clonan el repositorio:

1. Navegar al módulo `pukio-app-server/src/main/resources/`
2. Copiar el template:
   ```bash
   cp application-secrets.properties.template application-secrets.properties
   ```
3. Editar `application-secrets.properties` y rellenar todos los valores
4. Verificar que el archivo NO aparece en `git status`

### Perfiles Spring Boot — Entregable 2

El sistema soporta dos perfiles:

- **`dev`**: Para desarrollo local con logging verbose y pools pequeños
- **`prod`**: Para producción con logging INFO y pools optimizados

Los archivos `application-dev.properties` y `application-prod.properties` SÍ se suben a Git porque contienen solo configuración no sensible (tamaños de pool, niveles de log, timeouts). Las credenciales reales se leen desde `application-secrets.properties` o variables de entorno.

Para activar un perfil:
```bash
# Desarrollo
java -jar pukio-app-server.jar --spring.profiles.active=dev

# Producción
java -jar pukio-app-server.jar --spring.profiles.active=prod
```

### HikariCP — Pool de Conexiones

Los siguientes valores del pool de conexiones están configurados en los archivos de perfil (NO son secretos, pero son sensibles al entorno):

| Propiedad | Dev | Prod |
|---|---|---|
| **Transaccional**|||
| `spring.datasource.hikari.maximum-pool-size` | 5 | 20 |
| `spring.datasource.hikari.minimum-idle` | 2 | 5 |
| `spring.datasource.hikari.connection-timeout` | 20000 ms | 30000 ms |
| **Analytics**|||
| `analytics.datasource.hikari.maximum-pool-size` | 2 | 5 |
| `analytics.datasource.hikari.minimum-idle` | 1 | 1 |

Estos valores NO son secretos, pero deberían ajustarse según el entorno y carga esperada.

### ETL Asíncrono — Data Warehouse

El servicio `FactSalesEtlService` publica ventas al Data Warehouse de forma **asíncrona** usando `@Async`. Esto significa:

- La transacción transaccional NO espera a que termine el ETL
- Si el DW no está disponible, se loggea el error pero la venta se completa
- El pool async tiene configuración independiente por perfil:
  - **Dev**: 2-4 threads
  - **Prod**: 4-10 threads

---


---

## Superset — Configuración local de conexión a Analytics_Server

Apache Superset requiere credenciales para conectarse al Data Warehouse (Analytics_Server). Estas credenciales NUNCA deben aparecer en archivos commiteados al repositorio.

### Reglas de seguridad para Superset

1. **La SQLAlchemy URI con contraseña se introduce únicamente en la UI de Superset de forma local.**
   - Cada desarrollador configura la conexión localmente en `Settings → Database Connections`
   - La URI contiene credenciales y se almacena en la base de datos interna de Superset (SQLite o PostgreSQL de metadatos)
   - Esta base de datos de Superset NO se sube al repositorio

2. **Nunca exportar conexiones de Superset al repositorio.**
   - Al exportar dashboards (Export → JSON/ZIP), **NO incluir Database Connections**
   - Solo exportar: Dashboard metadata, Charts, Datasets (SQL queries)
   - Los archivos de exportación con conexiones contienen credenciales en texto plano

3. **Compartir configuración entre el equipo sin credenciales.**
   - Guardar exports de dashboards en `docs/superset-exports/`
   - Otros desarrolladores:
     1. Configuran primero la conexión localmente con sus propias credenciales
     2. Luego importan el dashboard desde `docs/superset-exports/`
   - Superset automáticamente asocia los datasets a la conexión local configurada

### Ejemplo de SQLAlchemy URI (desarrollo local)

```
postgresql+psycopg2://pukio_analytics_user:dev_password_local@localhost:5432/pukio_analytics
```

**IMPORTANTE:** Esta URI NUNCA debe aparecer en archivos del repositorio. Solo se ingresa en la UI de Superset.

### Permisos recomendados en PostgreSQL para Superset

El usuario de Superset solo necesita permisos de lectura:

```sql
-- Crear usuario de solo lectura para Superset
CREATE USER pukio_superset_readonly WITH PASSWORD 'strong_password_here';

-- Otorgar permisos de conexión
GRANT CONNECT ON DATABASE pukio_analytics TO pukio_superset_readonly;

-- Otorgar permisos de lectura en todas las tablas del esquema public
GRANT USAGE ON SCHEMA public TO pukio_superset_readonly;
GRANT SELECT ON ALL TABLES IN SCHEMA public TO pukio_superset_readonly;

-- Otorgar permisos de lectura en tablas futuras (opcional)
ALTER DEFAULT PRIVILEGES IN SCHEMA public
    GRANT SELECT ON TABLES TO pukio_superset_readonly;
```

Usar este usuario de solo lectura en la SQLAlchemy URI de Superset minimiza el riesgo en caso de compromiso.

### Archivos a excluir del repositorio (relacionados con Superset)

El `.gitignore` ya excluye estos patrones, pero como referencia:

```
# Superset — base de datos interna de metadatos (si se usa SQLite)
superset.db
superset.db-shm
superset.db-wal

# Superset — archivos de configuración con credenciales
superset_config.py  # si contiene URIs con contraseñas
.superset/

# Exports de Superset CON conexiones de BD
docs/superset-exports/*-with-connections.zip
```

Los exports seguros (sin conexiones) SÍ pueden commitearse en `docs/superset-exports/`.

---

## Auditoría de seguridad — Entregable 2

Se realizó auditoría del historial Git para verificar que ningún secreto fue commiteado:

### Comandos ejecutados

```bash
# Revisar historial de archivos .properties
git log --all -- "**/*.properties"

# Buscar patrones de contraseñas en archivos actuales
grep -r "password\|passwd\|secret" --include="*.properties" --include="*.yml" --include="*.yaml" .
```

### Resultado esperado

Todos los resultados deben mostrar:
- Solo placeholders `${VAR}` en archivos commiteados
- Solo comentarios o nombres de variables (sin valores)
- Archivos template (`.template`) con variables vacías

### Acciones correctivas si se detecta un secreto

Si la auditoría detecta un valor real de contraseña o token en el historial:
1. Seguir el **Protocolo de emergencia** (ver sección anterior de este documento)
2. Documentar el incidente en `docs/security.md`
3. Notificar al equipo de la rotación de credenciales
