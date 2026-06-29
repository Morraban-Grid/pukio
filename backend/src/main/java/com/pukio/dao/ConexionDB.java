package com.pukio.dao;

import com.pukio.config.DatabaseConfig;
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.sql.Connection;
import java.sql.SQLException;

/**
 * Wrapper sobre HikariCP para conexiones JDBC a Oracle.
 * Cada llamada a getConnection() obtiene una conexion del pool;
 * debe cerrarse (devolvese al pool) al terminar, idealmente con try-with-resources.
 */
public class ConexionDB {

    private static HikariDataSource dataSource;

    static {
        HikariConfig config = new HikariConfig();
        config.setJdbcUrl(DatabaseConfig.getUrl());
        config.setDriverClassName("oracle.jdbc.OracleDriver");
        config.setUsername(DatabaseConfig.getUser());
        config.setPassword(DatabaseConfig.getPassword());
        config.setMaximumPoolSize(10);
        config.setMinimumIdle(2);
        config.setConnectionTimeout(30_000);
        config.setIdleTimeout(600_000);
        config.setMaxLifetime(1_800_000);
        config.setPoolName("PukioPool");
        // Oracle no requiere setAutoCommit(false) en el pool; cada DAO maneja su transaccion.
        config.setAutoCommit(true);
        dataSource = new HikariDataSource(config);
    }

    /** Devuelve una conexion del pool. Debe cerrarse al terminar (try-with-resources). */
    public static Connection getConnection() throws SQLException {
        return dataSource.getConnection();
    }

    /**
     * Mantiene compatibilidad con el patron getInstance().getConexion() usado en los DAOs.
     * @deprecated Usa directamente ConexionDB.getConnection() con try-with-resources.
     */
    @Deprecated
    public static ConexionDB getInstance() {
        return Holder.INSTANCE;
    }

    /** @deprecated Usa ConexionDB.getConnection() */
    @Deprecated
    public Connection getConexion() throws SQLException {
        return getConnection();
    }

    /** No-op mantenido por compatibilidad; el commit lo hace cada DAO tras su operacion. */
    @Deprecated
    public void commit() throws SQLException {}

    /** No-op mantenido por compatibilidad. */
    @Deprecated
    public void rollback() {}

    /** No-op mantenido por compatibilidad; el pool administra el ciclo de vida. */
    @Deprecated
    public void cerrar() {}

    private static class Holder {
        static final ConexionDB INSTANCE = new ConexionDB();
    }

    private ConexionDB() {}
}
