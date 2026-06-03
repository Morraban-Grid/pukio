package com.pukio.posclient.ui.inventory;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.InventoryAdjustmentDto;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import java.awt.*;

/**
 * Diálogo modal para ajustes manuales de inventario.
 * TASK-E2-26m
 */
public class InventoryAdjustmentDialog extends JDialog {
    private final AppServerClient client;
    private boolean confirmed = false;
    
    private JTextField skuField;
    private JSpinner quantitySpinner;
    private JComboBox<String> reasonCombo;
    
    private JButton confirmButton;
    private JButton cancelButton;
    
    public InventoryAdjustmentDialog(Window owner, AppServerClient client, String preFillSku) {
        super(owner, "Ajuste Manual de Inventario", ModalityType.APPLICATION_MODAL);
        this.client = client;
        
        initComponents();
        layoutComponents();
        attachListeners();
        
        if (preFillSku != null) {
            skuField.setText(preFillSku);
        }
        
        pack();
        setResizable(false);
    }
    
    private void initComponents() {
        skuField = new JTextField(20);
        
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(0, -9999, 9999, 1);
        quantitySpinner = new JSpinner(spinnerModel);
        
        reasonCombo = new JComboBox<>(new String[]{
            "CORRECTION",
            "LOSS",
            "THEFT",
            "RETURN"
        });
        
        confirmButton = new JButton("Confirmar");
        cancelButton = new JButton("Cancelar");
    }
    
    private void layoutComponents() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // SKU
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(new JLabel("SKU:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(skuField, gbc);
        
        // Cantidad
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPanel.add(new JLabel("Cantidad (± unidades):"), gbc);
        gbc.gridx = 1;
        contentPanel.add(quantitySpinner, gbc);
        
        // Motivo
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(new JLabel("Motivo:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(reasonCombo, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(confirmButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        confirmButton.addActionListener(e -> confirm());
        cancelButton.addActionListener(e -> dispose());
    }
    
    private void confirm() {
        String sku = skuField.getText().trim();
        int quantity = (Integer) quantitySpinner.getValue();
        String reason = (String) reasonCombo.getSelectedItem();
        
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(
                this,
                "El SKU es obligatorio",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            skuField.requestFocus();
            return;
        }
        
        if (quantity == 0) {
            JOptionPane.showMessageDialog(
                this,
                "La cantidad debe ser diferente de cero",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            quantitySpinner.requestFocus();
            return;
        }
        
        InventoryAdjustmentDto dto = new InventoryAdjustmentDto();
        dto.setSku(sku);
        dto.setQuantity(quantity);
        dto.setReason(reason);
        
        SwingWorkerTask.execute(
            () -> {
                client.adjustInventory(dto);
                return null;
            },
            result -> {
                confirmed = true;
                JOptionPane.showMessageDialog(
                    this,
                    "Ajuste de inventario registrado exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE
                );
                dispose();
            },
            ex -> {
                JOptionPane.showMessageDialog(
                    this,
                    "Error al ajustar inventario: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            },
            confirmButton,
            null
        );
    }
    
    public boolean isConfirmed() {
        return confirmed;
    }
}
