package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.ImportPlayersResult;
import com.ra.base_spring_boot.services.IDataSeedService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/seed")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class SeedAdminController {
    private final IDataSeedService dataSeedService;

    @PostMapping("/players")
    public ResponseEntity<ResponseWrapper<ImportPlayersResult>> seedPlayers() {
        ImportPlayersResult result = dataSeedService.seedPlayers();
        return ResponseEntity.ok(
                ResponseWrapper.<ImportPlayersResult>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(result)
                        .build()
        );
    }
}
