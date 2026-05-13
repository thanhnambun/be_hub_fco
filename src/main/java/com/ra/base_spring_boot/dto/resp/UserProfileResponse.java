package com.ra.base_spring_boot.dto.resp;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.util.Set;

@Data
@Builder
public class UserProfileResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private BigDecimal balance;
    private Boolean isActive;
    private Set<String> roles;
}
