# Pukio â€” Sistema POS para Tiendas Minoristas

Sistema de Punto de Venta con evoluciÃ³n arquitectÃ³nica progresiva en 4 entregables.
Desarrollado con Java 21, Spring Boot 3.3.5 y Maven multi-mÃ³dulo.

---

## MÃ³dulos â€” Entregable 1

| MÃ³dulo | DescripciÃ³n |
|---|---|
| pukio-common | Modelos compartidos y API de archivos indexados |
| pukio-maintenance | GestiÃ³n de productos e inventario |
| pukio-pos-client | Terminal de ventas local |
| pukio-send-service | EnvÃ­o de datos al servidor central vÃ­a TCP |
| pukio-update-service | RecepciÃ³n y sincronizaciÃ³n con base de datos central |

---

## Requisitos previos

- Java 21
- Maven 3.9+
- PostgreSQL 18.1

---

## ConfiguraciÃ³n local â€” Primeros pasos

1. Clonar el repositorio.

2. En cada mÃ³dulo, copiar el archivo template al archivo real de secretos:

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

## âš ï¸ Seguridad â€” Archivos que NUNCA deben subirse a GitHub

| Archivo | Motivo |
|---|---|
| `application-secrets.properties` | Contiene contraseÃ±as y credenciales reales |
| `.env` | Contiene credenciales para Docker |
| `.env.*` (excepto `.env.template`) | Variantes de entorno con valores reales |
| `*.jks` | Java KeyStore con claves privadas |
| `*.p12` | Certificados con clave privada |
| `*.pem` | Claves privadas SSL |
| `*.key` | Claves privadas |
| `k8s/**/secret.yaml` | Secretos de Kubernetes con valores reales |
| `ssl/` | Directorio de certificados SSL |
| `secrets/` | Directorio de secretos locales |

Estos archivos estÃ¡n excluidos en el `.gitignore` del proyecto.

**Si accidentalmente subes uno, consulta `docs/security.md` para el protocolo de emergencia.**

---
