package com.ra.base_spring_boot.model;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "game_accounts")
public class GameAccount {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "game_title_id", nullable = false)
    private GameTitle gameTitle;

    @Column(name = "account_code", nullable = false, unique = true, length = 40)
    private String accountCode;

    @Column(nullable = false)
    private String title;

    @Lob
    private String description;

    @Column(name = "account_level")
    private Integer accountLevel;

    @Column(name = "rank_name", length = 120)
    private String rankName;

    @Column(name = "hero_count")
    private Integer heroCount;

    @Column(name = "skin_count")
    private Integer skinCount;

    @Column(nullable = false, precision = 15, scale = 2)
    private BigDecimal price;

    @Column(name = "login_method", length = 50)
    private String loginMethod;

    @Column(name = "source_note", length = 255)
    private String sourceNote;

    @Column(name = "account_status", nullable = false, length = 30)
    private String accountStatus;

    @Column(name = "reserved_at")
    private LocalDateTime reservedAt;

    @Column(name = "reserve_expires_at")
    private LocalDateTime reserveExpiresAt;

    @Column(name = "is_featured", nullable = false)
    @Builder.Default
    private Boolean isFeatured = false;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @Column(name = "sold_at")
    private LocalDateTime soldAt;

    @OneToMany(mappedBy = "gameAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccountAttribute> attributes = new ArrayList<>();

    @OneToMany(mappedBy = "gameAccount", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<AccountImage> images = new ArrayList<>();
}
