package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.dto.resp.PageResponse;
import com.ra.base_spring_boot.dto.resp.PlayerCardResponse;
import com.ra.base_spring_boot.services.IPlayerService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/players")
@RequiredArgsConstructor
public class PlayerController {
    private final IPlayerService playerService;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<PlayerCardResponse>>> getPlayers(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String seasonCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        PageResponse<PlayerCardResponse> payload = playerService.getPlayers(keyword, seasonCode, page, size);
        return ResponseEntity.ok(
                ResponseWrapper.<PageResponse<PlayerCardResponse>>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(payload)
                        .build()
        );
    }

    @GetMapping("/{id}")
    public ResponseEntity<ResponseWrapper<com.ra.base_spring_boot.dto.resp.PlayerDetailResponse>> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseWrapper.<com.ra.base_spring_boot.dto.resp.PlayerDetailResponse>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(playerService.getPlayerDetail(id))
                        .build()
        );
    }
}
