# Apache Superset 6.1.0 — Configuración para Pukio Analytics

Este documento describe el procedimiento completo de instalación y configuración de Apache Superset para visualización de datos del Data Warehouse de Pukio.

---

## 1. Instalación de Apache Superset 6.1.0

### Requisitos previos
- Python 3.9 o superior
- PostgreSQL 18.1 con base de datos `pukio_analytics` ya poblada
- pip actualizado: `pip install --upgrade pip`

### Instalación paso a paso

```bash
# Instalar Apache Superset 6.1.0
pip install apache-superset==6.1.0

# Inicializar la base de datos de Superset (metadatos)
superset db upgrade

# Crear usuario administrador
superset fab create-admin
# Se solicitará: username, firstname, lastname, email, password

# Inicializar Superset (roles y permisos)
superset init

# Arrancar el servidor de desarrollo
superset run -p 8088 --with-threads --reload --debugger
```

**Acceso:** Abrir navegador en `http://localhost:8088` e ingresar con las credenciales del admin creado.

---

## 2. Conexión a Analytics_Server (Data Warehouse)

### Configuración de la conexión a PostgreSQL

1. En Superset UI, ir a **Settings → Database Connections → + Database**
2. Seleccionar **Database type: PostgreSQL**
3. En el campo **SQLAlchemy URI**, ingresar:

```
postgresql+psycopg2://<ANALYTICS_DB_USERNAME>:<ANALYTICS_DB_PASSWORD>@<ANALYTICS_DB_HOST>:5432/pukio_analytics
```

**IMPORTANTE — SEGURIDAD:**
- Esta URI con contraseña se configura **únicamente de forma local** en la interfaz de Superset.
- **NUNCA se guarda en el repositorio Git.**
- Cada desarrollador debe configurar la conexión localmente con sus credenciales.
- NO exportar conexiones de Superset (Export → JSON) al repositorio porque contienen credenciales en texto plano.

### Ejemplo de URI para desarrollo local:
```
postgresql+psycopg2://pukio_analytics_user:dev_password_local@localhost:5432/pukio_analytics
```

4. Click en **Test Connection** para verificar conectividad.
5. Si la prueba es exitosa, click en **Connect**.

---

## 3. Creación de Datasets

Los datasets son vistas o consultas SQL que Superset usará como fuentes de datos para charts y dashboards.

### 3.1 Datasets sobre tablas físicas

En Superset → **Datasets → + Dataset**, crear los siguientes datasets basados en tablas físicas:

| Dataset Name | Database | Schema | Table |
|---|---|---|---|
| `fact_sales` | pukio_analytics | public | fact_sales |
| `dim_time` | pukio_analytics | public | dim_time |
| `dim_product` | pukio_analytics | public | dim_product |
| `dim_store` | pukio_analytics | public | dim_store |
| `dim_payment` | pukio_analytics | public | dim_payment |

**Configuración post-creación:**
- Ir a cada dataset → **Edit Dataset**
- En la pestaña **Columns**, revisar que los tipos de datos sean correctos
- En la pestaña **Calculated Columns**, se pueden agregar métricas derivadas si es necesario
- Click en **Save**

### 3.2 Datasets SQL personalizados

Crear los siguientes datasets basados en consultas SQL personalizadas:

#### Dataset: `ventas_por_producto`
```sql
SELECT 
    dp.sku, 
    dp.name, 
    dp.category,
    SUM(f.quantity) AS total_qty,
    SUM(f.total_amount) AS total_revenue
FROM fact_sales f
JOIN dim_product dp ON f.product_key = dp.product_key
GROUP BY dp.sku, dp.name, dp.category
ORDER BY total_revenue DESC
```

#### Dataset: `ventas_por_tiempo`
```sql
SELECT 
    dt.full_date, 
    dt.year, 
    dt.month,
    dt.day,
    dt.week,
    SUM(f.total_amount) AS daily_revenue,
    COUNT(DISTINCT f.sale_id) AS num_sales,
    SUM(f.quantity) AS total_units
FROM fact_sales f
JOIN dim_time dt ON f.time_key = dt.time_key
GROUP BY dt.full_date, dt.year, dt.month, dt.day, dt.week
ORDER BY dt.full_date
```

#### Dataset: `ventas_por_tienda`
```sql
SELECT 
    ds.store_key,
    ds.store_id,
    ds.store_name,
    ds.city,
    ds.region,
    SUM(f.total_amount) AS total_revenue,
    COUNT(DISTINCT f.sale_id) AS num_sales,
    SUM(f.quantity) AS total_units
FROM fact_sales f
JOIN dim_store ds ON f.store_key = ds.store_key
GROUP BY ds.store_key, ds.store_id, ds.store_name, ds.city, ds.region
ORDER BY total_revenue DESC
```

#### Dataset: `distribucion_pagos`
```sql
SELECT 
    dpay.payment_key,
    dpay.method, 
    dpay.description,
    SUM(f.total_amount) AS total_amount,
    COUNT(*) AS num_transactions,
    ROUND(AVG(f.total_amount), 2) AS avg_transaction_amount
FROM fact_sales f
JOIN dim_payment dpay ON f.payment_key = dpay.payment_key
GROUP BY dpay.payment_key, dpay.method, dpay.description
ORDER BY total_amount DESC
```

**Procedimiento para crear dataset SQL personalizado:**
1. Ir a **SQL Lab → SQL Editor**
2. Seleccionar la database `pukio_analytics`
3. Pegar la consulta SQL
4. Click en **Run** para verificar que funciona
5. Si la consulta es correcta, click en **Save → Save as dataset**
6. Asignar nombre al dataset
7. Click en **Save & Explore**

---

## 4. Dashboards

A continuación se describen los 4 dashboards principales a crear manualmente en Superset UI.

### Dashboard 1 — "Tendencia de Ventas"

**Objetivo:** Visualizar la evolución de las ventas en el tiempo.

**Chart 1.1 — Línea de Ventas Diarias:**
- Chart Type: **Line Chart**
- Dataset: `ventas_por_tiempo`
- X Axis: `full_date`
- Metrics: `daily_revenue`
- Filtros: **Time Range Filter** (últimos 90 días por defecto)
- Configuración adicional:
  - Show legend: Yes
  - Show markers: Yes
  - Line interpolation: linear

**Chart 1.2 — Número de Transacciones Diarias:**
- Chart Type: **Line Chart**
- Dataset: `ventas_por_tiempo`
- X Axis: `full_date`
- Metrics: `num_sales`
- Mismo filtro de fecha que el chart anterior

**Layout del Dashboard:**
- Ambos charts uno debajo del otro
- Añadir un filtro global de fecha (Time Range Filter) que afecte ambos charts

---

### Dashboard 2 — "Top Productos"

**Objetivo:** Identificar los productos más vendidos por revenue y cantidad.

**Chart 2.1 — Top 20 Productos por Revenue:**
- Chart Type: **Bar Chart**
- Dataset: `ventas_por_producto`
- Dimensions: `name`
- Metrics: `total_revenue`
- Row limit: 20
- Sort by: `total_revenue` DESC
- Configuración adicional:
  - Show bar values: Yes
  - Color scheme: Sequential

**Chart 2.2 — Top 20 Productos por Cantidad:**
- Chart Type: **Bar Chart**
- Dataset: `ventas_por_producto`
- Dimensions: `name`
- Metrics: `total_qty`
- Row limit: 20
- Sort by: `total_qty` DESC

**Chart 2.3 — Ventas por Categoría:**
- Chart Type: **Pie Chart**
- Dataset: `ventas_por_producto`
- Dimensions: `category`
- Metrics: `SUM(total_revenue)`
- Show labels: Yes
- Show legend: Yes

**Layout del Dashboard:**
- Chart 2.1 arriba (mitad superior)
- Chart 2.2 y 2.3 lado a lado en la mitad inferior

---

### Dashboard 3 — "Distribución por Método de Pago"

**Objetivo:** Analizar la preferencia de métodos de pago de los clientes.

**Chart 3.1 — Pie Chart de Métodos de Pago:**
- Chart Type: **Pie Chart**
- Dataset: `distribucion_pagos`
- Dimensions: `method`
- Metrics: `total_amount`
- Show labels: Yes
- Show percentage: Yes
- Show legend: Yes

**Chart 3.2 — Bar Chart de Transacciones por Método:**
- Chart Type: **Bar Chart**
- Dataset: `distribucion_pagos`
- Dimensions: `method`
- Metrics: `num_transactions`
- Sort by: `num_transactions` DESC

**Chart 3.3 — Table de Resumen por Método:**
- Chart Type: **Table**
- Dataset: `distribucion_pagos`
- Columns: `method`, `num_transactions`, `total_amount`, `avg_transaction_amount`
- Sort by: `total_amount` DESC

**Layout del Dashboard:**
- Chart 3.1 a la izquierda (40% ancho)
- Chart 3.2 a la derecha (60% ancho)
- Chart 3.3 abajo ocupando todo el ancho

**Filtro global:** Añadir filtro de rango de fechas que afecte todos los charts

---

### Dashboard 4 — "Ventas por Tienda"

**Objetivo:** Comparar el desempeño de ventas entre las diferentes tiendas.

**Chart 4.1 — Bar Chart de Revenue por Tienda:**
- Chart Type: **Bar Chart**
- Dataset: `ventas_por_tienda`
- Dimensions: `store_name`
- Metrics: `total_revenue`
- Sort by: `total_revenue` DESC
- Show bar values: Yes

**Chart 4.2 — Bar Chart de Unidades Vendidas por Tienda:**
- Chart Type: **Bar Chart**
- Dataset: `ventas_por_tienda`
- Dimensions: `store_name`
- Metrics: `total_units`
- Sort by: `total_units` DESC

**Chart 4.3 — Map Chart (si hay coordenadas geográficas):**
- Chart Type: **Country Map** o **Deck.gl Scatterplot**
- Dataset: `ventas_por_tienda`
- Location: `city` o coordenadas lat/lon si están disponibles
- Metric: `total_revenue`

**Chart 4.4 — Table de Detalle por Tienda:**
- Chart Type: **Table**
- Dataset: `ventas_por_tienda`
- Columns: `store_name`, `city`, `region`, `num_sales`, `total_revenue`, `total_units`
- Sort by: `total_revenue` DESC

**Layout del Dashboard:**
- Chart 4.1 y 4.2 arriba lado a lado
- Chart 4.3 (si aplica) en el medio
- Chart 4.4 abajo ocupando todo el ancho

**Filtro global:** Añadir filtros por región y ciudad

---

## 5. Análisis Multidimensional — Cross-Tab (Pivot Table)

Las tablas cruzadas permiten análisis multidimensional complejo con drill-down.

### Chart: "Ventas Mensuales por Tienda y Categoría"

**Procedimiento:**

1. Ir a **SQL Lab → SQL Editor**
2. Ejecutar la siguiente consulta:

```sql
SELECT 
    dt.year, 
    dt.month, 
    ds.store_name,
    dp.category,
    SUM(f.total_amount) AS revenue,
    SUM(f.quantity) AS units,
    COUNT(DISTINCT f.sale_id) AS num_sales
FROM fact_sales f
JOIN dim_time dt ON f.time_key = dt.time_key
JOIN dim_store ds ON f.store_key = ds.store_key
JOIN dim_product dp ON f.product_key = dp.product_key
GROUP BY dt.year, dt.month, ds.store_name, dp.category
ORDER BY dt.year, dt.month, ds.store_name, dp.category
```

3. Click en **Save → Save as dataset** → nombre: `ventas_multidimensional`
4. Click en **Save & Explore**
5. Seleccionar **Chart Type: Pivot Table**
6. Configuración:
   - **Rows:** `year`, `month`
   - **Columns:** `store_name`
   - **Metrics:** `SUM(revenue)`
   - **Aggregation function:** Sum
   - **Show totals:** Yes (tanto row totals como column totals)
   - **Transpose pivot:** No
7. Click en **Run** para visualizar la tabla cruzada
8. Click en **Save** → añadir a un dashboard nuevo o existente

**Drill-Down:**
- Superset permite hacer drill-down haciendo click en las celdas de la tabla
- Se pueden agregar más dimensiones (como `category`) para análisis más detallado

### Chart adicional: "Categoría × Método de Pago"

```sql
SELECT 
    dp.category,
    dpay.method,
    SUM(f.total_amount) AS revenue,
    COUNT(*) AS num_transactions
FROM fact_sales f
JOIN dim_product dp ON f.product_key = dp.product_key
JOIN dim_payment dpay ON f.payment_key = dpay.payment_key
GROUP BY dp.category, dpay.method
ORDER BY dp.category, dpay.method
```

- Chart Type: **Pivot Table**
- Rows: `category`
- Columns: `method`
- Metrics: `SUM(revenue)`

---

## 6. Filtros Globales y Drill-Down

### Configuración de filtros globales en dashboards

1. Editar el dashboard → Click en **⋮ → Edit dashboard**
2. Arrastrar componente **Filter Box** desde el panel izquierdo
3. Configurar filtros:
   - **Date Range Filter:** Seleccionar columna `full_date` del dataset
   - **Dropdown Filter (Store):** Seleccionar `store_name`
   - **Dropdown Filter (Category):** Seleccionar `category`
4. En cada chart del dashboard, ir a **Chart Configuration → Filters** y marcar el checkbox **Apply filter from dashboard**
5. Click en **Save** en el dashboard

### Drill-Down en charts

Superset permite drill-down automático haciendo click en elementos de charts:
- En un **Bar Chart**, hacer click en una barra permite filtrar otros charts del dashboard
- En un **Pie Chart**, hacer click en un segmento filtra por esa dimensión
- En una **Pivot Table**, hacer click en una celda permite ver el detalle de las transacciones

**Habilitar drill-down:**
1. Editar el chart
2. En **Chart Configuration → Filters**, habilitar **Emit dashboard cross-filters**
3. Save

---

## 7. Exportación de Dashboards (sin credenciales)

Para compartir la configuración de dashboards entre el equipo **sin exponer credenciales:**

### Procedimiento de exportación segura

1. En Superset UI, ir a **Dashboards**
2. Seleccionar el dashboard a exportar
3. Click en **⋮ → Export**
4. **IMPORTANTE:** Antes de exportar, verificar que la opción **Export related** NO incluya **Database Connections**
5. Exportar solo:
   - ✅ Dashboard metadata
   - ✅ Charts
   - ✅ Datasets (SQL queries sin conexión)
   - ❌ Database connections (NUNCA exportar)
6. Guardar el archivo `.zip` exportado en `docs/superset-exports/`

### Importación por otro desarrollador

1. Configurar primero la conexión a `pukio_analytics` localmente (paso 2 de este documento)
2. En Superset UI, ir a **Dashboards → Import dashboards**
3. Subir el archivo `.zip` desde `docs/superset-exports/`
4. Superset automáticamente asociará los datasets a la conexión configurada localmente
5. Verificar que los charts se visualicen correctamente

**REGLA DE ORO:** Nunca commitear archivos de exportación que contengan conexiones de base de datos.

---

## 8. Mantenimiento y Mejores Prácticas

### Actualización periódica de datasets

Los datasets SQL personalizados NO se actualizan automáticamente. Para refrescar los datos:
1. Ir a **Datasets → [nombre del dataset] → Edit**
2. Click en **Refresh metadata**
3. O simplemente forzar refresco en los charts con **Force refresh**

### Caching

Superset cachea resultados de queries para mejorar performance:
- Cache por defecto: 1 hora
- Para invalidar cache manualmente: en el chart, click en **Force refresh**
- Configuración global de cache en `superset_config.py` (fuera del alcance de esta guía)

### Performance

Si las queries son lentas:
1. Verificar que existan índices en las columnas usadas en JOINs (`time_key`, `product_key`, `store_key`, `payment_key`)
2. Considerar crear vistas materializadas en PostgreSQL para consultas complejas
3. Ajustar el `row_limit` en los charts para reducir volumen de datos

### Seguridad

- Nunca compartir credenciales de base de datos por canales públicos
- Rotar contraseñas periódicamente
- Usar roles de solo lectura en PostgreSQL para la conexión de Superset
- Configurar autenticación OAuth si Superset se expone externamente (fuera del alcance del Entregable 2)

---

## 9. Troubleshooting

### Error: "Could not connect to database"
- Verificar que PostgreSQL esté corriendo: `systemctl status postgresql` o `pg_ctl status`
- Verificar credenciales en la SQLAlchemy URI
- Verificar que el usuario tenga permisos: `GRANT SELECT ON ALL TABLES IN SCHEMA public TO pukio_analytics_user;`

### Error: "No data available"
- Verificar que las tablas del DW tengan datos: `SELECT COUNT(*) FROM fact_sales;`
- Ejecutar el ETL para poblar el DW (ver `pukio-app-server` → `FactSalesEtlService`)

### Charts no se actualizan
- Click en **Force refresh** en el chart
- Invalidar cache del dataset
- Verificar que la query SQL del dataset sea correcta en SQL Lab

### Superset no arranca
- Verificar version de Python: `python --version` (debe ser >= 3.9)
- Verificar instalación: `pip show apache-superset`
- Ver logs: `superset run -p 8088 --with-threads --reload --debugger` muestra logs en consola

---

*Documento generado para el proyecto Pukio — Sistema POS Minorista*
*Apache Superset 6.1.0 · PostgreSQL 18.1 · Data Warehouse con esquema estrella*
