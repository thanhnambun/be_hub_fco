package com.fco.platform.player.interfaces.web;

import com.fco.platform.card.domain.FcoSeason;
import com.fco.platform.card.infrastructure.persistence.IFcoSeasonRepository;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.common.dto.resp.PageResponse;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.player.interfaces.dto.AdminDtos;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/seasons")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminSeasonController {

    private final IFcoSeasonRepository seasonRepo;
    private final IPlayerCardRepository cardRepo;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<AdminDtos.SeasonItem>>> getSeasons(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<FcoSeason> seasonPage = (keyword == null || keyword.trim().isEmpty())
                ? seasonRepo.findAll(pageable)
                : seasonRepo.searchSeasons(keyword.trim(), pageable);

        PageResponse<AdminDtos.SeasonItem> payload = PageResponse.<AdminDtos.SeasonItem>builder()
                .items(seasonPage.getContent().stream().map(this::toSeasonItem).toList())
                .page(seasonPage.getNumber())
                .size(seasonPage.getSize())
                .totalItems(seasonPage.getTotalElements())
                .totalPages(seasonPage.getTotalPages())
                .hasNext(seasonPage.hasNext())
                .build();

        return ResponseEntity.ok(ResponseWrapper.<PageResponse<AdminDtos.SeasonItem>>builder()
                .status(HttpStatus.OK)
                .code(HttpStatus.OK.value())
                .data(payload)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<AdminDtos.SeasonItem>> createSeason(@RequestBody FcoSeason req) {
        if (req.getSeasonCode() == null || req.getSeasonCode().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_BAD_REQUEST);
        }
        if (seasonRepo.findBySeasonCode(req.getSeasonCode().trim().toUpperCase()).isPresent()) {
            throw new BusinessException(ErrorCode.RES_CONFLICT);
        }

        FcoSeason season = FcoSeason.builder()
                .seasonCode(req.getSeasonCode().trim().toUpperCase())
                .seasonName(req.getSeasonName() != null ? req.getSeasonName().trim() : req.getSeasonCode().trim().toUpperCase())
                .isCore(req.getIsCore() != null ? req.getIsCore() : true)
                .isActive(req.getIsActive() != null ? req.getIsActive() : true)
                .build();

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.<AdminDtos.SeasonItem>builder()
                .status(HttpStatus.CREATED)
                .code(HttpStatus.CREATED.value())
                .data(toSeasonItem(seasonRepo.save(season)))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<AdminDtos.SeasonItem>> updateSeason(@PathVariable Long id, @RequestBody FcoSeason req) {
        FcoSeason season = seasonRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        if (req.getSeasonName() != null && !req.getSeasonName().isBlank()) season.setSeasonName(req.getSeasonName().trim());
        if (req.getIsActive() != null) season.setIsActive(req.getIsActive());
        if (req.getIsCore() != null) season.setIsCore(req.getIsCore());
        return ResponseEntity.ok(ResponseWrapper.<AdminDtos.SeasonItem>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).data(toSeasonItem(seasonRepo.save(season))).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<Void>> deleteSeason(@PathVariable Long id) {
        FcoSeason season = seasonRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        if (cardRepo.existsBySeasonId(id)) throw new BusinessException(ErrorCode.RES_CONFLICT);
        seasonRepo.delete(season);
        return ResponseEntity.ok(ResponseWrapper.<Void>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).build());
    }

    private AdminDtos.SeasonItem toSeasonItem(FcoSeason season) {
        return AdminDtos.SeasonItem.builder()
                .id(season.getId())
                .seasonCode(season.getSeasonCode())
                .seasonName(season.getSeasonName())
                .isActive(season.getIsActive())
                .isCore(season.getIsCore())
                .createdAt(season.getCreatedAt())
                .build();
    }
}
