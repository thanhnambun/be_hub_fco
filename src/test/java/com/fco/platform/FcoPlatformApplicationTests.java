package com.fco.platform;

import com.fco.platform.auth.application.IAuthService;
import com.fco.platform.auth.domain.User;
import com.fco.platform.auth.infrastructure.persistence.IUserRepository;
import com.fco.platform.auth.interfaces.dto.RegisterRequest;
import com.fco.platform.auth.interfaces.dto.ResetPasswordRequest;
import com.fco.platform.common.application.RedisService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
class FcoPlatformApplicationTests {

    static {
        try {
            io.github.cdimascio.dotenv.Dotenv dotenv = io.github.cdimascio.dotenv.Dotenv.configure()
                    .directory(".")
                    .ignoreIfMissing()
                    .load();
            dotenv.entries().forEach(entry -> System.setProperty(entry.getKey(), entry.getValue()));
        } catch (Exception e) {
            System.err.println("Failed to load .env: " + e.getMessage());
        }
    }

    @Autowired
    private IAuthService authService;

    @Autowired
    private IUserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private RedisService redisService;

    @Test
    void testResetPasswordFlow() {
        String username = "testuser" + UUID.randomUUID().toString().substring(0, 8);
        String email = username + "@example.com";
        String password = "Password123";

        // 1. Register
        RegisterRequest regReq = new RegisterRequest();
        regReq.setFullName("Test User");
        regReq.setUsername(username);
        regReq.setEmail(email);
        regReq.setPhone("0987654321");
        regReq.setPassword(password);
        authService.register(regReq, new MockHttpServletRequest());

        // Verify registration
        User user = userRepository.findByUsername(username).orElseThrow();
        assertTrue(passwordEncoder.matches(password, user.getPassword()), "Original password should match");

        // 2. Set reset password token in Redis manually to mock the flow
        String token = UUID.randomUUID().toString();
        redisService.set("RESET_PW:" + token, email, java.time.Duration.ofMinutes(15));

        // 3. Reset password
        String newPassword = "NewPassword123";
        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setToken(token);
        resetReq.setNewPassword(newPassword);
        resetReq.setConfirmPassword(newPassword);
        authService.resetPassword(resetReq);

        // 4. Verify password in DB
        User updatedUser = userRepository.findByUsername(username).orElseThrow();
        boolean matches = passwordEncoder.matches(newPassword, updatedUser.getPassword());
        System.out.println("Matches: " + matches);
        assertTrue(matches, "New password should match");
        System.out.println("Test PASSED: reset password successfully verified in DB!");
    }

}
