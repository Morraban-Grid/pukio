package com.pukio.posclient.client;

import com.pukio.posclient.dto.*;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * HTTP client for communicating with the Application Server.
 * All methods throw RuntimeException with server error message on failure.
 * 
 * TASK-E2-25
 */
@Component
public class AppServerClient {

    private final RestTemplate restTemplate;
    private final String baseUrl;

    public AppServerClient(RestTemplate restTemplate, 
                          @Value("${app.server.url}") String baseUrl) {
        this.restTemplate = restTemplate;
        this.baseUrl = baseUrl;
    }

    /**
     * Authenticate user and obtain session token.
     * POST /api/v1/auth/login
     * 
     * @throws RuntimeException if login fails
     */
    public LoginResponse login(String username, String password) {
        try {
            Map<String, String> request = new HashMap<>();
            request.put("username", username);
            request.put("password", password);

            ResponseEntity<LoginResponse> response = restTemplate.postForEntity(
                baseUrl + "/api/v1/auth/login",
                request,
                LoginResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Login failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Login failed: " + e.getMessage(), e);
        }
    }

    /**
     * Process a sale transaction.
     * POST /api/sales/process
     * 
     * @throws RuntimeException if sale processing fails
     */
    public SaleResponse processSale(SaleRequest request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<SaleRequest> entity = new HttpEntity<>(request, headers);

            ResponseEntity<SaleResponse> response = restTemplate.exchange(
                baseUrl + "/api/sales/process",
                HttpMethod.POST,
                entity,
                SaleResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Sale processing failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Sale processing failed: " + e.getMessage(), e);
        }
    }

    /**
     * Search products with pagination.
     * GET /api/products/search
     * 
     * @throws RuntimeException if search fails
     */
    public Page<ProductDto> getProducts(String query, String category, int page) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            UriComponentsBuilder builder = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/products/search")
                .queryParam("page", page)
                .queryParam("size", 50); // 50 items per page as per requirements

            if (query != null && !query.isEmpty()) {
                builder.queryParam("query", query);
            }
            if (category != null && !category.isEmpty()) {
                builder.queryParam("category", category);
            }

            @SuppressWarnings("unchecked")
            ResponseEntity<Page<ProductDto>> response = restTemplate.exchange(
                builder.toUriString(),
                HttpMethod.GET,
                entity,
                (Class<Page<ProductDto>>) (Class<?>) Page.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Product search failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Product search failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get stock information for a specific product.
     * GET /api/inventory/{sku}?storeId=
     * 
     * @throws RuntimeException if query fails
     */
    public InventoryDto getStock(String sku, String storeId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/inventory/" + sku)
                .queryParam("storeId", storeId)
                .toUriString();

            ResponseEntity<InventoryDto> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                InventoryDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Stock query failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Stock query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get product information by SKU.
     * GET /api/products/{sku}
     * 
     * @throws RuntimeException if query fails
     */
    public ProductDto getProductBySku(String sku) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl + "/api/products/" + sku,
                HttpMethod.GET,
                entity,
                ProductDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Product query failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Product query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Adjust inventory quantity.
     * POST /api/inventory/adjustment
     * 
     * @throws RuntimeException if adjustment fails
     */
    public void adjustInventory(InventoryAdjustmentDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<InventoryAdjustmentDto> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(
                baseUrl + "/api/inventory/adjustment",
                HttpMethod.POST,
                entity,
                Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Inventory adjustment failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get all active promotions.
     * GET /api/promotions/active
     * 
     * @throws RuntimeException if query fails
     */
    public List<PromotionDto> getActivePromotions() {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<List<PromotionDto>> response = restTemplate.exchange(
                baseUrl + "/api/promotions/active",
                HttpMethod.GET,
                entity,
                (Class<List<PromotionDto>>) (Class<?>) List.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Promotion query failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Promotion query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new promotion.
     * POST /api/promotions
     * 
     * @throws RuntimeException if creation fails
     */
    public PromotionDto createPromotion(PromotionDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<PromotionDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<PromotionDto> response = restTemplate.exchange(
                baseUrl + "/api/promotions",
                HttpMethod.POST,
                entity,
                PromotionDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Promotion creation failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Promotion creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing promotion.
     * PUT /api/promotions/{id}
     * 
     * @throws RuntimeException if update fails
     */
    public PromotionDto updatePromotion(PromotionDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<PromotionDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<PromotionDto> response = restTemplate.exchange(
                baseUrl + "/api/promotions/" + request.getPromoId(),
                HttpMethod.PUT,
                entity,
                PromotionDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Promotion update failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Promotion update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get expected amounts for arqueo.
     * GET /api/arqueo/expected
     * 
     * @throws RuntimeException if query fails
     */
    public ArqueoSummaryResponse getExpectedAmounts(String storeId, String shiftId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            String url = UriComponentsBuilder.fromHttpUrl(baseUrl + "/api/arqueo/expected")
                .queryParam("storeId", storeId)
                .queryParam("shiftId", shiftId)
                .toUriString();

            ResponseEntity<ArqueoSummaryResponse> response = restTemplate.exchange(
                url,
                HttpMethod.GET,
                entity,
                ArqueoSummaryResponse.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Arqueo query failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Arqueo query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Submit arqueo (close shift).
     * POST /api/arqueo/close
     * 
     * @throws RuntimeException if submission fails
     */
    public ArqueoResult submitArqueo(ArqueoRequestDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<ArqueoRequestDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ArqueoResult> response = restTemplate.exchange(
                baseUrl + "/api/arqueo/close",
                HttpMethod.POST,
                entity,
                ArqueoResult.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Arqueo submission failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Arqueo submission failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get inventory by store.
     * GET /api/inventory/store/{storeId}
     * 
     * @throws RuntimeException if query fails
     */
    public List<InventoryDto> getInventoryByStore(String storeId) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<List<InventoryDto>> response = restTemplate.exchange(
                baseUrl + "/api/inventory/store/" + storeId,
                HttpMethod.GET,
                entity,
                (Class<List<InventoryDto>>) (Class<?>) List.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Inventory query failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Inventory query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Get all stores.
     * GET /api/stores
     * 
     * @throws RuntimeException if query fails
     */
    public List<StoreDto> getStores() {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            @SuppressWarnings("unchecked")
            ResponseEntity<List<StoreDto>> response = restTemplate.exchange(
                baseUrl + "/api/stores",
                HttpMethod.GET,
                entity,
                (Class<List<StoreDto>>) (Class<?>) List.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Stores query failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Stores query failed: " + e.getMessage(), e);
        }
    }

    /**
     * Transfer inventory between stores.
     * POST /api/inventory/transfer
     * 
     * @throws RuntimeException if transfer fails
     */
    public void transferInventory(InventoryTransferDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<InventoryTransferDto> entity = new HttpEntity<>(request, headers);

            restTemplate.exchange(
                baseUrl + "/api/inventory/transfer",
                HttpMethod.POST,
                entity,
                Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Inventory transfer failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create a new product.
     * POST /api/products
     * 
     * @throws RuntimeException if creation fails
     */
    public ProductDto createProduct(ProductDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<ProductDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl + "/api/products",
                HttpMethod.POST,
                entity,
                ProductDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Product creation failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Product creation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Update an existing product.
     * PUT /api/products/{sku}
     * 
     * @throws RuntimeException if update fails
     */
    public ProductDto updateProduct(ProductDto request) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<ProductDto> entity = new HttpEntity<>(request, headers);

            ResponseEntity<ProductDto> response = restTemplate.exchange(
                baseUrl + "/api/products/" + request.getSku(),
                HttpMethod.PUT,
                entity,
                ProductDto.class
            );

            if (response.getStatusCode() == HttpStatus.OK && response.getBody() != null) {
                return response.getBody();
            } else {
                throw new RuntimeException("Product update failed: Unexpected response");
            }
        } catch (Exception e) {
            throw new RuntimeException("Product update failed: " + e.getMessage(), e);
        }
    }

    /**
     * Deactivate a product.
     * DELETE /api/products/{sku}
     * 
     * @throws RuntimeException if deactivation fails
     */
    public void deactivateProduct(String sku) {
        try {
            HttpHeaders headers = createAuthHeaders();
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            restTemplate.exchange(
                baseUrl + "/api/products/" + sku,
                HttpMethod.DELETE,
                entity,
                Void.class
            );
        } catch (Exception e) {
            throw new RuntimeException("Product deactivation failed: " + e.getMessage(), e);
        }
    }

    /**
     * Create HTTP headers with Authorization token.
     */
    private HttpHeaders createAuthHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        
        String token = SessionContext.getToken();
        if (token != null && !token.isEmpty()) {
            headers.set("Authorization", "Bearer " + token);
        }
        
        return headers;
    }
}
