package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoCardPrice;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IFcoCardPriceRepository extends JpaRepository<FcoCardPrice, Long> {


    Optional<FcoCardPrice> findByCardIdAndGradeAndPriceDate(Long cardId, Integer grade, LocalDate priceDate);

    @Query("""
            SELECT p FROM FcoCardPrice p
            WHERE p.card.id = :cardId
              AND p.priceDate = (
                  SELECT MAX(p2.priceDate) FROM FcoCardPrice p2 WHERE p2.card.id = :cardId
              )
            ORDER BY p.grade ASC
            """)
    List<FcoCardPrice> findLatestPricesByCardId(@Param("cardId") Long cardId);
}
