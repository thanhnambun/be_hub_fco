package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoCardAiSummary;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface IFcoCardAiSummaryRepository extends JpaRepository<FcoCardAiSummary, Long> {

    /** Tìm bản tóm tắt AI theo cardId. */
    Optional<FcoCardAiSummary> findByCardId(Long cardId);
}
