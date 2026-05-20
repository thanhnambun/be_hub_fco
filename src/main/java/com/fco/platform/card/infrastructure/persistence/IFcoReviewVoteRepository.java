package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoReviewVote;
import com.fco.platform.card.domain.FcoReviewVoteId;
import com.fco.platform.card.domain.FcoReviewVote.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface IFcoReviewVoteRepository extends JpaRepository<FcoReviewVote, FcoReviewVoteId> {

    /** Đếm số lượt vote theo loại cho 1 review. */
    @Query("SELECT COUNT(v) FROM FcoReviewVote v WHERE v.review.id = :reviewId AND v.voteType = :voteType")
    long countByReviewIdAndVoteType(@Param("reviewId") Long reviewId, @Param("voteType") VoteType voteType);

    /** Tìm vote hiện tại của user trên 1 review (để biết user đã vote chưa và vote loại gì). */
    @Query("SELECT v FROM FcoReviewVote v WHERE v.review.id = :reviewId AND v.user.id = :userId")
    Optional<FcoReviewVote> findByReviewIdAndUserId(@Param("reviewId") Long reviewId, @Param("userId") Long userId);

    /** Xóa vote theo reviewId + userId (dùng khi toggle). */
    @Query("DELETE FROM FcoReviewVote v WHERE v.review.id = :reviewId AND v.user.id = :userId")
    @org.springframework.data.jpa.repository.Modifying
    @org.springframework.transaction.annotation.Transactional
    void deleteByReviewIdAndUserId(@Param("reviewId") Long reviewId, @Param("userId") Long userId);
}
