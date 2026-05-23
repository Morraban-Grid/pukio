package com.pukio.maintenance;

import com.pukio.common.model.ProductRecord;
import com.pukio.maintenance.service.InventoryService;
import com.pukio.maintenance.service.ProductService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.context.annotation.Bean;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.Scanner;

/**
 * Maintenance application entry point.
 * Provides a console menu for product and inventory management. (TASK-E1-17)
 */
@Slf4j
@SpringBootApplication
public class MaintenanceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MaintenanceApplication.class, args);
    }

    @Bean
    public CommandLineRunner consoleMenu(ProductService productService,
                                          InventoryService inventoryService) {
        return args -> {
            Scanner scanner = new Scanner(System.in);
            boolean running = true;

            while (running) {
                System.out.println("\n========================================");
                System.out.println("  PUKIO — Módulo de Mantenimiento");
                System.out.println("========================================");
                System.out.println("1. Crear producto");
                System.out.println("2. Actualizar producto");
                System.out.println("3. Eliminar producto");
                System.out.println("4. Buscar producto por SKU");
                System.out.println("5. Listar todos los productos");
                System.out.println("6. Actualizar stock");
                System.out.println("7. Decrementar stock");
                System.out.println("8. Consultar stock");
                System.out.println("0. Salir");
                System.out.print("Opción: ");

                String option = scanner.nextLine().trim();

                try {
                    switch (option) {
                        case "1" -> {
                            System.out.print("SKU: ");
                            String sku = scanner.nextLine().trim();
                            System.out.print("Nombre: ");
                            String name = scanner.nextLine().trim();
                            System.out.print("Precio: ");
                            BigDecimal price = new BigDecimal(scanner.nextLine().trim());
                            System.out.print("Categoría: ");
                            String category = scanner.nextLine().trim();
                            System.out.print("Descripción: ");
                            String description = scanner.nextLine().trim();

                            productService.createProduct(sku, name, price, category, description);
                            System.out.println("✅ Producto creado.");
                        }
                        case "2" -> {
                            System.out.print("SKU a actualizar: ");
                            String sku = scanner.nextLine().trim();
                            System.out.print("Nuevo nombre: ");
                            String name = scanner.nextLine().trim();
                            System.out.print("Nuevo precio: ");
                            BigDecimal price = new BigDecimal(scanner.nextLine().trim());
                            System.out.print("Nueva categoría: ");
                            String category = scanner.nextLine().trim();
                            System.out.print("Nueva descripción: ");
                            String description = scanner.nextLine().trim();

                            productService.updateProduct(sku, name, price, category, description);
                            System.out.println("✅ Producto actualizado.");
                        }
                        case "3" -> {
                            System.out.print("SKU a eliminar: ");
                            String sku = scanner.nextLine().trim();
                            productService.deleteProduct(sku);
                            System.out.println("✅ Producto eliminado.");
                        }
                        case "4" -> {
                            System.out.print("SKU a buscar: ");
                            String sku = scanner.nextLine().trim();
                            Optional<ProductRecord> found = productService.findBySku(sku);
                            found.ifPresentOrElse(
                                    p -> System.out.printf("SKU: %s | Nombre: %s | Precio: %s | Categoría: %s%n",
                                            p.getSku(), p.getName(), p.getPrice(), p.getCategory()),
                                    () -> System.out.println("❌ Producto no encontrado.")
                            );
                        }
                        case "5" -> {
                            List<ProductRecord> all = productService.listAll();
                            if (all.isEmpty()) {
                                System.out.println("No hay productos registrados.");
                            } else {
                                all.forEach(p -> System.out.printf("SKU: %-10s | %-20s | S/ %s%n",
                                        p.getSku(), p.getName(), p.getPrice()));
                            }
                        }
                        case "6" -> {
                            System.out.print("SKU: ");
                            String sku = scanner.nextLine().trim();
                            System.out.print("Nueva cantidad: ");
                            int qty = Integer.parseInt(scanner.nextLine().trim());
                            inventoryService.updateStock(sku, qty);
                            System.out.println("✅ Stock actualizado.");
                        }
                        case "7" -> {
                            System.out.print("SKU: ");
                            String sku = scanner.nextLine().trim();
                            System.out.print("Cantidad a decrementar: ");
                            int qty = Integer.parseInt(scanner.nextLine().trim());
                            inventoryService.decrementStock(sku, qty);
                            System.out.println("✅ Stock decrementado.");
                        }
                        case "8" -> {
                            System.out.print("SKU: ");
                            String sku = scanner.nextLine().trim();
                            inventoryService.getStock(sku).ifPresentOrElse(
                                    r -> System.out.printf("SKU: %s | Cantidad: %d | OutOfStock: %s%n",
                                            r.getSku(), r.getQuantity(), r.isOutOfStock()),
                                    () -> System.out.println("❌ Registro no encontrado.")
                            );
                        }
                        case "0" -> {
                            running = false;
                            System.out.println("Saliendo...");
                        }
                        default -> System.out.println("Opción no válida.");
                    }
                } catch (Exception e) {
                    System.out.println("❌ Error: " + e.getMessage());
                    log.error("Error en menú de mantenimiento", e);
                }
            }
        };
    }
}
