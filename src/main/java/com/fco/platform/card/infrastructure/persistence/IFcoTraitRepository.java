package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoTrait;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoTraitRepository extends JpaRepository<FcoTrait, Long> {

    Optional<FcoTrait> findByTraitCode(String traitCode);
}
