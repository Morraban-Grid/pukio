package com.pukio.appserver.controller;

import com.pukio.appserver.business.SaleService;
import com.pukio.appserver.dto.SaleRequest;
import com.pukio.appserver.dto.SaleResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/api/sales")
@RequiredArgsConstructor
public class SaleController {

    private final SaleService saleService;

    @PostMapping("/process")
    public ResponseEntity<SaleResponse> processSale(@Valid @RequestBody SaleRequest request) {
        SaleResponse response = saleService.processSale(request);
        return ResponseEntity.created(URI.create("/api/sales/" + response.getTransactionId())).body(response);
    }
}
