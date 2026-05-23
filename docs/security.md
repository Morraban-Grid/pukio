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
