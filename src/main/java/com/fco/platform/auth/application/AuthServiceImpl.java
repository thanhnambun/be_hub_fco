package com.fco.platform.auth.application;

import com.fco.platform.auth.interfaces.dto.LoginRequest;
import com.fco.platform.auth.interfaces.dto.ForgotPasswordRequest;
import com.fco.platform.auth.interfaces.dto.RegisterRequest;
import com.fco.platform.auth.interfaces.dto.ResetPasswordRequest;
import com.fco.platform.auth.interfaces.dto.AuthIssuanceResult;
import com.fco.platform.auth.interfaces.dto.AuthResponse;
import com.fco.platform.auth.interfaces.dto.UserProfileResponse;
import com.fco.platform.common.exception.AccountLockedException;
import com.fco.platform.common.exception.HttpBadRequest;
import com.fco.platform.common.exception.TooManyRequestsException;
import com.fco.platform.common.exception.UserNotFoundException;
import com.fco.platform.auth.domain.Role;
import com.fco.platform.auth.domain.RoleName;
import com.fco.platform.auth.domain.User;
import com.fco.platform.auth.infrastructure.persistence.IRoleRepository;
import com.fco.platform.auth.infrastructure.persistence.IUserRepository;
import com.fco.platform.auth.infrastructure.jwt.JwtService;
import com.fco.platform.common.application.MailService;
import com.fco.platform.common.application.RedisService;
import com.fco.platform.common.util.ClientIpUtils;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements IAuthService {

    private static final Duration REFRESH_TOKEN_TTL     = Duration.ofDays(7);
    private static final Duration LOCK_TTL              = Duration.ofMinutes(15);
    private static final Duration RESET_PASSWORD_TTL    = Duration.ofMinutes(15);
    private static final int      MAX_REGISTER_PER_DAY  = 3;
    private static final int      MAX_LOGIN_FAIL        = 5;

    private final IUserRepository       userRepository;
    private final IRoleRepository       roleRepository;
    private final PasswordEncoder       passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService            jwtService;
    private final RedisService          redisService;
    private final MailService           mailService;

    // ── Register ───────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public AuthIssuanceResult register(RegisterRequest request, HttpServletRequest httpRequest) {
        String ip = ClientIpUtils.extractClientIp(httpRequest);
        enforceRegisterRateLimit(ip);

        String username = request.getUsername().trim();
        String email    = request.getEmail().trim().toLowerCase();

        if (userRepository.existsByUsername(username)) {
            throw new HttpBadRequest("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new HttpBadRequest("Email already exists");
        }

        Role defaultRole = roleRepository.findByRoleName(RoleName.ROLE_CUSTOMER)
                .orElseThrow(() -> new HttpBadRequest("ROLE_CUSTOMER does not exist"));

        User savedUser = userRepository.save(User.builder()
                .fullName(request.getFullName().trim())
                .username(username)
                .email(email)
                .phone(request.getPhone())
                .password(passwordEncoder.encode(request.getPassword()))
                .balance(BigDecimal.ZERO)
                .status(true)
                .roles(Set.of(defaultRole))
                .build());

        return issueTokens(savedUser);
    }

    // ── Login ───────────────────────────────────────────────────────────────────

    @Override
    public AuthIssuanceResult login(LoginRequest request, HttpServletRequest httpRequest) {
        String ip        = ClientIpUtils.extractClientIp(httpRequest);
        String principal = request.getIdentifier().trim();
        ensureNotLoginLocked(ip, principal);

        try {
            Authentication authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(principal, request.getPassword())
            );
            User user = (User) authentication.getPrincipal();
            if (!Boolean.TRUE.equals(user.getStatus())) {
                log.warn("{\"event\":\"AUTH_ACCOUNT_LOCKED\", \"principal\":\"{}\", \"ip\":\"{}\", \"userId\":{}}", user.getUsername(), ip, user.getId());
                throw new AccountLockedException("Tài khoản của bạn đã bị khóa.");
            }
            clearLoginFailCounter(ip, principal);
            log.info("{\"event\":\"AUTH_LOGIN_SUCCESS\", \"principal\":\"{}\", \"ip\":\"{}\", \"userId\":{}}", user.getUsername(), ip, user.getId());
            return issueTokens(user);
        } catch (BadCredentialsException ex) {
            log.warn("{\"event\":\"AUTH_LOGIN_FAILURE\", \"principal\":\"{}\", \"ip\":\"{}\", \"reason\":\"Bad credentials\"}", principal, ip);
            increaseLoginFailCounter(ip, principal);
            throw ex;
        }
    }

    // ── Refresh ─────────────────────────────────────────────────────────────────

    @Override
    public AuthIssuanceResult refreshToken(String oldRefreshToken) {
        String username = redisService.get(refreshKey(oldRefreshToken));
        if (username == null || username.isBlank()) {
            log.warn("{\"event\":\"AUTH_REFRESH_ABUSE\", \"reason\":\"Invalid or replayed refresh token\", \"token\":\"{}\"}", oldRefreshToken);
            throw new HttpBadRequest("Refresh token is invalid or expired");
        }

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new HttpBadRequest("User not found"));

        if (!Boolean.TRUE.equals(user.getStatus())) {
            log.warn("{\"event\":\"AUTH_REFRESH_LOCKED\", \"userId\":{}, \"username\":\"{}\"}", user.getId(), username);
            throw new AccountLockedException("Tài khoản đã bị khóa");
        }

        // Rotate: xóa token cũ, cấp token mới
        redisService.delete(refreshKey(oldRefreshToken));

        log.info("{\"event\":\"AUTH_TOKEN_REFRESH\", \"userId\":{}, \"username\":\"{}\"}", user.getId(), username);
        return issueTokens(user);
    }

    // ── Logout ──────────────────────────────────────────────────────────────────

    @Override
    public void logout(String accessToken, String refreshToken) {
        // Blacklist access token cho đến khi hết hạn
        if (accessToken != null && !accessToken.isBlank()) {
            long remainingMillis = jwtService.getRemainingTtlMillis(accessToken);
            if (remainingMillis > 0) {
                redisService.set(blacklistKey(accessToken), "1", Duration.ofMillis(remainingMillis));
            }
        }
        // Xóa refresh token khỏi Redis
        if (refreshToken != null && !refreshToken.isBlank()) {
            redisService.delete(refreshKey(refreshToken));
        }
    }

    // ── Profile ─────────────────────────────────────────────────────────────────

    @Override
    public UserProfileResponse getMyProfile() {
        String username = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng đăng nhập"));
        return UserProfileResponse.builder()
                .id(user.getId())
                .fullName(user.getFullName())
                .username(user.getUsername())
                .email(user.getEmail())
                .phone(user.getPhone())
                .balance(user.getBalance())
                .isActive(user.getIsActive())
                .roles(roleNames(user))
                .build();
    }

    @Override
    public void forgotPassword(ForgotPasswordRequest request) {
        String email = request.getEmail().trim().toLowerCase();
        userRepository.findByEmail(email).ifPresent(user -> {
            String token = UUID.randomUUID().toString();
            redisService.set(resetPasswordKey(token), email, RESET_PASSWORD_TTL);
            String resetLink = "http://localhost:3000/auth/reset-password?token=" + token;
            mailService.sendResetPasswordEmail(email, resetLink);
        });
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new HttpBadRequest("Mật khẩu xác nhận không khớp");
        }

        String token = request.getToken().trim();
        String email = redisService.get(resetPasswordKey(token));

        if (email == null || email.isBlank()) {
            throw new HttpBadRequest("Token không hợp lệ hoặc hết hạn");
        }

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new HttpBadRequest("Token không hợp lệ hoặc hết hạn"));

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        redisService.delete(resetPasswordKey(token));
    }

    // ── Private helpers ─────────────────────────────────────────────────────────

    /**
     * Sinh cặp token mới, lưu refresh token vào Redis, trả về AuthIssuanceResult.
     * AuthResponse trong result chỉ chứa profile data — KHÔNG chứa token.
     * Token được controller lấy từ result để set vào httpOnly Cookie.
     */
    private AuthIssuanceResult issueTokens(User user) {
        String accessToken  = jwtService.generateAccessToken(user);
        String refreshToken = UUID.randomUUID().toString();
        redisService.set(refreshKey(refreshToken), user.getUsername(), REFRESH_TOKEN_TTL);

        AuthResponse profile = AuthResponse.builder()
                .id(user.getId())
                .username(user.getUsername())
                .email(user.getEmail())
                .fullName(user.getFullName())
                .phone(user.getPhone())
                .roles(roleNames(user))
                .build();

        return AuthIssuanceResult.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .profile(profile)
                .build();
    }

    private Set<String> roleNames(User user) {
        return user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toSet());
    }

    private void enforceRegisterRateLimit(String ip) {
        String key   = "REGISTER_IP:" + ip + ":" + LocalDate.now(ZoneOffset.UTC);
        Long   count = redisService.incrementCounter(key);
        if (count != null && count == 1L) {
            redisService.expireCounter(key, Duration.ofDays(1));
        }
        if (count != null && count > MAX_REGISTER_PER_DAY) {
            throw new TooManyRequestsException("Too many registrations from this IP today");
        }
    }

    private void ensureNotLoginLocked(String ip, String principal) {
        if (getFailCount(loginFailIpKey(ip)) >= MAX_LOGIN_FAIL) {
            throw new TooManyRequestsException("Too many failed logins by IP. Retry after 15 minutes");
        }
        if (getFailCount(loginFailUserKey(principal)) >= MAX_LOGIN_FAIL) {
            throw new TooManyRequestsException("Too many failed logins for this username. Retry after 15 minutes");
        }
    }

    private void increaseLoginFailCounter(String ip, String principal) {
        increaseWithTtl(loginFailIpKey(ip));
        increaseWithTtl(loginFailUserKey(principal));
    }

    private void increaseWithTtl(String key) {
        Long count = redisService.incrementCounter(key);
        if (count != null && count == 1L) {
            redisService.expireCounter(key, LOCK_TTL);
        }
    }

    private long getFailCount(String key) {
        String value = redisService.getCounter(key);
        if (value == null || value.isBlank()) return 0;
        return Long.parseLong(value);
    }

    private void clearLoginFailCounter(String ip, String principal) {
        redisService.deleteCounter(loginFailIpKey(ip));
        redisService.deleteCounter(loginFailUserKey(principal));
    }

    private String refreshKey(String token)     { return "RT:"                + token; }
    private String blacklistKey(String token)   { return "BL:"                + token; }
    private String resetPasswordKey(String token) { return "RESET_PW:"        + token; }
    private String loginFailIpKey(String ip)    { return "LOGIN_FAIL_IP:"     + ip; }
    private String loginFailUserKey(String u)   { return "LOGIN_FAIL_USER:"   + u.toLowerCase(); }
}
