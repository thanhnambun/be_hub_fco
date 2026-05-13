package com.ra.base_spring_boot.dto.resp;

import lombok.Data;

@Data
public class UserResponse {
    private Long id;
    private String fullName;
    private String username;
    private String email;
    private String phone;
    private Boolean status;
    private java.util.Set<String> roles;
    private java.time.LocalDateTime createdAt;
}
