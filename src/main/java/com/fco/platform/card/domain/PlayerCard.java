package com.fco.platform.card.domain;

import com.fco.platform.player.domain.FcoPlayer;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "fco_player_cards",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_player_card_level",
                columnNames = {"player_id", "season_id", "enhance_level"}
        )
)
public class PlayerCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "player_id", nullable = false)
    private FcoPlayer player;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "season_id", nullable = false)
    private FcoSeason season;

    @Column(nullable = false)
    private Integer ovr;

    @Column(name = "enhance_level", nullable = false)
    @Builder.Default
    private Integer enhanceLevel = 1;

    @Column(nullable = false)
    @Builder.Default
    private Integer salary = 0;

    @Column(name = "preferred_position", length = 10)
    private String preferredPosition;

    // Stats
    private Integer pace;
    private Integer shooting;
    private Integer passing;
    private Integer dribbling;
    private Integer defending;
    private Integer physicality;

    // Price
    @Column(name = "market_price_bp")
    private Long marketPriceBp;

    @Column(name = "price_updated_at")
    private LocalDate priceUpdatedAt;

    // Details
    @Column(nullable = false, columnDefinition = "TINYINT UNSIGNED DEFAULT 0")
    @Builder.Default
    private Integer liveperf = 0;

    @org.hibernate.annotations.JdbcTypeCode(org.hibernate.type.SqlTypes.JSON)
    @Column(name = "ovr_by_pos_json", columnDefinition = "json")
    private java.util.Map<String, Integer> ovrByPos;

    @Column(name = "skill_level")
    private Integer skillLevel;

    @Column(name = "secondary_position", length = 100)
    private String secondaryPosition;

    @Column(name = "workrate_att", length = 10)
    private String workerateAtt;

    @Column(name = "workrate_def", length = 10)
    private String workerateDef;

    @Column(length = 20)
    private String bodytype;

    @Column(length = 50)
    private String reputation;

    // Media
    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    // Timestamps
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relations
    @OneToMany(mappedBy = "card", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FcoCardPrice> prices = new ArrayList<>();

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "fco_player_card_traits",
            joinColumns        = @JoinColumn(name = "card_id"),
            inverseJoinColumns = @JoinColumn(name = "trait_id")
    )
    @Builder.Default
    private Set<FcoTrait> traits = new HashSet<>();

    @OneToMany(mappedBy = "card", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FcoFormationFit> formationFits = new ArrayList<>();

    @OneToMany(mappedBy = "card", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FcoCardReview> reviews = new ArrayList<>();
}
