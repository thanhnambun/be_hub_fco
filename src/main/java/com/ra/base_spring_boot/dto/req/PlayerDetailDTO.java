package com.ra.base_spring_boot.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class PlayerDetailDTO {

    @NotBlank
    @Size(max = 100)
    @JsonProperty("external_id") 
    private String externalId;

    @Valid
    @JsonProperty("player_bio")
    private PlayerBioDTO playerBio;

    @Valid
    @JsonProperty("card_detail")
    private CardDetailDTO cardDetail;

    @Valid
    private List<TraitDTO> traits;

    @Valid
    private List<PriceDTO> prices;

    @Data
    @Builder 
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PlayerBioDTO {
        private Integer height;
        private Integer weight;

        @Size(max = 20)
        private String birthdate;

        @Size(max = 10)
        @JsonProperty("preferred_foot")
        private String preferredFoot;

        @JsonProperty("weak_foot")
        private Integer weakFoot;

        @Size(max = 100)
        @JsonProperty("nation_name")
        private String nationName;

        @Size(max = 120)
        @JsonProperty("nation_slug")
        private String nationSlug;

        @Valid
        private List<ClubDTO> clubs;

        @Size(max = 150)
        @JsonProperty("league_name")
        private String leagueName;

        @Size(max = 180)
        @JsonProperty("league_slug")
        private String leagueSlug;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class ClubDTO {
        @Size(max = 150)
        @JsonProperty("club_name")
        private String clubName;

        @Size(max = 180)
        @JsonProperty("club_slug")
        private String clubSlug;

        @JsonProperty("club_fifaaddict_id")
        private Integer clubFifaaddictId;

        @Size(max = 500)
        @JsonProperty("crest_url")
        private String crestUrl;
    }

    @Data
    @Builder // Bổ sung Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class CardDetailDTO {
        @Min(0)
        private Integer liveperf;

        @JsonProperty("has_live_perf")
        private Boolean hasLivePerf;

        @Min(1) @Max(5)
        @JsonProperty("skill_level")
        private Integer skillLevel;

        @Size(max = 10)
        @JsonProperty("secondary_position")
        private String secondaryPosition;

        @Size(max = 10)
        @JsonProperty("workerate_att")
        private String workerateAtt;

        @Size(max = 10)
        @JsonProperty("workerate_def")
        private String workerateDef;

        @Size(max = 20)
        private String bodytype;

        @Size(max = 50)
        private String reputation;

        @JsonProperty("price_updated_at")
        private String priceUpdatedAt;

        @JsonProperty("ovr_by_pos_json")
        private String ovrByPosJson;

        @Min(0) @Max(255) private Integer pace;
        @Min(0) @Max(255) private Integer shooting;
        @Min(0) @Max(255) private Integer passing;
        @Min(0) @Max(255) private Integer dribbling;
        @Min(0) @Max(255) private Integer defending;
        @Min(0) @Max(255) private Integer physicality;
    }

    @Data
    @Builder // Bổ sung Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class TraitDTO {
        @NotBlank
        @Size(max = 60)
        @JsonProperty("trait_code")
        private String traitCode;

        @Size(max = 120)
        @JsonProperty("trait_name")
        private String traitName;

        @Size(max = 500)
        private String description;

        @Size(max = 10)
        @JsonProperty("icon_id")
        private String iconId;

        @Size(max = 500)
        @JsonProperty("icon_url")
        private String iconUrl;
    }

    @Data
    @Builder // Bổ sung Builder
    @NoArgsConstructor
    @AllArgsConstructor
    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class PriceDTO {
        @NotNull
        @Min(1) @Max(13)
        private Integer grade;

        @NotNull
        @PositiveOrZero
        @JsonProperty("price_bp")
        private Long priceBp;

        @Size(max = 30)
        @JsonProperty("price_raw")
        private String priceRaw;

        @JsonProperty("price_date")
        private String priceDate;
    }
}
