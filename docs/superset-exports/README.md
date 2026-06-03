# Superset Dashboard Exports

Este directorio contiene exports de dashboards de Apache Superset que pueden ser compartidos entre el equipo de desarrollo.

## ⚠️ Regla crítica de seguridad

**SOLO se deben subir exports SIN conexiones de base de datos.**

Cuando exportes un dashboard desde Superset:
1. Ir a **Dashboards → [nombre del dashboard] → ⋮ → Export**
2. **Asegurarse de NO incluir "Database Connections"** en las opciones de exportación
3. Solo exportar:
   - ✅ Dashboard metadata
   - ✅ Charts
   - ✅ Datasets (SQL queries sin conexión)
4. Guardar el archivo `.zip` en este directorio

## Cómo importar un dashboard

1. **Primero**, configurar la conexión a `pukio_analytics` localmente en tu instancia de Superset:
   - Settings → Database Connections → + Database
   - Usar tu propia SQLAlchemy URI con tus credenciales locales
   - Ver `docs/superset-setup.md` para instrucciones completas

2. **Luego**, importar el dashboard:
   - Dashboards → Import dashboards
   - Subir el archivo `.zip` de este directorio
   - Superset automáticamente asociará los datasets a tu conexión local

## Dashboards disponibles

*(Añadir aquí la lista de dashboards cuando se creen)*

- `dashboard-tendencia-ventas.zip` — Evolución de ventas en el tiempo
- `dashboard-top-productos.zip` — Productos más vendidos y categorías
- `dashboard-metodos-pago.zip` — Distribución de métodos de pago
- `dashboard-ventas-tiendas.zip` — Comparación entre tiendas

## Nomenclatura de archivos

Usar el formato: `dashboard-[nombre-descriptivo].zip`

Ejemplo: `dashboard-tendencia-ventas.zip`

---

**Nota:** Si un archivo de export contiene credenciales (porque se exportó con "Database Connections" por error), **NO subirlo al repositorio**. En su lugar, eliminarlo localmente y volver a exportar correctamente.
