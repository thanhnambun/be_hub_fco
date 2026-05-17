package com.fco.platform.auth.application;

import com.fco.platform.auth.interfaces.dto.ChangePasswordRequest;
import com.fco.platform.auth.interfaces.dto.UpdateProfileRequest;
import com.fco.platform.auth.interfaces.dto.AuthResponse;
import com.fco.platform.auth.interfaces.dto.UserResponse;
import com.fco.platform.auth.interfaces.dto.AdminUserUpdateRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

public interface IUserService {
    AuthResponse updateProfile(Long userId, UpdateProfileRequest request);
    
    void changePassword(Long userId, ChangePasswordRequest request);

    Page<UserResponse> findAllUsers(Pageable pageable, String search);

    UserResponse getUserById(Long id);

    UserResponse updateUserByAdmin(Long id, AdminUserUpdateRequest request);

    void toggleUserStatus(Long id);
}
