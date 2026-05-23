package com.pukio.pos;

import com.pukio.common.model.PaymentMethod;
import com.pukio.common.model.SaleRecord;
import com.pukio.pos.service.ReceiptPrinter;
import com.pukio.pos.service.SaleService;
import com.pukio.pos.ui.PosSwingUI;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import javax.swing.*;
import java.util.Scanner;

/**
 * POS Client application entry point.
 * Launches both the Swing UI and keeps a console menu available. (TASK-E1-24)
 */
@Slf4j
@SpringBootApplication
public class PosClientApplication {

    public static void main(String[] args) {
        SpringApplication.run(PosClientApplication.class, args);
    }

    @Bean
    public CommandLineRunner launchPos(SaleService saleService,
                                       ReceiptPrinter receiptPrinter) {
        return args -> {
            // Launch Swing UI on Event Dispatch Thread
            SwingUtilities.invokeLater(() ->
                    new PosSwingUI(saleService, receiptPrinter));

            // Console menu runs in parallel for terminal access
            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("\n========================================");
                System.out.println("  PUKIO — Terminal de Ventas (Consola)");
                System.out.println("========================================");
                System.out.println("1. Agregar producto al carrito");
                System.out.println("2. Ver carrito actual");
                System.out.println("3. Procesar venta");
                System.out.println("4. Cancelar venta");
                System.out.println("0. Salir");
                System.out.print("Opción: ");

                String option = scanner.nextLine().trim();

                try {
                    switch (option) {
                        case "1" -> {
                            System.out.print("SKU: ");
                            String sku = scanner.nextLine().trim();
                            System.out.print("Cantidad: ");
                            int qty = Integer.parseInt(scanner.nextLine().trim());
                            saleService.addToCart(sku, qty);
                            System.out.println("✅ Producto agregado al carrito.");
                        }
                        case "2" -> {
                            var cart = saleService.getCurrentCart();
                            if (cart.isEmpty()) {
                                System.out.println("El carrito está vacío.");
                            } else {
                                cart.forEach(item -> System.out.printf(
                                        "SKU: %-10s | %-20s | Cant: %d | S/ %s%n",
                                        item.getSku(), item.getProductName(),
                                        item.getQuantity(), item.getSubtotal()));
                            }
                        }
                        case "3" -> {
                            System.out.println("Método de pago: 1=CASH, 2=CARD, 3=TRANSFER");
                            System.out.print("Opción: ");
                            String pm = scanner.nextLine().trim();
                            PaymentMethod method = switch (pm) {
                                case "1" -> PaymentMethod.CASH;
                                case "2" -> PaymentMethod.CARD;
                                case "3" -> PaymentMethod.TRANSFER;
                                default -> throw new IllegalArgumentException("Método de pago inválido.");
                            };
                            SaleRecord sale = saleService.processSale(method);
                            receiptPrinter.printToConsole(sale);
                        }
                        case "4" -> {
                            saleService.cancelSale();
                            System.out.println("✅ Venta cancelada.");
                        }
                        case "0" -> {
                            running = false;
                            System.out.println("Saliendo...");
                        }
                        default -> System.out.println("Opción no válida.");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Error: " + e.getMessage());
                    log.error("Error en menú POS", e);
                }
            }
        };
    }
}
