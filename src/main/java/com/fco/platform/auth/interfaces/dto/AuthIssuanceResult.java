package com.fco.platform.auth.interfaces.dto;

import lombok.Builder;
import lombok.Getter;

/**
 * Internal-use only — KHÔNG serialize ra JSON.
 * Service trả về object này; Controller dùng để:
 *   1. Set accessToken + refreshToken vào httpOnly Cookie.
 *   2. Trả AuthResponse (profile data) trong JSON body.
 */
@Getter
@Builder
public class AuthIssuanceResult {
    private final String accessToken;
    private final String refreshToken;
    private final AuthResponse profile;
}
