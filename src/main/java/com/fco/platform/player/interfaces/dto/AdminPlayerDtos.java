package com.fco.platform.player.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

public final class AdminPlayerDtos {
    private AdminPlayerDtos() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PlayerItem {
        private Long id;
        private String playerName;
        private String externalId;
        private Integer height;
        private Integer weight;
        private String birthdate;
        private String preferredFoot;
        private Integer weakFoot;
        private Long nationId;
        private String nationName;
        private String nationSlug;
        private String flagUrl;
        private Long clubId;
        private String clubName;
        private String clubSlug;
        private String crestUrl;
        private Long leagueId;
        private String leagueName;
        private String leagueSlug;
        private String logoUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private Long id;
        private String label;
        private String slug;
        private String imageUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionsResponse {
        private List<OptionItem> nations;
        private List<OptionItem> clubs;
        private List<OptionItem> leagues;
        private List<OptionItem> seasons;
    }
}
