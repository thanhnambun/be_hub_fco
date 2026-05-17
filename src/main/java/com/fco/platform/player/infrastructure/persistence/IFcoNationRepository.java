package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoNation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoNationRepository extends JpaRepository<FcoNation, Long> {
    Optional<FcoNation> findByNationName(String nationName);

    Optional<FcoNation> findByNationSlug(String nationSlug);
}
