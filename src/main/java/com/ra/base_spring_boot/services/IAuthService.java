package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.LoginRequest;
import com.ra.base_spring_boot.dto.req.ForgotPasswordRequest;
import com.ra.base_spring_boot.dto.req.RegisterRequest;
import com.ra.base_spring_boot.dto.req.ResetPasswordRequest;
import com.ra.base_spring_boot.dto.resp.AuthIssuanceResult;
import com.ra.base_spring_boot.dto.resp.UserProfileResponse;
import jakarta.servlet.http.HttpServletRequest;

public interface IAuthService {

    AuthIssuanceResult register(RegisterRequest request, HttpServletRequest httpRequest);

    AuthIssuanceResult login(LoginRequest request, HttpServletRequest httpRequest);

    /**
     * Rotate refresh token. Nhận oldRefreshToken từ cookie, trả về pair mới.
     */
    AuthIssuanceResult refreshToken(String oldRefreshToken);

    /**
     * Blacklist access token và xóa refresh token khỏi Redis.
     */
    void logout(String accessToken, String refreshToken);

    UserProfileResponse getMyProfile();

    void forgotPassword(ForgotPasswordRequest request);

    void resetPassword(ResetPasswordRequest request);
}
