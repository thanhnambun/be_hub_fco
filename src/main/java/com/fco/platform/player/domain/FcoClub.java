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
@Table(
        name = "fco_clubs",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_club_name_league",
                columnNames = {"club_name", "league_id"}
        )
)
public class FcoClub {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "club_name", nullable = false, length = 150)
    private String clubName;

    @Column(name = "club_slug", length = 180)
    private String clubSlug;

    @Column(name = "fifaaddict_id")
    private Integer fifaaddictId;

    @Column(name = "crest_url", length = 500)
    private String crestUrl;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private FcoLeague league;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    @OneToMany(mappedBy = "club", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FcoPlayer> players = new ArrayList<>();
}
