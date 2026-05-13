package com.ra.base_spring_boot.services.impl;

import com.ra.base_spring_boot.dto.req.AdminUserUpdateRequest;
import com.ra.base_spring_boot.dto.req.ChangePasswordRequest;
import com.ra.base_spring_boot.dto.req.UpdateProfileRequest;
import com.ra.base_spring_boot.dto.resp.AuthResponse;
import com.ra.base_spring_boot.dto.resp.UserResponse;
import com.ra.base_spring_boot.exception.BusinessException;
import com.ra.base_spring_boot.exception.HttpBadRequest;
import com.ra.base_spring_boot.exception.ResourceNotFoundException;
import com.ra.base_spring_boot.exception.UserNotFoundException;
import com.ra.base_spring_boot.model.Role;
import com.ra.base_spring_boot.model.User;
import com.ra.base_spring_boot.repository.IRoleRepository;
import com.ra.base_spring_boot.repository.IUserRepository;
import com.ra.base_spring_boot.services.IUserService;
import com.ra.base_spring_boot.services.TokenBlacklistService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;

    @Override
    @Transactional
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));

        user.setFullName(request.getFullName().trim());
        
        if (request.getPhone() != null && !request.getPhone().trim().isEmpty()) {
            user.setPhone(request.getPhone().trim());
        } else {
            user.setPhone(null);
        }
        
        user.setUpdatedAt(LocalDateTime.now());

        User updatedUser = userRepository.save(user);

        return AuthResponse.builder()
                .id(updatedUser.getId())
                .username(updatedUser.getUsername())
                .email(updatedUser.getEmail())
                .fullName(updatedUser.getFullName())
                .phone(updatedUser.getPhone())
                .roles(updatedUser.getRoles().stream()
                        .map(role -> role.getRoleName().name())
                        .collect(Collectors.toSet()))
                .build();
    }

    @Override
    @Transactional
    public void changePassword(Long userId, ChangePasswordRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("KhÃ´ng tÃ¬m tháº¥y ngÆ°á»i dÃ¹ng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new HttpBadRequest("Máº­t kháº©u cÅ© khÃ´ng chÃ­nh xÃ¡c");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new HttpBadRequest("Máº­t kháº©u má»›i khÃ´ng Ä‘Æ°á»£c trÃ¹ng vá»›i máº­t kháº©u cÅ©");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> findAllUsers(Pageable pageable, String search) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isStaffOnly = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().name().equals("ROLE_STAFF")) &&
                currentUser.getRoles().stream()
                .noneMatch(r -> r.getRoleName().name().equals("ROLE_ADMIN"));

        Page<User> users;
        if (isStaffOnly) {
            if (search == null || search.trim().isEmpty()) {
                users = userRepository.findAllCustomers(pageable);
            } else {
                users = userRepository.searchOnlyCustomers(search.trim(), pageable);
            }
        } else {
            if (search == null || search.trim().isEmpty()) {
                users = userRepository.findAll(pageable);
            } else {
                users = userRepository.searchUsers(search.trim(), pageable);
            }
        }
        return users.map(this::mapToUserResponse);
    }

    @Override
    public UserResponse getUserById(Long id) {
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isStaffOnly = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().name().equals("ROLE_STAFF")) &&
                currentUser.getRoles().stream()
                .noneMatch(r -> r.getRoleName().name().equals("ROLE_ADMIN"));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Nếu là STAFF, chỉ được xem chi tiết CUSTOMER
        if (isStaffOnly) {
            boolean isCustomer = targetUser.getRoles().stream()
                    .anyMatch(r -> r.getRoleName().name().equals("ROLE_CUSTOMER"));
            if (!isCustomer) {
                throw new BusinessException("Bạn không có quyền xem thông tin tài khoản quản trị viên khác");
            }
        }

        return mapToUserResponse(targetUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserByAdmin(Long id, AdminUserUpdateRequest request) {
        if (id == 1) {
            throw new BusinessException("Không được phép sửa thông tin Root Admin");
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getRoleId() == 1) {
            throw new BusinessException("Không được phép cấp quyền ROLE_ADMIN");
        }

        Role role = roleRepository.findById(request.getRoleId())
                .orElseThrow(() -> new ResourceNotFoundException("Role not found with id: " + request.getRoleId()));

        user.setFullName(request.getFullName().trim());
        user.setPhone(request.getPhone());
        user.getRoles().clear();
        user.getRoles().add(role);
        user.setUpdatedAt(LocalDateTime.now());

        return mapToUserResponse(userRepository.save(user));
    }

    @Override
    @Transactional
    public void toggleUserStatus(Long id) {
        if (id == 1) {
            throw new BusinessException("Không được phép khóa Root Admin");
        }

        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isStaffOnly = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().name().equals("ROLE_STAFF")) &&
                currentUser.getRoles().stream()
                .noneMatch(r -> r.getRoleName().name().equals("ROLE_ADMIN"));

        User targetUser = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        // Nếu là STAFF, chỉ được khóa CUSTOMER
        if (isStaffOnly) {
            boolean isCustomer = targetUser.getRoles().stream()
                    .anyMatch(r -> r.getRoleName().name().equals("ROLE_CUSTOMER"));
            if (!isCustomer) {
                throw new BusinessException("Bạn không có quyền thay đổi trạng thái của tài khoản quản trị viên khác");
            }
        }

        targetUser.setStatus(!targetUser.getStatus());
        targetUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(targetUser);

        if (!targetUser.getStatus()) {
            tokenBlacklistService.blacklistAllUserTokens(id);
        }
    }

    private UserResponse mapToUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setFullName(user.getFullName());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setPhone(user.getPhone());
        response.setStatus(user.getStatus());
        response.setRoles(user.getRoles().stream()
                .map(role -> role.getRoleName().name())
                .collect(Collectors.toSet()));
        response.setCreatedAt(user.getCreatedAt());
        return response;
    }
}
