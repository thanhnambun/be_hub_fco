package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.FcoSeason;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoSeasonRepository extends JpaRepository<FcoSeason, Long> {
    Optional<FcoSeason> findBySeasonCode(String seasonCode);
}
