package com.pukio.posclient.ui.sale;

import com.pukio.posclient.dto.SaleResponse;
import com.pukio.posclient.ui.MainFrame;

import javax.swing.*;
import java.awt.*;
import java.awt.print.PageFormat;
import java.awt.print.Printable;
import java.awt.print.PrinterException;
import java.awt.print.PrinterJob;
import java.time.format.DateTimeFormatter;

/**
 * Panel que muestra el recibo de una venta completada.
 * Permite imprimir el recibo y regresar a una nueva venta.
 */
public class ReceiptPanel extends JPanel {
    
    private final MainFrame mainFrame;
    private final JTextArea receiptArea;
    private SaleResponse currentReceipt;
    
    private static final DateTimeFormatter DATE_FORMATTER = 
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    public ReceiptPanel(MainFrame mainFrame) {
        this.mainFrame = mainFrame;
        
        setLayout(new BorderLayout(10, 10));
        setBorder(BorderFactory.createEmptyBorder(20, 20, 20, 20));
        
        // Título
        JLabel titleLabel = new JLabel("Recibo de Venta", SwingConstants.CENTER);
        titleLabel.setFont(new Font("Arial", Font.BOLD, 20));
        add(titleLabel, BorderLayout.NORTH);
        
        // Área de texto del recibo
        receiptArea = new JTextArea();
        receiptArea.setEditable(false);
        receiptArea.setFont(new Font(Font.MONOSPACED, Font.PLAIN, 12));
        receiptArea.setBorder(BorderFactory.createLineBorder(Color.GRAY));
        
        JScrollPane scrollPane = new JScrollPane(receiptArea);
        add(scrollPane, BorderLayout.CENTER);
        
        // Panel de botones
        JPanel buttonPanel = new JPanel(new FlowLayout(FlowLayout.CENTER, 20, 10));
        
        JButton printButton = new JButton("Imprimir");
        printButton.setPreferredSize(new Dimension(150, 40));
        printButton.addActionListener(e -> printReceipt());
        
        JButton newSaleButton = new JButton("Nueva Venta");
        newSaleButton.setPreferredSize(new Dimension(150, 40));
        newSaleButton.addActionListener(e -> startNewSale());
        
        buttonPanel.add(printButton);
        buttonPanel.add(newSaleButton);
        add(buttonPanel, BorderLayout.SOUTH);
    }
    
    /**
     * Carga y muestra un recibo.
     */
    public void loadReceipt(SaleResponse saleResponse) {
        this.currentReceipt = saleResponse;
        
        StringBuilder receipt = new StringBuilder();
        
        // Encabezado
        receipt.append("===============================================\n");
        receipt.append("           SISTEMA POS PUKIO\n");
        receipt.append("===============================================\n");
        receipt.append("\n");
        receipt.append("Tienda:       ").append(saleResponse.getStoreName() != null 
            ? saleResponse.getStoreName() : "Lima Norte").append("\n");
        receipt.append("Dirección:    Av. Principal 123, Lima\n");
        receipt.append("Cajero:       ").append(saleResponse.getCashierName() != null 
            ? saleResponse.getCashierName() : "Sistema").append("\n");
        receipt.append("Fecha:        ").append(saleResponse.getSaleDate() != null 
            ? saleResponse.getSaleDate().format(DATE_FORMATTER) 
            : "N/A").append("\n");
        receipt.append("Transacción:  ").append(saleResponse.getTransactionId()).append("\n");
        receipt.append("-----------------------------------------------\n");
        receipt.append("\n");
        
        // Ítems
        receipt.append("ÍTEM                  CANT   P.UNIT   SUBTOTAL\n");
        receipt.append("-----------------------------------------------\n");
        
        if (saleResponse.getItems() != null) {
            for (var item : saleResponse.getItems()) {
                String name = item.getName();
                if (name.length() > 20) {
                    name = name.substring(0, 17) + "...";
                }
                
                receipt.append(String.format("%-20s %4d %8.2f %10.2f\n",
                    name,
                    item.getQuantity(),
                    item.getUnitPrice(),
                    item.getLineTotal()));
                
                // Mostrar descuento si existe
                if (item.getDiscountAmount() != null && 
                    item.getDiscountAmount().compareTo(java.math.BigDecimal.ZERO) > 0) {
                    receipt.append(String.format("  Descuento: -S/. %.2f\n", 
                        item.getDiscountAmount()));
                }
            }
        }
        
        receipt.append("-----------------------------------------------\n");
        receipt.append("\n");
        
        // Totales
        receipt.append(String.format("Subtotal:              S/. %10.2f\n", 
            saleResponse.getSubtotal()));
        
        if (saleResponse.getDiscountTotal() != null && 
            saleResponse.getDiscountTotal().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Descuento:            -S/. %10.2f\n", 
                saleResponse.getDiscountTotal()));
        }
        
        receipt.append(String.format("IGV (18%%):             S/. %10.2f\n", 
            saleResponse.getTaxTotal()));
        receipt.append("-----------------------------------------------\n");
        receipt.append(String.format("TOTAL:                 S/. %10.2f\n", 
            saleResponse.getGrandTotal()));
        receipt.append("-----------------------------------------------\n");
        receipt.append("\n");
        
        // Métodos de pago
        if (saleResponse.getPayments() != null && !saleResponse.getPayments().isEmpty()) {
            receipt.append("MÉTODOS DE PAGO:\n");
            for (var payment : saleResponse.getPayments()) {
                receipt.append(String.format("  %-20s  S/. %8.2f\n",
                    payment.getMethod(),
                    payment.getAmount()));
            }
            receipt.append("\n");
        }
        
        // Vuelto (si es pago en efectivo)
        if (saleResponse.getChange() != null && 
            saleResponse.getChange().compareTo(java.math.BigDecimal.ZERO) > 0) {
            receipt.append(String.format("Pagado:                S/. %10.2f\n", 
                saleResponse.getGrandTotal().add(saleResponse.getChange())));
            receipt.append(String.format("Vuelto:                S/. %10.2f\n", 
                saleResponse.getChange()));
            receipt.append("\n");
        }
        
        // Pie de página
        receipt.append("===============================================\n");
        receipt.append("      ¡GRACIAS POR SU COMPRA!\n");
        receipt.append("     www.pukio.com - 555-1234\n");
        receipt.append("===============================================\n");
        
        receiptArea.setText(receipt.toString());
        receiptArea.setCaretPosition(0); // Scroll al inicio
    }
    
    /**
     * Imprime el recibo usando la impresora por defecto.
     */
    private void printReceipt() {
        PrinterJob job = PrinterJob.getPrinterJob();
        
        job.setPrintable((graphics, pageFormat, pageIndex) -> {
            if (pageIndex > 0) {
                return Printable.NO_SUCH_PAGE;
            }
            
            Graphics2D g2d = (Graphics2D) graphics;
            g2d.translate(pageFormat.getImageableX(), pageFormat.getImageableY());
            
            // Configurar fuente
            Font font = new Font(Font.MONOSPACED, Font.PLAIN, 10);
            g2d.setFont(font);
            
            // Dibujar el texto línea por línea
            String[] lines = receiptArea.getText().split("\n");
            int y = 20;
            int lineHeight = g2d.getFontMetrics().getHeight();
            
            for (String line : lines) {
                g2d.drawString(line, 10, y);
                y += lineHeight;
            }
            
            return Printable.PAGE_EXISTS;
        });
        
        boolean doPrint = job.printDialog();
        if (doPrint) {
            try {
                job.print();
                JOptionPane.showMessageDialog(this,
                    "Recibo enviado a impresora",
                    "Impresión",
                    JOptionPane.INFORMATION_MESSAGE);
            } catch (PrinterException e) {
                JOptionPane.showMessageDialog(this,
                    "Error al imprimir: " + e.getMessage(),
                    "Error de Impresión",
                    JOptionPane.ERROR_MESSAGE);
            }
        }
    }
    
    /**
     * Inicia una nueva venta limpiando el panel de venta y regresando a él.
     */
    private void startNewSale() {
        // Obtener el SalePanel y limpiarlo
        SalePanel salePanel = mainFrame.getSalePanel();
        if (salePanel != null) {
            salePanel.clearSale();
        }
        
        // Mostrar el panel de venta
        mainFrame.showPanel("sale");
    }
    
    /**
     * Método público para refrescar (no hace nada en este panel).
     */
    public void refresh() {
        // El recibo no necesita refrescarse
    }
}
