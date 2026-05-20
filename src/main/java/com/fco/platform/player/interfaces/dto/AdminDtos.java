package com.fco.platform.player.interfaces.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public final class AdminDtos {
    private AdminDtos() {}

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class OptionItem {
        private Long id;
        private String label;
        private String slug;
        private String imageUrl;
        private Boolean active;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class NationItem {
        private Long id;
        private String nationName;
        private String nationSlug;
        private String flagUrl;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class LeagueItem {
        private Long id;
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
    public static class SeasonItem {
        private Long id;
        private String seasonCode;
        private String seasonName;
        private Boolean isActive;
        private Boolean isCore;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardItem {
        private Long id;
        private Long playerId;
        private String playerName;
        private String seasonCode;
        private String seasonName;
        private Integer enhanceLevel;
        private Integer ovr;
        private Integer salary;
        private String preferredPosition;
        private String secondaryPosition;
        private Long marketPriceBp;
        private String imageUrl;
        private LocalDate priceUpdatedAt;
        private Boolean isActive;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
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
