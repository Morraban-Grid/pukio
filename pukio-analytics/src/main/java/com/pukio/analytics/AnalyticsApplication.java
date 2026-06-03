package com.pukio.analytics;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Main application class for Pukio Analytics Server.
 * 
 * This module implements the Data Warehouse and analytical reporting
 * capabilities using Apache Superset for visualization.
 * 
 * @author Pukio Team
 * @since Entregable 2
 */
@SpringBootApplication
public class AnalyticsApplication {

    public static void main(String[] args) {
        SpringApplication.run(AnalyticsApplication.class, args);
    }
}
