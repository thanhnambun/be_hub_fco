package com.fco.platform.auth.application;

import com.fco.platform.auth.interfaces.dto.LoginRequest;
import com.fco.platform.auth.interfaces.dto.ForgotPasswordRequest;
import com.fco.platform.auth.interfaces.dto.RegisterRequest;
import com.fco.platform.auth.interfaces.dto.ResetPasswordRequest;
import com.fco.platform.auth.interfaces.dto.AuthIssuanceResult;
import com.fco.platform.auth.interfaces.dto.UserProfileResponse;
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
