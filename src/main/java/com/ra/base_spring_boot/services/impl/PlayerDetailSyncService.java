package com.ra.base_spring_boot.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ra.base_spring_boot.dto.req.PlayerDetailDTO;
import com.ra.base_spring_boot.dto.resp.SyncCardsResult;
import com.ra.base_spring_boot.model.DetailSyncErrorLog;
import com.ra.base_spring_boot.model.fco.*;
import com.ra.base_spring_boot.repository.*;
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
import java.util.Optional;

/**
 * Xử lý payload từ detail_crawler.py (Pha B).
 *
 * Với mỗi PlayerDetailDTO:
 *   1. Lookup fco_players bằng externalId
 *   2. UPSERT fco_leagues  → league entity
 *   3. UPSERT fco_clubs    → club entity (liên kết với league)
 *   4. UPSERT fco_nations  → nation entity
 *   5. UPDATE fco_players  (bio + FK)
 *   6. Lookup fco_player_cards (mọi enhance_level của player đó)
 *   7. UPDATE fco_player_cards (liveperf, stats, ovr_by_pos, info thẻ)
 *   8. UPSERT fco_traits   → upsert từng trait
 *   9. Gán traits vào card (add vào Set, Hibernate quản lý join table)
 *  10. INSERT giá 13 grade (bỏ qua nếu đã tồn tại theo UNIQUE KEY)
 *  11. Cache market_price_bp = giá grade 1 mới nhất lên fco_player_cards
 *
 * Lỗi / skip (không map được player, không có thẻ, exception) được ghi vào {@code fco_detail_sync_errors}.
 */
@Service
@RequiredArgsConstructor
public class PlayerDetailSyncService {

    private static final Logger log = LoggerFactory.getLogger(PlayerDetailSyncService.class);
    private static final DateTimeFormatter DATE_FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final int MAX_ERROR_MESSAGE_LEN = 5000;

    private final IFcoPlayerRepository      playerRepo;
    private final IPlayerCardRepository     cardRepo;
    private final IFcoNationRepository      nationRepo;
    private final IFcoLeagueRepository      leagueRepo;
    private final IFcoClubRepository        clubRepo;
    private final IFcoTraitRepository       traitRepo;
    private final IFcoCardPriceRepository   priceRepo;
    private final DetailSyncErrorLogRepository detailSyncErrorLogRepository;
    private final ObjectMapper              objectMapper;

    // ──────────────────────────────────────────────────────────────────────────
    // Public entry point
    // ──────────────────────────────────────────────────────────────────────────

    public SyncCardsResult syncDetailBatch(List<PlayerDetailDTO> batch) {
        long start = System.currentTimeMillis();
        if (batch == null || batch.isEmpty()) {
            return SyncCardsResult.builder()
                    .totalReceived(0).totalSuccessful(0).totalFailed(0).processingTimeMs(0L).build();
        }

        int success = 0;
        int failed  = 0;

        for (PlayerDetailDTO dto : batch) {
            try {
                DetailSyncOutcome outcome = syncOne(dto);
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

    // ──────────────────────────────────────────────────────────────────────────
    // Xử lý 1 cầu thủ — mỗi record là 1 transaction độc lập
    // ──────────────────────────────────────────────────────────────────────────

    /**
     * @return {@link DetailSyncOutcome#OK} nếu đã áp dụng payload lên ít nhất một thẻ; các trường hợp bỏ qua
     *         (không có player / không có card) trả về enum tương ứng và ghi vào {@code fco_detail_sync_errors}.
     */
    @Transactional
    public DetailSyncOutcome syncOne(PlayerDetailDTO dto) {
        // 1. Lookup player — phải tồn tại từ Pha A
        FcoPlayer player = playerRepo.findByExternalId(dto.getExternalId())
                .orElse(null);

        if (player == null) {
            log.warn("[detail-sync] Không tìm thấy player externalId={} — bỏ qua", dto.getExternalId());
            saveDetailSkip(dto, "[SKIP] Không tìm thấy fco_players.external_id — chạy Pha A (sync/cards) trước.");
            return DetailSyncOutcome.SKIPPED_PLAYER_NOT_FOUND;
        }

        PlayerDetailDTO.PlayerBioDTO bio = dto.getPlayerBio();

        if (bio != null) {
            // 2. UPSERT League
            FcoLeague league = null;
            if (hasText(bio.getLeagueName())) {
                league = upsertLeague(bio.getLeagueName(), bio.getLeagueSlug());
            }

            // 3. UPSERT Clubs (sự nghiệp CLB / Team Color)
            FcoClub currentClub = null;
            if (bio.getClubs() != null && !bio.getClubs().isEmpty()) {
                for (int i = 0; i < bio.getClubs().size(); i++) {
                    PlayerDetailDTO.ClubDTO clubDto = bio.getClubs().get(i);
                    if (hasText(clubDto.getClubName())) {
                        FcoClub c = upsertClub(clubDto, league);
                        player.getTeamColors().add(c);
                        
                        // Chọn club đầu tiên làm club hiển thị chính
                        if (currentClub == null) {
                            currentClub = c;
                        }
                    }
                }
            }

            // 4. UPSERT Nation
            FcoNation nation = null;
            if (hasText(bio.getNationName())) {
                nation = upsertNation(bio.getNationName(), bio.getNationSlug());
            }

            // 5. UPDATE player bio + FK
            updatePlayerBio(player, bio, nation, currentClub, league);
        }

        player = playerRepo.save(player);

        // 6. Lookup tất cả card của player (mọi enhance_level)
        List<PlayerCard> cards = cardRepo.findAllByPlayerId(player.getId());
        if (cards.isEmpty()) {
            log.warn("[detail-sync] Player {} chưa có card nào — bỏ qua card/trait/price", dto.getExternalId());
            saveDetailSkip(dto, "[SKIP] Cầu thủ tồn tại nhưng chưa có dòng fco_player_cards — chạy Pha A cho mùa/level tương ứng.");
            return DetailSyncOutcome.SKIPPED_NO_CARDS;
        }

        PlayerDetailDTO.CardDetailDTO cardDetail = dto.getCardDetail();

        for (PlayerCard card : cards) {
            // 7. UPDATE card detail
            if (cardDetail != null) {
                updateCardDetail(card, cardDetail);
            }

            // 8 & 9. UPSERT traits + gán vào card
            if (dto.getTraits() != null && !dto.getTraits().isEmpty()) {
                syncTraits(card, dto.getTraits());
            }

            // 10. INSERT giá 13 grade
            if (dto.getPrices() != null && !dto.getPrices().isEmpty()) {
                syncPrices(card, dto.getPrices());
            }

            cardRepo.save(card);
        }
        return DetailSyncOutcome.OK;
    }

    public enum DetailSyncOutcome {
        OK,
        SKIPPED_PLAYER_NOT_FOUND,
        SKIPPED_NO_CARDS
    }

    private void saveDetailDeadLetter(PlayerDetailDTO dto, Exception ex) {
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

    private void saveDetailSkip(PlayerDetailDTO dto, String reason) {
        detailSyncErrorLogRepository.save(
                DetailSyncErrorLog.builder()
                        .externalId(dto.getExternalId())
                        .payload(serializePayload(dto))
                        .errorMessage(truncate(reason))
                        .build()
        );
    }

    private String serializePayload(PlayerDetailDTO dto) {
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

    // ──────────────────────────────────────────────────────────────────────────
    // UPSERT helpers
    // ──────────────────────────────────────────────────────────────────────────

    private FcoLeague upsertLeague(String leagueName, String leagueSlug) {
        return leagueRepo.findByLeagueName(leagueName).orElseGet(() -> {
            FcoLeague l = FcoLeague.builder()
                    .leagueName(leagueName)
                    .leagueSlug(leagueSlug)
                    .build();
            log.info("[detail-sync] Tạo mới League: {}", leagueName);
            return leagueRepo.save(l);
        });
    }

    private FcoClub upsertClub(PlayerDetailDTO.ClubDTO clubDto, FcoLeague league) {
        // Nếu có fifaaddictId thì ưu tiên lookup theo ID trước (chính xác hơn)
        if (clubDto.getClubFifaaddictId() != null) {
            Optional<FcoClub> byFifaId = clubRepo.findByFifaaddictId(clubDto.getClubFifaaddictId());
            if (byFifaId.isPresent()) {
                FcoClub c = byFifaId.get();
                // Cập nhật league nếu chưa có
                if (c.getLeague() == null && league != null) {
                    c.setLeague(league);
                    c.setUpdatedAt(LocalDateTime.now());
                    return clubRepo.save(c);
                }
                return c;
            }
        }

        // Fallback: lookup theo tên + league
        Optional<FcoClub> byName = (league != null)
                ? clubRepo.findByClubNameAndLeague(clubDto.getClubName(), league)
                : clubRepo.findByClubName(clubDto.getClubName());

        return byName.orElseGet(() -> {
            FcoClub c = FcoClub.builder()
                    .clubName(clubDto.getClubName())
                    .clubSlug(clubDto.getClubSlug())
                    .fifaaddictId(clubDto.getClubFifaaddictId())
                    .crestUrl(clubDto.getCrestUrl())
                    .league(league)
                    .build();
            log.info("[detail-sync] Tạo mới Club: {}", clubDto.getClubName());
            return clubRepo.save(c);
        });
    }

    private FcoNation upsertNation(String nationName, String nationSlug) {
        return nationRepo.findByNationName(nationName).orElseGet(() -> {
            FcoNation n = FcoNation.builder()
                    .nationName(nationName)
                    .nationSlug(nationSlug)
                    .build();
            log.info("[detail-sync] Tạo mới Nation: {}", nationName);
            return nationRepo.save(n);
        });
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Update helpers
    // ──────────────────────────────────────────────────────────────────────────

    private void updatePlayerBio(FcoPlayer player,
                                 PlayerDetailDTO.PlayerBioDTO bio,
                                 FcoNation nation,
                                 FcoClub club,
                                 FcoLeague league) {
        if (bio.getHeight()       != null) player.setHeight(bio.getHeight());
        if (bio.getWeight()       != null) player.setWeight(bio.getWeight());
        if (hasText(bio.getBirthdate()))   player.setBirthdate(bio.getBirthdate());
        if (hasText(bio.getPreferredFoot())) player.setPreferredFoot(bio.getPreferredFoot());
        if (bio.getWeakFoot()     != null) player.setWeakFoot(bio.getWeakFoot());

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

    private void updateCardDetail(PlayerCard card, PlayerDetailDTO.CardDetailDTO d) {
        if (d.getLiveperf()          != null) card.setLiveperf(d.getLiveperf());
        if (d.getSkillLevel()        != null) card.setSkillLevel(d.getSkillLevel());
        if (hasText(d.getSecondaryPosition())) card.setSecondaryPosition(d.getSecondaryPosition());
        if (hasText(d.getWorkerateAtt()))      card.setWorkerateAtt(d.getWorkerateAtt());
        if (hasText(d.getWorkerateDef()))      card.setWorkerateDef(d.getWorkerateDef());
        if (hasText(d.getBodytype()))          card.setBodytype(d.getBodytype());
        if (hasText(d.getReputation()))        card.setReputation(d.getReputation());
        if (hasText(d.getOvrByPosJson()))      card.setOvrByPosJson(d.getOvrByPosJson());

        // 6 chỉ số — chỉ ghi đè nếu có giá trị mới
        if (d.getPace()        != null) card.setPace(d.getPace());
        if (d.getShooting()    != null) card.setShooting(d.getShooting());
        if (d.getPassing()     != null) card.setPassing(d.getPassing());
        if (d.getDribbling()   != null) card.setDribbling(d.getDribbling());
        if (d.getDefending()   != null) card.setDefending(d.getDefending());
        if (d.getPhysicality() != null) card.setPhysicality(d.getPhysicality());

        // Ngày cập nhật giá
        LocalDate pDate = parseDate(d.getPriceUpdatedAt());
        if (pDate != null) card.setPriceUpdatedAt(pDate);

        card.setUpdatedAt(LocalDateTime.now());
    }

    private void syncTraits(PlayerCard card, List<PlayerDetailDTO.TraitDTO> traitDTOs) {
        for (PlayerDetailDTO.TraitDTO dto : traitDTOs) {
            if (!hasText(dto.getTraitCode())) continue;

            FcoTrait trait = traitRepo.findByTraitCode(dto.getTraitCode()).orElseGet(() -> {
                FcoTrait t = FcoTrait.builder()
                        .traitCode(dto.getTraitCode())
                        .traitName(hasText(dto.getTraitName()) ? dto.getTraitName() : dto.getTraitCode())
                        .description(dto.getDescription())
                        .iconId(dto.getIconId())
                        .iconUrl(dto.getIconUrl())
                        .build();
                return traitRepo.save(t);
            });

            // Hibernate quản lý join table fco_player_card_traits
            card.getTraits().add(trait);
        }
    }

    private void syncPrices(PlayerCard card, List<PlayerDetailDTO.PriceDTO> priceDTOs) {
        for (PlayerDetailDTO.PriceDTO dto : priceDTOs) {
            if (dto.getGrade() == null || dto.getPriceBp() == null) continue;

            LocalDate priceDate = parseDate(dto.getPriceDate());
            if (priceDate == null) continue;

            // INSERT IGNORE — bỏ qua nếu đã tồn tại (theo UNIQUE KEY)
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

            // 11. Cache grade 1 lên card.market_price_bp
            if (dto.getGrade() == 1) {
                card.setMarketPriceBp(dto.getPriceBp());
            }
        }
    }

    // ──────────────────────────────────────────────────────────────────────────
    // Utils
    // ──────────────────────────────────────────────────────────────────────────

    private static boolean hasText(String s) {
        return s != null && !s.isBlank();
    }

    private static LocalDate parseDate(String dateStr) {
        if (!hasText(dateStr)) return null;
        try {
            // FIFAAddict updatetime có thể là "2026-05-11" hoặc epoch seconds
            // Thử parse ISO date trước
            return LocalDate.parse(dateStr.substring(0, 10), DATE_FMT);
        } catch (DateTimeParseException e) {
            // Thử parse epoch seconds
            try {
                long epoch = Long.parseLong(dateStr);
                return LocalDate.ofEpochDay(epoch / 86400);
            } catch (NumberFormatException nfe) {
                return null;
            }
        }
    }
}
