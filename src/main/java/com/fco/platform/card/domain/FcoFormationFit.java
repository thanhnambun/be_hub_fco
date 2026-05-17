package com.fco.platform.card.domain;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "fco_formation_fits",
        uniqueConstraints = @UniqueConstraint(name = "uq_card_formation", columnNames = {"card_id", "formation_code"})
)
public class FcoFormationFit {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private PlayerCard card;

    @Column(name = "formation_code", nullable = false, length = 20)
    private String formationCode;

    @Column(name = "fit_score", nullable = false, precision = 5, scale = 2)
    @Builder.Default
    private BigDecimal fitScore = BigDecimal.ZERO;

    @Column(length = 255)
    private String note;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
