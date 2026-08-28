package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.dto.NpaBorrowerHitRequest;
import com.abikananda.lendenclub.dto.NpaBorrowerResponse;
import com.abikananda.lendenclub.service.NpaBorrowerService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/npa-borrowers")
public class NpaBorrowerController {

    private final NpaBorrowerService service;

    public NpaBorrowerController(NpaBorrowerService service) {
        this.service = service;
    }

    @GetMapping
    public List<NpaBorrowerResponse> getActiveBorrowers() {
        return service.getActiveBorrowers();
    }

    @PostMapping("/{id}/hit")
    public ResponseEntity<Void> recordHit(@PathVariable Long id,
                                          @Valid @RequestBody NpaBorrowerHitRequest request) {
        service.recordHit(id, request);
        return ResponseEntity.noContent().build();
    }
}
