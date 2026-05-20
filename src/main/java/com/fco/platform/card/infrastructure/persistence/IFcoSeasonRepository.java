package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoSeason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoSeasonRepository extends JpaRepository<FcoSeason, Long> {
    Optional<FcoSeason> findBySeasonCode(String seasonCode);

    @org.springframework.data.jpa.repository.Query("""
            SELECT s
            FROM FcoSeason s
            WHERE LOWER(s.seasonCode) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(s.seasonName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    org.springframework.data.domain.Page<FcoSeason> searchSeasons(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );
}
