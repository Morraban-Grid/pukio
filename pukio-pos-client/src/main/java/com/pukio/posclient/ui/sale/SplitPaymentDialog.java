package com.pukio.posclient.ui.sale;

import com.pukio.posclient.dto.PaymentRequest;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import java.awt.*;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Diálogo modal para gestionar pago dividido (split payment).
 * Permite distribuir el total de la venta entre múltiples métodos de pago.
 */
public class SplitPaymentDialog extends JDialog {
    
    private final BigDecimal totalAmount;
    private final Map<String, JCheckBox> checkBoxes = new HashMap<>();
    private final Map<String, JTextField> amountFields = new HashMap<>();
    private final JLabel sumLabel;
    private final JButton acceptButton;
    
    private List<PaymentRequest> payments;
    private boolean accepted = false;
    
    private static final String[] PAYMENT_METHODS = {
        "Efectivo",
        "Tarjeta Crédito",
        "Tarjeta Débito",
        "Transferencia",
        "Billetera Digital"
    };
    
    public SplitPaymentDialog(Frame parent, BigDecimal totalAmount) {
        super(parent, "Pago Dividido", true);
        this.totalAmount = totalAmount;
        
        setLayout(new BorderLayout(10, 10));
        setSize(500, 350);
        setLocationRelativeTo(parent);
        setDefaultCloseOperation(DISPOSE_ON_CLOSE);
        
        // Panel central con los métodos de pago
        JPanel methodsPanel = new JPanel();
        methodsPanel.setLayout(new GridBagLayout());
        methodsPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        // Cabecera
        gbc.gridx = 0; gbc.gridy = 0; gbc.weightx = 0.3;
        methodsPanel.add(new JLabel("Método"), gbc);
        gbc.gridx = 1; gbc.weightx = 0.7;
        methodsPanel.add(new JLabel("Monto (S/.)"), gbc);
        
        // Crear filas para cada método de pago
        DocumentListener updateListener = new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateSum(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateSum(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateSum(); }
        };
        
        for (int i = 0; i < PAYMENT_METHODS.length; i++) {
            String method = PAYMENT_METHODS[i];
            
            gbc.gridy = i + 1;
            
            // Checkbox para habilitar/deshabilitar
            JCheckBox checkBox = new JCheckBox(method);
            gbc.gridx = 0; gbc.weightx = 0.3;
            methodsPanel.add(checkBox, gbc);
            checkBoxes.put(method, checkBox);
            
            // TextField para el monto
            JTextField amountField = new JTextField();
            amountField.setEnabled(false);
            amountField.setText("0.00");
            gbc.gridx = 1; gbc.weightx = 0.7;
            methodsPanel.add(amountField, gbc);
            amountFields.put(method, amountField);
            
            // Listener para habilitar/deshabilitar el campo
            checkBox.addActionListener(e -> {
                amountField.setEnabled(checkBox.isSelected());
                if (!checkBox.isSelected()) {
                    amountField.setText("0.00");
                }
                updateSum();
            });
            
            // Listener para actualizar la suma
            amountField.getDocument().addDocumentListener(updateListener);
        }
        
        JScrollPane scrollPane = new JScrollPane(methodsPanel);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel inferior con suma y botones
        JPanel bottomPanel = new JPanel(new BorderLayout());
        bottomPanel.setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // Panel de suma
        JPanel sumPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        sumPanel.add(new JLabel("Total a pagar: S/. " + totalAmount.toPlainString()));
        sumPanel.add(Box.createHorizontalStrut(20));
        sumLabel = new JLabel("Suma: S/. 0.00");
        sumLabel.setFont(sumLabel.getFont().deriveFont(Font.BOLD, 14f));
        sumPanel.add(sumLabel);
        bottomPanel.add(sumPanel, BorderLayout.NORTH);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.RIGHT));
        acceptButton = new JButton("Aceptar");
        acceptButton.setEnabled(false);
        acceptButton.addActionListener(e -> acceptPayment());
        
        JButton cancelButton = new JButton("Cancelar");
        cancelButton.addActionListener(e -> dispose());
        
        buttonPanel.add(cancelButton);
        buttonPanel.add(acceptButton);
        bottomPanel.add(buttonPanel, BorderLayout.SOUTH);
        
        add(bottomPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Actualiza la suma total y habilita/deshabilita el botón Aceptar.
     */
    private void updateSum() {
        BigDecimal sum = BigDecimal.ZERO;
        
        for (String method : PAYMENT_METHODS) {
            JCheckBox checkBox = checkBoxes.get(method);
            JTextField amountField = amountFields.get(method);
            
            if (checkBox.isSelected()) {
                try {
                    String text = amountField.getText().trim();
                    if (!text.isEmpty()) {
                        BigDecimal amount = new BigDecimal(text);
                        if (amount.compareTo(BigDecimal.ZERO) > 0) {
                            sum = sum.add(amount);
                        }
                    }
                } catch (NumberFormatException e) {
                    // Ignorar valores no numéricos
                }
            }
        }
        
        sumLabel.setText("Suma: S/. " + sum.toPlainString());
        
        // Habilitar botón solo si la suma es igual al total
        boolean isValid = sum.compareTo(totalAmount) == 0;
        acceptButton.setEnabled(isValid);
        
        // Colorear el label según el estado
        if (isValid) {
            sumLabel.setForeground(new Color(0, 128, 0)); // Verde
        } else if (sum.compareTo(totalAmount) > 0) {
            sumLabel.setForeground(Color.RED); // Rojo si excede
        } else {
            sumLabel.setForeground(Color.ORANGE); // Naranja si falta
        }
    }
    
    /**
     * Acepta el pago y construye la lista de PaymentRequest.
     */
    private void acceptPayment() {
        payments = new ArrayList<>();
        
        for (String method : PAYMENT_METHODS) {
            JCheckBox checkBox = checkBoxes.get(method);
            JTextField amountField = amountFields.get(method);
            
            if (checkBox.isSelected()) {
                try {
                    BigDecimal amount = new BigDecimal(amountField.getText().trim());
                    if (amount.compareTo(BigDecimal.ZERO) > 0) {
                        payments.add(new PaymentRequest(method, amount));
                    }
                } catch (NumberFormatException e) {
                    // Ya validado en updateSum(), no debería ocurrir
                }
            }
        }
        
        accepted = true;
        dispose();
    }
    
    /**
     * Retorna true si el usuario aceptó el diálogo.
     */
    public boolean isAccepted() {
        return accepted;
    }
    
    /**
     * Retorna la lista de pagos configurados.
     */
    public List<PaymentRequest> getPayments() {
        return payments != null ? new ArrayList<>(payments) : new ArrayList<>();
    }
    
    /**
     * Muestra el diálogo y retorna la lista de pagos si fue aceptado.
     * Retorna null si fue cancelado.
     */
    public static List<PaymentRequest> showDialog(Frame parent, BigDecimal totalAmount) {
        SplitPaymentDialog dialog = new SplitPaymentDialog(parent, totalAmount);
        dialog.setVisible(true);
        return dialog.isAccepted() ? dialog.getPayments() : null;
    }
}
