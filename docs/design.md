# Documento de Diseño y Arquitectura — PUKIO

> Arquitectura derivada del análisis del código fuente. PUKIO es un sistema POS web con **arquitectura en capas** en el backend (Servlets → Service → DAO → Modelo), un **frontend desacoplado** (JS vanilla sin frameworks ni build tools) y una **base de datos Oracle 21c XE multi-esquema** que separa la capa transaccional (OLTP) de la capa analítica (Data Warehouse / OLAP).

---

## 1. Visión General del Sistema

```mermaid
flowchart TB
    subgraph Cliente["Navegador del Usuario"]
        FE["Frontend Web<br/>HTML + CSS + JS Vanilla<br/>(servido por http.server / serve)"]
    end

    subgraph Servidor["Servidor de Aplicaciones — Apache Tomcat 10.1.x"]
        WAR["backend.war<br/>Jakarta Servlets (Java 21)"]
    end

    subgraph BD["Oracle 21c XE (Docker)"]
        PUKIO_DB["Esquema PUKIO_DB<br/>(Transaccional / OLTP)"]
        PUKIO_DWH["Esquema PUKIO_DWH<br/>(Analítico / OLAP)"]
    end

    FE -- "fetch() JSON sobre HTTP<br/>credentials: include" --> WAR
    WAR -- "JDBC (HikariCP Pool)<br/>ojdbc11" --> PUKIO_DB
    WAR -- "JDBC (HikariCP Pool)" --> PUKIO_DWH
    PUKIO_DWH -. "Lectura cross-schema<br/>VW_VENTAS_DETALLADA" .-> PUKIO_DB

    classDef cliente fill:#e3f2fd,stroke:#1565c0,stroke-width:1px;
    classDef server fill:#fff3e0,stroke:#ef6c00,stroke-width:1px;
    classDef db fill:#e8f5e9,stroke:#2e7d32,stroke-width:1px;
    class FE cliente;
    class WAR server;
    class PUKIO_DB,PUKIO_DWH db;
```

---

## 2. Arquitectura en Capas del Backend

El backend sigue un patrón **N-capas** clásico: `Controller (Servlet)` → `Service` → `DAO` → `Model`, con utilidades transversales (`util`, `config`) y un mecanismo de extensión por plugins (`plugin` + `ServiceLoader`).

```mermaid
flowchart TB
    subgraph L1["Capa de Presentación (controller)"]
        AuthServlet
        ProductoServlet
        ClienteServlet
        ProveedorServlet
        CategoriaServlet
        VentaServlet
        ReporteServlet
        CorsFilter["CorsFilter (Filter)"]
        SecurityFilter["SecurityFilter (Filter)"]
    end

    subgraph L2["Capa de Negocio (service)"]
        AuthService
        ProductoService
        VentaService
        ReporteService
        DataWarehouseService
    end

    subgraph L3["Capa de Acceso a Datos (dao)"]
        UsuarioDAO
        ProductoDAO
        ClienteDAO
        ProveedorDAO
        CategoriaDAO
        VentaDAO
        DataWarehouseDAO
        ConexionDB["ConexionDB<br/>(HikariCP wrapper)"]
    end

    subgraph L4["Capa de Modelo (model)"]
        Usuario
        Producto
        Cliente
        Proveedor
        Categoria
        Venta
        DetalleVenta
    end

    subgraph L5["Transversal"]
        JsonUtil
        SecurityUtil["SecurityUtil (BCrypt)"]
        FormatoUtil
        AppConfig
        DatabaseConfig
        ReportExporter["ReportExporter (SPI)"]
        CsvReportExporter
    end

    CorsFilter --> AuthServlet
    SecurityFilter --> AuthServlet
    AuthServlet --> AuthService --> UsuarioDAO
    ProductoServlet --> ProductoService --> ProductoDAO
    ClienteServlet --> ClienteDAO
    ProveedorServlet --> ProveedorDAO
    CategoriaServlet --> CategoriaDAO
    VentaServlet --> VentaService --> VentaDAO
    VentaService --> ProductoDAO
    ReporteServlet --> ReporteService --> VentaDAO
    ReporteServlet --> DataWarehouseService --> DataWarehouseDAO
    ReporteServlet -. "ServiceLoader.load()" .-> ReportExporter
    CsvReportExporter -. "implements" .-> ReportExporter

    UsuarioDAO --> ConexionDB
    ProductoDAO --> ConexionDB
    ClienteDAO --> ConexionDB
    ProveedorDAO --> ConexionDB
    CategoriaDAO --> ConexionDB
    VentaDAO --> ConexionDB
    DataWarehouseDAO --> ConexionDB

    UsuarioDAO -.-> Usuario
    ProductoDAO -.-> Producto
    ClienteDAO -.-> Cliente
    ProveedorDAO -.-> Proveedor
    CategoriaDAO -.-> Categoria
    VentaDAO -.-> Venta
    Venta --> DetalleVenta

    AuthService --> SecurityUtil
    L1 --> JsonUtil
    ConexionDB --> DatabaseConfig
    Venta --> AppConfig
```

**Decisiones de diseño observadas:**

- **Servlets por recurso**: cada `@WebServlet` mapea uno o varios `urlPatterns` relacionados a un mismo recurso REST (ej. `ProductoServlet` atiende `/api/productos`, `/api/productos/buscar` y `/api/productos/bajo-stock`).
- **Filtros transversales (`Filter`)**: `CorsFilter` (whitelist de origen) y `SecurityFilter` (validación de `HttpSession`) interceptan todo `/api/*` antes de llegar al Servlet.
- **DAO con JDBC puro**: no se usa un ORM; cada DAO abre conexión vía `ConexionDB.getConnection()` (pool HikariCP) con `try-with-resources` y maneja sus propias transacciones (`setAutoCommit(false)` + `commit()`/`rollback()`).
- **Lógica crítica delegada a la base de datos**: el registro de venta usa un **procedimiento almacenado** (`SP_REGISTRAR_VENTA`) para la cabecera, y un **trigger** (`TRG_DETALLE_VENTA_STOCK`) para validar y descontar stock línea por línea — esto garantiza atomicidad incluso ante accesos concurrentes a nivel de motor de base de datos.
- **Extensibilidad por SPI**: `ReportExporter` es una interfaz cargada dinámicamente vía `java.util.ServiceLoader`, registrada en `META-INF/services/com.pukio.plugin.ReportExporter`. Agregar un nuevo formato de exportación (PDF, XLSX) no requiere modificar `ReporteServlet`.
- **Utilitarios de procesos batch (`dwh`)**: `GenerarDataWarehouse`, `CreateCrossTab` y `ViewCrossTab` son clases con `main()` ejecutables de forma independiente (CLI), pensadas para tareas programadas (cron/Task Scheduler) fuera del ciclo de vida web.

---

## 3. Arquitectura del Frontend

```mermaid
flowchart TB
    subgraph Pages["Páginas (HTML)"]
        idx["index.html (Login)"]
        dash["pages/dashboard.html"]
        prod["pages/productos.html"]
        cli["pages/clientes.html"]
        pos["pages/pos.html"]
        rep["pages/reportes.html"]
    end

    subgraph Views["Vistas (js/views) — Controladores de página"]
        loginjs["login.js"]
        dashjs["dashboard.js"]
        prodjs["productos.js"]
        clijs["clientes.js"]
        posjs["pos.js"]
        repjs["reportes.js"]
    end

    subgraph Services["Servicios (js/services) — Cliente de dominio"]
        authsvc["authService.js"]
        prodsvc["productoService.js"]
        ventasvc["ventaService.js"]
    end

    subgraph Core["Núcleo"]
        api["api.js<br/>(HTTP client + Mock Router)"]
        appjs["app.js<br/>(Toast, Loader, Guards UI, Sidebar)"]
        config["config.js<br/>(API_BASE_URL, MOCK_MODE)"]
    end

    idx --> loginjs
    dash --> dashjs
    prod --> prodjs
    cli --> clijs
    pos --> posjs
    rep --> repjs

    loginjs --> authsvc
    dashjs --> ventasvc
    dashjs --> prodsvc
    prodjs --> prodsvc
    clijs --> ventasvc
    posjs --> prodsvc
    posjs --> ventasvc
    posjs --> authsvc
    repjs --> ventasvc

    authsvc --> api
    prodsvc --> api
    ventasvc --> api
    api --> config

    Views --> appjs
```

**Características de diseño del frontend:**

- **Sin frameworks ni bundlers**: módulos ES (`import`/`export`) cargados directamente por el navegador (`<script type="module">`), sin Webpack/Vite.
- **Patrón de capas equivalente al backend**: `views` (controladores de UI) → `services` (lógica de dominio/validación cliente) → `api.js` (transporte HTTP).
- **Doble modo de operación en `api.js`**:
  - **Modo real** (`MOCK_MODE=false`, valor por defecto en `config.example.js`): usa `fetch()` contra `API_BASE_URL` con `credentials: 'include'` para enviar la cookie de sesión.
  - **Modo simulado** (`MOCK_MODE=true`): enrutador interno (`handleMockRequest`) que persiste un dataset de ejemplo en `localStorage`, permitiendo desarrollar/demostrar la UI sin backend Java ni Oracle activos.
- **Guardas de sesión en cliente** (`AuthService.checkGuard` / `checkLoginGuard`): complementan (no sustituyen) al `SecurityFilter` del backend.
- **Control de acceso por rol en la UI**: `app.js` oculta el ítem de menú "Reportes" si el usuario autenticado tiene rol `CAJERO`.

---

## 4. Diagrama de Despliegue

```mermaid
flowchart LR
    subgraph Dev["Equipo de Desarrollo / Servidor"]
        subgraph Docker["Contenedor Docker"]
            OracleXE["gvenzl/oracle-xe:21-slim<br/>Puerto 1521"]
        end

        subgraph TomcatBox["Apache Tomcat 10.1.x"]
            WARApp["backend.war<br/>contexto /backend"]
        end

        subgraph StaticServer["Servidor HTTP estático<br/>(python -m http.server / npx serve)"]
            StaticFiles["/frontend<br/>Puerto 8000"]
        end
    end

    Browser["Navegador<br/>http://localhost:8000/frontend/index.html"]

    Browser -- "HTTP GET (HTML/CSS/JS)" --> StaticFiles
    Browser -- "fetch() /backend/api/*<br/>(CORS, credentials)" --> WARApp
    WARApp -- "JDBC Thin :1521/XEPDB1" --> OracleXE
```

| Componente | Tecnología | Puerto por defecto |
|---|---|---|
| Base de datos | Oracle 21c XE (`gvenzl/oracle-xe:21-slim`, Docker) | 1521 |
| Backend | Java 21 + Jakarta Servlet API 6.0 sobre Apache Tomcat 10.1.x | 8080 |
| Frontend | Archivos estáticos (HTML/CSS/JS) servidos por `http.server` o `serve` | 8000 |
| Cliente DB (admin) | DBeaver Community u otro cliente Oracle | — |

---

## 5. Modelo de Paquetes Java (`com.pukio`)

```mermaid
classDiagram
    class controller {
        <<package>>
        AuthServlet
        ProductoServlet
        ClienteServlet
        ProveedorServlet
        CategoriaServlet
        VentaServlet
        ReporteServlet
        CorsFilter
        SecurityFilter
    }
    class service {
        <<package>>
        AuthService
        ProductoService
        VentaService
        ReporteService
        DataWarehouseService
    }
    class dao {
        <<package>>
        ConexionDB
        UsuarioDAO
        ProductoDAO
        ClienteDAO
        ProveedorDAO
        CategoriaDAO
        VentaDAO
        DataWarehouseDAO
    }
    class model {
        <<package>>
        Usuario
        Producto
        Cliente
        Proveedor
        Categoria
        Venta
        DetalleVenta
    }
    class plugin {
        <<package>>
        ReportExporter
        CsvReportExporter
    }
    class dwh {
        <<package>>
        GenerarDataWarehouse
        CreateCrossTab
        ViewCrossTab
    }
    class util {
        <<package>>
        JsonUtil
        SecurityUtil
        FormatoUtil
    }
    class config {
        <<package>>
        AppConfig
        DatabaseConfig
    }

    controller ..> service : usa
    service ..> dao : usa
    dao ..> model : retorna/mapea
    dao ..> config : ConexionDB lee
    controller ..> util : JsonUtil
    service ..> util : SecurityUtil
    controller ..> plugin : ServiceLoader
    dwh ..> service : DataWarehouseService
```

---

## 6. Flujo Crítico: Registro de una Venta (Secuencia)

Este flujo ilustra cómo colaboran frontend, backend y base de datos (incluido el procedimiento almacenado y el trigger de stock) en la operación más sensible del sistema.

```mermaid
sequenceDiagram
    actor Cajero
    participant POS as pos.js (Frontend)
    participant VS as VentaService.js
    participant API as api.js
    participant Servlet as VentaServlet
    participant Svc as VentaService (Java)
    participant PDAO as ProductoDAO
    participant VDAO as VentaDAO
    participant SP as SP_REGISTRAR_VENTA
    participant TRG as TRG_DETALLE_VENTA_STOCK
    participant DB as Oracle (PUKIO_DB)

    Cajero->>POS: Agrega productos al carrito
    Cajero->>POS: Click "Pagar"
    POS->>VS: registrarVenta(venta)
    VS->>VS: Validar cliente y detalles no vacíos
    VS->>API: POST /api/ventas
    API->>Servlet: HTTP POST (con cookie de sesión)
    Servlet->>Servlet: Verificar HttpSession activa
    Servlet->>Svc: registrar(venta)
    Svc->>PDAO: buscarPorCodigo() por cada item
    PDAO-->>Svc: Producto (con stock actual)
    Svc->>Svc: Validar stock suficiente (capa app)
    Svc->>Svc: calcularTotales() → subtotal, IGV, total
    Svc->>VDAO: insertar(venta)
    VDAO->>DB: BEGIN TRANSACTION (autoCommit=false)
    VDAO->>SP: CALL SP_REGISTRAR_VENTA(...)
    SP->>DB: INSERT INTO VENTAS (...)
    SP-->>VDAO: OUT idVenta, numeroComprobante
    loop Por cada DetalleVenta
        VDAO->>DB: INSERT INTO DETALLE_VENTA (...)
        DB->>TRG: BEFORE INSERT trigger
        TRG->>DB: SELECT STOCK FROM PRODUCTOS
        alt Stock insuficiente
            TRG-->>DB: RAISE_APPLICATION_ERROR(-20001)
            DB-->>VDAO: SQLException (rollback)
            VDAO->>DB: ROLLBACK
            VDAO-->>Svc: SQLException "Error de inventario"
            Svc-->>Servlet: Exception
            Servlet-->>API: HTTP 400 + mensaje
            API-->>POS: showToast(error, 'error')
        else Stock suficiente
            TRG->>DB: UPDATE PRODUCTOS SET STOCK = STOCK - cantidad
        end
    end
    VDAO->>DB: COMMIT
    VDAO-->>Svc: Venta con idVenta y numeroComprobante
    Svc-->>Servlet: Venta registrada
    Servlet-->>API: HTTP 201 Created (JSON Venta)
    API-->>VS: Venta resultante
    VS-->>POS: result
    POS->>POS: Abrir modal de comprobante / limpiar carrito
    POS->>POS: Recargar catálogo (stock actualizado)
```

**Punto de diseño destacado:** existe una **doble validación de stock** — una optimista en `VentaService` (Java, antes del INSERT) y otra autoritativa en `TRG_DETALLE_VENTA_STOCK` (PL/SQL, durante el INSERT). La validación de base de datos es la que realmente garantiza la consistencia bajo concurrencia, ya que la validación de aplicación puede sufrir condiciones de carrera (TOCTOU) entre la lectura del stock y el INSERT real.

---

## 7. Flujo del Proceso ETL / Data Warehouse

```mermaid
flowchart LR
    subgraph OLTP["PUKIO_DB (Transaccional)"]
        VENTAS[("VENTAS")]
        DETALLE[("DETALLE_VENTA")]
        PRODUCTOS[("PRODUCTOS")]
        CLIENTES[("CLIENTES")]
        USUARIOS[("USUARIOS")]
        CATEGORIAS[("CATEGORIAS")]
    end

    subgraph View["Vista de Integración (cross-schema)"]
        VW["VW_VENTAS_DETALLADA<br/>(JOIN de las 6 tablas + cálculo<br/>ANIO/MES/DIA/TRIMESTRE)"]
    end

    subgraph OLAP["PUKIO_DWH (Analítico)"]
        DWHV[("DWH_VENTAS<br/>(tabla de hechos desnormalizada)")]
        CROSSTAB[("CROSSTAB_VENTAS<br/>(cubo OLAP: año/mes/categoría)")]
    end

    VENTAS --> VW
    DETALLE --> VW
    PRODUCTOS --> VW
    CLIENTES --> VW
    USUARIOS --> VW
    CATEGORIAS --> VW

    VW -- "1. DataWarehouseDAO.cargarDWH()<br/>DELETE + INSERT SELECT" --> DWHV
    DWHV -- "2. DataWarehouseDAO.generarCrossTab()<br/>GROUP BY anio, mes, categoria" --> CROSSTAB

    Trigger(["POST /api/dwh/procesar<br/>(manual, vía ReporteServlet)"]) -.-> DWHV
    CLI(["main() CLI:<br/>GenerarDataWarehouse / CreateCrossTab<br/>(candidato a Job nocturno)"]) -.-> DWHV
```

- El proceso es **ELT manual** disparado por `POST /api/dwh/procesar` desde la UI de Reportes, o ejecutable por CLI (`GenerarDataWarehouse`, `CreateCrossTab`) para automatización por **Task Scheduler / cron**.
- `DWH_VENTAS` es una tabla de hechos desnormalizada (incluye nombres en texto en vez de solo IDs), típica de un Data Warehouse orientado a lectura analítica.
- `CROSSTAB_VENTAS` es el resultado de una agregación OLAP simple (`SUM`, `COUNT DISTINCT`) materializada como tabla, simulando un cubo de "ventas por categoría y mes".

---

## 8. Estados de una Venta

```mermaid
stateDiagram-v2
    [*] --> Carrito : Cajero agrega productos
    Carrito --> Carrito : Modificar cantidad / Eliminar item
    Carrito --> Validando : Click "Pagar"
    Validando --> Rechazada : Stock insuficiente (trigger -20001)
    Validando --> Rechazada : Carrito vacío / Usuario no autenticado
    Validando --> Completada : SP_REGISTRAR_VENTA + INSERT detalle OK
    Rechazada --> Carrito : Cajero corrige y reintenta
    Completada --> [*] : Comprobante emitido, carrito limpiado
```

---

## 9. Resumen de Endpoints REST Expuestos

| Recurso | Método | Ruta | Servlet | Autenticación |
|---|---|---|---|---|
| Autenticación | POST | `/api/auth/login` | `AuthServlet` | Pública |
| Autenticación | POST | `/api/auth/logout` | `AuthServlet` | Requiere sesión |
| Autenticación | GET | `/api/auth/session` | `AuthServlet` | Pública (verifica) |
| Productos | GET | `/api/productos` | `ProductoServlet` | Requiere sesión |
| Productos | GET | `/api/productos/buscar` | `ProductoServlet` | Requiere sesión |
| Productos | GET | `/api/productos/bajo-stock` | `ProductoServlet` | Requiere sesión |
| Productos | POST/PUT | `/api/productos` | `ProductoServlet` | Requiere sesión |
| Productos | DELETE | `/api/productos?id=` | `ProductoServlet` | Requiere sesión |
| Categorías | GET | `/api/categorias` | `CategoriaServlet` | Requiere sesión |
| Proveedores | GET | `/api/proveedores` | `ProveedorServlet` | Requiere sesión |
| Clientes | GET | `/api/clientes`, `/api/clientes/buscar` | `ClienteServlet` | Requiere sesión |
| Clientes | POST/PUT/DELETE | `/api/clientes` | `ClienteServlet` | Requiere sesión |
| Ventas | GET | `/api/ventas/resumen-hoy` | `VentaServlet` | Requiere sesión |
| Ventas | POST | `/api/ventas` | `VentaServlet` | Requiere sesión |
| Reportes | GET | `/api/reportes/ventas` | `ReporteServlet` | Requiere sesión |
| Reportes | GET | `/api/reportes/productos-top` | `ReporteServlet` | Requiere sesión |
| Reportes | GET | `/api/reportes/exportar?formato=csv` | `ReporteServlet` | Requiere sesión |
| DWH/OLAP | GET | `/api/dwh/crosstab` | `ReporteServlet` | Requiere sesión |
| DWH/OLAP | POST | `/api/dwh/procesar` | `ReporteServlet` | Requiere sesión |

---

## 10. Stack Tecnológico

| Capa | Tecnología |
|---|---|
| Lenguaje backend | Java 21 |
| Framework web backend | Jakarta Servlet API 6.0 (sin Spring) |
| Servidor de aplicaciones | Apache Tomcat 10.1.x |
| Build tool backend | Apache Maven (empaquetado `.war`) |
| Driver de base de datos | Oracle JDBC Thin (`ojdbc11` 23.3.0.23.09) |
| Pool de conexiones | HikariCP 5.1.0 |
| Serialización JSON | Google Gson 2.10.1 |
| Hashing de contraseñas | jBCrypt 0.4 (factor de costo 12) |
| Base de datos | Oracle 21c XE (contenedor `gvenzl/oracle-xe:21-slim`) |
| Frontend | HTML5 + CSS3 + JavaScript ES Modules (sin frameworks) |
| Servidor de archivos estáticos (dev) | `python -m http.server` / `npx serve` |