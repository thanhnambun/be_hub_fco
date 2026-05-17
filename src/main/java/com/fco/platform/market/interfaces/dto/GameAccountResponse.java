package com.fco.platform.market.interfaces.dto;

import lombok.Data;
import java.math.BigDecimal;

@Data
public class GameAccountResponse {
    private Long id;
    private String accountCode;
    private String title;
    private String rankName;
    private BigDecimal price;
    private String accountStatus;
    private Boolean isFeatured;
}
