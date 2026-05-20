package com.fco.platform.player.interfaces.web;

import com.fco.platform.player.domain.FcoNation;
import com.fco.platform.player.infrastructure.persistence.IFcoNationRepository;
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
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/nations")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminNationController {

    private final IFcoNationRepository nationRepo;
    private final IFcoPlayerRepository playerRepo;

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<AdminDtos.NationItem>>> getNations(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<FcoNation> nationPage;
        if (keyword == null || keyword.trim().isEmpty()) {
            nationPage = nationRepo.findAllByDeletedFalse(pageable);
        } else {
            nationPage = nationRepo.searchNations(keyword.trim(), pageable);
        }

        PageResponse<AdminDtos.NationItem> payload = PageResponse.<AdminDtos.NationItem>builder()
                .items(nationPage.getContent().stream().map(this::toNationItem).toList())
                .page(nationPage.getNumber())
                .size(nationPage.getSize())
                .totalItems(nationPage.getTotalElements())
                .totalPages(nationPage.getTotalPages())
                .hasNext(nationPage.hasNext())
                .build();

        return ResponseEntity.ok(ResponseWrapper.<PageResponse<AdminDtos.NationItem>>builder()
                .status(HttpStatus.OK)
                .code(HttpStatus.OK.value())
                .data(payload)
                .build());
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<AdminDtos.NationItem>> createNation(@RequestBody FcoNation req) {
        if (req.getNationName() == null || req.getNationName().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_BAD_REQUEST);
        }
        String cleanName = req.getNationName().trim();
        String slug = req.getNationSlug() != null ? req.getNationSlug().trim().toLowerCase() : cleanName.toLowerCase().replaceAll("\\s+", "-");

        java.util.Optional<FcoNation> existingOpt = nationRepo.findByNationName(cleanName);
        FcoNation nation;

        if (existingOpt.isPresent()) {
            FcoNation existing = existingOpt.get();
            if (existing.isDeleted()) {
                existing.setDeleted(false);
                existing.setNationSlug(slug);
                if (req.getFlagUrl() != null) {
                    existing.setFlagUrl(req.getFlagUrl().trim());
                }
                existing.setUpdatedAt(java.time.LocalDateTime.now());
                nation = nationRepo.save(existing);
            } else {
                throw new BusinessException(ErrorCode.RES_CONFLICT);
            }
        } else {
            nation = FcoNation.builder()
                    .nationName(cleanName)
                    .nationSlug(slug)
                    .flagUrl(req.getFlagUrl() != null ? req.getFlagUrl().trim() : null)
                    .build();
            nation = nationRepo.save(nation);
        }

        return ResponseEntity.status(HttpStatus.CREATED).body(ResponseWrapper.<AdminDtos.NationItem>builder()
                .status(HttpStatus.CREATED)
                .code(HttpStatus.CREATED.value())
                .data(toNationItem(nation))
                .build());
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<AdminDtos.NationItem>> updateNation(@PathVariable Long id, @RequestBody FcoNation req) {
        FcoNation nation = nationRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        if (req.getNationName() != null && !req.getNationName().isBlank()) nation.setNationName(req.getNationName().trim());
        if (req.getNationSlug() != null) nation.setNationSlug(req.getNationSlug().trim().toLowerCase());
        if (req.getFlagUrl() != null) nation.setFlagUrl(req.getFlagUrl().trim());
        nation.setUpdatedAt(java.time.LocalDateTime.now());
        return ResponseEntity.ok(ResponseWrapper.<AdminDtos.NationItem>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).data(toNationItem(nationRepo.save(nation))).build());
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<Void>> deleteNation(@PathVariable Long id) {
        FcoNation nation = nationRepo.findById(id).orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));
        nation.setDeleted(true);
        nation.setUpdatedAt(java.time.LocalDateTime.now());
        nationRepo.save(nation);
        return ResponseEntity.ok(ResponseWrapper.<Void>builder().status(HttpStatus.OK).code(HttpStatus.OK.value()).build());
    }

    private AdminDtos.NationItem toNationItem(FcoNation nation) {
        return AdminDtos.NationItem.builder()
                .id(nation.getId())
                .nationName(nation.getNationName())
                .nationSlug(nation.getNationSlug())
                .flagUrl(nation.getFlagUrl())
                .createdAt(nation.getCreatedAt())
                .updatedAt(nation.getUpdatedAt())
                .build();
    }
}
