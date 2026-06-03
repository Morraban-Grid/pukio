package com.pukio.appserver.controller;

import com.pukio.appserver.business.ProductService;
import com.pukio.appserver.domain.Product;
import com.pukio.appserver.dto.ProductRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/products")
@RequiredArgsConstructor
public class ProductController {

    private final ProductService productService;

    @GetMapping
    public ResponseEntity<Page<Product>> searchProducts(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "50") int size) {
        Page<Product> products = productService.searchProducts(query, category, page, size);
        return ResponseEntity.ok(products);
    }

    @GetMapping("/{sku}")
    public ResponseEntity<Product> getProductBySku(@PathVariable String sku) {
        Product product = productService.findBySku(sku);
        return ResponseEntity.ok(product);
    }

    @PostMapping
    public ResponseEntity<Product> createProduct(@Valid @RequestBody ProductRequest request) {
        Product product = Product.builder()
                .sku(request.getSku())
                .name(request.getName())
                .price(request.getPrice())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();
        Product created = productService.createProduct(product, null, null);
        return ResponseEntity.created(URI.create("/api/products/" + created.getSku())).body(created);
    }

    @PutMapping("/{sku}")
    public ResponseEntity<Product> updateProduct(
            @PathVariable String sku,
            @Valid @RequestBody ProductRequest request) {
        Product product = Product.builder()
                .name(request.getName())
                .price(request.getPrice())
                .category(request.getCategory())
                .description(request.getDescription())
                .build();
        Product updated = productService.updateProduct(sku, product, null, null);
        return ResponseEntity.ok(updated);
    }

    @DeleteMapping("/{sku}")
    public ResponseEntity<Void> deactivateProduct(@PathVariable String sku) {
        productService.deactivateProduct(sku, null, null);
        return ResponseEntity.noContent().build();
    }
}
