package com.fco.platform.player.interfaces.web;

import com.fco.platform.player.application.IPlayerService;
import com.fco.platform.player.interfaces.dto.PlayerCardResponse;
import com.fco.platform.player.interfaces.dto.PlayerDetailResponse;
import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.common.dto.resp.PageResponse;
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
    public ResponseEntity<ResponseWrapper<PlayerDetailResponse>> getPlayer(@PathVariable Long id) {
        return ResponseEntity.ok(
                ResponseWrapper.<PlayerDetailResponse>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(playerService.getPlayerDetail(id))
                        .build()
        );
    }
}
