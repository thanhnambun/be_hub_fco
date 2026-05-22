package com.fco.platform.card.infrastructure.persistence;

import com.fco.platform.card.domain.FcoReviewVote;
import com.fco.platform.card.domain.FcoReviewVoteId;
import com.fco.platform.card.domain.FcoReviewVote.VoteType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
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

    /** Lấy danh sách thống kê vote (reviewId, voteType, count) cho một nhóm reviewId. */
    @Query("SELECT v.review.id, v.voteType, COUNT(v) FROM FcoReviewVote v WHERE v.review.id IN :reviewIds GROUP BY v.review.id, v.voteType")
    List<Object[]> countVotesByReviewIdsIn(@Param("reviewIds") java.util.Collection<Long> reviewIds);

    /** Lấy toàn bộ vote của user hiện tại trên nhóm reviewId này. */
    @Query("SELECT v FROM FcoReviewVote v WHERE v.review.id IN :reviewIds AND v.user.id = :userId")
    List<FcoReviewVote> findByReviewIdInAndUserId(@Param("reviewIds") java.util.Collection<Long> reviewIds, @Param("userId") Long userId);
}
