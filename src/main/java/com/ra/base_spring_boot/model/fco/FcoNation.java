package com.ra.base_spring_boot.model.fco;

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
@Table(name = "fco_nations")
public class FcoNation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "nation_name", nullable = false, unique = true, length = 100)
    private String nationName;

    @Column(name = "nation_slug", length = 120)
    private String nationSlug;

    @Column(name = "flag_url", length = 500)
    private String flagUrl;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();

    // Ngược lại: các cầu thủ thuộc quốc gia này
    @OneToMany(mappedBy = "nation", fetch = FetchType.LAZY)
    @Builder.Default
    private List<FcoPlayer> players = new ArrayList<>();
}
