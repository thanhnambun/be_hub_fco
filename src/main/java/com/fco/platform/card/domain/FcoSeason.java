package com.fco.platform.card.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fco_seasons")
public class FcoSeason {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "season_code", nullable = false, unique = true, length = 20)
    private String seasonCode;

    @Column(name = "season_name", nullable = false, length = 100)
    private String seasonName;

    @Column(name = "is_core", nullable = false)
    @Builder.Default
    private Boolean isCore = true;

    @Column(name = "is_active", nullable = false)
    @Builder.Default
    private Boolean isActive = true;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "season", fetch = FetchType.LAZY)
    @Builder.Default
    private List<PlayerCard> cards = new ArrayList<>();
}
