package com.fco.platform.sync.interfaces.web;

import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.sync.interfaces.dto.SyncPlayerCardRequest;
import com.fco.platform.sync.interfaces.dto.SyncPlayerDetailRequest;
import com.fco.platform.sync.interfaces.dto.SyncCardsResult;
import com.fco.platform.sync.application.PlayerDetailSyncService;
import com.fco.platform.sync.application.PlayerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
@Validated
@PreAuthorize("hasRole('ADMIN')")
public class SyncAdminController {

    private final PlayerSyncService       playerSyncService;
    private final PlayerDetailSyncService playerDetailSyncService;

    /**
     * Pha A: Sync danh sách thẻ cơ bản (từ main_crawler.py)
     * POST /api/v1/admin/sync/cards
     */
    @PostMapping("/cards")
    public ResponseEntity<ResponseWrapper<SyncCardsResult>> syncCards(
            @RequestBody @Size(max = 2000) List<@Valid SyncPlayerCardRequest> payload) {
        SyncCardsResult result = playerSyncService.syncPlayerCards(payload);
        return ResponseEntity.ok(
                ResponseWrapper.<SyncCardsResult>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(result)
                        .build()
        );
    }

    /**
     * Pha B: Sync chi tiết cầu thủ (từ detail_crawler.py)
     * POST /api/v1/admin/sync/cards/detail
     *
     * Payload: List<SyncPlayerDetailRequest> — xem SyncPlayerDetailRequest.java
     */
    @PostMapping("/cards/detail")
    public ResponseEntity<ResponseWrapper<SyncCardsResult>> syncCardsDetail(
            @RequestBody @Size(max = 2000) List<@Valid SyncPlayerDetailRequest> payload) {
        SyncCardsResult result = playerDetailSyncService.syncDetailBatch(payload);
        return ResponseEntity.ok(
                ResponseWrapper.<SyncCardsResult>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(result)
                        .build()
        );
    }
}
