package com.fco.platform.card.domain;

import com.fco.platform.auth.domain.User;
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
@Table(name = "fco_card_reviews")
public class FcoCardReview {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private PlayerCard card;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 280)
    private String content;

    @Column(name = "ingame_rank", length = 50)
    private String ingameRank;

    @Column(name = "trust_weight", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal trustWeight = BigDecimal.ONE;

    @Column(nullable = false, length = 20)
    @Builder.Default
    private String status = "ACTIVE";

    /** false = chờ AI kiểm duyệt | true = đã được AI thẩm định */
    @Column(name = "is_ai_checked", nullable = false)
    @Builder.Default
    private Boolean isAiChecked = false;

    @OneToMany(mappedBy = "review", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<FcoCardReviewReply> replies = new ArrayList<>();

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}

