package com.ra.base_spring_boot.repository;

import com.ra.base_spring_boot.model.GameTitle;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IGameTitleRepository extends JpaRepository<GameTitle, Long> {
    Optional<GameTitle> findBySlug(String slug);
}
