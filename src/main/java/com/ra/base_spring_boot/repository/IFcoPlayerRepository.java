package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.FcoPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoPlayerRepository extends JpaRepository<FcoPlayer, Long> {
    Optional<FcoPlayer> findByPlayerNameIgnoreCase(String playerName);
    Optional<FcoPlayer> findByExternalId(String externalId);
}
