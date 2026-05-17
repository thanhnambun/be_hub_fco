package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoPlayer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface IFcoPlayerRepository extends JpaRepository<FcoPlayer, Long> {
    Optional<FcoPlayer> findByPlayerNameIgnoreCase(String playerName);
    Optional<FcoPlayer> findByExternalId(String externalId);
    List<FcoPlayer> findAllByExternalIdIn(List<String> externalIds);
}
