package com.pukio.posclient.ui.inventory;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.InventoryTransferDto;
import com.pukio.posclient.dto.StoreDto;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import java.awt.*;
import java.util.List;

/**
 * Diálogo modal para transferencias de inventario entre tiendas.
 * TASK-E2-26m (mismo archivo que InventoryAdjustmentDialog)
 */
public class TransferDialog extends JDialog {
    private final AppServerClient client;
    private boolean transferred = false;
    
    private JTextField skuField;
    private JComboBox<StoreDto> storeOrigenCombo;
    private JComboBox<StoreDto> storeDestinoCombo;
    private JSpinner quantitySpinner;
    
    private JButton transferButton;
    private JButton cancelButton;
    
    public TransferDialog(Window owner, AppServerClient client) {
        super(owner, "Transferencia de Inventario", ModalityType.APPLICATION_MODAL);
        this.client = client;
        
        initComponents();
        layoutComponents();
        attachListeners();
        loadStores();
        
        pack();
        setResizable(false);
    }
    
    private void initComponents() {
        skuField = new JTextField(20);
        
        storeOrigenCombo = new JComboBox<>();
        storeDestinoCombo = new JComboBox<>();
        
        SpinnerNumberModel spinnerModel = new SpinnerNumberModel(1, 1, 9999, 1);
        quantitySpinner = new JSpinner(spinnerModel);
        
        transferButton = new JButton("Transferir");
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
        
        // Tienda origen
        gbc.gridx = 0;
        gbc.gridy = 1;
        contentPanel.add(new JLabel("Tienda Origen:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(storeOrigenCombo, gbc);
        
        // Tienda destino
        gbc.gridx = 0;
        gbc.gridy = 2;
        contentPanel.add(new JLabel("Tienda Destino:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(storeDestinoCombo, gbc);
        
        // Cantidad
        gbc.gridx = 0;
        gbc.gridy = 3;
        contentPanel.add(new JLabel("Cantidad:"), gbc);
        gbc.gridx = 1;
        contentPanel.add(quantitySpinner, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(transferButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        transferButton.addActionListener(e -> transfer());
        cancelButton.addActionListener(e -> dispose());
    }
    
    private void loadStores() {
        SwingWorkerTask.execute(
            () -> client.getStores(),
            this::populateStoreComboBoxes,
            ex -> {
                JOptionPane.showMessageDialog(
                    this,
                    "Error al cargar tiendas: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            },
            null,
            null
        );
    }
    
    private void populateStoreComboBoxes(List<StoreDto> stores) {
        for (StoreDto store : stores) {
            storeOrigenCombo.addItem(store);
            storeDestinoCombo.addItem(store);
        }
    }
    
    private void transfer() {
        String sku = skuField.getText().trim();
        StoreDto storeOrigen = (StoreDto) storeOrigenCombo.getSelectedItem();
        StoreDto storeDestino = (StoreDto) storeDestinoCombo.getSelectedItem();
        int quantity = (Integer) quantitySpinner.getValue();
        
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
        
        if (storeOrigen == null || storeDestino == null) {
            JOptionPane.showMessageDialog(
                this,
                "Debe seleccionar tiendas de origen y destino",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        if (storeOrigen.getStoreId().equals(storeDestino.getStoreId())) {
            JOptionPane.showMessageDialog(
                this,
                "La tienda de origen y destino deben ser diferentes",
                "Validación",
                JOptionPane.WARNING_MESSAGE
            );
            return;
        }
        
        InventoryTransferDto dto = new InventoryTransferDto();
        dto.setSku(sku);
        dto.setStoreOrigenId(storeOrigen.getStoreId());
        dto.setStoreDestinoId(storeDestino.getStoreId());
        dto.setQuantity(quantity);
        
        SwingWorkerTask.execute(
            () -> {
                client.transferInventory(dto);
                return null;
            },
            result -> {
                transferred = true;
                JOptionPane.showMessageDialog(
                    this,
                    "Transferencia registrada exitosamente",
                    "Éxito",
                    JOptionPane.INFORMATION_MESSAGE
                );
                dispose();
            },
            ex -> {
                JOptionPane.showMessageDialog(
                    this,
                    "Error en la transferencia: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            },
            transferButton,
            null
        );
    }
    
    public boolean isTransferred() {
        return transferred;
    }
}
