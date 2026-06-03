package com.pukio.posclient.ui.promotion;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.PromotionDto;
import com.pukio.posclient.dto.StoreDto;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import java.awt.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;

/**
 * Diálogo modal para crear/editar promociones.
 * TASK-E2-26p
 */
public class PromotionFormDialog extends JDialog {
    private final AppServerClient client;
    private final PromotionDto promotion; // null = creación
    private boolean saved = false;
    
    private JTextField nameField;
    private JComboBox<String> typeCombo;
    private JTextField valueField;
    private JTextField minPurchaseField;
    private JSpinner startDateSpinner;
    private JSpinner endDateSpinner;
    private JComboBox<String> storeCombo;
    private JCheckBox activeCheckbox;
    
    private JButton saveButton;
    private JButton cancelButton;
    
    public PromotionFormDialog(Window owner, AppServerClient client, PromotionDto promotion) {
        super(owner, promotion == null ? "Nueva Promoción" : "Editar Promoción", ModalityType.APPLICATION_MODAL);
        this.client = client;
        this.promotion = promotion;
        
        initComponents();
        layoutComponents();
        attachListeners();
        loadStores();
        
        if (promotion != null) {
            populateFields();
        }
        
        pack();
        setResizable(false);
    }
    
    private void initComponents() {
        nameField = new JTextField(30);
        
        typeCombo = new JComboBox<>(new String[]{
            "PERCENTAGE",
            "FIXED_AMOUNT",
            "BUY_X_GET_Y"
        });
        
        valueField = new JTextField(15);
        minPurchaseField = new JTextField(15);
        
        // Spinners de fecha
        SpinnerDateModel startModel = new SpinnerDateModel();
        startDateSpinner = new JSpinner(startModel);
        JSpinner.DateEditor startEditor = new JSpinner.DateEditor(startDateSpinner, "dd/MM/yyyy");
        startDateSpinner.setEditor(startEditor);
        
        SpinnerDateModel endModel = new SpinnerDateModel();
        endDateSpinner = new JSpinner(endModel);
        JSpinner.DateEditor endEditor = new JSpinner.DateEditor(endDateSpinner, "dd/MM/yyyy");
        endDateSpinner.setEditor(endEditor);
        
        storeCombo = new JComboBox<>();
        storeCombo.addItem("Todas");
        
        activeCheckbox = new JCheckBox("Activa");
        activeCheckbox.setSelected(true);
        
        saveButton = new JButton("Guardar");
        cancelButton = new JButton("Cancelar");
    }
    
    private void layoutComponents() {
        JPanel contentPanel = new JPanel(new GridBagLayout());
        contentPanel.setBorder(BorderFactory.createEmptyBorder(15, 15, 15, 15));
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Nombre
        gbc.gridx = 0;
        gbc.gridy = 0;
        contentPanel.add(new JLabel("Nombre:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(nameField, gbc);
        
        // Tipo
        gbc.gridx = 0;
        gbc.gridy = 1;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Tipo:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(typeCombo, gbc);
        
        // Valor
        gbc.gridx = 0;
        gbc.gridy = 2;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Valor:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(valueField, gbc);
        
        // Monto mínimo
        gbc.gridx = 0;
        gbc.gridy = 3;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Monto Mínimo (S/.):"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(minPurchaseField, gbc);
        
        // Fecha inicio
        gbc.gridx = 0;
        gbc.gridy = 4;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Fecha Inicio:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(startDateSpinner, gbc);
        
        // Fecha fin
        gbc.gridx = 0;
        gbc.gridy = 5;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Fecha Fin:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(endDateSpinner, gbc);
        
        // Tienda
        gbc.gridx = 0;
        gbc.gridy = 6;
        gbc.gridwidth = 1;
        contentPanel.add(new JLabel("Tienda:"), gbc);
        gbc.gridx = 1;
        gbc.gridwidth = 2;
        contentPanel.add(storeCombo, gbc);
        
        // Activa
        gbc.gridx = 0;
        gbc.gridy = 7;
        gbc.gridwidth = 3;
        contentPanel.add(activeCheckbox, gbc);
        
        add(contentPanel, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        buttonPanel.add(saveButton);
        buttonPanel.add(cancelButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    private void attachListeners() {
        saveButton.addActionListener(e -> save());
        cancelButton.addActionListener(e -> dispose());
    }
    
    private void loadStores() {
        SwingWorkerTask.execute(
            () -> client.getStores(),
            this::populateStoreCombo,
            ex -> {
                // Error no crítico, continuar con "Todas"
            },
            null,
            null
        );
    }
    
    private void populateStoreCombo(List<StoreDto> stores) {
        for (StoreDto store : stores) {
            storeCombo.addItem(store.getName());
        }
    }
    
    private void populateFields() {
        nameField.setText(promotion.getName());
        typeCombo.setSelectedItem(promotion.getType());
        valueField.setText(promotion.getValue().toString());
        minPurchaseField.setText(promotion.getMinPurchase().toString());
        
        // Fechas
        Date startDate = Date.from(promotion.getStartDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endDate = Date.from(promotion.getEndDate().atStartOfDay(ZoneId.systemDefault()).toInstant());
        startDateSpinner.setValue(startDate);
        endDateSpinner.setValue(endDate);
        
        // Tienda
        if (promotion.getScope() != null) {
            storeCombo.setSelectedItem(promotion.getScope());
        }
        
        activeCheckbox.setSelected(promotion.isActive());
    }
    
    private void save() {
        // Validaciones
        String name = nameField.getText().trim();
        String type = (String) typeCombo.getSelectedItem();
        String valueText = valueField.getText().trim();
        String minPurchaseText = minPurchaseField.getText().trim();
        String scope = storeCombo.getSelectedIndex() == 0 ? null : (String) storeCombo.getSelectedItem();
        boolean active = activeCheckbox.isSelected();
        
        if (name.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El nombre es obligatorio", "Validación", JOptionPane.WARNING_MESSAGE);
            nameField.requestFocus();
            return;
        }
        
        if (valueText.isEmpty()) {
            JOptionPane.showMessageDialog(this, "El valor es obligatorio", "Validación", JOptionPane.WARNING_MESSAGE);
            valueField.requestFocus();
            return;
        }
        
        if (minPurchaseText.isEmpty()) {
            minPurchaseText = "0";
        }
        
        BigDecimal value;
        BigDecimal minPurchase;
        try {
            value = new BigDecimal(valueText);
            minPurchase = new BigDecimal(minPurchaseText);
        } catch (NumberFormatException e) {
            JOptionPane.showMessageDialog(this, "Valor y monto mínimo deben ser numéricos", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Fechas
        Date startDate = (Date) startDateSpinner.getValue();
        Date endDate = (Date) endDateSpinner.getValue();
        
        LocalDate startLocalDate = startDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        LocalDate endLocalDate = endDate.toInstant().atZone(ZoneId.systemDefault()).toLocalDate();
        
        if (endLocalDate.isBefore(startLocalDate)) {
            JOptionPane.showMessageDialog(this, "La fecha de fin debe ser posterior a la fecha de inicio", "Validación", JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Crear DTO
        PromotionDto dto = new PromotionDto();
        if (promotion != null) {
            dto.setPromoId(promotion.getPromoId());
        }
        dto.setName(name);
        dto.setType(type);
        dto.setValue(value);
        dto.setMinPurchase(minPurchase);
        dto.setStartDate(startLocalDate);
        dto.setEndDate(endLocalDate);
        dto.setScope(scope);
        dto.setActive(active);
        
        // Guardar
        SwingWorkerTask.execute(
            () -> {
                if (promotion == null) {
                    client.createPromotion(dto);
                } else {
                    client.updatePromotion(dto);
                }
                return null;
            },
            result -> {
                saved = true;
                dispose();
            },
            ex -> {
                JOptionPane.showMessageDialog(
                    this,
                    "Error al guardar la promoción: " + ex.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE
                );
            },
            saveButton,
            null
        );
    }
    
    public boolean isSaved() {
        return saved;
    }
}
