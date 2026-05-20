package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IFcoPlayerRepository extends JpaRepository<FcoPlayer, Long> {
    Optional<FcoPlayer> findByPlayerNameIgnoreCase(String playerName);
    Optional<FcoPlayer> findByExternalId(String externalId);
    List<FcoPlayer> findAllByExternalIdIn(List<String> externalIds);
    boolean existsByNationId(Long nationId);
    boolean existsByLeagueId(Long leagueId);

    @Override
    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nation", "club", "league"})
    org.springframework.data.domain.Page<FcoPlayer> findAll(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.EntityGraph(attributePaths = {"nation", "club", "league"})
    @org.springframework.data.jpa.repository.Query("""
            SELECT p
            FROM FcoPlayer p
            WHERE LOWER(p.playerName) LIKE LOWER(CONCAT('%', :keyword, '%'))
               OR LOWER(p.externalId) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    org.springframework.data.domain.Page<FcoPlayer> searchPlayers(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );
}
