package com.pukio.config;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Configuracion de conexion a la base de datos Oracle.
 * Los valores se leen desde /config.properties en el classpath.
 * Copia backend/src/main/resources/config.properties.example a
 * backend/src/main/resources/config.properties y completa los valores.
 */
public class DatabaseConfig {

    private static final String HOST;
    private static final String PORT;
    private static final String SERVICE;
    private static final String DB_USER;
    private static final String DB_PASS;
    private static final String CORS_ALLOWED_ORIGIN;

    static {
        Properties props = new Properties();
        try (InputStream in = DatabaseConfig.class.getResourceAsStream("/config.properties")) {
            if (in == null) {
                throw new ExceptionInInitializerError(
                    "No se encontro config.properties en el classpath. " +
                    "Copia backend/src/main/resources/config.properties.example a " +
                    "backend/src/main/resources/config.properties y completa los valores."
                );
            }
            props.load(in);
        } catch (IOException e) {
            throw new ExceptionInInitializerError(
                "Error al leer config.properties: " + e.getMessage()
            );
        }
        HOST               = props.getProperty("db.host");
        PORT               = props.getProperty("db.port");
        SERVICE            = props.getProperty("db.service");
        DB_USER            = props.getProperty("db.user");
        DB_PASS            = props.getProperty("db.password");
        CORS_ALLOWED_ORIGIN = props.getProperty("cors.allowed.origin");
    }

    public static String getUrl() {
        return "jdbc:oracle:thin:@" + HOST + ":" + PORT + "/" + SERVICE;
    }

    public static String getUser() {
        return DB_USER;
    }

    public static String getPassword() {
        return DB_PASS;
    }

    public static String getCorsAllowedOrigin() {
        return CORS_ALLOWED_ORIGIN;
    }

    private DatabaseConfig() {}
}
