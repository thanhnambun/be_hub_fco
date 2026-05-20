package com.fco.platform.player.interfaces.web;

import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
import com.fco.platform.player.domain.FcoClub;
import com.fco.platform.player.domain.FcoLeague;
import com.fco.platform.player.domain.FcoNation;
import com.fco.platform.player.domain.FcoPlayer;
import com.fco.platform.player.infrastructure.persistence.IFcoClubRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoLeagueRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoNationRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoPlayerRepository;
import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.common.dto.resp.PageResponse;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.player.interfaces.dto.AdminPlayerDtos;
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

import java.util.List;

import com.fco.platform.card.domain.FcoSeason;
import com.fco.platform.card.infrastructure.persistence.IFcoSeasonRepository;

@RestController
@RequestMapping("/api/v1/admin/players")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminPlayerController {

    private final IFcoPlayerRepository playerRepo;
    private final IPlayerCardRepository cardRepo;
    private final IFcoNationRepository nationRepo;
    private final IFcoClubRepository clubRepo;
    private final IFcoLeagueRepository leagueRepo;
    private final IFcoSeasonRepository seasonRepo;

    @GetMapping("/options")
    public ResponseEntity<ResponseWrapper<AdminPlayerDtos.OptionsResponse>> getOptions() {
        AdminPlayerDtos.OptionsResponse options = AdminPlayerDtos.OptionsResponse.builder()
                .nations(nationRepo.findAllByDeletedFalse().stream().map(this::toOptionItem).toList())
                .clubs(clubRepo.findAllByDeletedFalse().stream().map(this::toOptionItem).toList())
                .leagues(leagueRepo.findAllByDeletedFalse().stream().map(this::toOptionItem).toList())
                .seasons(seasonRepo.findAll().stream().map(s -> AdminPlayerDtos.OptionItem.builder()
                        .id(s.getId())
                        .label(s.getSeasonName())
                        .slug(s.getSeasonCode())
                        .imageUrl(null)
                        .build()).toList())
                .build();
        return ResponseEntity.ok(
                ResponseWrapper.<AdminPlayerDtos.OptionsResponse>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(options)
                        .build()
        );
    }

    @GetMapping
    public ResponseEntity<ResponseWrapper<PageResponse<AdminPlayerDtos.PlayerItem>>> getPlayers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size
    ) {
        int normalizedPage = Math.max(page, 0);
        int normalizedSize = Math.min(Math.max(size, 1), 100);
        Pageable pageable = PageRequest.of(normalizedPage, normalizedSize);

        Page<FcoPlayer> playerPage;
        if (keyword == null || keyword.trim().isEmpty()) {
            playerPage = playerRepo.findAll(pageable);
        } else {
            playerPage = playerRepo.searchPlayers(keyword.trim(), pageable);
        }

        PageResponse<AdminPlayerDtos.PlayerItem> payload = PageResponse.<AdminPlayerDtos.PlayerItem>builder()
                .items(playerPage.getContent().stream().map(this::toPlayerItem).toList())
                .page(playerPage.getNumber())
                .size(playerPage.getSize())
                .totalItems(playerPage.getTotalElements())
                .totalPages(playerPage.getTotalPages())
                .hasNext(playerPage.hasNext())
                .build();

        return ResponseEntity.ok(
                ResponseWrapper.<PageResponse<AdminPlayerDtos.PlayerItem>>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(payload)
                        .build()
        );
    }

    @PostMapping
    @Transactional
    public ResponseEntity<ResponseWrapper<AdminPlayerDtos.PlayerItem>> createPlayer(@RequestBody AdminPlayerRequest req) {
        if (req.getPlayerName() == null || req.getPlayerName().isBlank()) {
            throw new BusinessException(ErrorCode.VAL_BAD_REQUEST);
        }
        String extId = req.getExternalId() != null ? req.getExternalId().trim() : "adm_" + System.currentTimeMillis();
        if (playerRepo.findByExternalId(extId).isPresent()) {
            throw new BusinessException(ErrorCode.RES_CONFLICT);
        }

        FcoPlayer player = FcoPlayer.builder()
                .playerName(req.getPlayerName().trim())
                .externalId(extId)
                .height(req.getHeight())
                .weight(req.getWeight())
                .birthdate(req.getBirthdate())
                .preferredFoot(req.getPreferredFoot())
                .weakFoot(req.getWeakFoot())
                .build();

        resolvePlayerRelations(player, req);
        FcoPlayer saved = playerRepo.save(player);

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.<AdminPlayerDtos.PlayerItem>builder()
                        .status(HttpStatus.CREATED)
                        .code(HttpStatus.CREATED.value())
                        .data(toPlayerItem(saved))
                        .build()
        );
    }

    @PutMapping("/{id}")
    @Transactional
    public ResponseEntity<ResponseWrapper<AdminPlayerDtos.PlayerItem>> updatePlayer(
            @PathVariable Long id,
            @RequestBody AdminPlayerRequest req
    ) {
        FcoPlayer player = playerRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));

        if (req.getPlayerName() != null && !req.getPlayerName().isBlank()) {
            player.setPlayerName(req.getPlayerName().trim());
        }
        if (req.getHeight() != null) player.setHeight(req.getHeight());
        if (req.getWeight() != null) player.setWeight(req.getWeight());
        if (req.getBirthdate() != null) player.setBirthdate(req.getBirthdate().trim());
        if (req.getPreferredFoot() != null) player.setPreferredFoot(req.getPreferredFoot().trim());
        if (req.getWeakFoot() != null) player.setWeakFoot(req.getWeakFoot());

        resolvePlayerRelations(player, req);
        player.setUpdatedAt(java.time.LocalDateTime.now());

        FcoPlayer saved = playerRepo.save(player);
        return ResponseEntity.ok(
                ResponseWrapper.<AdminPlayerDtos.PlayerItem>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .data(toPlayerItem(saved))
                        .build()
        );
    }

    @DeleteMapping("/{id}")
    @Transactional
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResponseWrapper<Void>> deletePlayer(@PathVariable Long id) {
        FcoPlayer player = playerRepo.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.RES_NOT_FOUND));

        List<PlayerCard> cards = cardRepo.findAllByPlayerId(id);
        if (!cards.isEmpty()) {
            cardRepo.deleteAll(cards);
        }

        playerRepo.delete(player);

        return ResponseEntity.ok(
                ResponseWrapper.<Void>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .build()
        );
    }

    private AdminPlayerDtos.PlayerItem toPlayerItem(FcoPlayer player) {
        return AdminPlayerDtos.PlayerItem.builder()
                .id(player.getId())
                .playerName(player.getPlayerName())
                .externalId(player.getExternalId())
                .height(player.getHeight())
                .weight(player.getWeight())
                .birthdate(player.getBirthdate())
                .preferredFoot(player.getPreferredFoot())
                .weakFoot(player.getWeakFoot())
                .nationId(player.getNation() != null ? player.getNation().getId() : null)
                .nationName(player.getNationName())
                .nationSlug(player.getNation() != null ? player.getNation().getNationSlug() : null)
                .flagUrl(player.getNation() != null ? player.getNation().getFlagUrl() : null)
                .clubId(player.getClub() != null ? player.getClub().getId() : null)
                .clubName(player.getClubName())
                .clubSlug(player.getClub() != null ? player.getClub().getClubSlug() : null)
                .crestUrl(player.getClub() != null ? player.getClub().getCrestUrl() : null)
                .leagueId(player.getLeague() != null ? player.getLeague().getId() : null)
                .leagueName(player.getLeagueName())
                .leagueSlug(player.getLeague() != null ? player.getLeague().getLeagueSlug() : null)
                .logoUrl(player.getLeague() != null ? player.getLeague().getLogoUrl() : null)
                .createdAt(player.getCreatedAt())
                .updatedAt(player.getUpdatedAt())
                .build();
    }

    private AdminPlayerDtos.OptionItem toOptionItem(FcoNation nation) {
        return AdminPlayerDtos.OptionItem.builder()
                .id(nation.getId())
                .label(nation.getNationName())
                .slug(nation.getNationSlug())
                .imageUrl(nation.getFlagUrl())
                .build();
    }

    private AdminPlayerDtos.OptionItem toOptionItem(FcoClub club) {
        return AdminPlayerDtos.OptionItem.builder()
                .id(club.getId())
                .label(club.getClubName())
                .slug(club.getClubSlug())
                .imageUrl(club.getCrestUrl())
                .build();
    }

    private AdminPlayerDtos.OptionItem toOptionItem(FcoLeague league) {
        return AdminPlayerDtos.OptionItem.builder()
                .id(league.getId())
                .label(league.getLeagueName())
                .slug(league.getLeagueSlug())
                .imageUrl(league.getLogoUrl())
                .build();
    }

    private void resolvePlayerRelations(FcoPlayer player, AdminPlayerRequest req) {
        if (req.getNationId() != null) {
            FcoNation nation = nationRepo.findById(req.getNationId()).orElse(null);
            if (nation != null) {
                player.setNation(nation);
                player.setNationName(nation.getNationName());
            }
        }
        if (req.getClubId() != null) {
            FcoClub club = clubRepo.findById(req.getClubId()).orElse(null);
            if (club != null) {
                player.setClub(club);
                player.setClubName(club.getClubName());
            }
        }
        if (req.getLeagueId() != null) {
            FcoLeague league = leagueRepo.findById(req.getLeagueId()).orElse(null);
            if (league != null) {
                player.setLeague(league);
                player.setLeagueName(league.getLeagueName());
            }
        }
    }

    @Getter
    @Setter
    public static class AdminPlayerRequest {
        private String playerName;
        private String externalId;
        private Integer height;
        private Integer weight;
        private String birthdate;
        private String preferredFoot;
        private Integer weakFoot;
        private Long nationId;
        private Long clubId;
        private Long leagueId;
    }
}
