package com.fco.platform.auth.interfaces.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

/**
 * Body trả về sau khi login / register / refresh.
 * access_token và refresh_token KHÔNG nằm ở đây nữa —
 * chúng được set qua httpOnly Cookie bởi AuthController.
 */
@Data
@Builder
public class AuthResponse {
    private Long id;
    private String username;
    private String email;
    private String fullName;
    private String phone;
    private Set<String> roles;
}
