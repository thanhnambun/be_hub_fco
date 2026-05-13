package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.FcoClub;
import com.ra.base_spring_boot.model.fco.FcoLeague;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoClubRepository extends JpaRepository<FcoClub, Long> {

    Optional<FcoClub> findByClubNameAndLeague(String clubName, FcoLeague league);

    Optional<FcoClub> findByClubName(String clubName);

    Optional<FcoClub> findByFifaaddictId(Integer fifaaddictId);
}
