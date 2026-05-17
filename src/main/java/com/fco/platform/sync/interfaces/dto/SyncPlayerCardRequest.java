package com.fco.platform.sync.interfaces.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class SyncPlayerCardRequest {

    @JsonProperty("external_id")
    @NotBlank
    @Size(max = 128)
    private String externalId;

    @Size(max = 255)
    private String name;

    @JsonProperty("season_code")
    @NotBlank
    @Size(max = 32)
    private String seasonCode;

    @JsonProperty("enhance_level")
    @Min(1)
    private Integer enhanceLevel;

    @JsonProperty("preferredPosition")
    @JsonAlias({"position", "preferred_position"})
    @Size(max = 10)
    private String preferredPosition;

    @NotNull
    @Min(0)
    @Max(255)
    private Integer ovr;

    @JsonProperty("market_price_bp")
    @PositiveOrZero
    private Long marketPriceBp;

    @JsonProperty("salary")
    @PositiveOrZero
    private Integer salary;

    @JsonProperty("market_price_text")
    @Size(max = 64)
    private String marketPriceText;

    @JsonProperty("source_url")
    @Size(max = 2048)
    private String sourceUrl;

    @JsonProperty("image_url")
    @Size(max = 500)
    private String imageUrl;

    @Min(0)
    @Max(255)
    private Integer pace;

    @Min(0)
    @Max(255)
    private Integer shooting;

    @Min(0)
    @Max(255)
    private Integer passing;

    @Min(0)
    @Max(255)
    private Integer dribbling;

    @Min(0)
    @Max(255)
    private Integer defending;

    @Min(0)
    @Max(255)
    private Integer physicality;

    @JsonProperty("crawl_at")
    private OffsetDateTime crawlAt;
}
