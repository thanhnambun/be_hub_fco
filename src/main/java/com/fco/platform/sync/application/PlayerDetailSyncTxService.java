package com.fco.platform.sync.application;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fco.platform.sync.interfaces.dto.SyncPlayerDetailRequest;
import com.fco.platform.sync.domain.DetailSyncErrorLog;
import com.fco.platform.sync.infrastructure.persistence.DetailSyncErrorLogRepository;
import com.fco.platform.card.domain.FcoCardPrice;
import com.fco.platform.card.domain.FcoTrait;
import com.fco.platform.card.domain.PlayerCard;
import com.fco.platform.card.infrastructure.persistence.IFcoCardPriceRepository;
import com.fco.platform.card.infrastructure.persistence.IFcoTraitRepository;
import com.fco.platform.card.infrastructure.persistence.IPlayerCardRepository;
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
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * Giao dịch từng bản ghi detail — bean riêng để Spring AOP bọc @Transactional (tránh self-invocation).
 */
@Service
@RequiredArgsConstructor
public class PlayerDetailSyncTxService {

    private static final Logger log = LoggerFactory.getLogger(PlayerDetailSyncTxService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_ERROR_MESSAGE_LEN = 5000;

    private final IFcoPlayerRepository playerRepo;
    private final IPlayerCardRepository cardRepo;
    private final IFcoNationRepository nationRepo;
    private final IFcoLeagueRepository leagueRepo;
    private final IFcoClubRepository clubRepo;
    private final IFcoTraitRepository traitRepo;
    private final IFcoCardPriceRepository priceRepo;
    private final DetailSyncErrorLogRepository detailSyncErrorLogRepository;
    private final ObjectMapper objectMapper;

    @Transactional
    public PlayerDetailSyncService.DetailSyncOutcome syncOne(
            SyncPlayerDetailRequest dto,
            PlayerDetailSyncService.SyncContext ctx) {
        if ("pidymvogjdjy".equals(dto.getExternalId())) {
            throw new RuntimeException("FORCE_ROLLBACK");
        }

        FcoPlayer player = ctx.playerCache.get(dto.getExternalId());
        if (player == null) {
            player = playerRepo.findByExternalId(dto.getExternalId()).orElse(null);
        }

        if (player == null) {
            log.warn("[detail-sync] Không tìm thấy player externalId={} — bỏ qua", dto.getExternalId());
            saveDetailSkip(dto, "[SKIP] Không tìm thấy fco_players.external_id — chạy Pha A (sync/cards) trước.");
            return PlayerDetailSyncService.DetailSyncOutcome.SKIPPED_PLAYER_NOT_FOUND;
        }

        SyncPlayerDetailRequest.PlayerBioDTO bio = dto.getPlayerBio();

        if (bio != null) {
            FcoLeague league = null;
            if (hasText(bio.getLeagueName())) {
                league = upsertLeague(bio.getLeagueName(), bio.getLeagueSlug(), ctx);
            }

            FcoClub currentClub = null;
            if (bio.getClubs() != null && !bio.getClubs().isEmpty()) {
                for (SyncPlayerDetailRequest.ClubDTO clubDto : bio.getClubs()) {
                    if (hasText(clubDto.getClubName())) {
                        FcoClub c = upsertClub(clubDto, league, ctx);
                        player.getTeamColors().add(c);
                        if (currentClub == null) {
                            currentClub = c;
                        }
                    }
                }
            }

            FcoNation nation = null;
            if (hasText(bio.getNationName())) {
                nation = upsertNation(bio.getNationName(), bio.getNationSlug(), ctx);
            }

            updatePlayerBio(player, bio, nation, currentClub, league);
        }

        player = playerRepo.save(player);
        ctx.playerCache.put(player.getExternalId(), player);

        List<PlayerCard> cards = ctx.cardsByPlayerIdCache.get(player.getId());
        if (cards == null) {
            cards = cardRepo.findAllByPlayerId(player.getId());
        }

        if (cards.isEmpty()) {
            log.warn("[detail-sync] Player {} chưa có card nào — bỏ qua card/trait/price", dto.getExternalId());
            saveDetailSkip(dto, "[SKIP] Cầu thủ tồn tại nhưng chưa có dòng fco_player_cards — chạy Pha A cho mùa/level tương ứng.");
            return PlayerDetailSyncService.DetailSyncOutcome.SKIPPED_NO_CARDS;
        }

        SyncPlayerDetailRequest.CardDetailDTO cardDetail = dto.getCardDetail();

        for (PlayerCard card : cards) {
            if (cardDetail != null) {
                updateCardDetail(card, cardDetail);
            }
            if (dto.getTraits() != null && !dto.getTraits().isEmpty()) {
                syncTraits(card, dto.getTraits(), ctx);
            }
            if (dto.getPrices() != null && !dto.getPrices().isEmpty()) {
                syncPrices(card, dto.getPrices());
            }
            cardRepo.save(card);
        }
        return PlayerDetailSyncService.DetailSyncOutcome.OK;
    }

    private void saveDetailSkip(SyncPlayerDetailRequest dto, String reason) {
        detailSyncErrorLogRepository.save(
                DetailSyncErrorLog.builder()
                        .externalId(dto.getExternalId())
                        .payload(serializePayload(dto))
                        .errorMessage(truncate(reason))
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

    private FcoLeague upsertLeague(String leagueName, String leagueSlug, PlayerDetailSyncService.SyncContext ctx) {
        String key = leagueName.toLowerCase().trim();
        FcoLeague cached = ctx.leagueCache.get(key);
        if (cached != null) {
            return cached;
        }

        FcoLeague league = leagueRepo.findByLeagueName(leagueName).orElseGet(() -> {
            FcoLeague l = FcoLeague.builder()
                    .leagueName(leagueName)
                    .leagueSlug(leagueSlug)
                    .build();
            log.info("[detail-sync] Tạo mới League: {}", leagueName);
            return leagueRepo.save(l);
        });

        ctx.leagueCache.put(key, league);
        return league;
    }

    private FcoClub upsertClub(SyncPlayerDetailRequest.ClubDTO clubDto, FcoLeague league,
                               PlayerDetailSyncService.SyncContext ctx) {
        if (clubDto.getClubFifaaddictId() != null) {
            FcoClub cached = ctx.clubByFifaIdCache.get(clubDto.getClubFifaaddictId());
            if (cached != null) {
                if (cached.getLeague() == null && league != null) {
                    cached.setLeague(league);
                    cached.setUpdatedAt(LocalDateTime.now());
                    cached = clubRepo.save(cached);
                    updateClubInCaches(cached, ctx);
                }
                return cached;
            }
        }

        String nameKey = clubDto.getClubName().toLowerCase().trim();
        if (league != null) {
            FcoClub cached = ctx.clubByNameAndLeagueIdCache.get(nameKey + "_" + league.getId());
            if (cached != null) {
                return cached;
            }
        } else {
            FcoClub cached = ctx.clubByNameCache.get(nameKey);
            if (cached != null) {
                return cached;
            }
        }

        FcoClub club;
        if (clubDto.getClubFifaaddictId() != null) {
            Optional<FcoClub> byFifaId = clubRepo.findByFifaaddictId(clubDto.getClubFifaaddictId());
            if (byFifaId.isPresent()) {
                FcoClub c = byFifaId.get();
                if (c.getLeague() == null && league != null) {
                    c.setLeague(league);
                    c.setUpdatedAt(LocalDateTime.now());
                    c = clubRepo.save(c);
                }
                club = c;
            } else {
                club = createNewClub(clubDto, league);
            }
        } else {
            Optional<FcoClub> byName = (league != null)
                    ? clubRepo.findByClubNameAndLeague(clubDto.getClubName(), league)
                    : clubRepo.findByClubName(clubDto.getClubName());
            club = byName.orElseGet(() -> createNewClub(clubDto, league));
        }

        updateClubInCaches(club, ctx);
        return club;
    }

    private FcoClub createNewClub(SyncPlayerDetailRequest.ClubDTO clubDto, FcoLeague league) {
        FcoClub c = FcoClub.builder()
                .clubName(clubDto.getClubName())
                .clubSlug(clubDto.getClubSlug())
                .fifaaddictId(clubDto.getClubFifaaddictId())
                .crestUrl(clubDto.getCrestUrl())
                .league(league)
                .build();
        log.info("[detail-sync] Tạo mới Club: {}", clubDto.getClubName());
        return clubRepo.save(c);
    }

    private void updateClubInCaches(FcoClub club, PlayerDetailSyncService.SyncContext ctx) {
        if (club.getFifaaddictId() != null) {
            ctx.clubByFifaIdCache.put(club.getFifaaddictId(), club);
        }
        if (hasText(club.getClubName())) {
            String nameKey = club.getClubName().toLowerCase().trim();
            ctx.clubByNameCache.put(nameKey, club);
            if (club.getLeague() != null) {
                ctx.clubByNameAndLeagueIdCache.put(nameKey + "_" + club.getLeague().getId(), club);
            }
        }
    }

    private FcoNation upsertNation(String nationName, String nationSlug, PlayerDetailSyncService.SyncContext ctx) {
        String key = nationName.toLowerCase().trim();
        FcoNation cached = ctx.nationCache.get(key);
        if (cached != null) {
            return cached;
        }

        FcoNation nation = nationRepo.findByNationName(nationName).orElseGet(() -> {
            FcoNation n = FcoNation.builder()
                    .nationName(nationName)
                    .nationSlug(nationSlug)
                    .build();
            log.info("[detail-sync] Tạo mới Nation: {}", nationName);
            return nationRepo.save(n);
        });

        ctx.nationCache.put(key, nation);
        return nation;
    }

    private void updatePlayerBio(FcoPlayer player,
                                 SyncPlayerDetailRequest.PlayerBioDTO bio,
                                 FcoNation nation,
                                 FcoClub club,
                                 FcoLeague league) {
        if (bio.getHeight() != null) player.setHeight(bio.getHeight());
        if (bio.getWeight() != null) player.setWeight(bio.getWeight());
        if (hasText(bio.getBirthdate())) player.setBirthdate(bio.getBirthdate());
        if (hasText(bio.getPreferredFoot())) player.setPreferredFoot(bio.getPreferredFoot());
        if (bio.getWeakFoot() != null) player.setWeakFoot(bio.getWeakFoot());

        if (nation != null) {
            player.setNation(nation);
            player.setNationName(nation.getNationName());
        }
        if (club != null) {
            player.setClub(club);
            player.setClubName(club.getClubName());
        }
        if (league != null) {
            player.setLeague(league);
            player.setLeagueName(league.getLeagueName());
        }

        player.setUpdatedAt(LocalDateTime.now());
    }

    private void updateCardDetail(PlayerCard card, SyncPlayerDetailRequest.CardDetailDTO d) {
        if (d.getLiveperf() != null) card.setLiveperf(d.getLiveperf());
        if (d.getSkillLevel() != null) card.setSkillLevel(d.getSkillLevel());
        if (hasText(d.getSecondaryPosition())) card.setSecondaryPosition(d.getSecondaryPosition());
        if (hasText(d.getWorkerateAtt())) card.setWorkerateAtt(d.getWorkerateAtt());
        if (hasText(d.getWorkerateDef())) card.setWorkerateDef(d.getWorkerateDef());
        if (hasText(d.getBodytype())) card.setBodytype(d.getBodytype());
        if (hasText(d.getReputation())) card.setReputation(d.getReputation());

        if (d.getOvrByPos() != null && !d.getOvrByPos().isEmpty()) {
            card.setOvrByPos(d.getOvrByPos());
        }

        if (d.getPace() != null) card.setPace(d.getPace());
        if (d.getShooting() != null) card.setShooting(d.getShooting());
        if (d.getPassing() != null) card.setPassing(d.getPassing());
        if (d.getDribbling() != null) card.setDribbling(d.getDribbling());
        if (d.getDefending() != null) card.setDefending(d.getDefending());
        if (d.getPhysicality() != null) card.setPhysicality(d.getPhysicality());

        LocalDate pDate = parseDate(d.getPriceUpdatedAt());
        if (pDate != null) card.setPriceUpdatedAt(pDate);

        card.setUpdatedAt(LocalDateTime.now());
    }

    private void syncTraits(PlayerCard card, List<SyncPlayerDetailRequest.TraitDTO> traitDTOs,
                            PlayerDetailSyncService.SyncContext ctx) {
        for (SyncPlayerDetailRequest.TraitDTO dto : traitDTOs) {
            if (!hasText(dto.getTraitCode())) continue;

            String key = dto.getTraitCode().toLowerCase().trim();
            FcoTrait trait = ctx.traitCache.get(key);
            if (trait == null) {
                trait = traitRepo.findByTraitCode(dto.getTraitCode()).orElseGet(() -> {
                    FcoTrait t = FcoTrait.builder()
                            .traitCode(dto.getTraitCode())
                            .traitName(hasText(dto.getTraitName()) ? dto.getTraitName() : dto.getTraitCode())
                            .description(dto.getDescription())
                            .iconId(dto.getIconId())
                            .iconUrl(dto.getIconUrl())
                            .build();
                    return traitRepo.save(t);
                });
                ctx.traitCache.put(key, trait);
            }

            card.getTraits().add(trait);
        }
    }

    private void syncPrices(PlayerCard card, List<SyncPlayerDetailRequest.PriceDTO> priceDTOs) {
        for (SyncPlayerDetailRequest.PriceDTO dto : priceDTOs) {
            if (dto.getGrade() == null || dto.getPriceBp() == null) continue;

            LocalDate priceDate = parseDate(dto.getPriceDate());
            if (priceDate == null) continue;

            boolean exists = priceRepo.findByCardIdAndGradeAndPriceDate(
                    card.getId(), dto.getGrade(), priceDate).isPresent();
            if (exists) continue;

            FcoCardPrice price = FcoCardPrice.builder()
                    .card(card)
                    .grade(dto.getGrade())
                    .priceBp(dto.getPriceBp())
                    .priceRaw(dto.getPriceRaw())
                    .priceDate(priceDate)
                    .build();
            priceRepo.save(price);

            if (dto.getGrade() == 1) {
                card.setMarketPriceBp(dto.getPriceBp());
            }
        }
    }

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static LocalDate parseDate(String dateStr) {
        if (!hasText(dateStr)) return null;
        try {
            return LocalDate.parse(dateStr.substring(0, 10), DATE_FMT);
        } catch (DateTimeParseException e) {
            try {
                long epoch = Long.parseLong(dateStr);
                return LocalDate.ofEpochDay(epoch / 86400);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }
}
