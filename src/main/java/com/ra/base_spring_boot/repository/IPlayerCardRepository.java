package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.PlayerCard;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;

public interface IPlayerCardRepository extends JpaRepository<PlayerCard, Long> {

    List<PlayerCard> findAllByPlayerId(Long playerId);

    Optional<PlayerCard> findFirstByPlayerIdAndSeasonIdOrderByUpdatedAtDesc(Long playerId, Long seasonId);
    Optional<PlayerCard> findFirstByPlayerExternalIdAndSeasonSeasonCodeAndOvrOrderByUpdatedAtDesc(
            String externalId,
            String seasonCode,
            Integer ovr
    );

    @Query("""
            SELECT pc
            FROM PlayerCard pc
            JOIN pc.player p
            JOIN pc.season s
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(p.playerName) LIKE LOWER(CONCAT('%', :keyword, '%')))
              AND (:seasonCode IS NULL OR :seasonCode = '' OR LOWER(s.seasonCode) = LOWER(:seasonCode))
            ORDER BY pc.updatedAt DESC
            """)
    Page<PlayerCard> search(@Param("keyword") String keyword, @Param("seasonCode") String seasonCode, Pageable pageable);

    @EntityGraph(attributePaths = {
            "player",
            "player.nation",
            "player.league",
            "player.teamColors",
            "season"
    })
    @Query("SELECT pc FROM PlayerCard pc WHERE pc.id = :id")
    Optional<PlayerCard> findByIdWithDetails(@Param("id") Long id);
}
