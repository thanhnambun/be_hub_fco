package com.ra.base_spring_boot.dto.resp;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PlayerDetailResponse {
    // --- Core Card Info ---
    private Long id;
    private String externalId;
    private String playerName;
    private String seasonCode;
    private Integer enhanceLevel;
    private Integer ovr;
    private Integer salary;
    private String preferredPosition;
    private Long marketPriceBp;
    private String imageUrl;

    // --- Core Stats ---
    private Integer pace;
    private Integer shooting;
    private Integer passing;
    private Integer dribbling;
    private Integer defending;
    private Integer physicality;

    // --- Detailed Card Info ---
    private Integer liveperf;
    private Boolean hasLivePerf;
    private Integer skillLevel;
    private String secondaryPosition;
    private String workerateAtt;
    private String workerateDef;
    private String bodytype;
    private String reputation;
    private LocalDate priceUpdatedAt;
    private String ovrByPosJson;

    // --- Bio Info (from FcoPlayer) ---
    private Integer height;
    private Integer weight;
    private String birthdate;
    private String preferredFoot;
    private Integer weakFoot;
    private String nationName;
    private String nationSlug;
    private String leagueName;
    private String leagueSlug;

    // --- Nested Collections ---
    private List<ClubResponse> clubs;
    private List<TraitResponse> traits;
    private List<PriceResponse> prices;

    // Nested Classes
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ClubResponse {
        private String clubName;
        private String clubSlug;
        private Integer clubFifaaddictId;
        private String crestUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TraitResponse {
        private String traitCode;
        private String traitName;
        private String description;
        private String iconId;
        private String iconUrl;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class PriceResponse {
        private Integer grade;
        private Long priceBp;
        private String priceRaw;
        private LocalDate priceDate;
    }
}
