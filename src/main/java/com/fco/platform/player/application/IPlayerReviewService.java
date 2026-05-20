package com.fco.platform.player.application;

import com.fco.platform.player.interfaces.dto.ReviewDtos;
import org.springframework.data.domain.Page;

public interface IPlayerReviewService {

    /** Lấy danh sách đánh giá ACTIVE của 1 thẻ, kèm số vote và trạng thái vote của currentUserId. */
    Page<ReviewDtos.ReviewResponse> getCardReviews(Long cardId, int page, int size, Long currentUserId);

    /** Người dùng gửi đánh giá mới (qua bộ lọc thô trước). */
    ReviewDtos.ReviewResponse submitReview(Long cardId, ReviewDtos.ReviewRequest request, Long userId);

    /** Toggle vote NGON/PHE cho 1 review. */
    ReviewDtos.VoteResponse toggleVote(Long reviewId, ReviewDtos.VoteRequest request, Long userId);

    /** Người dùng xóa đánh giá của chính mình (hoặc Admin/Staff). */
    void deleteReview(Long reviewId, Long userId, boolean isAdmin);

    /** Thêm phản hồi 1 cấp vào 1 review. */
    ReviewDtos.ReplyResponse addReply(Long reviewId, ReviewDtos.ReplyRequest request, Long userId);

    /** Lấy tóm tắt AI của 1 thẻ. */
    ReviewDtos.AiSummaryResponse getAiSummary(Long cardId);

    /** Lấy danh sách đánh giá mới nhất toàn hệ thống (trang /reviews). */
    Page<ReviewDtos.RecentReviewResponse> getRecentReviews(int page, int size);
}
