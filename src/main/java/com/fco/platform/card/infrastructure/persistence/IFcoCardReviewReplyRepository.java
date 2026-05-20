package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoCardReviewReply;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.EntityGraph;

import java.util.List;

public interface IFcoCardReviewReplyRepository extends JpaRepository<FcoCardReviewReply, Long> {

    /** Lấy toàn bộ replies của 1 review, kèm thông tin user (tránh N+1). */
    @EntityGraph(attributePaths = {"user"})
    List<FcoCardReviewReply> findByReviewIdOrderByCreatedAtAsc(Long reviewId);
}
