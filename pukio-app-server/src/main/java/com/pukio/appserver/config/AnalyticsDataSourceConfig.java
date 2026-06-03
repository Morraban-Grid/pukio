package com.pukio.appserver.config;

import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;

/**
 * Configuración de la segunda fuente de datos: Analytics Server (Data Warehouse).
 * 
 * Esta configuración define un DataSource secundario que apunta al servidor PostgreSQL
 * del Data Warehouse, separado del servidor transaccional principal.
 * 
 * REQ 2.7 — Analytics_Server SHALL expose data through separate database connection.
 * REQ 4.2 — System SHALL use connection pooling for optimal database connections.
 */
@Configuration
public class AnalyticsDataSourceConfig {

    @Value("${analytics.datasource.url}")
    private String analyticsDbUrl;

    @Value("${analytics.datasource.username}")
    private String analyticsDbUsername;

    @Value("${analytics.datasource.password}")
    private String analyticsDbPassword;

    @Value("${analytics.datasource.driver-class-name}")
    private String driverClassName;

    @Value("${analytics.datasource.hikari.maximum-pool-size}")
    private int maximumPoolSize;

    @Value("${analytics.datasource.hikari.minimum-idle}")
    private int minimumIdle;

    @Value("${analytics.datasource.hikari.connection-timeout}")
    private long connectionTimeout;

    @Value("${analytics.datasource.hikari.idle-timeout}")
    private long idleTimeout;

    @Value("${analytics.datasource.hikari.max-lifetime}")
    private long maxLifetime;

    /**
     * Crea el DataSource secundario para el Analytics Server (Data Warehouse).
     * 
     * Este DataSource usa HikariCP con un pool reducido (max 5 conexiones) ya que
     * las operaciones al DW son asíncronas y no requieren la misma capacidad que
     * el servidor transaccional.
     * 
     * @return DataSource configurado para el Analytics Server
     */
    @Bean(name = "analyticsDataSource")
    @Qualifier("analyticsDataSource")
    public DataSource analyticsDataSource() {
        HikariConfig config = new HikariConfig();
        
        config.setJdbcUrl(analyticsDbUrl);
        config.setUsername(analyticsDbUsername);
        config.setPassword(analyticsDbPassword);
        config.setDriverClassName(driverClassName);
        
        // Configuración del pool (reducido para DW)
        config.setMaximumPoolSize(maximumPoolSize);
        config.setMinimumIdle(minimumIdle);
        config.setConnectionTimeout(connectionTimeout);
        config.setIdleTimeout(idleTimeout);
        config.setMaxLifetime(maxLifetime);
        
        // Configuración adicional para optimizar el pool
        config.setPoolName("AnalyticsHikariPool");
        config.setAutoCommit(true);
        config.setConnectionTestQuery("SELECT 1");
        config.setValidationTimeout(5000);
        config.setLeakDetectionThreshold(60000); // Detectar leaks después de 60s
        
        // Propiedades específicas de PostgreSQL
        config.addDataSourceProperty("cachePrepStmts", "true");
        config.addDataSourceProperty("prepStmtCacheSize", "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit", "2048");
        config.addDataSourceProperty("useServerPrepStmts", "true");
        
        return new HikariDataSource(config);
    }

    /**
     * Crea un JdbcTemplate dedicado para el Analytics Server.
     * 
     * Este JdbcTemplate se inyecta en los servicios ETL que necesitan insertar
     * datos en el Data Warehouse de forma asíncrona.
     * 
     * @param analyticsDataSource DataSource del Analytics Server
     * @return JdbcTemplate configurado para Analytics
     */
    @Bean(name = "analyticsJdbcTemplate")
    public JdbcTemplate analyticsJdbcTemplate(
            @Qualifier("analyticsDataSource") DataSource analyticsDataSource) {
        return new JdbcTemplate(analyticsDataSource);
    }
}
