package com.fco.platform.auth.interfaces.web;

import com.fco.platform.auth.application.IAuthService;
import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.auth.interfaces.dto.LoginRequest;
import com.fco.platform.auth.interfaces.dto.ForgotPasswordRequest;
import com.fco.platform.auth.interfaces.dto.RegisterRequest;
import com.fco.platform.auth.interfaces.dto.ResetPasswordRequest;
import com.fco.platform.auth.interfaces.dto.AuthIssuanceResult;
import com.fco.platform.auth.interfaces.dto.AuthResponse;
import com.fco.platform.auth.interfaces.dto.UserProfileResponse;
import com.fco.platform.common.util.CookieUtils;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Arrays;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final IAuthService authService;
    private final CookieUtils cookieUtils;

    // ── Register ──────────────────────────────────────────────────────────────

    @PostMapping("/register")
    public ResponseEntity<ResponseWrapper<AuthResponse>> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest  httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthIssuanceResult result = authService.register(request, httpRequest);
        cookieUtils.setTokenCookies(httpResponse, result.getAccessToken(), result.getRefreshToken());

        return ResponseEntity.status(HttpStatus.CREATED).body(
                ResponseWrapper.<AuthResponse>builder()
                        .status(HttpStatus.CREATED)
                        .code(HttpStatus.CREATED.value())
                        .message("Đăng ký tài khoản thành công")
                        .data(result.getProfile())
                        .build()
        );
    }

    // ── Login ─────────────────────────────────────────────────────────────────

    @PostMapping("/login")
    public ResponseEntity<ResponseWrapper<AuthResponse>> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest  httpRequest,
            HttpServletResponse httpResponse
    ) {
        AuthIssuanceResult result = authService.login(request, httpRequest);
        cookieUtils.setTokenCookies(httpResponse, result.getAccessToken(), result.getRefreshToken());

        return ResponseEntity.ok(
                ResponseWrapper.<AuthResponse>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .message("Đăng nhập thành công")
                        .data(result.getProfile())
                        .build()
        );
    }

    // ── Silent Refresh ────────────────────────────────────────────────────────

    /**
     * Frontend tự động gọi endpoint này (no body) khi nhận 401.
     * Browser gửi cookie refresh_token nhờ withCredentials=true.
     */
    @PostMapping("/refresh")
    public ResponseEntity<ResponseWrapper<AuthResponse>> refresh(
            HttpServletRequest  httpRequest,
            HttpServletResponse httpResponse
    ) {
        String refreshToken = extractCookieValue(httpRequest, "refresh_token");
        if (refreshToken == null || refreshToken.isBlank()) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(
                    ResponseWrapper.<AuthResponse>builder()
                            .status(HttpStatus.UNAUTHORIZED)
                            .code(HttpStatus.UNAUTHORIZED.value())
                            .message("Refresh token cookie is missing")
                            .build()
            );
        }

        AuthIssuanceResult result = authService.refreshToken(refreshToken);
        cookieUtils.rotateTokenCookies(httpResponse, result.getAccessToken(), result.getRefreshToken());

        return ResponseEntity.ok(
                ResponseWrapper.<AuthResponse>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .message("Làm mới token thành công")
                        .data(result.getProfile())
                        .build()
        );
    }

    // ── Logout ────────────────────────────────────────────────────────────────

    @PostMapping("/logout")
    public ResponseEntity<ResponseWrapper<String>> logout(
            HttpServletRequest  httpRequest,
            HttpServletResponse httpResponse
    ) {
        String accessToken  = extractCookieValue(httpRequest, "access_token");
        String refreshToken = extractCookieValue(httpRequest, "refresh_token");

        authService.logout(accessToken, refreshToken);
        cookieUtils.clearTokenCookies(httpResponse);

        return ResponseEntity.ok(
                ResponseWrapper.<String>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .message("Đăng xuất thành công")
                        .data("Logged out")
                        .build()
        );
    }

    // ── Me ────────────────────────────────────────────────────────────────────

    @GetMapping("/me")
    public ResponseEntity<ResponseWrapper<UserProfileResponse>> me() {
        UserProfileResponse payload = authService.getMyProfile();
        return ResponseEntity.ok(
                ResponseWrapper.<UserProfileResponse>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .message("Lấy thông tin người dùng thành công")
                        .data(payload)
                        .build()
        );
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<ResponseWrapper<String>> forgotPassword(@Valid @RequestBody ForgotPasswordRequest request) {
        authService.forgotPassword(request);
        return ResponseEntity.ok(
                ResponseWrapper.<String>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .message("Nếu email tồn tại, thư hướng dẫn đã được gửi đi.")
                        .data("Accepted")
                        .build()
        );
    }

    @PostMapping("/reset-password")
    public ResponseEntity<ResponseWrapper<String>> resetPassword(@Valid @RequestBody ResetPasswordRequest request) {
        authService.resetPassword(request);
        return ResponseEntity.ok(
                ResponseWrapper.<String>builder()
                        .status(HttpStatus.OK)
                        .code(HttpStatus.OK.value())
                        .message("Đặt lại mật khẩu thành công")
                        .data("Password reset successfully")
                        .build()
        );
    }

    // ── Private helpers ───────────────────────────────────────────────────────

    private String extractCookieValue(HttpServletRequest request, String name) {
        if (request.getCookies() == null) return null;
        return Arrays.stream(request.getCookies())
                .filter(c -> name.equals(c.getName()))
                .map(Cookie::getValue)
                .findFirst()
                .orElse(null);
    }
}
