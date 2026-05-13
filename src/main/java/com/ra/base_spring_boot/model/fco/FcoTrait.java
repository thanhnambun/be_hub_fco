package com.ra.base_spring_boot.model.fco;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fco_traits")
public class FcoTrait {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "trait_code", nullable = false, unique = true, length = 60)
    private String traitCode;

    @Column(name = "trait_name", nullable = false, length = 120)
    private String traitName;

    @Column(name = "description", length = 500)
    private String description;

    @Column(name = "icon_id", length = 10)
    private String iconId;

    @Column(name = "icon_url", length = 500)
    private String iconUrl;

    @Column(name = "created_at", nullable = false)
    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
