package com.fco.platform.market.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "account_deliveries")
public class AccountDelivery {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "order_item_id", nullable = false, unique = true)
    private Long orderItemId;

    @Column(name = "delivery_method", nullable = false, length = 30)
    private String deliveryMethod;

    @Column(name = "account_login", length = 150)
    private String accountLogin;

    // SECURITY: Must be encrypted with AES-256 in service layer before persistence.
    @Column(name = "account_password", length = 255)
    private String accountPassword;

    @Column(name = "email_recovery", length = 150)
    private String emailRecovery;

    // SECURITY: Must be encrypted with AES-256 in service layer before persistence.
    @Column(name = "email_password", length = 255)
    private String emailPassword;

    // SECURITY: Must be encrypted with AES-256 in service layer before persistence.
    @Column(name = "two_factor_backup_code", length = 255)
    private String twoFactorBackupCode;

    @Column(name = "delivered_at")
    private LocalDateTime deliveredAt;

    @Column(name = "confirmed_at")
    private LocalDateTime confirmedAt;

    @Column(name = "auto_delivery_attempts", nullable = false)
    @Builder.Default
    private Integer autoDeliveryAttempts = 0;

    @Column(name = "last_attempt_at")
    private LocalDateTime lastAttemptAt;

    @Column(name = "error_message", length = 500)
    private String errorMessage;

    @Column(name = "delivery_status", nullable = false, length = 30)
    private String deliveryStatus;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
