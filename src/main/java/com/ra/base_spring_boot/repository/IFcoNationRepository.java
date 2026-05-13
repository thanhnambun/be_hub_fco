package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.FcoNation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoNationRepository extends JpaRepository<FcoNation, Long> {
    Optional<FcoNation> findByNationName(String nationName);

    Optional<FcoNation> findByNationSlug(String nationSlug);
}
