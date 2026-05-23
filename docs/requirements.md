# Requirements Document — Sistema POS Pukio

## Introducción

Pukio es un sistema de Punto de Venta (POS) para tiendas minoristas que evoluciona a través de 4 entregables arquitectónicos progresivos: desde una arquitectura unitaria con servidor de datos, pasando por cliente/servidor y N-capas, hasta una solución cloud-native completamente escalable. El sistema gestiona productos, inventario, ventas, promociones, arqueo de caja y reportes analíticos, utilizando exclusivamente tecnologías gratuitas y open-source.

**Stack Tecnológico Base:**

| Tecnología    | Versión   |
|---------------|-----------|
| Oracle JDK    | 21        |
| Spring Boot   | 3.3.5     |
| PostgreSQL    | 18.1      |
| Maven         | 3.9.9     |

---

## Glosario

| Término               | Definición |
|-----------------------|------------|
| **POS_Client**        | Terminal de punto de venta en tienda que procesa transacciones |
| **Maintenance_System**| Módulo de gestión CRUD de datos maestros (productos, inventario) |
| **Send_Service**      | Programa independiente que transfiere archivos indexados al servidor central |
| **Update_Service**    | Servicio que recibe archivos de clientes y actualiza la base de datos centralizada |
| **Application_Server**| Servidor que procesa lógica de negocio y coordina operaciones |
| **Data_Server**       | Servidor PostgreSQL 18.1 que almacena datos transaccionales |
| **Analytics_Server**  | Servidor Data Warehouse optimizado para reportes y análisis |
| **FTP_Server**        | Servidor de transferencia de archivos multimedia y auditoría |
| **Mirror_Server**     | Réplica exacta de la base de datos principal para alta disponibilidad |
| **Indexed_File**      | Archivo binario con organización indexada (B-tree, hash o secuencial) |
| **SKU**               | Stock Keeping Unit, identificador único de producto |
| **Arqueo**            | Proceso de cierre y conciliación de caja |
| **Cross_Tab**         | Tabla cruzada para análisis multidimensional |
| **Container**         | Contenedor Docker 29.2.0 que encapsula componentes del sistema |
| **Object_Storage**    | Almacenamiento MinIO compatible con S3 para archivos binarios |
| **HPA**               | Horizontal Pod Autoscaler de Kubernetes para escalado automático |
| **Streaming_Replication** | Replicación nativa de PostgreSQL en tiempo real |

---

## Entregable 1: Arquitectura Unitaria con Servidor de Datos

### Requirement 1.1: Gestión de Productos con Archivos Indexados

**User Story:** Como administrador de tienda, quiero gestionar productos localmente usando archivos indexados, para poder operar sin depender de bases de datos locales.

**Acceptance Criteria:**

- THE Maintenance_System SHALL store product data in Indexed_File format using binary serialization.
- THE Maintenance_System SHALL implement B-tree, hash, or sequential indexing for product SKU lookups.
- WHEN a product is created, THE Maintenance_System SHALL write the record to the Indexed_File and update the index structure.
- WHEN a product is modified, THE Maintenance_System SHALL locate the record using the index and update it in place.
- WHEN a product is deleted, THE Maintenance_System SHALL mark the record as deleted in the Indexed_File.
- WHEN a product is queried by SKU, THE Maintenance_System SHALL use the index to retrieve the record in O(log n) or O(1) time.
- THE Maintenance_System SHALL NOT use SQLite, PostgreSQL, or any embedded database engine for local storage.

---

### Requirement 1.2: Gestión de Inventario con Archivos Indexados

**User Story:** Como administrador de tienda, quiero controlar el inventario localmente con archivos indexados, para mantener registros de stock sin bases de datos locales.

**Acceptance Criteria:**

- THE Maintenance_System SHALL store inventory data in Indexed_File format with SKU as primary key.
- WHEN stock quantity changes, THE Maintenance_System SHALL update the corresponding record in the Indexed_File.
- WHEN stock reaches zero, THE Maintenance_System SHALL flag the product as out-of-stock in the index.
- THE Maintenance_System SHALL support concurrent read operations on the Indexed_File.
- THE Maintenance_System SHALL implement file locking to prevent concurrent write conflicts.

---

### Requirement 1.3: Transferencia de Archivos al Servidor Central

**User Story:** Como sistema local, quiero transferir archivos indexados al servidor central, para consolidar información de todas las tiendas.

**Acceptance Criteria:**

- THE Send_Service SHALL be an independent executable program separate from Maintenance_System.
- WHEN triggered, THE Send_Service SHALL read the local Indexed_File completely.
- THE Send_Service SHALL establish TCP/IP socket connection to Data_Server on configured port.
- THE Send_Service SHALL transmit the Indexed_File content using Java socket streams.
- WHEN transmission completes, THE Send_Service SHALL receive acknowledgment from Update_Service.
- IF connection fails, THEN THE Send_Service SHALL retry transmission up to 3 times with exponential backoff.
- THE Send_Service SHALL log all transmission attempts with timestamp and status.

---

### Requirement 1.4: Recepción y Actualización Centralizada

**User Story:** Como servidor central, quiero recibir archivos de tiendas y actualizar la base de datos automáticamente, para consolidar información en tiempo real.

**Acceptance Criteria:**

- THE Update_Service SHALL be an independent service running on Data_Server.
- THE Update_Service SHALL listen on configured TCP port for incoming connections from Send_Service.
- WHEN a file is received, THE Update_Service SHALL parse the Indexed_File structure.
- THE Update_Service SHALL extract product and inventory records from the received file.
- FOR EACH record, THE Update_Service SHALL insert or update corresponding rows in PostgreSQL 18.1 database.
- THE Update_Service SHALL execute all database operations within a single transaction.
- IF any database operation fails, THEN THE Update_Service SHALL rollback the entire transaction.
- WHEN processing completes, THE Update_Service SHALL send acknowledgment to Send_Service.
- THE Update_Service SHALL log all received files with source identifier, timestamp, and record count.

---

### Requirement 1.5: Base de Datos Centralizada PostgreSQL

**User Story:** Como sistema central, quiero almacenar datos consolidados en PostgreSQL, para tener un repositorio confiable y gratuito.

**Acceptance Criteria:**

- THE Data_Server SHALL use PostgreSQL 18.1 as the database engine.
- THE Data_Server SHALL store product master data with columns: SKU, name, price, category, description.
- THE Data_Server SHALL store inventory data with columns: SKU, store_id, quantity, last_updated.
- THE Data_Server SHALL enforce referential integrity between products and inventory tables.
- THE Data_Server SHALL create indexes on SKU and store_id columns for query performance.
- THE Data_Server SHALL accept connections from Update_Service using JDBC protocol.

---

### Requirement 1.6: Procesamiento de Ventas Local

**User Story:** Como cajero, quiero procesar ventas en el terminal local, para registrar transacciones incluso sin conexión al servidor.

**Acceptance Criteria:**

- WHEN a sale is initiated, THE POS_Client SHALL capture product SKU and quantity.
- THE POS_Client SHALL read product price from local Indexed_File using SKU index.
- THE POS_Client SHALL calculate total amount including all line items.
- WHEN payment is received, THE POS_Client SHALL record payment method (cash, card, transfer).
- THE POS_Client SHALL write sale transaction to local Indexed_File with timestamp.
- THE POS_Client SHALL decrement inventory quantity in local Indexed_File.
- THE POS_Client SHALL generate receipt with transaction details.

---

### Requirement 1.7: Documento de Arquitectura Completo

**User Story:** Como arquitecto de software, quiero documentar la arquitectura del sistema, para guiar la implementación y mantenimiento.

**Acceptance Criteria:**

- THE Architecture_Document SHALL include system overview and business context.
- THE Architecture_Document SHALL describe component architecture with diagrams.
- THE Architecture_Document SHALL specify technology stack (Oracle JDK 21, Spring Boot 3.3.5, PostgreSQL 18.1, Maven 3.9.9).
- THE Architecture_Document SHALL document network protocols and data formats.
- THE Architecture_Document SHALL define deployment topology for clients and server.
- THE Architecture_Document SHALL specify security considerations for file transfer.
- THE Architecture_Document SHALL include data flow diagrams for CRUD and synchronization.
- THE Architecture_Document SHALL document file structure and indexing algorithms.
- THE Architecture_Document SHALL specify error handling and recovery procedures.
- THE Architecture_Document SHALL define performance requirements and constraints.
- THE Architecture_Document SHALL list all open-source and free tools used.

---

## Entregable 2: Arquitectura Cliente/Servidor

### Requirement 2.1: Cliente Ligero sin Lógica de Negocio

**User Story:** Como cajero, quiero usar un terminal ligero que solo capture datos, para que toda la lógica esté centralizada en el servidor.

**Acceptance Criteria:**

- THE POS_Client SHALL implement only graphical user interface components.
- THE POS_Client SHALL capture product SKU through barcode scanner or manual input.
- THE POS_Client SHALL capture payment method selection (cash, card, transfer).
- WHEN data is captured, THE POS_Client SHALL package it into a request message.
- THE POS_Client SHALL send request to Application_Server via TCP socket or REST API.
- THE POS_Client SHALL display response received from Application_Server.
- THE POS_Client SHALL NOT perform calculations, validations, or business rules locally.
- THE POS_Client SHALL NOT maintain local Indexed_File or any data structures.

---

### Requirement 2.2: Servidor de Aplicaciones con Lógica de Negocio

**User Story:** Como sistema centralizado, quiero procesar toda la lógica de negocio en el servidor, para mantener consistencia y control.

**Acceptance Criteria:**

- THE Application_Server SHALL use Spring Boot 3.3.5 framework.
- WHEN a sale request is received, THE Application_Server SHALL validate product SKU exists.
- THE Application_Server SHALL query Data_Server for current product price and stock.
- IF stock is insufficient, THEN THE Application_Server SHALL return error to POS_Client.
- THE Application_Server SHALL apply promotion rules and calculate discounts.
- THE Application_Server SHALL calculate final sale amount including taxes.
- THE Application_Server SHALL persist sale transaction to Data_Server via JDBC.
- THE Application_Server SHALL update inventory quantity in Data_Server.
- THE Application_Server SHALL return sale confirmation with transaction ID to POS_Client.

---

### Requirement 2.3: Verificación de Stock en Tiempo Real

**User Story:** Como sistema de ventas, quiero verificar stock antes de completar una venta, para evitar sobreventa de productos.

**Acceptance Criteria:**

- WHEN a sale is processed, THE Application_Server SHALL query current stock from Data_Server.
- THE Application_Server SHALL compare requested quantity with available stock.
- IF requested quantity exceeds available stock, THEN THE Application_Server SHALL reject the sale.
- THE Application_Server SHALL lock inventory record during sale transaction.
- THE Application_Server SHALL commit inventory update and sale record atomically.

---

### Requirement 2.4: Aplicación de Promociones Automáticas

**User Story:** Como gerente de tienda, quiero que el sistema aplique promociones automáticamente, para incentivar ventas sin intervención manual.

**Acceptance Criteria:**

- THE Application_Server SHALL store promotion rules in Data_Server (discount percentage, conditions, validity dates).
- WHEN a sale is processed, THE Application_Server SHALL evaluate all active promotions.
- THE Application_Server SHALL apply promotion if product SKU matches promotion criteria.
- THE Application_Server SHALL apply promotion if sale amount exceeds minimum threshold.
- THE Application_Server SHALL calculate discounted price and update sale total.
- THE Application_Server SHALL record applied promotion in sale transaction.
- WHERE multiple promotions apply, THE Application_Server SHALL apply the most beneficial discount.

---

### Requirement 2.5: Arqueo de Caja Automatizado

**User Story:** Como cajero, quiero realizar arqueo de caja al final del turno, para conciliar efectivo y transacciones.

**Acceptance Criteria:**

- WHEN arqueo is requested, THE Application_Server SHALL query all sales for the current shift from Data_Server.
- THE Application_Server SHALL calculate expected cash amount from cash transactions.
- THE Application_Server SHALL calculate expected card amount from card transactions.
- THE Application_Server SHALL generate arqueo report with breakdown by payment method.
- THE Application_Server SHALL compare expected amounts with declared amounts from POS_Client.
- THE Application_Server SHALL calculate variance (difference between expected and declared).
- THE Application_Server SHALL persist arqueo record to Data_Server with timestamp and cashier ID.
- IF variance exceeds configured threshold, THEN THE Application_Server SHALL flag arqueo for review.

---

### Requirement 2.6: Servidor de Datos Centralizado

**User Story:** Como sistema, quiero mantener todos los datos en un servidor PostgreSQL centralizado, para eliminar archivos locales y sincronización.

**Acceptance Criteria:**

- THE Data_Server SHALL use PostgreSQL 18.1 as centralized database.
- THE Data_Server SHALL store products, inventory, sales, promotions, and arqueo tables.
- THE Application_Server SHALL connect to Data_Server using JDBC connection pool.
- THE Data_Server SHALL process SQL transactions in real-time.
- THE Data_Server SHALL enforce ACID properties for all transactions.
- THE Data_Server SHALL create indexes on frequently queried columns (SKU, transaction_date, store_id).

---

### Requirement 2.7: Data Warehouse para Análisis

**User Story:** Como gerente, quiero analizar ventas y tendencias, para tomar decisiones informadas de negocio.

**Acceptance Criteria:**

- THE Analytics_Server SHALL use PostgreSQL 18.1 with analytical extensions.
- THE Analytics_Server SHALL implement star schema with fact and dimension tables.
- THE Analytics_Server SHALL have fact table for sales with measures: quantity, amount, discount.
- THE Analytics_Server SHALL have dimension tables: products, time, stores, payment_methods.
- WHEN transactional data is committed, THE Application_Server SHALL populate Analytics_Server tables.
- THE Analytics_Server SHALL support complex analytical queries with joins across dimensions.
- THE Analytics_Server SHALL calculate aggregations: total sales by product, sales by time period, sales by store.

---

### Requirement 2.8: Reportes Analíticos con Apache Superset

**User Story:** Como gerente, quiero visualizar reportes analíticos interactivos, para explorar datos de ventas fácilmente.

**Acceptance Criteria:**

- THE Analytics_Server SHALL integrate with Apache Superset 6.1.0 for visualization.
- THE Analytics_Server SHALL expose data through Superset database connection.
- WHERE analytical reports are requested, THE Analytics_Server SHALL generate cross-tab tables.
- THE Analytics_Server SHALL support drill-down from summary to detail level.
- THE Analytics_Server SHALL generate charts: sales trends, top products, payment method distribution.
- THE Analytics_Server SHALL allow filtering by date range, store, and product category.
- THE Analytics_Server SHALL refresh dashboards with latest data from Data_Server.

---

## Entregable 3: Arquitectura N-Capas

### Requirement 3.1: Separación Estricta de Capas

**User Story:** Como arquitecto, quiero separar el sistema en capas independientes, para mejorar mantenibilidad y escalabilidad.

**Acceptance Criteria:**

- THE System SHALL implement three distinct layers: Presentation, Business, Data_Access.
- THE Presentation_Layer SHALL contain only POS_Client user interface components.
- THE Business_Layer SHALL contain only Application_Server business logic.
- THE Data_Access_Layer SHALL contain only database query and persistence components.
- THE Presentation_Layer SHALL communicate only with Business_Layer.
- THE Business_Layer SHALL communicate only with Data_Access_Layer.
- THE Data_Access_Layer SHALL communicate only with Data_Server.
- EACH layer SHALL be deployable independently.

---

### Requirement 3.2: Balanceo de Carga para Alta Disponibilidad

**User Story:** Como administrador de sistemas, quiero distribuir carga entre múltiples servidores, para evitar puntos únicos de falla.

**Acceptance Criteria:**

- THE System SHALL deploy Nginx 1.28.3 as load balancer.
- THE Load_Balancer SHALL distribute incoming requests across multiple Application_Server instances.
- THE Load_Balancer SHALL use round-robin or least-connections algorithm.
- THE Load_Balancer SHALL perform health checks on Application_Server instances every 10 seconds.
- IF an Application_Server instance fails health check, THEN THE Load_Balancer SHALL remove it from rotation.
- WHEN a failed instance recovers, THE Load_Balancer SHALL add it back to rotation.
- THE Load_Balancer SHALL maintain session affinity for stateful operations.

---

### Requirement 3.3: Múltiples Instancias de Servidor de Aplicaciones

**User Story:** Como sistema, quiero ejecutar múltiples instancias del servidor de aplicaciones, para manejar alta concurrencia.

**Acceptance Criteria:**

- THE System SHALL support running 2 or more Application_Server instances simultaneously.
- EACH Application_Server instance SHALL connect to the same Data_Server.
- EACH Application_Server instance SHALL process requests independently.
- THE Application_Server instances SHALL NOT share local state or memory.
- THE Application_Server instances SHALL coordinate through Data_Server for distributed locks.

---

### Requirement 3.4: Capa de Acceso a Datos Abstracta

**User Story:** Como desarrollador, quiero una capa de acceso a datos independiente, para aislar la lógica de negocio de los detalles de persistencia.

**Acceptance Criteria:**

- THE Data_Access_Layer SHALL implement repository pattern for data operations.
- THE Data_Access_Layer SHALL expose methods: findById, save, update, delete, findAll.
- THE Data_Access_Layer SHALL encapsulate all SQL queries and JDBC operations.
- THE Business_Layer SHALL invoke Data_Access_Layer methods without writing SQL.
- THE Data_Access_Layer SHALL handle database connection pooling.
- THE Data_Access_Layer SHALL translate database exceptions to application exceptions.

---

### Requirement 3.5: Servidor FTP para Archivos Multimedia

**User Story:** Como sistema, quiero almacenar imágenes de productos en servidor FTP, para no sobrecargar la base de datos relacional.

**Acceptance Criteria:**

- THE FTP_Server SHALL use vsftpd 3.0.5 or ProFTPD 1.3.8b as FTP daemon.
- THE FTP_Server SHALL store product images in organized directory structure by SKU.
- WHEN a product is created, THE Application_Server SHALL upload product image to FTP_Server.
- WHEN POS_Client displays product, THE POS_Client SHALL download image from FTP_Server.
- THE FTP_Server SHALL support concurrent file uploads and downloads.
- THE FTP_Server SHALL implement user authentication for secure access.
- THE FTP_Server SHALL log all file transfer operations.

---

### Requirement 3.6: Almacenamiento de Copias de Seguridad en FTP

**User Story:** Como administrador, quiero almacenar copias de seguridad de cierres de caja en FTP, para auditoría y recuperación.

**Acceptance Criteria:**

- WHEN arqueo is completed, THE Application_Server SHALL generate backup file in JSON format.
- THE Application_Server SHALL upload arqueo backup file to FTP_Server.
- THE FTP_Server SHALL organize backup files by date and store_id.
- THE FTP_Server SHALL retain backup files for configured retention period.
- THE Application_Server SHALL verify successful upload before confirming arqueo completion.

---

### Requirement 3.7: Logs de Auditoría Fiscal en FTP

**User Story:** Como auditor fiscal, quiero acceder a logs completos de transacciones, para verificar cumplimiento tributario.

**Acceptance Criteria:**

- WHEN a sale is completed, THE Application_Server SHALL generate audit log in XML or JSON format.
- THE Audit_Log SHALL include: transaction_id, timestamp, SKU, quantity, price, tax, total, payment_method.
- THE Application_Server SHALL upload audit log to FTP_Server immediately after sale.
- THE FTP_Server SHALL organize audit logs by date and store_id.
- THE Audit_Log SHALL be immutable and tamper-evident.
- THE FTP_Server SHALL retain audit logs for minimum 5 years.
- THE System SHALL provide search capability across audit logs by date range and transaction_id.

---

### Requirement 3.8: Servidor Espejo para Failover

**User Story:** Como administrador de sistemas, quiero un servidor espejo de base de datos, para mantener operaciones si el servidor principal falla.

**Acceptance Criteria:**

- THE Mirror_Server SHALL be an exact replica of Data_Server using PostgreSQL 18.1.
- THE Data_Server SHALL replicate data to Mirror_Server using streaming replication.
- THE Mirror_Server SHALL synchronize with Data_Server in near real-time (lag < 1 second).
- THE Mirror_Server SHALL be ready to accept connections at any time.
- IF Data_Server becomes unavailable, THEN THE System SHALL redirect connections to Mirror_Server.
- THE Application_Server SHALL detect Data_Server failure within 5 seconds.
- THE Application_Server SHALL automatically failover to Mirror_Server without manual intervention.
- WHEN Data_Server recovers, THE System SHALL resynchronize and restore primary role.

---

### Requirement 3.9: Continuidad de Operaciones Durante Failover

**User Story:** Como cajero, quiero seguir procesando ventas incluso si el servidor principal falla, para no interrumpir atención al cliente.

**Acceptance Criteria:**

- WHEN failover occurs, THE POS_Client SHALL continue processing sales without interruption.
- THE POS_Client SHALL NOT display error messages during transparent failover.
- THE Mirror_Server SHALL accept all write operations during failover period.
- THE Mirror_Server SHALL maintain transaction consistency and ACID properties.
- WHEN Data_Server recovers, THE System SHALL synchronize any transactions processed on Mirror_Server.

---

### Requirement 3.10: Monitoreo de Salud de Servidores

**User Story:** Como administrador de sistemas, quiero monitorear la salud de todos los servidores, para detectar problemas proactivamente.

**Acceptance Criteria:**

- THE System SHALL monitor Data_Server availability every 5 seconds.
- THE System SHALL monitor Mirror_Server replication lag every 10 seconds.
- THE System SHALL monitor Application_Server response time for each request.
- THE System SHALL monitor FTP_Server disk space usage every minute.
- IF any server fails health check, THEN THE System SHALL send alert notification.
- THE System SHALL log all health check results with timestamp.
- THE System SHALL provide dashboard showing status of all components.

---

## Entregable 4: Cloud Computing

### Requirement 4.1: Contenedorización con Docker

**User Story:** Como DevOps engineer, quiero empaquetar componentes en contenedores Docker, para despliegue consistente y portable.

**Acceptance Criteria:**

- THE Application_Server SHALL be packaged as Docker 29.2.0 container image.
- THE Data_Server SHALL be packaged as Docker 29.2.0 container image using official PostgreSQL 18.1 image.
- THE Analytics_Server SHALL be packaged as Docker 29.2.0 container image.
- EACH Container SHALL include only necessary dependencies and runtime.
- THE Container images SHALL be built using multi-stage Dockerfile for minimal size.
- THE System SHALL use Docker Compose to orchestrate multiple containers.
- THE Docker_Compose configuration SHALL define networks, volumes, and environment variables.

---

### Requirement 4.2: Orquestación con Kubernetes

**User Story:** Como DevOps engineer, quiero orquestar contenedores con Kubernetes, para gestión automatizada y escalado.

**Acceptance Criteria:**

- THE System SHALL deploy on Kubernetes using Minikube v1.38.1.
- THE System SHALL define Kubernetes Deployment for Application_Server with replica count.
- THE System SHALL define Kubernetes Service for load balancing across Application_Server pods.
- THE System SHALL define Kubernetes StatefulSet for Data_Server with persistent volumes.
- THE System SHALL define Kubernetes ConfigMap for application configuration.
- THE System SHALL define Kubernetes Secret for database credentials.
- THE System SHALL use Kubernetes namespaces to isolate environments (dev, staging, prod).

---

### Requirement 4.3: Auto-Escalado Horizontal

**User Story:** Como sistema cloud, quiero escalar automáticamente según demanda, para manejar picos de tráfico sin intervención manual.

**Acceptance Criteria:**

- THE System SHALL implement Kubernetes Horizontal Pod Autoscaler (HPA) for Application_Server.
- WHEN CPU utilization exceeds 70%, THE HPA SHALL increase Application_Server pod count.
- WHEN CPU utilization drops below 30%, THE HPA SHALL decrease Application_Server pod count.
- THE HPA SHALL scale between configured minimum (2 pods) and maximum (10 pods).
- THE HPA SHALL evaluate metrics every 15 seconds.
- WHERE KEDA is used, THE System SHALL scale based on custom metrics (request queue length, database connections).
- THE System SHALL maintain at least minimum pod count during low traffic periods.

---

### Requirement 4.4: Escalado Basado en Eventos Especiales

**User Story:** Como gerente de operaciones, quiero que el sistema escale automáticamente durante eventos especiales, para manejar demanda de temporada alta.

**Acceptance Criteria:**

- WHEN date matches configured special event (Black Friday, Christmas), THE System SHALL pre-scale to maximum capacity.
- THE System SHALL schedule scaling operations based on historical traffic patterns.
- THE System SHALL scale up 1 hour before expected traffic peak.
- THE System SHALL scale down 2 hours after traffic returns to normal.
- THE System SHALL log all scaling events with timestamp and reason.

---

### Requirement 4.5: Base de Datos Distribuida Multi-AZ

**User Story:** Como arquitecto cloud, quiero distribuir la base de datos en múltiples zonas, para tolerancia a desastres geográficos.

**Acceptance Criteria:**

- THE Data_Server SHALL deploy PostgreSQL 18.1 instances in multiple availability zones (simulated as separate containers or VMs).
- THE System SHALL configure PostgreSQL 18.1 streaming replication across zones.
- EACH zone SHALL have at least one PostgreSQL 18.1 replica.
- IF primary zone fails, THEN THE System SHALL promote replica in secondary zone to primary.
- THE System SHALL distribute read queries across replicas for load balancing.
- THE System SHALL route write queries only to primary instance.
- THE System SHALL monitor replication lag between zones and alert if lag exceeds 5 seconds.

---

### Requirement 4.6: Almacenamiento de Objetos con MinIO

**User Story:** Como sistema cloud, quiero almacenar archivos en object storage compatible con S3, para reemplazar FTP tradicional con solución más segura.

**Acceptance Criteria:**

- THE Object_Storage SHALL use MinIO RELEASE.2025-09-07T16-13-09Z as S3-compatible storage service.
- THE Object_Storage SHALL organize objects in buckets: product-images, audit-logs, backups.
- WHEN a product image is uploaded, THE Application_Server SHALL use S3 API to store in product-images bucket.
- WHEN an audit log is generated, THE Application_Server SHALL store in audit-logs bucket with timestamp prefix.
- THE Object_Storage SHALL implement versioning for audit-logs bucket.
- THE Object_Storage SHALL enforce access policies: public read for product-images, authenticated access for audit-logs.
- THE Object_Storage SHALL replicate objects across multiple MinIO nodes for redundancy.
- THE POS_Client SHALL retrieve product images using pre-signed URLs from Object_Storage.

---

### Requirement 4.7: Gestión de Identidad y Acceso con Keycloak

**User Story:** Como administrador de seguridad, quiero gestionar identidades y permisos centralizadamente, para control de acceso unificado.

**Acceptance Criteria:**

- THE System SHALL use Keycloak 26.5.6 for identity and access management.
- THE Keycloak 26.5.6 SHALL manage user accounts for: cashiers, managers, administrators, auditors.
- THE Keycloak 26.5.6 SHALL implement role-based access control (RBAC).
- THE Application_Server SHALL validate JWT tokens issued by Keycloak 26.5.6 for each request.
- THE POS_Client SHALL authenticate users through Keycloak 26.5.6 login flow.
- THE Keycloak 26.5.6 SHALL enforce password policies: minimum length, complexity, expiration.
- THE Keycloak 26.5.6 SHALL support multi-factor authentication for administrator accounts.
- THE Keycloak 26.5.6 SHALL log all authentication attempts and authorization decisions.

---

### Requirement 4.8: Seguridad de Red con Firewall

**User Story:** Como administrador de seguridad, quiero proteger los componentes con firewalls, para prevenir acceso no autorizado.

**Acceptance Criteria:**

- THE System SHALL use iptables or firewalld for network filtering.
- THE Firewall SHALL allow incoming traffic only on required ports: 80 (HTTP), 443 (HTTPS), 5432 (PostgreSQL 18.1).
- THE Firewall SHALL block all other incoming traffic by default.
- THE Firewall SHALL allow outgoing traffic for application needs.
- THE Firewall SHALL implement rate limiting to prevent DDoS attacks.
- THE Firewall SHALL log all blocked connection attempts.
- WHERE Kubernetes is used, THE System SHALL implement NetworkPolicy for pod-to-pod communication rules.

---

### Requirement 4.9: Certificados SSL con Let's Encrypt

**User Story:** Como administrador de seguridad, quiero cifrar comunicaciones con SSL/TLS, para proteger datos en tránsito.

**Acceptance Criteria:**

- THE System SHALL use Let's Encrypt (Certbot 3.3.0) for SSL/TLS certificates.
- THE System SHALL configure HTTPS on all external endpoints.
- THE System SHALL automatically renew certificates before expiration.
- THE System SHALL redirect HTTP traffic to HTTPS.
- THE System SHALL use TLS 1.2 or higher.
- THE System SHALL disable weak cipher suites.
- THE POS_Client SHALL validate server certificates before establishing connection.

---

### Requirement 4.10: Monitoreo con Prometheus y Grafana

**User Story:** Como DevOps engineer, quiero monitorear métricas del sistema en tiempo real, para detectar y resolver problemas rápidamente.

**Acceptance Criteria:**

- THE System SHALL use Prometheus 3.4.0 for metrics collection.
- THE Application_Server SHALL expose metrics endpoint in Prometheus format.
- THE Prometheus 3.4.0 SHALL scrape metrics from all Application_Server pods every 15 seconds.
- THE Prometheus 3.4.0 SHALL collect metrics: request rate, response time, error rate, CPU usage, memory usage.
- THE System SHALL use Grafana 12.0.1 OSS for metrics visualization.
- THE Grafana 12.0.1 OSS SHALL display dashboards: system overview, application performance, database performance.
- THE Grafana 12.0.1 OSS SHALL support drill-down from summary to detailed metrics.
- THE Grafana 12.0.1 OSS SHALL allow filtering by time range, pod, and store.

---

### Requirement 4.11: Alertas Proactivas

**User Story:** Como administrador de sistemas, quiero recibir alertas cuando ocurren problemas, para responder antes de que afecten usuarios.

**Acceptance Criteria:**

- THE Prometheus 3.4.0 SHALL evaluate alert rules every 30 seconds.
- WHEN error rate exceeds 5%, THE Prometheus 3.4.0 SHALL trigger alert.
- WHEN response time exceeds 2 seconds, THE Prometheus 3.4.0 SHALL trigger alert.
- WHEN pod is down for more than 1 minute, THE Prometheus 3.4.0 SHALL trigger alert.
- WHEN database replication lag exceeds 10 seconds, THE Prometheus 3.4.0 SHALL trigger alert.
- THE Prometheus 3.4.0 SHALL send alerts to configured notification channels (email, Slack, webhook).
- THE Prometheus 3.4.0 SHALL group related alerts to avoid notification spam.

---

### Requirement 4.12: Gestión de Logs con ELK Stack o Loki

**User Story:** Como DevOps engineer, quiero centralizar y buscar logs de todos los componentes, para troubleshooting eficiente.

**Acceptance Criteria:**

- THE System SHALL use Elasticsearch 8.17.0 + Logstash 8.17.0 + Kibana 8.17.0 (ELK Stack) or Grafana Loki 3.4.3 for log management.
- THE Application_Server SHALL write structured logs in JSON format.
- THE Log_Aggregator SHALL collect logs from all Application_Server pods.
- THE Log_Aggregator SHALL index logs by timestamp, level, pod, and message.
- THE Log_Aggregator SHALL retain logs for configured retention period (30 days minimum).
- THE Kibana 8.17.0 or Grafana 12.0.1 OSS SHALL provide log search interface with filters.
- THE System SHALL support full-text search across all logs.
- THE System SHALL correlate logs with traces using correlation IDs.

---

### Requirement 4.13: Respaldo y Recuperación en Cloud

**User Story:** Como administrador de datos, quiero respaldar datos automáticamente en cloud, para recuperación ante desastres.

**Acceptance Criteria:**

- THE System SHALL backup Data_Server to Object_Storage daily at configured time.
- THE Backup process SHALL create full backup weekly and incremental backups daily.
- THE Backup SHALL be compressed and encrypted before upload to Object_Storage.
- THE System SHALL verify backup integrity after upload.
- THE System SHALL retain backups according to policy: daily for 7 days, weekly for 4 weeks, monthly for 12 months.
- THE System SHALL provide restore procedure to recover from backup.
- THE System SHALL test backup restore process monthly to verify recoverability.

---

### Requirement 4.14: Disaster Recovery Plan

**User Story:** Como arquitecto de sistemas, quiero un plan de recuperación ante desastres, para minimizar tiempo de inactividad.

**Acceptance Criteria:**

- THE System SHALL document disaster recovery procedures for each component.
- THE System SHALL define Recovery Time Objective (RTO) of 1 hour.
- THE System SHALL define Recovery Point Objective (RPO) of 15 minutes.
- THE System SHALL maintain standby environment in secondary region.
- IF primary region fails, THEN THE System SHALL failover to secondary region.
- THE System SHALL test disaster recovery plan quarterly.
- THE System SHALL document lessons learned from each DR test.

---

## Requisitos Funcionales Transversales

### Requirement 5.1: Gestión Completa de Productos (CRUD)

**User Story:** Como administrador, quiero gestionar el catálogo de productos completamente, para mantener información actualizada.

**Acceptance Criteria:**

- THE System SHALL allow creating new products with fields: SKU, name, description, price, category, image.
- THE System SHALL validate SKU uniqueness before creating product.
- THE System SHALL allow updating product information except SKU.
- THE System SHALL allow deleting products that have no associated sales.
- IF a product has sales history, THEN THE System SHALL mark it as inactive instead of deleting.
- THE System SHALL allow searching products by SKU, name, or category.
- THE System SHALL display product list with pagination (50 products per page).

---

### Requirement 5.2: Control de Inventario Multi-Tienda

**User Story:** Como gerente de inventario, quiero controlar stock por tienda, para evitar quiebres de stock.

**Acceptance Criteria:**

- THE System SHALL maintain inventory quantity for each product per store.
- WHEN a sale is completed, THE System SHALL decrement inventory for the corresponding store.
- WHEN inventory reaches reorder point, THE System SHALL generate reorder alert.
- THE System SHALL allow manual inventory adjustments with reason code.
- THE System SHALL log all inventory changes with timestamp, user, and reason.
- THE System SHALL support inventory transfers between stores.
- THE System SHALL calculate inventory value based on current stock and product prices.

---

### Requirement 5.3: Procesamiento de Ventas Multi-Pago

**User Story:** Como cajero, quiero procesar ventas con múltiples medios de pago, para flexibilidad en transacciones.

**Acceptance Criteria:**

- THE System SHALL support payment methods: cash, credit card, debit card, bank transfer, digital wallet.
- THE System SHALL allow splitting payment across multiple methods.
- WHEN payment is split, THE System SHALL validate that sum of payments equals sale total.
- THE System SHALL record each payment method and amount separately.
- THE System SHALL calculate change for cash payments.
- THE System SHALL generate receipt showing all payment methods used.
- IF payment fails, THEN THE System SHALL rollback inventory changes.

---

### Requirement 5.4: Gestión de Promociones Configurables

**User Story:** Como gerente de marketing, quiero configurar promociones flexibles, para ejecutar campañas de ventas.

**Acceptance Criteria:**

- THE System SHALL support promotion types: percentage discount, fixed amount discount, buy X get Y free.
- THE System SHALL allow configuring promotion conditions: minimum purchase amount, specific products, product categories.
- THE System SHALL allow setting promotion validity period with start and end dates.
- THE System SHALL allow limiting promotion to specific stores or all stores.
- THE System SHALL allow stacking multiple promotions or enforce mutual exclusivity.
- THE System SHALL display active promotions to cashier during sale.
- THE System SHALL generate promotion effectiveness report showing usage and revenue impact.

---

### Requirement 5.5: Arqueo de Caja con Conciliación

**User Story:** Como cajero, quiero realizar arqueo de caja con conciliación detallada, para cerrar turno correctamente.

**Acceptance Criteria:**

- WHEN arqueo is initiated, THE System SHALL lock the POS terminal for new sales.
- THE System SHALL calculate expected amounts by payment method from all sales in shift.
- THE Cashier SHALL input declared amounts for each payment method.
- THE System SHALL calculate variance for each payment method.
- THE System SHALL require supervisor approval if total variance exceeds configured threshold.
- THE System SHALL generate arqueo report with: shift details, sales summary, payment breakdown, variances.
- THE System SHALL upload arqueo report to Object_Storage or FTP_Server.
- WHEN arqueo is completed, THE System SHALL unlock POS terminal for next shift.

---

### Requirement 5.6: Reportes de Ventas Analíticos

**User Story:** Como gerente, quiero analizar ventas desde múltiples perspectivas, para identificar oportunidades de negocio.

**Acceptance Criteria:**

- THE System SHALL generate sales report by time period (daily, weekly, monthly, yearly).
- THE System SHALL generate sales report by product showing quantity and revenue.
- THE System SHALL generate sales report by category showing contribution to total sales.
- THE System SHALL generate sales report by store for multi-store comparison.
- THE System SHALL generate sales report by payment method showing distribution.
- THE System SHALL generate top products report showing best sellers.
- THE System SHALL generate sales trend report showing growth or decline over time.
- THE System SHALL allow exporting reports to PDF, Excel, or CSV format.

---

### Requirement 5.7: Auditoría Completa de Operaciones

**User Story:** Como auditor, quiero rastrear todas las operaciones del sistema, para cumplimiento y seguridad.

**Acceptance Criteria:**

- THE System SHALL log all user authentication attempts with timestamp, username, and result.
- THE System SHALL log all product modifications with before and after values.
- THE System SHALL log all inventory adjustments with user, reason, and quantity.
- THE System SHALL log all sales transactions with complete details.
- THE System SHALL log all arqueo operations with variances.
- THE System SHALL log all system configuration changes.
- THE Audit_Log SHALL be immutable and stored in append-only format.
- THE System SHALL generate audit trail report for specified date range.
- THE System SHALL support filtering audit logs by user, operation type, or entity.

---

### Requirement 5.8: Gestión de Usuarios y Roles

**User Story:** Como administrador, quiero gestionar usuarios y sus permisos, para control de acceso granular.

**Acceptance Criteria:**

- THE System SHALL support user roles: cashier, supervisor, manager, administrator, auditor.
- THE Cashier role SHALL have permissions: process sales, view products, perform arqueo.
- THE Supervisor role SHALL have cashier permissions plus: approve arqueo variances, adjust inventory.
- THE Manager role SHALL have supervisor permissions plus: manage products, view reports, configure promotions.
- THE Administrator role SHALL have manager permissions plus: manage users, configure system.
- THE Auditor role SHALL have read-only access to all data and audit logs.
- THE System SHALL enforce role-based access control on all operations.
- THE System SHALL allow assigning users to specific stores or all stores.

---

### Requirement 5.9: Manejo de Errores y Recuperación

**User Story:** Como usuario, quiero que el sistema maneje errores gracefully, para no perder datos ni interrumpir operaciones.

**Acceptance Criteria:**

- WHEN a network error occurs, THE System SHALL retry the operation up to 3 times.
- IF all retries fail, THEN THE System SHALL display user-friendly error message.
- WHEN a database error occurs, THE System SHALL rollback the transaction.
- THE System SHALL log all errors with stack trace and context.
- WHEN a sale is interrupted, THE System SHALL allow resuming or canceling the transaction.
- THE System SHALL maintain transaction integrity during failures.
- THE System SHALL provide recovery procedures for common failure scenarios.

---

### Requirement 5.10: Rendimiento y Escalabilidad

**User Story:** Como arquitecto, quiero que el sistema maneje carga creciente, para soportar expansión del negocio.

**Acceptance Criteria:**

- THE System SHALL process sale transactions in less than 2 seconds under normal load.
- THE System SHALL support at least 100 concurrent POS terminals.
- THE System SHALL handle at least 1000 transactions per minute across all stores.
- THE System SHALL scale horizontally by adding more Application_Server instances.
- THE Database queries SHALL execute in less than 100 milliseconds for 95th percentile.
- THE System SHALL use connection pooling to optimize database connections.
- THE System SHALL implement caching for frequently accessed data (product catalog).

---

## Stack Tecnológico

### Requirement 6.1: Tecnologías 100% Gratuitas y Open Source

**User Story:** Como arquitecto de costos, quiero usar solo tecnologías gratuitas, para minimizar gastos operativos.

**Acceptance Criteria:**

- THE System SHALL use Oracle JDK 21 as Java runtime environment.
- THE System SHALL use PostgreSQL 18.1 as relational database.
- THE System SHALL use Spring Boot 3.3.5 as application framework.
- THE System SHALL use Maven 3.9.9 as build tool.
- THE System SHALL use Docker 29.2.0 for containerization.
- THE System SHALL use Kubernetes via Minikube v1.38.1 for orchestration.
- THE System SHALL use Nginx 1.28.3 for load balancing.
- THE System SHALL use vsftpd 3.0.5 or ProFTPD 1.3.8b for FTP server.
- THE System SHALL use MinIO RELEASE.2025-09-07T16-13-09Z for object storage (GNU AGPLv3).
- THE System SHALL use Prometheus 3.4.0 for metrics collection.
- THE System SHALL use Grafana 12.0.1 OSS for visualization.
- THE System SHALL use Elasticsearch 8.17.0 + Logstash 8.17.0 + Kibana 8.17.0 (ELK Stack) or Grafana Loki 3.4.3 for log management.
- THE System SHALL use Keycloak 26.5.6 for identity management.
- THE System SHALL use Let's Encrypt (Certbot 3.3.0) for SSL certificates.
- THE System SHALL use Apache Superset 6.1.0 for analytics visualization.
- THE System SHALL use iptables or firewalld for network security.
- ALL components SHALL be deployable without licensing costs.

---

## Seguridad de Credenciales y Gestión de Secretos

### Requirement 7.1: Separación de Configuración Sensible del Código Fuente

**User Story:** Como desarrollador, quiero separar toda información sensible del código fuente y del repositorio Git, para que credenciales, contraseñas, tokens y claves nunca sean expuestas públicamente.

**Acceptance Criteria:**

- THE System SHALL NEVER commit passwords, API keys, tokens, secret keys, IPs internas, or database connection strings to Git.
- THE Repository SHALL include a `.gitignore` that explicitly excludes: `application-secrets.properties`, `.env`, `*.env`, `*.key`, `*.pem`, `*.jks`, `*.p12`, `k8s/app/secret.yaml`, and any file containing the word `secret` or `credentials` in its name.
- THE Project SHALL use Spring Boot profiles to separate non-sensitive configuration (committed) from sensitive configuration (excluded): `application.properties` (committed, uses `${VAR}` placeholders), `application-dev.properties` (committed, no passwords), and `application-secrets.properties` (never committed).
- WHEN a developer clones the repository, THE Project SHALL provide a `application-secrets.properties.template` file (committed) listing every required variable without values, so developers know what to fill in locally.
- THE Architecture_Document SHALL include a dedicated section documenting which files must never be committed and why.

---

### Requirement 7.2: Uso de Variables de Entorno para Secretos en Todos los Entregables

**User Story:** Como desarrollador, quiero que todos los módulos lean credenciales desde variables de entorno, para evitar valores hardcodeados en cualquier archivo del proyecto.

**Acceptance Criteria:**

- THE System SHALL NEVER hardcode credentials, ports, IPs, or tokens directly in Java source code (`.java` files).
- THE System SHALL NEVER hardcode credentials in any committed properties file (`application.properties`, `application-dev.properties`).
- ALL modules SHALL reference sensitive values exclusively via Spring Boot placeholders: `${DB_USERNAME}`, `${DB_PASSWORD}`, `${MINIO_ACCESS_KEY}`, `${MINIO_SECRET_KEY}`, `${KEYCLOAK_CLIENT_SECRET}`, `${FTP_PASSWORD}`, `${JWT_SECRET}`.
- THE `application.properties` file (committed) SHALL contain only the placeholder references, for example:

  ```properties
  # application.properties — se sube a GitHub (sin valores sensibles)
  spring.datasource.url=jdbc:postgresql://localhost:5432/pukio_central
  spring.datasource.username=${DB_USERNAME}
  spring.datasource.password=${DB_PASSWORD}
  minio.access-key=${MINIO_ACCESS_KEY}
  minio.secret-key=${MINIO_SECRET_KEY}
  keycloak.client-secret=${KEYCLOAK_CLIENT_SECRET}
  ftp.password=${FTP_PASSWORD}
  ```

- THE `application-secrets.properties` file (never committed) SHALL define the actual values for each placeholder on the developer's local machine.
- IF a required environment variable is missing at startup, THEN THE Application SHALL fail fast with a descriptive error message indicating which variable is absent.

---

### Requirement 7.3: Gestión de Secretos en Docker y Docker Compose

**User Story:** Como DevOps engineer, quiero que los secretos del entorno Docker nunca estén en el `docker-compose.yml` commiteado, para proteger credenciales de contenedores.

**Acceptance Criteria:**

- THE `docker-compose.yml` (committed) SHALL reference environment variables using `${VAR}` syntax rather than hardcoded values for all sensitive fields: database passwords, MinIO credentials, Keycloak secrets, and JWT secrets.
- THE Project SHALL provide a `.env.template` file (committed) listing all variables required by Docker Compose without values.
- THE `.env` file (actual values, never committed) SHALL be used locally to supply values to `docker-compose.yml`.
- THE `.gitignore` SHALL explicitly exclude `.env` and any variant (`.env.local`, `.env.prod`, `.env.staging`).
- THE `docker-compose.yml` MAY use Docker Secrets (`secrets:` block) for production-grade deployments as an alternative to `.env` files.
- WHEN a new developer sets up the environment, THE `README.md` SHALL instruct them to copy `.env.template` to `.env` and fill in all required values before running `docker compose up`.

---

### Requirement 7.4: Gestión de Secretos en Kubernetes

**User Story:** Como DevOps engineer, quiero que los manifiestos Kubernetes no contengan credenciales en texto plano en el repositorio, para no exponer secretos de producción en Git.

**Acceptance Criteria:**

- THE `k8s/app/secret.yaml` file (Kubernetes Secret manifest with real values) SHALL NEVER be committed to the repository.
- THE Repository SHALL include a `k8s/app/secret.yaml.template` (committed) listing all keys of the Secret without base64 values.
- THE `.gitignore` SHALL explicitly exclude `k8s/**/secret.yaml` and any file matching `*secret*.yaml` in the `k8s/` directory.
- THE `k8s/app/configmap.yaml` (committed) SHALL contain only non-sensitive configuration such as `DB_URL`, `APP_PORT`, `FTP_HOST`, `MINIO_ENDPOINT`, and `KEYCLOAK_URL`.
- ALL sensitive values (passwords, tokens, keys) SHALL be stored exclusively in Kubernetes Secrets and injected into pods as environment variables via `envFrom.secretRef`.
- THE `README.md` and `docs/security.md` SHALL document the procedure to create the Kubernetes Secret manually before deploying: `kubectl create secret generic pukio-secrets --from-env-file=.env`.
- WHERE a secrets management tool is used (e.g., Sealed Secrets or external vault), THE Project MAY commit encrypted secret manifests, provided the encryption key is never committed.

---

### Requirement 7.5: Política del Repositorio Git para Prevención de Leaks

**User Story:** Como líder técnico, quiero que el repositorio tenga salvaguardas activas para prevenir que secretos sean subidos por error, para reducir el riesgo humano de exposición de credenciales.

**Acceptance Criteria:**

- THE `.gitignore` SHALL include the following entries as minimum (in addition to the standard Java/Maven ignores):

  ```
  # Secretos — NUNCA subir a GitHub
  application-secrets.properties
  .env
  .env.*
  !.env.template
  *.jks
  *.p12
  *.pem
  *.key
  k8s/**/secret.yaml
  ssl/
  ```

- THE `README.md` SHALL include a clearly visible section titled **"⚠️ Seguridad — Archivos que NUNCA deben subirse a GitHub"** listing every excluded file type and the reason.
- THE Project SHALL provide a `docs/security.md` document describing the full secrets management strategy: local development (application-secrets.properties), Docker (`.env`), and Kubernetes (Secret manifest).
- ALL team members SHALL be instructed (via `docs/security.md`) that if a secret is accidentally committed, the procedure is: immediately rotate the compromised credential, use `git filter-repo` or BFG Repo Cleaner to purge history, and force-push.
- THE CI/CD pipeline (if configured) SHOULD include a secret-scanning step (e.g., `truffleHog` or `gitleaks`) that fails the build if a potential secret pattern is detected in a commit.

---

### Requirement 7.6: Estructura de Archivos de Configuración por Entregable

**User Story:** Como desarrollador, quiero una estructura de configuración clara y consistente en todos los entregables, para saber exactamente qué se sube a Git y qué se mantiene local.

**Acceptance Criteria:**

- THE Project SHALL maintain the following file structure convention for all Spring Boot modules:

  ```
  src/main/resources/
  ├── application.properties              ← SÍ se sube (placeholders ${VAR}, sin valores)
  ├── application-dev.properties          ← SÍ se sube (config de dev sin passwords)
  ├── application-prod.properties         ← SÍ se sube (config de prod sin passwords)
  └── application-secrets.properties      ← NUNCA se sube (valores reales locales)
  ```

- THE Project root SHALL maintain the following convention for Docker and Kubernetes:

  ```
  /                                       (raíz del repositorio)
  ├── .env.template                       ← SÍ se sube (variables sin valores)
  ├── .env                                ← NUNCA se sube (valores reales)
  ├── docker-compose.yml                  ← SÍ se sube (referencias a ${VAR})
  └── k8s/
      ├── app/
      │   ├── configmap.yaml              ← SÍ se sube (config no sensible)
      │   ├── secret.yaml.template        ← SÍ se sube (keys sin valores)
      │   └── secret.yaml                ← NUNCA se sube (valores base64 reales)
      └── ...
  ```

- THE convention SHALL be documented in the `README.md` with a table distinguishing **"Se sube a Git"** vs **"NUNCA se sube a Git"** for every configuration file in the project.
- FOR Entregable 1, the minimum required exclusions are: `application-secrets.properties` with `DB_USERNAME`, `DB_PASSWORD`, and `server.port` (if internal).
- FOR Entregable 2 and 3, the exclusions extend to include: FTP credentials (`FTP_PASSWORD`, `FTP_USER`) and any Analytics_Server connection strings.
- FOR Entregable 4, the exclusions extend to include: `KEYCLOAK_CLIENT_SECRET`, `MINIO_ACCESS_KEY`, `MINIO_SECRET_KEY`, `JWT_SECRET`, SSL certificate private keys (`*.pem`, `*.key`), and all Kubernetes Secret manifests with real values.

---

## Notas Finales

Este documento de requisitos cubre la evolución completa del sistema POS Pukio a través de 4 entregables arquitectónicos:

- **Entregable 1:** Arquitectura unitaria con archivos indexados locales y sincronización al servidor central.
- **Entregable 2:** Arquitectura cliente/servidor con lógica centralizada y Data Warehouse para análisis.
- **Entregable 3:** Arquitectura N-capas con alta disponibilidad, balanceo de carga, FTP y servidor espejo.
- **Entregable 4:** Cloud computing con contenedores, auto-escalado, object storage y monitoreo avanzado.

Todos los requisitos siguen patrones EARS y cumplen con reglas de calidad INCOSE. El sistema utiliza exclusivamente tecnologías gratuitas y open-source, garantizando viabilidad económica para implementación en tiendas minoristas.

La **Sección 7 — Seguridad de Credenciales y Gestión de Secretos** aplica de forma transversal a todos los entregables desde el inicio del proyecto. Ningún archivo con valores sensibles (contraseñas, tokens, claves API, claves privadas SSL) deberá aparecer jamás en el historial de Git.

### Versiones de Herramientas y Servicios

| Componente                          | Versión                              |
|-------------------------------------|--------------------------------------|
| Oracle JDK                          | 21                                   |
| Spring Boot                         | 3.3.5                                |
| PostgreSQL                          | 18.1                                 |
| Maven                               | 3.9.9                                |
| Docker                              | 29.2.0                               |
| Minikube                            | v1.38.1                              |
| Nginx                               | 1.28.3 (stable)                      |
| Prometheus                          | 3.4.0                                |
| Grafana OSS                         | 12.0.1                               |
| Keycloak                            | 26.5.6                               |
| Apache Superset                     | 6.1.0                                |
| MinIO                               | RELEASE.2025-09-07T16-13-09Z (GNU AGPLv3) |
| Elasticsearch / Logstash / Kibana   | 8.17.0                               |
| Grafana Loki (alternativa a ELK)    | 3.4.3                                |
| vsftpd / ProFTPD                    | 3.0.5 / 1.3.8b                       |
| Let's Encrypt / Certbot             | 3.3.0                                |
| git-filter-repo / BFG Repo Cleaner | latest                               |