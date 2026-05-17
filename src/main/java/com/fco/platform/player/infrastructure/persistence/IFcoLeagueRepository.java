package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoLeagueRepository extends JpaRepository<FcoLeague, Long> {

    Optional<FcoLeague> findByLeagueName(String leagueName);

    Optional<FcoLeague> findByLeagueSlug(String leagueSlug);
}
