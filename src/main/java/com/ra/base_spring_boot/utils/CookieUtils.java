package com.ra.base_spring_boot.utils;

import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ResponseCookie;

public final class CookieUtils {

    private static final String ACCESS_TOKEN_COOKIE  = "access_token";
    private static final String REFRESH_TOKEN_COOKIE = "refresh_token";

    // access_token: 15 phút (khớp jwt.expired.access = 900000ms)
    private static final int ACCESS_TOKEN_MAX_AGE_SECONDS  = 15 * 60;
    // refresh_token: 7 ngày
    private static final int REFRESH_TOKEN_MAX_AGE_SECONDS = 7 * 24 * 60 * 60;

    private CookieUtils() {}

    /** Set cả 2 httpOnly cookie sau khi login / register thành công */
    public static void setTokenCookies(HttpServletResponse response,
                                       String accessToken,
                                       String refreshToken) {
        response.addHeader("Set-Cookie", buildAccessTokenCookie(accessToken, ACCESS_TOKEN_MAX_AGE_SECONDS));
        response.addHeader("Set-Cookie", buildRefreshTokenCookie(refreshToken, REFRESH_TOKEN_MAX_AGE_SECONDS));
    }

    /** Chỉ set lại access_token (dùng sau silent-refresh) */
    public static void setAccessTokenCookie(HttpServletResponse response, String accessToken) {
        response.addHeader("Set-Cookie", buildAccessTokenCookie(accessToken, ACCESS_TOKEN_MAX_AGE_SECONDS));
    }

    /** Set lại cả 2 cookie khi rotate refresh token */
    public static void rotateTokenCookies(HttpServletResponse response,
                                          String newAccessToken,
                                          String newRefreshToken) {
        response.addHeader("Set-Cookie", buildAccessTokenCookie(newAccessToken, ACCESS_TOKEN_MAX_AGE_SECONDS));
        response.addHeader("Set-Cookie", buildRefreshTokenCookie(newRefreshToken, REFRESH_TOKEN_MAX_AGE_SECONDS));
    }

    /** Xóa cả 2 cookie khi logout */
    public static void clearTokenCookies(HttpServletResponse response) {
        response.addHeader("Set-Cookie", buildAccessTokenCookie("", 0));
        response.addHeader("Set-Cookie", buildRefreshTokenCookie("", 0));
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private static String buildAccessTokenCookie(String value, int maxAge) {
        return ResponseCookie.from(ACCESS_TOKEN_COOKIE, value)
                .httpOnly(true)
                .sameSite("Lax")
                .path("/")
                .maxAge(maxAge)
                .build()
                .toString();
    }

    private static String buildRefreshTokenCookie(String value, int maxAge) {
        return ResponseCookie.from(REFRESH_TOKEN_COOKIE, value)
                .httpOnly(true)
                .sameSite("Lax")
                // Giới hạn scope cookie chỉ gửi đến refresh endpoint — giảm attack surface
                .path("/api/v1/auth/refresh")
                .maxAge(maxAge)
                .build()
                .toString();
    }
}
