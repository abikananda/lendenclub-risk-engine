package com.abikananda.lendenclub.controller;

import com.abikananda.lendenclub.service.SeleniumBorrowerMigrationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/migrations")
public class SeleniumBorrowerMigrationController {
    private final SeleniumBorrowerMigrationService migrationService;

    public SeleniumBorrowerMigrationController(SeleniumBorrowerMigrationService migrationService) {
        this.migrationService = migrationService;
    }

    @PostMapping("/selenium-borrowers")
    public ResponseEntity<SeleniumBorrowerMigrationService.ImportResult> migrate(
            @RequestParam(defaultValue = "0") long afterId,
            @RequestParam(defaultValue = "500") int maxRecords) {
        return ResponseEntity.ok(migrationService.migrate(afterId, maxRecords));
    }
}
