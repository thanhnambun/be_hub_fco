package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoNation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoNationRepository extends JpaRepository<FcoNation, Long> {
    Optional<FcoNation> findByNationName(String nationName);

    Optional<FcoNation> findByNationSlug(String nationSlug);

    java.util.List<FcoNation> findAllByDeletedFalse();

    org.springframework.data.domain.Page<FcoNation> findAllByDeletedFalse(org.springframework.data.domain.Pageable pageable);

    @org.springframework.data.jpa.repository.Query("""
            SELECT n
            FROM FcoNation n
            WHERE n.deleted = false
              AND LOWER(n.nationName) LIKE LOWER(CONCAT('%', :keyword, '%'))
            """)
    org.springframework.data.domain.Page<FcoNation> searchNations(
            @org.springframework.data.repository.query.Param("keyword") String keyword,
            org.springframework.data.domain.Pageable pageable
    );
}
