package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoSeason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoSeasonRepository extends JpaRepository<FcoSeason, Long> {
    Optional<FcoSeason> findBySeasonCode(String seasonCode);
}
