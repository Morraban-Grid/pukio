package com.pukio.appserver.controller;

import com.pukio.appserver.business.ArqueoService;
import com.pukio.appserver.domain.Arqueo;
import com.pukio.appserver.dto.ArqueoRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/arqueo")
@RequiredArgsConstructor
public class ArqueoController {

    private final ArqueoService arqueoService;

    @PostMapping("/start")
    public ResponseEntity<Arqueo> startArqueo(@Valid @RequestBody ArqueoRequest request) {
        Arqueo arqueo = arqueoService.calcularArqueo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(arqueo);
    }

    @PostMapping("/close")
    public ResponseEntity<Arqueo> closeArqueo(@Valid @RequestBody ArqueoRequest request) {
        Arqueo arqueo = arqueoService.calcularArqueo(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(arqueo);
    }

    @PutMapping("/{id}/approve")
    public ResponseEntity<Arqueo> approveArqueo(@PathVariable Long id) {
        Arqueo arqueo = arqueoService.approveArqueo(id);
        return ResponseEntity.ok(arqueo);
    }
}
