package com.fco.platform.player.domain;

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
@Table(name = "fco_leagues")
public class FcoLeague {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "league_name", nullable = false, unique = true, length = 150)
    private String leagueName;

    @Column(name = "league_slug", length = 180)
    private String leagueSlug;

    @Column(name = "logo_url", length = 500)
    private String logoUrl;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Các CLB thuộc giải đấu này
    @OneToMany(mappedBy = "league", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FcoClub> clubs = new ArrayList<>();
}
