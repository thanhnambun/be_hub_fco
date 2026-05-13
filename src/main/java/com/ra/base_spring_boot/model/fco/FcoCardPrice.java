package com.ra.base_spring_boot.model.fco;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(
        name = "fco_card_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_card_grade_date",
                columnNames = {"card_id", "grade", "price_date"}
        )
)
public class FcoCardPrice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "card_id", nullable = false)
    private PlayerCard card;

    
    @Column(nullable = false)
    private Integer grade;

    @Column(name = "price_bp", nullable = false)
    private Long priceBp;

    @Column(name = "price_raw", length = 30)
    private String priceRaw;

    @Column(name = "price_date", nullable = false)
    private LocalDate priceDate;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();
}
