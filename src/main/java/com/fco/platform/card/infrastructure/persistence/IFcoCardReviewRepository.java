package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoCardReview;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface IFcoCardReviewRepository extends JpaRepository<FcoCardReview, Long> {

    /** Lấy danh sách đánh giá ACTIVE của 1 thẻ, kèm user và replies (tránh N+1). */
    @EntityGraph(attributePaths = {"user"})
    Page<FcoCardReview> findByCardIdAndStatus(Long cardId, String status, Pageable pageable);

    /** Kiểm tra user đã viết đánh giá cho thẻ này chưa (enforce unique constraint). */
    boolean existsByCardIdAndUserId(Long cardId, Long userId);

    /** Tìm đánh giá của 1 user cho 1 thẻ cụ thể. */
    Optional<FcoCardReview> findByCardIdAndUserId(Long cardId, Long userId);

    /** Lấy tất cả review chưa qua AI kiểm duyệt (dùng cho Scheduled Job). */
    @Query("SELECT r FROM FcoCardReview r WHERE r.isAiChecked = false ORDER BY r.createdAt ASC")
    List<FcoCardReview> findAllPendingAiCheck();

    /** Lấy các đánh giá mới nhất toàn hệ thống (trang /reviews tổng hợp). */
    @EntityGraph(attributePaths = {"user", "card", "card.player", "card.season"})
    @Query("SELECT r FROM FcoCardReview r WHERE r.status = 'ACTIVE' ORDER BY r.createdAt DESC")
    Page<FcoCardReview> findRecentActiveReviews(Pageable pageable);

    /** Đếm tổng số review ACTIVE theo cardId (dùng cho badge tab). */
    long countByCardIdAndStatus(Long cardId, String status);
}
