package com.pukio.pos.service;

import com.pukio.common.model.LineItem;
import com.pukio.common.model.SaleRecord;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;

/**
 * Generates formatted sale receipts for console and Swing UI. (TASK-E1-26)
 */
@Component
public class ReceiptPrinter {

    private static final DateTimeFormatter FORMATTER =
            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    /**
     * Generate receipt as a formatted String (usable in console or Swing JTextArea).
     */
    public String generateReceipt(SaleRecord sale) {
        StringBuilder sb = new StringBuilder();
        sb.append("\n================================================\n");
        sb.append("             PUKIO — RECIBO DE VENTA           \n");
        sb.append("================================================\n");
        sb.append(String.format("ID Transacción : %s%n", sale.getTransactionId()));
        sb.append(String.format("Fecha/Hora     : %s%n",
                sale.getTimestamp().format(FORMATTER)));
        sb.append(String.format("Método de Pago : %s%n", sale.getPaymentMethod()));
        sb.append("------------------------------------------------\n");
        sb.append(String.format("%-20s %5s %10s %10s%n",
                "Producto", "Cant.", "P.Unit.", "Subtotal"));
        sb.append("------------------------------------------------\n");

        for (LineItem item : sale.getItems()) {
            sb.append(String.format("%-20s %5d %10s %10s%n",
                    item.getProductName(),
                    item.getQuantity(),
                    "S/ " + item.getUnitPrice(),
                    "S/ " + item.getSubtotal()));
        }

        sb.append("================================================\n");
        sb.append(String.format("TOTAL          :          S/ %s%n", sale.getTotal()));
        sb.append("================================================\n");
        sb.append("         ¡Gracias por su compra!               \n");
        sb.append("================================================\n");

        return sb.toString();
    }

    /**
     * Print receipt directly to console output.
     */
    public void printToConsole(SaleRecord sale) {
        System.out.println(generateReceipt(sale));
    }
}
