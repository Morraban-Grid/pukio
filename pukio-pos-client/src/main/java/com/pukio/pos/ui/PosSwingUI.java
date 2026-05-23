package com.pukio.pos.ui;

import com.pukio.common.model.LineItem;
import com.pukio.common.model.PaymentMethod;
import com.pukio.common.model.SaleRecord;
import com.pukio.pos.service.ReceiptPrinter;
import com.pukio.pos.service.SaleService;
import lombok.extern.slf4j.Slf4j;

import javax.swing.*;
import javax.swing.table.DefaultTableModel;
import java.awt.*;
import java.util.List;

/**
 * Basic Swing UI for the POS Client terminal. (TASK-E1-24)
 * Allows cashiers to add items to cart, process sales, and view receipts.
 */
@Slf4j
public class PosSwingUI extends JFrame {

    private final SaleService saleService;
    private final ReceiptPrinter receiptPrinter;

    // Cart table
    private DefaultTableModel cartTableModel;

    // Input fields
    private JTextField txtSku;
    private JTextField txtQuantity;
    private JComboBox<PaymentMethod> cmbPaymentMethod;

    // Receipt area
    private JTextArea txtReceipt;

    // Total label
    private JLabel lblTotal;

    public PosSwingUI(SaleService saleService, ReceiptPrinter receiptPrinter) {
        this.saleService = saleService;
        this.receiptPrinter = receiptPrinter;
        initUI();
    }

    private void initUI() {
        setTitle("Pukio — Terminal de Ventas");
        setSize(900, 650);
        setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
        setLocationRelativeTo(null);
        setLayout(new BorderLayout(10, 10));

        add(buildTopPanel(), BorderLayout.NORTH);
        add(buildCenterPanel(), BorderLayout.CENTER);
        add(buildBottomPanel(), BorderLayout.SOUTH);

        setVisible(true);
    }

    // -------------------------------------------------------------------------
    // Top panel — input form
    // -------------------------------------------------------------------------
    private JPanel buildTopPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.LEFT, 10, 10));
        panel.setBorder(BorderFactory.createTitledBorder("Agregar Producto"));

        txtSku = new JTextField(10);
        txtQuantity = new JTextField(5);
        cmbPaymentMethod = new JComboBox<>(PaymentMethod.values());

        JButton btnAdd = new JButton("Agregar al carrito");
        JButton btnCancel = new JButton("Cancelar venta");

        panel.add(new JLabel("SKU:"));
        panel.add(txtSku);
        panel.add(new JLabel("Cantidad:"));
        panel.add(txtQuantity);
        panel.add(btnAdd);
        panel.add(new JLabel("  |  Método de pago:"));
        panel.add(cmbPaymentMethod);
        panel.add(btnCancel);

        btnAdd.addActionListener(e -> {
            try {
                String sku = txtSku.getText().trim();
                int qty = Integer.parseInt(txtQuantity.getText().trim());
                LineItem item = saleService.addToCart(sku, qty);
                addItemToCartTable(item);
                updateTotal();
                txtSku.setText("");
                txtQuantity.setText("");
                txtSku.requestFocus();
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        btnCancel.addActionListener(e -> {
            saleService.cancelSale();
            cartTableModel.setRowCount(0);
            updateTotal();
            txtReceipt.setText("");
            showInfo("Venta cancelada.");
        });

        return panel;
    }

    // -------------------------------------------------------------------------
    // Center panel — cart table + receipt
    // -------------------------------------------------------------------------
    private JSplitPane buildCenterPanel() {
        // Cart table
        String[] columns = {"SKU", "Producto", "Cantidad", "P. Unit.", "Subtotal"};
        cartTableModel = new DefaultTableModel(columns, 0) {
            @Override public boolean isCellEditable(int r, int c) { return false; }
        };
        JTable cartTable = new JTable(cartTableModel);
        JScrollPane cartScroll = new JScrollPane(cartTable);
        cartScroll.setBorder(BorderFactory.createTitledBorder("Carrito de Venta"));

        // Receipt area
        txtReceipt = new JTextArea();
        txtReceipt.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        txtReceipt.setEditable(false);
        JScrollPane receiptScroll = new JScrollPane(txtReceipt);
        receiptScroll.setBorder(BorderFactory.createTitledBorder("Recibo"));

        JSplitPane split = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT,
                cartScroll, receiptScroll);
        split.setDividerLocation(500);
        return split;
    }

    // -------------------------------------------------------------------------
    // Bottom panel — total + process sale button
    // -------------------------------------------------------------------------
    private JPanel buildBottomPanel() {
        JPanel panel = new JPanel(new FlowLayout(FlowLayout.RIGHT, 15, 10));

        lblTotal = new JLabel("TOTAL: S/ 0.00");
        lblTotal.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 18));
        lblTotal.setForeground(new Color(0, 100, 0));

        JButton btnProcess = new JButton("✔ Procesar Venta");
        btnProcess.setFont(new Font(Font.SANS_SERIF, Font.BOLD, 14));
        btnProcess.setBackground(new Color(34, 139, 34));
        btnProcess.setForeground(Color.WHITE);

        panel.add(lblTotal);
        panel.add(btnProcess);

        btnProcess.addActionListener(e -> {
            try {
                PaymentMethod method =
                        (PaymentMethod) cmbPaymentMethod.getSelectedItem();
                SaleRecord sale = saleService.processSale(method);
                String receipt = receiptPrinter.generateReceipt(sale);
                txtReceipt.setText(receipt);
                receiptPrinter.printToConsole(sale);
                cartTableModel.setRowCount(0);
                updateTotal();
                showInfo("Venta procesada. ID: " + sale.getTransactionId());
            } catch (Exception ex) {
                showError(ex.getMessage());
            }
        });

        return panel;
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------
    private void addItemToCartTable(LineItem item) {
        cartTableModel.addRow(new Object[]{
                item.getSku(),
                item.getProductName(),
                item.getQuantity(),
                "S/ " + item.getUnitPrice(),
                "S/ " + item.getSubtotal()
        });
    }

    private void updateTotal() {
        List<LineItem> cart = saleService.getCurrentCart();
        java.math.BigDecimal total = cart.stream()
                .map(LineItem::getSubtotal)
                .reduce(java.math.BigDecimal.ZERO, java.math.BigDecimal::add);
        lblTotal.setText("TOTAL: S/ " + total.toPlainString());
    }

    private void showInfo(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Info",
                JOptionPane.INFORMATION_MESSAGE);
    }

    private void showError(String msg) {
        JOptionPane.showMessageDialog(this, msg, "Error",
                JOptionPane.ERROR_MESSAGE);
    }
}
