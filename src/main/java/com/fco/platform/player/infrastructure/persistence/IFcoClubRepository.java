package com.fco.platform.player.infrastructure.persistence;

import com.fco.platform.player.domain.FcoClub;
import com.fco.platform.player.domain.FcoLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoClubRepository extends JpaRepository<FcoClub, Long> {

    Optional<FcoClub> findByClubNameAndLeague(String clubName, FcoLeague league);

    Optional<FcoClub> findByClubName(String clubName);

    Optional<FcoClub> findByFifaaddictId(Integer fifaaddictId);
}
