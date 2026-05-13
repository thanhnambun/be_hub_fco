package com.ra.base_spring_boot.services;

import com.ra.base_spring_boot.dto.req.ChangePasswordRequest;
import com.ra.base_spring_boot.dto.req.UpdateProfileRequest;
import com.ra.base_spring_boot.dto.resp.AuthResponse;

public interface IUserService {
    AuthResponse updateProfile(Long userId, UpdateProfileRequest request);
    
    void changePassword(Long userId, ChangePasswordRequest request);
}
