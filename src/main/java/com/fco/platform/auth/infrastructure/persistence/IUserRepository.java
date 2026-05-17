package com.fco.platform.auth.infrastructure.persistence;

import com.fco.platform.auth.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IUserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    Optional<User> findByUsernameOrEmail(String username, String email);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM AuthUser u WHERE " +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "u.phone LIKE CONCAT('%', :search, '%')")
    org.springframework.data.domain.Page<User> searchUsers(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);

    // EXISTS trên u.roles: tránh JOIN r ở mệnh đề FROM — Sort/Pageable áp vào đúng root User (không PropertyReferenceException)
    @org.springframework.data.jpa.repository.Query("SELECT u FROM AuthUser u WHERE EXISTS (" +
            "SELECT ur FROM u.roles ur WHERE ur.roleName = com.fco.platform.auth.domain.RoleName.ROLE_CUSTOMER" +
            ") AND (" +
            "LOWER(u.fullName) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.username) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "LOWER(u.email) LIKE LOWER(CONCAT('%', :search, '%')) OR " +
            "u.phone LIKE CONCAT('%', :search, '%'))")
    org.springframework.data.domain.Page<User> searchOnlyCustomers(@org.springframework.data.repository.query.Param("search") String search, org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("SELECT u FROM AuthUser u WHERE EXISTS (" +
            "SELECT ur FROM u.roles ur WHERE ur.roleName = com.fco.platform.auth.domain.RoleName.ROLE_CUSTOMER)")
    org.springframework.data.domain.Page<User> findAllCustomers(org.springframework.data.domain.Pageable pageable);
}
