package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ChangePasswordRequest;
import com.ra.base_spring_boot.dto.req.UpdateProfileRequest;
import com.ra.base_spring_boot.dto.resp.AuthResponse;

public interface IUserService {
    AuthResponse updateProfile(Long userId, UpdateProfileRequest request);
    
    void changePassword(Long userId, ChangePasswordRequest request);

    org.springframework.data.domain.Page<com.ra.base_spring_boot.dto.resp.UserResponse> findAllUsers(org.springframework.data.domain.Pageable pageable, String search);

    com.ra.base_spring_boot.dto.resp.UserResponse getUserById(Long id);

    com.ra.base_spring_boot.dto.resp.UserResponse updateUserByAdmin(Long id, com.ra.base_spring_boot.dto.req.AdminUserUpdateRequest request);

    void toggleUserStatus(Long id);
}
