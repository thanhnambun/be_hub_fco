package com.ra.base_spring_boot.dto.resp;

import lombok.Data;

@Data
public class PlayerCardResponse {
    private Long id;
    private String externalId;
    private String playerName;
    private String seasonCode;
    private Integer ovr;
    private Integer salary;
    private String preferredPosition;
    private Long marketPriceBp;
    private String imageUrl;
    private Integer pace;
    private Integer shooting;
    private Integer passing;
    private Integer dribbling;
    private Integer defending;
    private Integer physicality;
}
