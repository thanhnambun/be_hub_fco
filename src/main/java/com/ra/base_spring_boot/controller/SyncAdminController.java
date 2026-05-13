package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.req.PlayerCardDTO;
import com.ra.base_spring_boot.dto.req.PlayerDetailDTO;
import com.ra.base_spring_boot.dto.resp.SyncCardsResult;
import com.ra.base_spring_boot.services.impl.PlayerDetailSyncService;
import com.ra.base_spring_boot.services.impl.PlayerSyncService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/admin/sync")
@RequiredArgsConstructor
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
            @RequestBody List<PlayerCardDTO> payload) {
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
     * Payload: List<PlayerDetailDTO> — xem PlayerDetailDTO.java
     */
    @PostMapping("/cards/detail")
    public ResponseEntity<ResponseWrapper<SyncCardsResult>> syncCardsDetail(
            @RequestBody List<PlayerDetailDTO> payload) {
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
