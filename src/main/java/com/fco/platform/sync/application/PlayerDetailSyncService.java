package com.fco.platform.sync.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fco.platform.sync.interfaces.dto.SyncPlayerDetailRequest;
import com.fco.platform.sync.interfaces.dto.SyncCardsResult;
import com.fco.platform.sync.domain.DetailSyncErrorLog;
import com.fco.platform.sync.infrastructure.persistence.DetailSyncErrorLogRepository;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.card.domain.FcoTrait;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
import com.fco.platform.card.infrastructure.persistence.IFcoTraitRepository;
import com.fco.platform.player.domain.FcoClub;
import com.fco.platform.player.domain.FcoLeague;
import com.fco.platform.player.domain.FcoNation;
import com.fco.platform.player.domain.FcoPlayer;
import com.fco.platform.player.infrastructure.persistence.IFcoClubRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoLeagueRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoNationRepository;
import com.fco.platform.player.infrastructure.persistence.IFcoPlayerRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerDetailSyncService {

    private static final Logger log = LoggerFactory.getLogger(PlayerDetailSyncService.class);
    private static final int MAX_ERROR_MESSAGE_LEN = 5000;

    private final IFcoPlayerRepository      playerRepo;
    private final IPlayerCardRepository     cardRepo;
    private final IFcoNationRepository      nationRepo;
    private final IFcoLeagueRepository      leagueRepo;
    private final IFcoClubRepository        clubRepo;
    private final IFcoTraitRepository       traitRepo;
    private final DetailSyncErrorLogRepository detailSyncErrorLogRepository;
    private final ObjectMapper              objectMapper;
    private final PlayerDetailSyncTxService detailSyncTxService;

    /**
     * Local thread-confined context to pre-load and cache lookups.
     * Prevents N+1 queries by caching Nations, Leagues, Clubs, and Traits.
     */
    public static class SyncContext {
        final Map<String, FcoPlayer> playerCache = new HashMap<>();
        final Map<Long, List<PlayerCard>> cardsByPlayerIdCache = new HashMap<>();
        final Map<String, FcoNation> nationCache = new HashMap<>();
        final Map<String, FcoLeague> leagueCache = new HashMap<>();
        final Map<Integer, FcoClub> clubByFifaIdCache = new HashMap<>();
        final Map<String, FcoClub> clubByNameAndLeagueIdCache = new HashMap<>();
        final Map<String, FcoClub> clubByNameCache = new HashMap<>();
        final Map<String, FcoTrait> traitCache = new HashMap<>();
    }

    public SyncCardsResult syncDetailBatch(List<SyncPlayerDetailRequest> batch) {
        long start = System.currentTimeMillis();
        if (batch == null || batch.isEmpty()) {
            return SyncCardsResult.builder()
                    .totalReceived(0).totalSuccessful(0).totalFailed(0).processingTimeMs(0L).build();
        }

        int success = 0;
        int failed  = 0;

        // 1. Build and pre-populate high-performance lookups
        SyncContext ctx = buildSyncContext(batch);

        for (SyncPlayerDetailRequest dto : batch) {
            try {
                DetailSyncOutcome outcome = detailSyncTxService.syncOne(dto, ctx);
                if (outcome == DetailSyncOutcome.OK) {
                    success++;
                } else {
                    failed++;
                }
            } catch (Exception ex) {
                failed++;
                log.error("[detail-sync] FAILED externalId={} — {}", dto.getExternalId(), ex.getMessage(), ex);
                saveDetailDeadLetter(dto, ex);
            }
        }

        return SyncCardsResult.builder()
                .totalReceived(batch.size())
                .totalSuccessful(success)
                .totalFailed(failed)
                .processingTimeMs(System.currentTimeMillis() - start)
                .build();
    }

    private SyncContext buildSyncContext(List<SyncPlayerDetailRequest> batch) {
        SyncContext ctx = new SyncContext();
        try {
            // A. Pre-load Players
            List<String> externalIds = batch.stream()
                    .map(SyncPlayerDetailRequest::getExternalId)
                    .filter(Objects::nonNull)
                    .distinct()
                    .collect(Collectors.toList());

            if (!externalIds.isEmpty()) {
                List<FcoPlayer> players = playerRepo.findAllByExternalIdIn(externalIds);
                for (FcoPlayer p : players) {
                    ctx.playerCache.put(p.getExternalId(), p);
                }

                // B. Pre-load Player Cards
                List<Long> playerIds = players.stream()
                        .map(FcoPlayer::getId)
                        .collect(Collectors.toList());

                if (!playerIds.isEmpty()) {
                    List<PlayerCard> cards = cardRepo.findAllByPlayerIdIn(playerIds);
                    ctx.cardsByPlayerIdCache.putAll(
                            cards.stream().collect(Collectors.groupingBy(card -> card.getPlayer().getId()))
                    );
                }
            }

            // C. Pre-load Nations
            List<FcoNation> nations = nationRepo.findAll();
            for (FcoNation n : nations) {
                if (hasText(n.getNationName())) {
                    ctx.nationCache.put(n.getNationName().toLowerCase().trim(), n);
                }
            }

            // D. Pre-load Leagues
            List<FcoLeague> leagues = leagueRepo.findAll();
            for (FcoLeague l : leagues) {
                if (hasText(l.getLeagueName())) {
                    ctx.leagueCache.put(l.getLeagueName().toLowerCase().trim(), l);
                }
            }

            // E. Pre-load Clubs
            List<FcoClub> clubs = clubRepo.findAll();
            for (FcoClub c : clubs) {
                if (c.getFifaaddictId() != null) {
                    ctx.clubByFifaIdCache.put(c.getFifaaddictId(), c);
                }
                if (hasText(c.getClubName())) {
                    String nameKey = c.getClubName().toLowerCase().trim();
                    ctx.clubByNameCache.put(nameKey, c);
                    if (c.getLeague() != null) {
                        ctx.clubByNameAndLeagueIdCache.put(nameKey + "_" + c.getLeague().getId(), c);
                    }
                }
            }

            // F. Pre-load Traits
            List<FcoTrait> traits = traitRepo.findAll();
            for (FcoTrait t : traits) {
                if (hasText(t.getTraitCode())) {
                    ctx.traitCache.put(t.getTraitCode().toLowerCase().trim(), t);
                }
            }

            log.info("[detail-sync] Warm-up lookups complete. Players: {}, Cards: {}, Nations: {}, Leagues: {}, Clubs: {}, Traits: {}",
                    ctx.playerCache.size(), cardsCount(ctx.cardsByPlayerIdCache), ctx.nationCache.size(),
                    ctx.leagueCache.size(), ctx.clubByNameCache.size(), ctx.traitCache.size());

        } catch (Exception ex) {
            log.error("[detail-sync] Warm-up lookup failure (running without pre-cache): {}", ex.getMessage(), ex);
        }
        return ctx;
    }

    private static int cardsCount(Map<Long, List<PlayerCard>> map) {
        return map.values().stream().mapToInt(List::size).sum();
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    public enum DetailSyncOutcome {
        OK,
        SKIPPED_PLAYER_NOT_FOUND,
        SKIPPED_NO_CARDS
    }

    private void saveDetailDeadLetter(SyncPlayerDetailRequest dto, Exception ex) {
        String payload = serializePayload(dto);
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        message = truncate(message);
        detailSyncErrorLogRepository.save(
                DetailSyncErrorLog.builder()
                        .externalId(dto.getExternalId())
                        .payload(payload)
                        .errorMessage(message)
                        .build()
        );
    }

    private String serializePayload(SyncPlayerDetailRequest dto) {
        try {
            return objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException e) {
            return "{\"error\":\"serialize_failed\"}";
        }
    }

    private static String truncate(String message) {
        if (message == null) {
            return "";
        }
        if (message.length() > MAX_ERROR_MESSAGE_LEN) {
            return message.substring(0, MAX_ERROR_MESSAGE_LEN);
        }
        return message;
    }
}
