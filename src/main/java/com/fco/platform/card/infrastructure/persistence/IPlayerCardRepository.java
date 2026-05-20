package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.PlayerCard;
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
    List<PlayerCard> findAllByPlayerIdIn(List<Long> playerIds);
    boolean existsBySeasonId(Long seasonId);

    Optional<PlayerCard> findFirstByPlayerIdAndSeasonIdOrderByUpdatedAtDesc(Long playerId, Long seasonId);
    Optional<PlayerCard> findFirstByPlayerExternalIdAndSeasonSeasonCodeAndOvrOrderByUpdatedAtDesc(
            String externalId,
            String seasonCode,
            Integer ovr
    );

    @EntityGraph(attributePaths = {"player", "season", "player.nation", "player.club", "player.league"})
    @Query("""
            SELECT pc
            FROM PlayerCard pc
            JOIN pc.player p
            JOIN pc.season s
            LEFT JOIN p.nation n
            WHERE (:keyword IS NULL OR :keyword = '' OR LOWER(TRIM(p.playerName)) LIKE LOWER(CONCAT('%', TRIM(:keyword), '%')))
              AND (:seasonCode IS NULL OR :seasonCode = '' OR LOWER(TRIM(s.seasonCode)) = LOWER(TRIM(:seasonCode)))
              AND (:nationId IS NULL OR n.id = :nationId)
              AND (:position IS NULL OR :position = '' OR LOWER(TRIM(pc.preferredPosition)) = LOWER(TRIM(:position)) OR LOWER(TRIM(pc.secondaryPosition)) = LOWER(TRIM(:position)))
              AND (:minPrice IS NULL OR pc.marketPriceBp >= :minPrice)
              AND (:maxPrice IS NULL OR pc.marketPriceBp <= :maxPrice)
            ORDER BY pc.ovr DESC, pc.updatedAt DESC
            """)
    Page<PlayerCard> search(
            @Param("keyword") String keyword,
            @Param("seasonCode") String seasonCode,
            @Param("nationId") Long nationId,
            @Param("position") String position,
            @Param("minPrice") Long minPrice,
            @Param("maxPrice") Long maxPrice,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {
            "player",
            "player.nation",
            "player.league",
            "player.teamColors",
            "season",
            "traits",
            "prices"
    })
    @Query("SELECT pc FROM PlayerCard pc WHERE pc.id = :id")
    Optional<PlayerCard> findByIdWithDetails(@Param("id") Long id);
}
