package com.fco.platform.market.infrastructure.persistence;

import com.fco.platform.market.domain.GameTitle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface IGameTitleRepository extends JpaRepository<GameTitle, Long> {
    Optional<GameTitle> findBySlug(String slug);
}
