package com.fco.platform.auth.application;

import com.fco.platform.common.exception.*;
import com.fco.platform.auth.interfaces.dto.AdminUserUpdateRequest;
import com.fco.platform.auth.interfaces.dto.ChangePasswordRequest;
import com.fco.platform.auth.interfaces.dto.UpdateProfileRequest;
import com.fco.platform.auth.interfaces.dto.AuthResponse;
import com.fco.platform.auth.interfaces.dto.UserResponse;
import com.fco.platform.auth.domain.Role;
import com.fco.platform.auth.domain.User;
import com.fco.platform.auth.infrastructure.persistence.IRoleRepository;
import com.fco.platform.auth.infrastructure.persistence.IUserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements IUserService {

    private final IUserRepository userRepository;
    private final IRoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenBlacklistService tokenBlacklistService;
    private final org.springframework.messaging.simp.SimpMessagingTemplate messagingTemplate;

    @Override
    @Transactional
    public AuthResponse updateProfile(Long userId, UpdateProfileRequest request) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

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
                .orElseThrow(() -> new UserNotFoundException("Không tìm thấy người dùng"));

        if (!passwordEncoder.matches(request.getOldPassword(), user.getPassword())) {
            throw new HttpBadRequest("Mật khẩu cũ không chính xác");
        }

        if (passwordEncoder.matches(request.getNewPassword(), user.getPassword())) {
            throw new HttpBadRequest("Mật khẩu mới không được trùng với mật khẩu cũ");
        }

        user.setPassword(passwordEncoder.encode(request.getNewPassword()));
        user.setUpdatedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    public Page<UserResponse> findAllUsers(Pageable pageable, String search) {
        Pageable safePageable = sanitizePageableForUser(pageable);
        User currentUser = (User) SecurityContextHolder.getContext().getAuthentication().getPrincipal();
        boolean isStaffOnly = currentUser.getRoles().stream()
                .anyMatch(r -> r.getRoleName().name().equals("ROLE_STAFF")) &&
                currentUser.getRoles().stream()
                .noneMatch(r -> r.getRoleName().name().equals("ROLE_ADMIN"));

        Page<User> users;
        if (isStaffOnly) {
            if (search == null || search.trim().isEmpty()) {
                users = userRepository.findAllCustomers(safePageable);
            } else {
                users = userRepository.searchOnlyCustomers(search.trim(), safePageable);
            }
        } else {
            if (search == null || search.trim().isEmpty()) {
                users = userRepository.findAll(safePageable);
            } else {
                users = userRepository.searchUsers(search.trim(), safePageable);
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
                throw new BusinessException("Bạn không có quyền xem thông tin tài khoản quản trị viên khác", ErrorCode.AUTH_ACCESS_DENIED);
            }
        }

        return mapToUserResponse(targetUser);
    }

    @Override
    @Transactional
    public UserResponse updateUserByAdmin(Long id, AdminUserUpdateRequest request) {
        if (id == 1) {
            throw new BusinessException("Không được phép sửa thông tin Root Admin", ErrorCode.AUTH_ACCESS_DENIED);
        }

        User user = userRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("User not found with id: " + id));

        if (request.getRoleId() == 1) {
            throw new BusinessException("Không được phép cấp quyền ROLE_ADMIN", ErrorCode.AUTH_ACCESS_DENIED);
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
            throw new BusinessException("Không được phép khóa Root Admin", ErrorCode.AUTH_ACCESS_DENIED);
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
                throw new BusinessException("Bạn không có quyền thay đổi trạng thái của tài khoản quản trị viên khác", ErrorCode.AUTH_ACCESS_DENIED);
            }
        }

        targetUser.setStatus(!targetUser.getStatus());
        targetUser.setUpdatedAt(LocalDateTime.now());
        userRepository.save(targetUser);

        if (!targetUser.getStatus()) {
            tokenBlacklistService.blacklistAllUserTokens(id);
            // Notify user via WebSocket for instant logout
            messagingTemplate.convertAndSend("/topic/user-" + id, "ACCOUNT_LOCKED");
        }
    }

    /** Các field cho phép sort — Swagger hay để mẫu "string" trong sort[] gây PropertyReferenceException. */
    private static final Set<String> ALLOWED_USER_SORT_FIELDS = Set.of(
            "id", "username", "email", "fullName", "phone", "status", "balance", "createdAt", "updatedAt");

    // Chuẩn hoá Pageable: bỏ sort rác/từ khoá mẫu OpenAPI + map snake_case → property User.
    private Pageable sanitizePageableForUser(Pageable pageable) {
        Sort sort;
        if (pageable.getSort().isSorted()) {
            var orders = pageable.getSort().stream()
                    .map(this::rewriteUserSortOrder)
                    .flatMap(Optional::stream)
                    .toList();
            sort = orders.isEmpty() ? Sort.by(Sort.Order.desc("createdAt")) : Sort.by(orders);
        } else {
            sort = Sort.by(Sort.Order.desc("createdAt"));
        }
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private Optional<Sort.Order> rewriteUserSortOrder(Sort.Order o) {
        String raw = o.getProperty();
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        raw = raw.trim();
        Sort.Direction dir = o.getDirection();
        // Kiểu "createdAt,desc" trong một chuỗi (Swagger/JSON hay gửi một phần tử như vậy)
        if (raw.contains(",")) {
            String[] parts = raw.split(",", 2);
            raw = parts[0].trim();
            if (parts.length > 1) {
                try {
                    dir = Sort.Direction.fromString(parts[1].trim().toUpperCase(Locale.ROOT));
                } catch (IllegalArgumentException ignored) {
                    // giữ dir từ order gốc
                }
            }
        }
        // Placeholder của OpenAPI / người dùng không sửa
        if ("string".equalsIgnoreCase(raw) || "example".equalsIgnoreCase(raw)) {
            return Optional.empty();
        }
        String camel = snakeOrKebabToFieldName(raw);
        if (!ALLOWED_USER_SORT_FIELDS.contains(camel)) {
            return Optional.empty();
        }
        return Optional.of(new Sort.Order(dir, camel, o.getNullHandling()));
    }

    private static String snakeOrKebabToFieldName(String raw) {
        String p = raw;
        boolean quoted = (p.startsWith("'") && p.endsWith("'"))
                || (p.startsWith("\"") && p.endsWith("\""));
        if (quoted && p.length() >= 2) {
            p = p.substring(1, p.length() - 1).trim();
        }
        if (p.contains("_")) {
            StringBuilder sb = new StringBuilder();
            boolean up = false;
            for (int i = 0; i < p.length(); i++) {
                char c = p.charAt(i);
                if (c == '_') {
                    up = true;
                } else if (up) {
                    sb.append(Character.toUpperCase(c));
                    up = false;
                } else {
                    sb.append(c);
                }
            }
            p = sb.toString();
            if (!p.isEmpty()) {
                p = Character.toLowerCase(p.charAt(0)) + p.substring(1);
            }
        }
        switch (p.toLowerCase(Locale.ROOT)) {
            case "created_at" -> {
                return "createdAt";
            }
            case "updated_at" -> {
                return "updatedAt";
            }
            case "full_name" -> {
                return "fullName";
            }
            default -> {
                return p;
            }
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
