package com.fco.platform.sync.interfaces.web;

import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.player.interfaces.dto.ImportPlayersResult;
import com.fco.platform.sync.application.IDataSeedService;
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
