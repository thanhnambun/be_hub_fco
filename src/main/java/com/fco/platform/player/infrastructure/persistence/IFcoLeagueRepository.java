package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoLeagueRepository extends JpaRepository<FcoLeague, Long> {

    Optional<FcoLeague> findByLeagueName(String leagueName);

    Optional<FcoLeague> findByLeagueSlug(String leagueSlug);

    java.util.List<FcoLeague> findAllByDeletedFalse();

    org.springframework.data.domain.Page<FcoLeague> findAllByDeletedFalse(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT l
            FROM FcoLeague l
            WHERE l.deleted = false
              AND (LOWER(l.leagueName) LIKE LOWER(CONCAT('%', :keyword, '%'))
                   OR LOWER(l.leagueSlug) LIKE LOWER(CONCAT('%', :keyword, '%')))
            """)
    org.springframework.data.domain.Page<FcoLeague> searchLeagues(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );
}
