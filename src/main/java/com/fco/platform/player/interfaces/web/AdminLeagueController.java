package com.fco.platform.player.interfaces.web;

import com.fco.platform.player.domain.FcoLeague;
import com.fco.platform.player.infrastructure.persistence.IFcoLeagueRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoPlayerRepository;
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
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/leagues")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminLeagueController {

    private final IFcoLeagueRepository leagueRepo;
    private final IFcoPlayerRepository playerRepo;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<AdminDtos.LeagueItem>>> getLeagues(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);
        Page<FcoLeague> leaguePage = searchLeagues(keyword, pageable);

        PageResponse<AdminDtos.LeagueItem> payload = PageResponse.<AdminDtos.LeagueItem>builder()
                .items(leaguePage.getContent().stream().map(this::toLeagueItem).toList())
                .page(leaguePage.getNumber())
                .size(leaguePage.getSize())
                .totalItems(leaguePage.getTotalElements())
                .totalPages(leaguePage.getTotalPages())
                .hasNext(leaguePage.hasNext())
                .build();

        return ResponseEntity.ok(ResponseWrapper.<PageResponse<AdminDtos.LeagueItem>>builder()
                .status(HttpStatus.OK)
                .code(HttpStatus.OK.value())
                .data(payload)
                .build());
    }

    @PostMapping
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<AdminDtos.LeagueItem>> createLeague(@RequestBody FcoLeague req) {
        if (req.getLeagueName() == null || req.getLeagueName().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_BAD_REQUEST);
        }
        String cleanName = req.getLeagueName().trim();
        String slug = req.getLeagueSlug() != null ? req.getLeagueSlug().trim().toLowerCase() : cleanName.toLowerCase().replaceAll("\\s+", "-");

        java.util.Optional<FcoLeague> existingOpt = leagueRepo.findByLeagueName(cleanName);
        FcoLeague league;

        if (existingOpt.isPresent()) {
            FcoLeague existing = existingOpt.get();
            if (existing.isDeleted()) {
                existing.setDeleted(false);
                existing.setLeagueSlug(slug);
                if (req.getLogoUrl() != null) {
                    existing.setLogoUrl(req.getLogoUrl().trim());
                }
                existing.setUpdatedAt(java.time.LocalDateTime.now());
                league = leagueRepo.save(existing);
            } else {
                throw new BusinessException(ErrorCode.RES_CONFLICT);
            }
        } else {
            league = FcoLeague.builder()
                    .leagueName(cleanName)
                    .leagueSlug(slug)
                    .logoUrl(req.getLogoUrl() != null ? req.getLogoUrl().trim() : null)
                    .build();
            league = leagueRepo.save(league);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.<AdminDtos.LeagueItem>builder()
                .status(HttpStatus.CREATED)
                .code(HttpStatus.CREATED.value())
                .data(toLeagueItem(league))
                .build());
    }

    @PutMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<AdminDtos.LeagueItem>> updateLeague(@PathVariable Long id, @RequestBody FcoLeague req) {
        FcoLeague league = leagueRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        if (req.getLeagueName() != null && !req.getLeagueName().isBlank()) league.setLeagueName(req.getLeagueName().trim());
        if (req.getLeagueSlug() != null) league.setLeagueSlug(req.getLeagueSlug().trim().toLowerCase());
        if (req.getLogoUrl() != null) league.setLogoUrl(req.getLogoUrl().trim());
        league.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(ResponseWrapper.<AdminDtos.LeagueItem>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).data(toLeagueItem(leagueRepo.save(league))).build());
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<Void>> deleteLeague(@PathVariable Long id) {
        FcoLeague league = leagueRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        league.setDeleted(true);
        league.setUpdatedAt(java.time.LocalDateTime.now());
        leagueRepo.save(league);
        return ResponseEntity.ok(ResponseWrapper.<Void>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).build());
    }

    private Page<FcoLeague> searchLeagues(String keyword, Pageable pageable) {
        if (keyword == null || keyword.trim().isEmpty()) return leagueRepo.findAllByDeletedFalse(pageable);
        return leagueRepo.searchLeagues(keyword.trim(), pageable);
    }

    private AdminDtos.LeagueItem toLeagueItem(FcoLeague league) {
        return AdminDtos.LeagueItem.builder()
                .id(league.getId())
                .leagueName(league.getLeagueName())
                .leagueSlug(league.getLeagueSlug())
                .logoUrl(league.getLogoUrl())
                .createdAt(league.getCreatedAt())
                .updatedAt(league.getUpdatedAt())
                .build();
    }
}
