package com.pukio.posclient.ui;

import com.pukio.posclient.client.AppServerClient;

import javax.swing.*;
import java.awt.*;

/**
 * Main sales panel (placeholder for future implementation).
 * Will be implemented in subsequent tasks.
 */
public class SalePanel extends JPanel {

    private final AppServerClient client;

    public SalePanel(AppServerClient client) {
        this.client = client;
        initComponents();
    }

    private void initComponents() {
        setLayout(new BorderLayout());

        JLabel placeholderLabel = new JLabel("Panel de Ventas - En Construcción", SwingConstants.CENTER);
        placeholderLabel.setFont(new Font("SansSerif", Font.BOLD, 24));
        placeholderLabel.setForeground(Color.GRAY);
        
        add(placeholderLabel, BorderLayout.CENTER);
    }

    /**
     * Focus the SKU input field (called by F2 shortcut).
     */
    public void focusSku() {
        // Will be implemented when SKU field is added
    }

    /**
     * Refresh the panel (called by F5 shortcut).
     */
    public void refresh() {
        // Will be implemented
    }
}
