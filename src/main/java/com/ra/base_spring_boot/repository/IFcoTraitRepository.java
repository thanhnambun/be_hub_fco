package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.fco.FcoTrait;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoTraitRepository extends JpaRepository<FcoTrait, Long> {

    Optional<FcoTrait> findByTraitCode(String traitCode);
}
