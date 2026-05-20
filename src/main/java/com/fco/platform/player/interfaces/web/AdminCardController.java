package com.fco.platform.player.interfaces.web;

import com.fco.platform.card.domain.FcoSeason;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.card.infrastructure.persistence.IFcoSeasonRepository;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
import com.fco.platform.player.domain.FcoPlayer;
import com.fco.platform.player.infrastructure.persistence.IFcoPlayerRepository;
import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.common.dto.resp.PageResponse;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.player.interfaces.dto.AdminDtos;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@RestController
@RequestMapping("/api/v1/admin/cards")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminCardController {

    private final IPlayerCardRepository cardRepo;
    private final IFcoPlayerRepository playerRepo;
    private final IFcoSeasonRepository seasonRepo;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<AdminDtos.CardItem>>> getCards(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String seasonCode,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<PlayerCard> cardPage = cardRepo.search(keyword, seasonCode, null, null, null, null, pageable);

        PageResponse<AdminDtos.CardItem> payload = PageResponse.<AdminDtos.CardItem>builder()
                .items(cardPage.getContent().stream().map(this::toCardItem).toList())
                .page(cardPage.getNumber())
                .size(cardPage.getSize())
                .totalItems(cardPage.getTotalElements())
                .totalPages(cardPage.getTotalPages())
                .hasNext(cardPage.hasNext())
                .build();

        return ResponseEntity.ok(ResponseWrapper.<PageResponse<AdminDtos.CardItem>>builder()
                .status(HttpStatus.OK)
                .code(HttpStatus.OK.value())
                .data(payload)
                .build());
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ResponseWrapper<AdminDtos.CardItem>> createCard(@RequestBody AdminCardRequest req) {
        if (req.getPlayerId() == null || req.getSeasonId() == null || req.getOvr() == null) {
            throw new BusinessException(ErrorCode.VAL_BAD_REQUEST);
        }

        validateSecondaryPosition(req.getSecondaryPosition());

        FcoPlayer player = playerRepo.findById(req.getPlayerId()).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        FcoSeason season = seasonRepo.findById(req.getSeasonId()).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        if (cardRepo.findFirstByPlayerIdAndSeasonIdOrderByUpdatedAtDesc(req.getPlayerId(), req.getSeasonId()).isPresent()) {
            throw new BusinessException(ErrorCode.RES_CONFLICT);
        }

        PlayerCard card = PlayerCard.builder()
                .player(player)
                .season(season)
                .ovr(req.getOvr())
                .enhanceLevel(1)
                .salary(req.getSalary() != null ? req.getSalary() : 0)
                .preferredPosition(req.getPreferredPosition())
                .secondaryPosition(req.getSecondaryPosition())
                .marketPriceBp(req.getMarketPriceBp() != null ? req.getMarketPriceBp() : 0L)
                .imageUrl(req.getImageUrl())
                .priceUpdatedAt(LocalDate.now())
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .isActive(true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.<AdminDtos.CardItem>builder()
                .status(HttpStatus.CREATED)
                .code(HttpStatus.CREATED.value())
                .data(toCardItem(cardRepo.save(card)))
                .build());
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ResponseWrapper<AdminDtos.CardItem>> updateCard(@PathVariable Long id, @RequestBody AdminCardRequest req) {
        PlayerCard card = cardRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        if (req.getOvr() != null) card.setOvr(req.getOvr());
        if (req.getSalary() != null) card.setSalary(req.getSalary());
        if (req.getPreferredPosition() != null) card.setPreferredPosition(req.getPreferredPosition().trim());
        if (req.getSecondaryPosition() != null) {
            validateSecondaryPosition(req.getSecondaryPosition());
            card.setSecondaryPosition(req.getSecondaryPosition().trim());
        }
        if (req.getMarketPriceBp() != null) card.setMarketPriceBp(req.getMarketPriceBp());
        if (req.getImageUrl() != null) card.setImageUrl(req.getImageUrl().trim());
        card.setUpdatedAt(LocalDateTime.now());
        return ResponseEntity.ok(ResponseWrapper.<AdminDtos.CardItem>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).data(toCardItem(cardRepo.save(card))).build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<Void>> deleteCard(@PathVariable Long id) {
        PlayerCard card = cardRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        cardRepo.delete(card);
        return ResponseEntity.ok(ResponseWrapper.<Void>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).build());
    }

    private void validateSecondaryPosition(String secPos) {
        if (secPos == null || secPos.trim().isEmpty()) {
            return;
        }
        if (secPos.length() > 100) {
            throw new BusinessException("Vị trí phụ không được vượt quá 100 ký tự", ErrorCode.VAL_BAD_REQUEST);
        }

        java.util.List<String> validPositions = java.util.List.of(
            "ST", "CF", "LW", "RW", "CAM", "CM", "LM", "RM", "CDM", "CB", "LB", "RB", "LWB", "RWB", "GK"
        );

        String[] parts = secPos.split(",");
        for (String part : parts) {
            String trimmed = part.trim().toUpperCase();
            if (!trimmed.isEmpty() && !validPositions.contains(trimmed)) {
                throw new BusinessException("Vị trí phụ không hợp lệ: " + trimmed + ". Các vị trí hợp lệ: ST, CF, LW, RW, CAM, CM, LM, RM, CDM, CB, LB, RB, LWB, RWB, GK", ErrorCode.VAL_BAD_REQUEST);
            }
        }
    }

    private AdminDtos.CardItem toCardItem(PlayerCard card) {
        return AdminDtos.CardItem.builder()
                .id(card.getId())
                .playerId(card.getPlayer() != null ? card.getPlayer().getId() : null)
                .playerName(card.getPlayer() != null ? card.getPlayer().getPlayerName() : null)
                .seasonCode(card.getSeason() != null ? card.getSeason().getSeasonCode() : null)
                .seasonName(card.getSeason() != null ? card.getSeason().getSeasonName() : null)
                .enhanceLevel(card.getEnhanceLevel())
                .ovr(card.getOvr())
                .salary(card.getSalary())
                .preferredPosition(card.getPreferredPosition())
                .secondaryPosition(card.getSecondaryPosition())
                .marketPriceBp(card.getMarketPriceBp())
                .imageUrl(card.getImageUrl())
                .priceUpdatedAt(card.getPriceUpdatedAt())
                .isActive(card.getIsActive())
                .createdAt(card.getCreatedAt())
                .updatedAt(card.getUpdatedAt())
                .build();
    }

    @Getter
    @Setter
    public static class AdminCardRequest {
        private Long playerId;
        private Long seasonId;
        private Integer ovr;
        private Integer salary;
        private String preferredPosition;
        private String secondaryPosition;
        private Long marketPriceBp;
        private String imageUrl;
    }
}
