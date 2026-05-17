package com.fco.platform.auth.interfaces.web;

import com.fco.platform.auth.interfaces.dto.UpdateProfileRequest;
import com.fco.platform.auth.interfaces.dto.AuthResponse;
import com.fco.platform.auth.interfaces.dto.ChangePasswordRequest;
import com.fco.platform.auth.domain.User;
import com.fco.platform.auth.application.IUserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final IUserService userService;

    @PutMapping("/profile")
    public ResponseEntity<AuthResponse> updateProfile(
            @AuthenticationPrincipal User userDetails,
            @Valid @RequestBody UpdateProfileRequest request) {
        
        Long userId = userDetails.getId();
        AuthResponse updatedProfile = userService.updateProfile(userId, request);
        
        return ResponseEntity.ok(updatedProfile);
    }

    @PutMapping("/password")
    public ResponseEntity<java.util.Map<String, String>> changePassword(
            @AuthenticationPrincipal User userDetails,
            @Valid @RequestBody ChangePasswordRequest request) {
        
        userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Đổi mật khẩu thành công"));
    }
}
