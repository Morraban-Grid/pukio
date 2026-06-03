package com.pukio.posclient.ui.sale;

import com.pukio.posclient.client.AppServerClient;
import com.pukio.posclient.dto.PaymentRequest;
import com.pukio.posclient.dto.ProductResponse;
import com.pukio.posclient.dto.SaleRequest;
import com.pukio.posclient.dto.SaleResponse;
import com.pukio.posclient.ui.MainFrame;
import com.pukio.posclient.ui.common.SwingWorkerTask;

import javax.swing.*;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;
import javax.swing.event.TableModelEvent;
import javax.swing.table.TableCellEditor;
import javax.swing.table.TableCellRenderer;
import java.awt.*;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

/**
 * Panel principal de venta activa.
 * Permite agregar productos, gestionar ítems y procesar el cobro.
 */
public class SalePanel extends JPanel {
    
    private final MainFrame mainFrame;
    private final AppServerClient appServerClient;
    
    // Componentes del panel izquierdo
    private final JTextField skuInput;
    private final JButton addButton;
    private final JTable itemsTable;
    private final SaleItemTableModel tableModel;
    
    // Componentes del panel derecho
    private final JLabel subtotalLabel;
    private final JLabel discountLabel;
    private final JLabel taxLabel;
    private final JLabel totalLabel;
    private final JComboBox<String> paymentMethodCombo;
    private final JTextField amountField;
    private final JLabel changeLabel;
    private final JButton splitPaymentButton;
    private final JButton cobrarButton;
    private final JButton cancelButton;
    
    // Estado
    private List<PaymentRequest> splitPayments = null;
    
    private static final String[] PAYMENT_METHODS = {
        "Efectivo",
        "Tarjeta Crédito",
        "Tarjeta Débito",
        "Transferencia",
        "Billetera Digital"
    };
    
    public SalePanel(MainFrame mainFrame, AppServerClient appServerClient) {
        this.mainFrame = mainFrame;
        this.appServerClient = appServerClient;
        this.tableModel = new SaleItemTableModel();
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(10, 10, 10, 10));
        
        // SplitPane horizontal (60/40)
        JSplitPane splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT);
        splitPane.setDividerLocation(0.6);
        splitPane.setResizeWeight(0.6);
        
        // ========== Panel Izquierdo ==========
        JPanel leftPanel = new JPanel(new BorderLayout(5, 5));
        
        // Panel de entrada SKU
        JPanel skuPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        skuPanel.add(new JLabel("SKU:"));
        skuInput = new JTextField(15);
        skuInput.addActionListener(e -> addProduct());
        skuPanel.add(skuInput);
        addButton = new JButton("Agregar");
        addButton.addActionListener(e -> addProduct());
        skuPanel.add(addButton);
        leftPanel.add(skuPanel, BorderLayout.NORTH);
        
        // Tabla de ítems
        itemsTable = new JTable(tableModel);
        itemsTable.setRowHeight(30);
        itemsTable.setSelectionMode(ListSelectionModel.SINGLE_SELECTION);
        
        // Configurar la columna de botón Eliminar
        itemsTable.getColumn("").setCellRenderer(new ButtonRenderer());
        itemsTable.getColumn("").setCellEditor(new ButtonEditor(new JCheckBox()));
        
        JScrollPane tableScroll = new JScrollPane(itemsTable);
        leftPanel.add(tableScroll, BorderLayout.CENTER);
        
        // Botón Cancelar Venta
        JPanel leftButtonPanel = new JPanel(new FlowLayout(FlowLayout.LEFT));
        cancelButton = new JButton("Cancelar Venta");
        cancelButton.addActionListener(e -> cancelSale());
        leftButtonPanel.add(cancelButton);
        leftPanel.add(leftButtonPanel, BorderLayout.SOUTH);
        
        splitPane.setLeftComponent(leftPanel);
        
        // ========== Panel Derecho ==========
        JPanel rightPanel = new JPanel(new BorderLayout(5, 5));
        rightPanel.setBorder(BorderFactory.createEmptyBorder(5, 10, 5, 10));
        
        // Panel de totales
        JPanel totalsPanel = new JPanel();
        totalsPanel.setLayout(new GridBagLayout());
        GridBagConstraints gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.anchor = GridBagConstraints.WEST;
        
        Font labelFont = new Font("Arial", Font.PLAIN, 14);
        Font totalFont = new Font("Arial", Font.BOLD, 16);
        
        gbc.gridx = 0; gbc.gridy = 0;
        totalsPanel.add(new JLabel("Subtotal:"), gbc);
        gbc.gridx = 1;
        subtotalLabel = new JLabel("S/. 0.00");
        subtotalLabel.setFont(labelFont);
        totalsPanel.add(subtotalLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        totalsPanel.add(new JLabel("Descuento:"), gbc);
        gbc.gridx = 1;
        discountLabel = new JLabel("S/. 0.00");
        discountLabel.setFont(labelFont);
        totalsPanel.add(discountLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        totalsPanel.add(new JLabel("IGV (18%):"), gbc);
        gbc.gridx = 1;
        taxLabel = new JLabel("S/. 0.00");
        taxLabel.setFont(labelFont);
        totalsPanel.add(taxLabel, gbc);
        
        gbc.gridx = 0; gbc.gridy = 3;
        gbc.gridwidth = 2;
        totalsPanel.add(new JSeparator(), gbc);
        
        gbc.gridx = 0; gbc.gridy = 4; gbc.gridwidth = 1;
        JLabel totalTitleLabel = new JLabel("TOTAL:");
        totalTitleLabel.setFont(totalFont);
        totalsPanel.add(totalTitleLabel, gbc);
        gbc.gridx = 1;
        totalLabel = new JLabel("S/. 0.00");
        totalLabel.setFont(totalFont);
        totalLabel.setForeground(new Color(0, 100, 0));
        totalsPanel.add(totalLabel, gbc);
        
        rightPanel.add(totalsPanel, BorderLayout.NORTH);
        
        // Panel de pago
        JPanel paymentPanel = new JPanel();
        paymentPanel.setLayout(new GridBagLayout());
        paymentPanel.setBorder(BorderFactory.createTitledBorder("Pago"));
        
        gbc = new GridBagConstraints();
        gbc.insets = new Insets(5, 5, 5, 5);
        gbc.fill = GridBagConstraints.HORIZONTAL;
        
        gbc.gridx = 0; gbc.gridy = 0;
        paymentPanel.add(new JLabel("Método:"), gbc);
        gbc.gridx = 1;
        paymentMethodCombo = new JComboBox<>(PAYMENT_METHODS);
        paymentMethodCombo.addActionListener(e -> updateChangeCalculation());
        paymentPanel.add(paymentMethodCombo, gbc);
        
        gbc.gridx = 0; gbc.gridy = 1;
        paymentPanel.add(new JLabel("Monto:"), gbc);
        gbc.gridx = 1;
        amountField = new JTextField();
        amountField.getDocument().addDocumentListener(new DocumentListener() {
            @Override
            public void insertUpdate(DocumentEvent e) { updateChangeCalculation(); }
            @Override
            public void removeUpdate(DocumentEvent e) { updateChangeCalculation(); }
            @Override
            public void changedUpdate(DocumentEvent e) { updateChangeCalculation(); }
        });
        paymentPanel.add(amountField, gbc);
        
        gbc.gridx = 0; gbc.gridy = 2;
        paymentPanel.add(new JLabel("Vuelto:"), gbc);
        gbc.gridx = 1;
        changeLabel = new JLabel("S/. 0.00");
        changeLabel.setFont(new Font("Arial", Font.BOLD, 14));
        changeLabel.setForeground(new Color(0, 100, 0));
        paymentPanel.add(changeLabel, gbc);
        
        rightPanel.add(paymentPanel, BorderLayout.CENTER);
        
        // Panel de botones de acción
        JPanel actionPanel = new JPanel(new GridLayout(2, 1, 5, 5));
        
        splitPaymentButton = new JButton("Pago Dividido");
        splitPaymentButton.addActionListener(e -> openSplitPaymentDialog());
        actionPanel.add(splitPaymentButton);
        
        cobrarButton = new JButton("Cobrar");
        cobrarButton.setBackground(new Color(0, 128, 0));
        cobrarButton.setForeground(Color.WHITE);
        cobrarButton.setFont(new Font("Arial", Font.BOLD, 16));
        cobrarButton.addActionListener(e -> processSale());
        actionPanel.add(cobrarButton);
        
        rightPanel.add(actionPanel, BorderLayout.SOUTH);
        
        splitPane.setRightComponent(rightPanel);
        
        add(splitPane, BorderLayout.CENTER);
        
        // Listener para actualizar totales cuando cambia la tabla
        tableModel.addTableModelListener(e -> updateTotals());
    }
    
    /**
     * Agrega un producto a la venta.
     */
    private void addProduct() {
        String sku = skuInput.getText().trim();
        if (sku.isEmpty()) {
            JOptionPane.showMessageDialog(this,
                "Por favor ingrese un SKU",
                "SKU Requerido",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Deshabilitar botón y campo durante la consulta
        addButton.setEnabled(false);
        skuInput.setEnabled(false);
        
        SwingWorkerTask.execute(
            () -> appServerClient.getProductBySku(sku),
            product -> {
                if (product == null) {
                    JOptionPane.showMessageDialog(this,
                        "Producto no encontrado: " + sku,
                        "Error",
                        JOptionPane.ERROR_MESSAGE);
                } else if (product.getStock() <= 0) {
                    JOptionPane.showMessageDialog(this,
                        "Producto sin stock: " + product.getName(),
                        "Sin Stock",
                        JOptionPane.WARNING_MESSAGE);
                } else {
                    tableModel.addItem(product, 1);
                    skuInput.setText("");
                }
                addButton.setEnabled(true);
                skuInput.setEnabled(true);
                skuInput.requestFocus();
            },
            error -> {
                JOptionPane.showMessageDialog(this,
                    "Error al consultar producto: " + error.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                addButton.setEnabled(true);
                skuInput.setEnabled(true);
            },
            addButton,
            null
        );
    }
    
    /**
     * Actualiza los totales mostrados.
     */
    private void updateTotals() {
        SaleItemTableModel.SaleTotals totals = tableModel.getTotals();
        
        subtotalLabel.setText("S/. " + totals.getSubtotal().setScale(2, RoundingMode.HALF_UP).toPlainString());
        discountLabel.setText("S/. " + totals.getDiscountTotal().setScale(2, RoundingMode.HALF_UP).toPlainString());
        taxLabel.setText("S/. " + totals.getTaxTotal().setScale(2, RoundingMode.HALF_UP).toPlainString());
        totalLabel.setText("S/. " + totals.getGrandTotal().setScale(2, RoundingMode.HALF_UP).toPlainString());
        
        updateChangeCalculation();
    }
    
    /**
     * Actualiza el cálculo del vuelto.
     */
    private void updateChangeCalculation() {
        String method = (String) paymentMethodCombo.getSelectedItem();
        if (!"Efectivo".equals(method)) {
            changeLabel.setText("S/. 0.00");
            return;
        }
        
        try {
            String amountText = amountField.getText().trim();
            if (amountText.isEmpty()) {
                changeLabel.setText("S/. 0.00");
                return;
            }
            
            BigDecimal amountTendered = new BigDecimal(amountText);
            BigDecimal grandTotal = tableModel.getTotals().getGrandTotal();
            BigDecimal change = amountTendered.subtract(grandTotal);
            
            if (change.compareTo(BigDecimal.ZERO) >= 0) {
                changeLabel.setText("S/. " + change.setScale(2, RoundingMode.HALF_UP).toPlainString());
                changeLabel.setForeground(new Color(0, 100, 0));
            } else {
                changeLabel.setText("S/. " + change.abs().setScale(2, RoundingMode.HALF_UP).toPlainString() + " falta");
                changeLabel.setForeground(Color.RED);
            }
        } catch (NumberFormatException e) {
            changeLabel.setText("S/. 0.00");
        }
    }
    
    /**
     * Abre el diálogo de pago dividido.
     */
    private void openSplitPaymentDialog() {
        BigDecimal grandTotal = tableModel.getTotals().getGrandTotal();
        
        if (grandTotal.compareTo(BigDecimal.ZERO) <= 0) {
            JOptionPane.showMessageDialog(this,
                "No hay ítems en la venta",
                "Venta Vacía",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        List<PaymentRequest> payments = SplitPaymentDialog.showDialog(
            (Frame) SwingUtilities.getWindowAncestor(this),
            grandTotal
        );
        
        if (payments != null) {
            splitPayments = payments;
            JOptionPane.showMessageDialog(this,
                "Pago dividido configurado (" + payments.size() + " métodos)",
                "Pago Dividido",
                JOptionPane.INFORMATION_MESSAGE);
        }
    }
    
    /**
     * Procesa la venta.
     */
    private void processSale() {
        if (tableModel.getRowCount() == 0) {
            JOptionPane.showMessageDialog(this,
                "No hay ítems en la venta",
                "Venta Vacía",
                JOptionPane.WARNING_MESSAGE);
            return;
        }
        
        // Preparar request
        SaleRequest request = new SaleRequest();
        
        // Construir lista de ítems
        List<SaleRequest.SaleItemRequest> items = new ArrayList<>();
        for (var row : tableModel.getItems()) {
            SaleRequest.SaleItemRequest item = new SaleRequest.SaleItemRequest();
            item.setSku(row.getSku());
            item.setQuantity(row.getQuantity());
            items.add(item);
        }
        request.setItems(items);
        
        // Configurar pagos
        if (splitPayments != null) {
            request.setPayments(splitPayments);
        } else {
            String method = (String) paymentMethodCombo.getSelectedItem();
            BigDecimal grandTotal = tableModel.getTotals().getGrandTotal();
            
            BigDecimal amountTendered = grandTotal;
            try {
                String amountText = amountField.getText().trim();
                if (!amountText.isEmpty()) {
                    amountTendered = new BigDecimal(amountText);
                }
            } catch (NumberFormatException e) {
                // Usar el total si el monto no es válido
            }
            
            List<PaymentRequest> singlePayment = new ArrayList<>();
            singlePayment.add(new PaymentRequest(method, amountTendered));
            request.setPayments(singlePayment);
        }
        
        // Deshabilitar botón
        cobrarButton.setEnabled(false);
        
        // Enviar al servidor
        SwingWorkerTask.execute(
            () -> appServerClient.processSale(request),
            response -> {
                // Mostrar recibo
                ReceiptPanel receiptPanel = mainFrame.getReceiptPanel();
                if (receiptPanel != null) {
                    receiptPanel.loadReceipt(response);
                    mainFrame.showPanel("receipt");
                }
                cobrarButton.setEnabled(true);
            },
            error -> {
                JOptionPane.showMessageDialog(this,
                    "Error al procesar venta: " + error.getMessage(),
                    "Error",
                    JOptionPane.ERROR_MESSAGE);
                cobrarButton.setEnabled(true);
            },
            cobrarButton,
            null
        );
    }
    
    /**
     * Cancela la venta actual.
     */
    private void cancelSale() {
        if (tableModel.getRowCount() == 0) {
            return;
        }
        
        int confirm = JOptionPane.showConfirmDialog(this,
            "¿Está seguro que desea cancelar la venta actual?",
            "Cancelar Venta",
            JOptionPane.YES_NO_OPTION,
            JOptionPane.WARNING_MESSAGE);
        
        if (confirm == JOptionPane.YES_OPTION) {
            clearSale();
        }
    }
    
    /**
     * Limpia la venta actual.
     */
    public void clearSale() {
        tableModel.clear();
        skuInput.setText("");
        amountField.setText("");
        splitPayments = null;
        updateTotals();
        skuInput.requestFocus();
    }
    
    /**
     * Pone el foco en el campo SKU (llamado por atajo F2).
     */
    public void focusSku() {
        skuInput.requestFocus();
        skuInput.selectAll();
    }
    
    /**
     * Refresca el panel (limpia si es necesario).
     */
    public void refresh() {
        // Opcionalmente limpiar si se refresca
    }
    
    // ==================== Button Renderer/Editor ====================
    
    /**
     * Renderer para la columna de botón Eliminar.
     */
    class ButtonRenderer extends JButton implements TableCellRenderer {
        public ButtonRenderer() {
            setOpaque(true);
        }
        
        @Override
        public Component getTableCellRendererComponent(JTable table, Object value,
                                                       boolean isSelected, boolean hasFocus,
                                                       int row, int column) {
            setText((value == null) ? "Eliminar" : value.toString());
            return this;
        }
    }
    
    /**
     * Editor para la columna de botón Eliminar.
     */
    class ButtonEditor extends DefaultCellEditor {
        private JButton button;
        private String label;
        private boolean clicked;
        private int row;
        
        public ButtonEditor(JCheckBox checkBox) {
            super(checkBox);
            button = new JButton();
            button.setOpaque(true);
            button.addActionListener(e -> fireEditingStopped());
        }
        
        @Override
        public Component getTableCellEditorComponent(JTable table, Object value,
                                                     boolean isSelected, int row, int column) {
            this.row = row;
            label = (value == null) ? "Eliminar" : value.toString();
            button.setText(label);
            clicked = true;
            return button;
        }
        
        @Override
        public Object getCellEditorValue() {
            if (clicked) {
                tableModel.removeItem(row);
            }
            clicked = false;
            return label;
        }
        
        @Override
        public boolean stopCellEditing() {
            clicked = false;
            return super.stopCellEditing();
        }
    }
}
