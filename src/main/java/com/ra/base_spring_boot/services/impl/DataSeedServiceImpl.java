package com.ra.base_spring_boot.services.impl;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ra.base_spring_boot.dto.resp.ImportPlayersResult;
import com.ra.base_spring_boot.model.fco.FcoPlayer;
import com.ra.base_spring_boot.model.fco.FcoSeason;
import com.ra.base_spring_boot.model.fco.PlayerCard;
import com.ra.base_spring_boot.repository.IFcoPlayerRepository;
import com.ra.base_spring_boot.repository.IFcoSeasonRepository;
import com.ra.base_spring_boot.repository.IPlayerCardRepository;
import com.ra.base_spring_boot.services.IDataSeedService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
public class DataSeedServiceImpl implements IDataSeedService {
    private static final Logger log = LoggerFactory.getLogger(DataSeedServiceImpl.class);

    private final IFcoPlayerRepository playerRepository;
    private final IFcoSeasonRepository seasonRepository;
    private final IPlayerCardRepository playerCardRepository;
    private final ObjectMapper objectMapper;
    @Value("${seed.players-file-path:}")
    private String externalPlayersFilePath;

    @Override
    @Transactional
    public ImportPlayersResult seedPlayers() {
        log.info("Bat dau seed du lieu player tu file resources/data/players.json");

        int inserted = 0;
        int updated = 0;
        int skipped = 0;

        List<ScrapedPlayerPayload> rows = readPayloadFromResources();
        for (ScrapedPlayerPayload row : rows) {
            String externalId = resolveExternalId(row);
            try {
                if (externalId == null || externalId.isBlank() || row.name == null || row.name.isBlank()
                        || row.seasonCode == null || row.seasonCode.isBlank() || row.ovr == null || row.salary == null) {
                    skipped++;
                    log.warn("Bo qua ban ghi do thieu du lieu. ID: {}", externalId);
                    continue;
                }

                String normalizedExternalId = externalId.trim();
                String normalizedSeasonCode = row.seasonCode.trim().toUpperCase(Locale.ROOT);
                LocalDateTime now = LocalDateTime.now();

                FcoPlayer player = playerRepository.findByExternalId(normalizedExternalId)
                        .map(existing -> {
                            existing.setPlayerName(row.name.trim());
                            existing.setUpdatedAt(now);
                            return existing;
                        })
                        .orElseGet(() -> FcoPlayer.builder()
                                .playerName(row.name.trim())
                                .externalId(normalizedExternalId)
                                .build());

                player.setExternalId(normalizedExternalId);
                player = playerRepository.save(player);

                FcoSeason season = seasonRepository.findBySeasonCode(normalizedSeasonCode)
                        .orElseGet(() -> seasonRepository.save(FcoSeason.builder()
                                .seasonCode(normalizedSeasonCode)
                                .seasonName(normalizedSeasonCode)
                                .build()));

                PlayerCard card = playerCardRepository
                        .findFirstByPlayerExternalIdAndSeasonSeasonCodeAndOvrOrderByUpdatedAtDesc(
                                normalizedExternalId,
                                normalizedSeasonCode,
                                row.ovr
                        )
                        .orElse(null);

                if (card != null) {
                    card.setMarketPriceBp(parseMarketPrice(row.marketPriceText));
                    card.setUpdatedAt(now);
                    playerCardRepository.save(card);
                    updated++;
                } else {
                    card = new PlayerCard();
                    card.setPlayer(player);
                    card.setSeason(season);
                    card.setOvr(row.ovr);
                    card.setSalary(row.salary);
                    card.setImageUrl(row.imageUrl);
                    card.setMarketPriceBp(parseMarketPrice(row.marketPriceText));
                    card.setUpdatedAt(now);
                    playerCardRepository.save(card);
                    inserted++;
                }
            } catch (Exception ex) {
                log.error("Loi o ban ghi ID: {}", externalId, ex);
                throw ex;
            }
        }

        log.info("Seed thanh cong {} ban ghi (inserted={}, updated={}, skipped={})", rows.size(), inserted, updated, skipped);
        return ImportPlayersResult.builder()
                .totalRows(rows.size())
                .inserted(inserted)
                .updated(updated)
                .skipped(skipped)
                .build();
    }

    private List<ScrapedPlayerPayload> readPayloadFromResources() {
        String configuredPath = externalPlayersFilePath == null ? "" : externalPlayersFilePath.trim();
        if (!configuredPath.isBlank()) {
            Path path = Path.of(configuredPath).toAbsolutePath().normalize();
            if (Files.exists(path)) {
                try {
                    log.info("Doc du lieu seed tu external file: {}", path);
                    return objectMapper.readValue(path.toFile(), new TypeReference<List<ScrapedPlayerPayload>>() {
                    });
                } catch (IOException e) {
                    throw new IllegalArgumentException("Khong the doc/parse file external: " + path, e);
                }
            }
            log.warn("Khong tim thay external file {}, se fallback classpath:data/players.json", path);
        }

        try {
            ClassPathResource resource = new ClassPathResource("data/players.json");
            log.info("Doc du lieu seed tu classpath:data/players.json");
            return objectMapper.readValue(resource.getInputStream(), new TypeReference<List<ScrapedPlayerPayload>>() {
            });
        } catch (IOException e) {
            throw new IllegalArgumentException("Khong the doc/parse resources/data/players.json", e);
        }
    }

    private String resolveExternalId(ScrapedPlayerPayload row) {
        if (row.externalId != null && !row.externalId.isBlank()) {
            return row.externalId.trim();
        }
        if (row.sourceUrl == null || row.sourceUrl.isBlank()) {
            return null;
        }
        int idx = row.sourceUrl.lastIndexOf('/');
        return idx >= 0 ? row.sourceUrl.substring(idx + 1).trim() : row.sourceUrl.trim();
    }

    private Long parseMarketPrice(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String digits = value.replaceAll("[^0-9]", "");
        if (digits.isBlank()) {
            return null;
        }
        try {
            return Long.parseLong(digits);
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private static class ScrapedPlayerPayload {
        public String name;
        @JsonProperty("external_id")
        public String externalId;
        @JsonProperty("season_code")
        public String seasonCode;
        public Integer ovr;
        public Integer salary;
        @JsonProperty("market_price_text")
        public String marketPriceText;
        @JsonProperty("image_url")
        public String imageUrl;
        @JsonProperty("source_url")
        public String sourceUrl;
    }
}
