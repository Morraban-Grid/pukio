package com.pukio.posclient.ui;

import com.pukio.posclient.client.AppServerClient;

import javax.swing.*;
import java.awt.*;

/**
 * Status bar showing connection status, last operation result, and user role.
 * 
 * TASK-E2-26d
 */
public class StatusBar extends JPanel {

    private final JLabel connectionStatus;
    private final JLabel lastOperation;
    private final JLabel roleLabel;
    private final AppServerClient client;
    private final Timer connectionCheckTimer;

    public StatusBar(AppServerClient client) {
        this.client = client;

        setLayout(new FlowLayout(FlowLayout.LEFT, 15, 5));
        setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        setBackground(new Color(245, 245, 245));

        // Connection status indicator
        connectionStatus = new JLabel("● DESCONECTADO");
        connectionStatus.setForeground(Color.RED);
        connectionStatus.setFont(new Font("SansSerif", Font.BOLD, 12));
        add(connectionStatus);

        // Separator
        add(new JLabel("|"));

        // Last operation result
        lastOperation = new JLabel("Estado: Iniciando...");
        lastOperation.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(lastOperation);

        // Separator
        add(new JLabel("|"));

        // User role
        roleLabel = new JLabel("Rol: --");
        roleLabel.setFont(new Font("SansSerif", Font.PLAIN, 12));
        add(roleLabel);

        // Start connection check timer (every 15 seconds)
        connectionCheckTimer = new Timer(15000, e -> checkConnection());
        connectionCheckTimer.setInitialDelay(2000); // First check after 2 seconds
        connectionCheckTimer.start();
    }

    /**
     * Set connection status.
     */
    public void setConnected(boolean connected) {
        if (connected) {
            connectionStatus.setText("● CONECTADO");
            connectionStatus.setForeground(new Color(0, 150, 0));
        } else {
            connectionStatus.setText("● DESCONECTADO");
            connectionStatus.setForeground(Color.RED);
        }
    }

    /**
     * Set last operation result message.
     */
    public void setLastOperation(String message) {
        lastOperation.setText("Estado: " + message);
    }

    /**
     * Set user role label.
     */
    public void setRole(String role) {
        roleLabel.setText("Rol: " + role);
    }

    /**
     * Check connection to Application Server.
     */
    private void checkConnection() {
        // Don't check if not logged in
        if (!com.pukio.posclient.client.SessionContext.isLoggedIn()) {
            setConnected(false);
            return;
        }

        // Try a lightweight request to check connectivity
        SwingUtilities.invokeLater(() -> {
            try {
                // Attempt to get products with page 0, size 1
                client.getProducts("", "", 0);
                setConnected(true);
            } catch (Exception e) {
                setConnected(false);
            }
        });
    }

    /**
     * Stop the connection check timer when panel is removed.
     */
    public void stopTimer() {
        if (connectionCheckTimer != null) {
            connectionCheckTimer.stop();
        }
    }
}
