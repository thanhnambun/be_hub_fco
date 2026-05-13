package com.ra.base_spring_boot.services.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ra.base_spring_boot.dto.req.PlayerCardDTO;
import com.ra.base_spring_boot.dto.resp.SyncCardsResult;
import com.ra.base_spring_boot.model.SyncErrorLog;
import com.ra.base_spring_boot.repository.SyncErrorLogRepository;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validator;

import java.sql.PreparedStatement;
import java.sql.SQLException;
import java.sql.Types;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PlayerSyncService {
    private static final Logger log = LoggerFactory.getLogger(PlayerSyncService.class);
    private static final int CHUNK_SIZE = 1000;
    private static final int MAX_IMAGE_URL_LEN = 500;

    // Lá»‡nh SQL tá»± Ä‘á»™ng thÃªm/cáº­p nháº­t thÃ´ng tin gá»‘c cá»§a cáº§u thá»§
    private static final String UPSERT_PLAYERS_SQL = """
            INSERT INTO fco_players (external_id, player_name)
            VALUES (?, ?)
            ON DUPLICATE KEY UPDATE player_name = VALUES(player_name)
            """;

    private static final String INSERT_IGNORE_SEASON_SQL = """
            INSERT IGNORE INTO fco_seasons (season_code, season_name)
            VALUES (?, ?)
            """;

    private static final String UPSERT_CARDS_SQL = """
            INSERT INTO fco_player_cards (player_id, season_id, enhance_level, ovr, salary, preferred_position, market_price_bp, image_url, pace, shooting, passing, dribbling, defending, physicality, updated_at, liveperf, created_at, is_active)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, NOW(), 0, NOW(), 1)
            ON DUPLICATE KEY UPDATE
                ovr = VALUES(ovr),
                salary = VALUES(salary),
                preferred_position = VALUES(preferred_position),
                market_price_bp = VALUES(market_price_bp),
                image_url = VALUES(image_url),
                pace = COALESCE(VALUES(pace), pace),
                shooting = COALESCE(VALUES(shooting), shooting),
                passing = COALESCE(VALUES(passing), passing),
                dribbling = COALESCE(VALUES(dribbling), dribbling),
                defending = COALESCE(VALUES(defending), defending),
                physicality = COALESCE(VALUES(physicality), physicality),
                updated_at = NOW()
            """;

    private final JdbcTemplate jdbcTemplate;
    private final SyncErrorLogRepository syncErrorLogRepository;
    private final ObjectMapper objectMapper;
    private final Validator validator;

    public SyncCardsResult syncPlayerCards(List<PlayerCardDTO> batchList) {
        long startedAt = System.currentTimeMillis();
        if (batchList == null || batchList.isEmpty()) {
            return SyncCardsResult.builder()
                    .totalReceived(0).totalSuccessful(0).totalFailed(0).processingTimeMs(0L).build();
        }
        int totalReceived = batchList.size();
        int failed = 0;

        List<PlayerCardDTO> validRows = new ArrayList<>();
        for (PlayerCardDTO dto : batchList) {
            if (dto == null) {
                failed++;
                continue;
            }
            Set<ConstraintViolation<PlayerCardDTO>> violations = validator.validate(dto);
            if (!violations.isEmpty()) {
                String errorMsg = violations.stream()
                        .map(v -> v.getPropertyPath() + ": " + v.getMessage())
                        .collect(Collectors.joining(", "));
                log.warn("Validation failed (externalId={}): {}", dto.getExternalId(), errorMsg);
                saveDeadLetter(dto, new IllegalArgumentException("Validation Error: " + errorMsg));
                failed++;
            } else {
                validRows.add(dto);
            }
        }

        if (validRows.isEmpty()) {
            return SyncCardsResult.builder()
                    .totalReceived(totalReceived).totalSuccessful(0).totalFailed(failed)
                    .processingTimeMs(System.currentTimeMillis() - startedAt).build();
        }

        // =================================================================================
        // BÆ¯á»šC 1: PRE-SYNC PHASE (Auto-Create Seasons & Players)
        // =================================================================================
        
        // 1.1. Auto-Season (INSERT IGNORE)
        List<PlayerCardDTO> uniqueSeasons = validRows.stream()
                .filter(distinctByKey(dto -> dto.getSeasonCode().trim().toUpperCase(Locale.ROOT)))
                .toList();
        for (int start = 0; start < uniqueSeasons.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, uniqueSeasons.size());
            List<PlayerCardDTO> seasonChunk = uniqueSeasons.subList(start, end);
            jdbcTemplate.batchUpdate(INSERT_IGNORE_SEASON_SQL, seasonChunk, seasonChunk.size(), (ps, dto) -> {
                String code = dto.getSeasonCode().trim().toUpperCase(Locale.ROOT);
                ps.setString(1, code);
                ps.setString(2, code); // Sá»­ dá»¥ng code lÃ m tÃªn máº·c Ä‘á»‹nh
            });
        }

        // 1.2. Auto-Player (UPSERT - Cáº­p nháº­t tÃªn náº¿u Ä‘Ã£ tá»“n táº¡i)
        List<PlayerCardDTO> uniquePlayers = validRows.stream()
                .filter(distinctByKey(dto -> dto.getExternalId().trim()))
                .toList();
        for (int start = 0; start < uniquePlayers.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, uniquePlayers.size());
            List<PlayerCardDTO> playerChunk = uniquePlayers.subList(start, end);
            jdbcTemplate.batchUpdate(UPSERT_PLAYERS_SQL, playerChunk, playerChunk.size(), (ps, dto) -> {
                ps.setString(1, dto.getExternalId().trim());
                ps.setString(2, dto.getName() != null ? dto.getName() : "Unknown");
            });
        }

        // =================================================================================
        // BÆ¯á»šC 2: RE-FETCH MAPS (Load ID má»›i lÃªn RAM)
        // =================================================================================
        Set<String> externalIds = validRows.stream()
                .map(PlayerCardDTO::getExternalId)
                .map(String::trim)
                .collect(Collectors.toCollection(LinkedHashSet::new));

        Set<String> seasonCodes = validRows.stream()
                .map(PlayerCardDTO::getSeasonCode)
                .map(code -> code.trim().toUpperCase(Locale.ROOT))
                .collect(Collectors.toCollection(LinkedHashSet::new));

        // Äáº£m báº£o Player vÃ  Season tá»“n táº¡i trÆ°á»›c khi láº¥y ID Map
        ensureParentsExist(validRows, seasonCodes);

        // Cache ID Ä‘á»ƒ trÃ¡nh N+1 query khi xá»­ lÃ½ danh sÃ¡ch lá»›n.
        Map<String, Long> playerIdByExternalId = fetchPlayerIdMap(externalIds);
        Map<String, Long> seasonIdByCode = fetchSeasonIdMap(seasonCodes);

        List<ResolvedRow> resolvedRows = new ArrayList<>(validRows.size());
        for (PlayerCardDTO dto : validRows) {
            String externalId = dto.getExternalId().trim();
            String seasonCode = dto.getSeasonCode().trim().toUpperCase(Locale.ROOT);

            Long playerId = playerIdByExternalId.get(externalId);
            Long seasonId = seasonIdByCode.get(seasonCode);
            
            if (playerId == null || seasonId == null) {
                log.warn("Bo qua ban ghi do khong map duoc ID (externalId={}, seasonCode={})", externalId, seasonCode);
                continue;
            }

            resolvedRows.add(new ResolvedRow(
                    externalId, playerId, seasonId,
                    dto.getEnhanceLevel() == null ? 1 : Math.max(dto.getEnhanceLevel(), 1),
                    dto.getOvr(),
                    dto.getSalary() == null ? 0 : Math.max(dto.getSalary(), 0),
                    dto.getPreferredPosition(),
                    dto.getMarketPriceBp(),
                    dto.getImageUrl(),
                    dto.getPace(),
                    dto.getShooting(),
                    dto.getPassing(),
                    dto.getDribbling(),
                    dto.getDefending(),
                    dto.getPhysicality(),
                    dto
            ));
        }

        failed += validRows.size() - resolvedRows.size();
        int successful = 0;

        // BÆ¯á»šC 3: Batch Update Tháº» Cáº§u Thá»§ (Child)
        for (int start = 0; start < resolvedRows.size(); start += CHUNK_SIZE) {
            int end = Math.min(start + CHUNK_SIZE, resolvedRows.size());
            List<ResolvedRow> chunk = resolvedRows.subList(start, end);
            try {
                jdbcTemplate.batchUpdate(UPSERT_CARDS_SQL, chunk, chunk.size(), this::bindUpsertParams);
                successful += chunk.size();
            } catch (DataAccessException ex) {
                log.error("Chunk batch loi (size={}), fallback sang upsert tung dong", chunk.size(), ex);
                for (ResolvedRow row : chunk) {
                    try {
                        jdbcTemplate.update(UPSERT_CARDS_SQL, ps -> bindUpsertParams(ps, row));
                        successful++;
                    } catch (DataAccessException singleEx) {
                        failed++;
                        saveDeadLetter(row.originalDto(), singleEx);
                    }
                }
            }
        }

        return SyncCardsResult.builder()
                .totalReceived(totalReceived)
                .totalSuccessful(successful)
                .totalFailed(failed)
                .processingTimeMs(System.currentTimeMillis() - startedAt)
                .build();
    }

    private void bindUpsertParams(PreparedStatement ps, ResolvedRow row) throws SQLException {
        ps.setLong(1, row.playerId());
        ps.setLong(2, row.seasonId());
        ps.setInt(3, row.enhanceLevel());
        ps.setInt(4, row.ovr());
        ps.setInt(5, row.salary());
        if (row.preferredPosition() == null || row.preferredPosition().isBlank()) {
            ps.setNull(6, Types.VARCHAR);
        } else {
            ps.setString(6, row.preferredPosition().trim().toUpperCase(Locale.ROOT));
        }
        if (row.marketPriceBp() == null) {
            ps.setNull(7, Types.BIGINT);
        } else {
            ps.setLong(7, row.marketPriceBp());
        }
        if (row.imageUrl() == null || row.imageUrl().isBlank()) {
            ps.setNull(8, Types.VARCHAR);
        } else {
            String img = row.imageUrl();
            if (img.length() > MAX_IMAGE_URL_LEN) {
                img = img.substring(0, MAX_IMAGE_URL_LEN);
            }
            ps.setString(8, img);
        }
        setNullableInt(ps, 9, row.pace());
        setNullableInt(ps, 10, row.shooting());
        setNullableInt(ps, 11, row.passing());
        setNullableInt(ps, 12, row.dribbling());
        setNullableInt(ps, 13, row.defending());
        setNullableInt(ps, 14, row.physicality());
    }

    private static void setNullableInt(PreparedStatement ps, int index, Integer value) throws SQLException {
        if (value == null) {
            ps.setNull(index, Types.INTEGER);
        } else {
            ps.setInt(index, value);
        }
    }

    private void ensureParentsExist(List<PlayerCardDTO> rows, Set<String> seasonCodes) {
        // 1. Tá»± Ä‘á»™ng táº¡o Seasons náº¿u chÆ°a cÃ³
        if (!seasonCodes.isEmpty()) {
            String sql = "INSERT IGNORE INTO fco_seasons (season_code, season_name) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sql, seasonCodes.stream()
                    .map(code -> new Object[]{code, code})
                    .toList());
        }

        // 2. Tá»± Ä‘á»™ng táº¡o Players náº¿u chÆ°a cÃ³
        if (!rows.isEmpty()) {
            // Láº¥y danh sÃ¡ch duy nháº¥t theo externalId Ä‘á»ƒ batch insert hiá»‡u quáº£
            Map<String, String> uniquePlayers = rows.stream()
                    .filter(r -> r.getExternalId() != null && !r.getExternalId().isBlank())
                    .collect(Collectors.toMap(
                            PlayerCardDTO::getExternalId,
                            r -> r.getName() != null ? r.getName() : "Unknown",
                            (existing, replacement) -> existing
                    ));

            String sql = "INSERT IGNORE INTO fco_players (external_id, player_name) VALUES (?, ?)";
            jdbcTemplate.batchUpdate(sql, uniquePlayers.entrySet().stream()
                    .map(e -> new Object[]{e.getKey(), e.getValue()})
                    .toList());
        }
    }

    private Map<String, Long> fetchPlayerIdMap(Set<String> externalIds) {
        Map<String, Long> map = new HashMap<>();
        for (List<String> chunk : partition(externalIds, CHUNK_SIZE)) {
            String sql = "SELECT id, external_id FROM fco_players WHERE external_id IN (" + placeholders(chunk.size()) + ")";
            jdbcTemplate.query(sql, chunk.toArray(), rs -> {
                map.put(rs.getString("external_id"), rs.getLong("id"));
            });
        }
        return map;
    }

    private Map<String, Long> fetchSeasonIdMap(Set<String> seasonCodes) {
        Map<String, Long> map = new HashMap<>();
        for (List<String> chunk : partition(seasonCodes, CHUNK_SIZE)) {
            String sql = "SELECT id, season_code FROM fco_seasons WHERE season_code IN (" + placeholders(chunk.size()) + ")";
            jdbcTemplate.query(sql, chunk.toArray(), rs -> {
                map.put(rs.getString("season_code"), rs.getLong("id"));
            });
        }
        return map;
    }

    private String placeholders(int count) {
        return "?,".repeat(Math.max(0, count - 1)) + "?";
    }

    private <T> List<List<T>> partition(Collection<T> values, int size) {
        List<T> list = new ArrayList<>(values);
        List<List<T>> out = new ArrayList<>();
        for (int i = 0; i < list.size(); i += size) {
            out.add(list.subList(i, Math.min(i + size, list.size())));
        }
        return out;
    }

    private void saveDeadLetter(PlayerCardDTO dto, Exception ex) {
        String payload;
        try {
            payload = objectMapper.writeValueAsString(dto);
        } catch (JsonProcessingException jsonEx) {
            payload = "{\"error\":\"serialize_failed\"}";
        }

        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = ex.getClass().getSimpleName();
        }
        if (message.length() > 5000) {
            message = message.substring(0, 5000);
        }

        syncErrorLogRepository.save(
                SyncErrorLog.builder()
                        .externalId(dto.getExternalId())
                        .payload(payload)
                        .errorMessage(message)
                        .build()
        );
    }

    public static <T> Predicate<T> distinctByKey(Function<? super T, ?> keyExtractor) {
        Map<Object, Boolean> seen = new ConcurrentHashMap<>();
        return t -> seen.putIfAbsent(keyExtractor.apply(t), Boolean.TRUE) == null;
    }

    private record ResolvedRow(
            String externalId,
            Long playerId,
            Long seasonId,
            Integer enhanceLevel,
            Integer ovr,
            Integer salary,
            String preferredPosition,
            Long marketPriceBp,
            String imageUrl,
            Integer pace,
            Integer shooting,
            Integer passing,
            Integer dribbling,
            Integer defending,
            Integer physicality,
            PlayerCardDTO originalDto
    ) {
    }
}