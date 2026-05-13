package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.FcoLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoLeagueRepository extends JpaRepository<FcoLeague, Long> {

    Optional<FcoLeague> findByLeagueName(String leagueName);

    Optional<FcoLeague> findByLeagueSlug(String leagueSlug);
}
