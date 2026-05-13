package com.ra.base_spring_boot.controller;

import com.ra.base_spring_boot.dto.req.UpdateProfileRequest;
import com.ra.base_spring_boot.dto.resp.AuthResponse;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.services.IUserService;
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
            @Valid @RequestBody com.ra.base_spring_boot.dto.req.ChangePasswordRequest request) {
        
        userService.changePassword(userDetails.getId(), request);
        return ResponseEntity.ok(java.util.Map.of("message", "Äá»•i máº­t kháº©u thÃ nh cÃ´ng"));
    }
}
