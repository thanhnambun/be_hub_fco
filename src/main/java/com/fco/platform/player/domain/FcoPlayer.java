package com.fco.platform.player.domain;

import com.fco.platform.card.domain.PlayerCard;
import jakarta.persistence.*;
import lombok.*;

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
@Table(name = "fco_players")
public class FcoPlayer {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "player_name", nullable = false, length = 150)
    private String playerName;

    @Column(name = "external_id", unique = true, length = 100)
    private String externalId;

    @Column
    private Integer height;

    @Column
    private Integer weight;

    @Column(length = 20)
    private String birthdate;

    @Column(name = "preferred_foot", length = 10)
    private String preferredFoot;

    @Column(name = "weak_foot")
    private Integer weakFoot;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "nation_id")
    private FcoNation nation;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "club_id")
    private FcoClub club;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "league_id")
    private FcoLeague league;

    @Column(name = "nation_name", length = 100)
    private String nationName;

    @Column(name = "club_name", length = 150)
    private String clubName;

    @Column(name = "league_name", length = 150)
    private String leagueName;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "fco_player_team_colors",
            joinColumns        = @JoinColumn(name = "player_id"),
            inverseJoinColumns = @JoinColumn(name = "club_id")
    )
    @Builder.Default
    private Set<FcoClub> teamColors = new HashSet<>();

    // Timestamps
    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Relations
    @OneToMany(mappedBy = "player", fetch = FetchType.LAZY)
    @Builder.Default
    @com.fasterxml.jackson.annotation.JsonIgnore
    private List<PlayerCard> cards = new ArrayList<>();
}
