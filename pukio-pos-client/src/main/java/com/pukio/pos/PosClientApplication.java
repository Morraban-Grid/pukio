package com.pukio.pos;

import com.formdev.flatlaf.FlatLightLaf;
import com.pukio.posclient.ui.MainFrame;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.web.client.RestTemplate;

import javax.swing.*;

/**
 * POS Client application entry point.
 * 
 * Entregable 2: This is now a LIGHTWEIGHT CLIENT with NO business logic.
 * All business logic has been moved to pukio-app-server.
 * This client only contains UI components and HTTP client for server communication.
 * 
 * @author Pukio Team
 * @since Entregable 2 - Refactored for client/server architecture
 */
@Slf4j
@SpringBootApplication
@ComponentScan(basePackages = {"com.pukio.pos", "com.pukio.posclient"})
public class PosClientApplication {

    public static void main(String[] args) {
        // Setup FlatLaf Look and Feel
        FlatLightLaf.setup();

        // Launch Spring Boot context
        SpringApplication.run(PosClientApplication.class, args);

        // Launch Swing UI on Event Dispatch Thread
        SwingUtilities.invokeLater(() -> {
            MainFrame mainFrame = new MainFrame();
            mainFrame.setVisible(true);
        });

        log.info("===========================================");
        log.info("  PUKIO POS Client — Entregable 2");
        log.info("  Lightweight Client Architecture");
        log.info("===========================================");
        log.info("✓ Swing UI launched successfully");
        log.info("✓ FlatLaf theme applied");
        log.info("✓ All business logic delegated to Application Server");
    }

    /**
     * Bean for RestTemplate (HTTP client).
     */
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
