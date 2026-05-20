package com.fco.platform.player.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fco.platform.auth.domain.User;
import com.fco.platform.auth.infrastructure.persistence.IUserRepository;
import com.fco.platform.card.domain.*;
import com.fco.platform.card.infrastructure.persistence.*;
import com.fco.platform.common.application.BadWordFilterService;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.player.interfaces.dto.ReviewDtos;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlayerReviewServiceImpl implements IPlayerReviewService {

    private final IFcoCardReviewRepository    reviewRepo;
    private final IFcoReviewVoteRepository    voteRepo;
    private final IFcoCardReviewReplyRepository replyRepo;
    private final IFcoCardAiSummaryRepository aiSummaryRepo;
    private final IPlayerCardRepository       cardRepo;
    private final IUserRepository             userRepo;
    private final BadWordFilterService        badWordFilter;
    private final ObjectMapper                objectMapper;

    // ── Get Reviews ────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDtos.ReviewResponse> getCardReviews(Long cardId, int page, int size, Long currentUserId) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 50),
                Sort.by(Sort.Direction.DESC, "createdAt"));
        return reviewRepo.findByCardIdAndStatus(cardId, "ACTIVE", pageable)
                .map(r -> toReviewResponse(r, currentUserId));
    }

    // ── Submit Review ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewDtos.ReviewResponse submitReview(Long cardId, ReviewDtos.ReviewRequest request, Long userId) {
        // 1. Kiểm tra thẻ tồn tại
        PlayerCard card = cardRepo.findById(cardId)
                .orElseThrow(() -> new BusinessException(ErrorCode.CARD_NOT_FOUND));

        // 2. Kiểm tra duplicate (mỗi user chỉ được 1 review/thẻ)
        if (reviewRepo.existsByCardIdAndUserId(cardId, userId)) {
            throw new BusinessException(ErrorCode.REVIEW_DUPLICATE);
        }

        // 3. Bộ lọc thô cục bộ (0ms)
        if (badWordFilter.containsBadWord(request.getContent())) {
            throw new BusinessException(ErrorCode.REVIEW_CONTENT_REJECTED);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FcoCardReview review = FcoCardReview.builder()
                .card(card)
                .user(user)
                .content(request.getContent().trim())
                .ingameRank(request.getIngameRank())
                .build();

        FcoCardReview saved = reviewRepo.save(review);
        log.info("Review mới: cardId={}, userId={}, reviewId={}", cardId, userId, saved.getId());
        return toReviewResponse(saved, userId);
    }

    // ── Toggle Vote ────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewDtos.VoteResponse toggleVote(Long reviewId, ReviewDtos.VoteRequest request, Long userId) {
        FcoCardReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_MODERATION));

        FcoReviewVote.VoteType newType = FcoReviewVote.VoteType.valueOf(request.getVoteType().toUpperCase());
        Optional<FcoReviewVote> existingOpt = voteRepo.findByReviewIdAndUserId(reviewId, userId);

        if (existingOpt.isPresent()) {
            FcoReviewVote existing = existingOpt.get();
            if (existing.getVoteType() == newType) {
                // Nhấn lại cùng loại → hủy vote (Toggle off)
                voteRepo.deleteByReviewIdAndUserId(reviewId, userId);
                return buildVoteResponse(reviewId, review, null);
            } else {
                // Đổi sang loại khác
                existing.setVoteType(newType);
                voteRepo.save(existing);
                return buildVoteResponse(reviewId, review, newType.name());
            }
        } else {
            User user = userRepo.findById(userId)
                    .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));
            FcoReviewVote vote = FcoReviewVote.builder()
                    .review(review).user(user).voteType(newType).build();
            voteRepo.save(vote);
            return buildVoteResponse(reviewId, review, newType.name());
        }
    }

    // ── Delete Review ──────────────────────────────────────────────────────────

    @Override
    @Transactional
    public void deleteReview(Long reviewId, Long userId, boolean isAdmin) {
        FcoCardReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_MODERATION));

        if (!isAdmin && !review.getUser().getId().equals(userId)) {
            throw new AccessDeniedException("Bạn không có quyền xóa đánh giá này.");
        }
        reviewRepo.delete(review);
        log.info("Đã xóa review {}, bởi userId={}, isAdmin={}", reviewId, userId, isAdmin);
    }

    // ── Add Reply ──────────────────────────────────────────────────────────────

    @Override
    @Transactional
    public ReviewDtos.ReplyResponse addReply(Long reviewId, ReviewDtos.ReplyRequest request, Long userId) {
        FcoCardReview review = reviewRepo.findById(reviewId)
                .orElseThrow(() -> new BusinessException(ErrorCode.REVIEW_MODERATION));

        if (badWordFilter.containsBadWord(request.getContent())) {
            throw new BusinessException(ErrorCode.REVIEW_CONTENT_REJECTED);
        }

        User user = userRepo.findById(userId)
                .orElseThrow(() -> new BusinessException(ErrorCode.USER_NOT_FOUND));

        FcoCardReviewReply reply = FcoCardReviewReply.builder()
                .review(review)
                .user(user)
                .content(request.getContent().trim())
                .build();

        FcoCardReviewReply saved = replyRepo.save(reply);
        return toReplyResponse(saved);
    }

    // ── AI Summary ─────────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public ReviewDtos.AiSummaryResponse getAiSummary(Long cardId) {
        return aiSummaryRepo.findByCardId(cardId)
                .map(this::toAiSummaryResponse)
                .orElse(null); // null = chưa có AI summary, frontend ẩn panel
    }

    // ── Recent Reviews ─────────────────────────────────────────────────────────

    @Override
    @Transactional(readOnly = true)
    public Page<ReviewDtos.RecentReviewResponse> getRecentReviews(int page, int size) {
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.min(size, 20));
        return reviewRepo.findRecentActiveReviews(pageable)
                .map(this::toRecentReviewResponse);
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private ReviewDtos.ReviewResponse toReviewResponse(FcoCardReview r, Long currentUserId) {
        long ngon = voteRepo.countByReviewIdAndVoteType(r.getId(), FcoReviewVote.VoteType.NGON);
        long phe  = voteRepo.countByReviewIdAndVoteType(r.getId(), FcoReviewVote.VoteType.PHE);

        String currentVote = null;
        if (currentUserId != null) {
            currentVote = voteRepo.findByReviewIdAndUserId(r.getId(), currentUserId)
                    .map(v -> v.getVoteType().name())
                    .orElse(null);
        }

        List<ReviewDtos.ReplyResponse> replyResponses = r.getReplies().stream()
                .map(this::toReplyResponse).toList();

        return ReviewDtos.ReviewResponse.builder()
                .id(r.getId())
                .cardId(r.getCard().getId())
                .authorUsername(r.getUser().getUsername())
                .authorFullName(r.getUser().getFullName())
                .ingameRank(r.getIngameRank())
                .content(r.getContent())
                .status(r.getStatus())
                .ngonCount(ngon)
                .pheCount(phe)
                .currentUserVote(currentVote)
                .replyCount(replyResponses.size())
                .replies(replyResponses)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private ReviewDtos.ReplyResponse toReplyResponse(FcoCardReviewReply rp) {
        return ReviewDtos.ReplyResponse.builder()
                .id(rp.getId())
                .authorUsername(rp.getUser().getUsername())
                .authorFullName(rp.getUser().getFullName())
                .content(rp.getContent())
                .createdAt(rp.getCreatedAt())
                .build();
    }

    private ReviewDtos.VoteResponse buildVoteResponse(Long reviewId, FcoCardReview review, String currentVote) {
        long ngon = voteRepo.countByReviewIdAndVoteType(review.getId(), FcoReviewVote.VoteType.NGON);
        long phe  = voteRepo.countByReviewIdAndVoteType(review.getId(), FcoReviewVote.VoteType.PHE);
        return ReviewDtos.VoteResponse.builder()
                .reviewId(reviewId)
                .ngonCount(ngon)
                .pheCount(phe)
                .currentUserVote(currentVote)
                .build();
    }

    private ReviewDtos.AiSummaryResponse toAiSummaryResponse(FcoCardAiSummary s) {
        List<String> pos = parseJsonArray(s.getPositiveTags());
        List<String> neg = parseJsonArray(s.getNegativeTags());
        return ReviewDtos.AiSummaryResponse.builder()
                .cardId(s.getCardId())
                .positiveTags(pos)
                .negativeTags(neg)
                .summary(s.getSummary())
                .updatedAt(s.getUpdatedAt())
                .build();
    }

    private ReviewDtos.RecentReviewResponse toRecentReviewResponse(FcoCardReview r) {
        long ngon = voteRepo.countByReviewIdAndVoteType(r.getId(), FcoReviewVote.VoteType.NGON);
        long phe  = voteRepo.countByReviewIdAndVoteType(r.getId(), FcoReviewVote.VoteType.PHE);
        var card = r.getCard();
        var player = card.getPlayer();
        return ReviewDtos.RecentReviewResponse.builder()
                .reviewId(r.getId())
                .cardId(card.getId())
                .playerName(player != null ? player.getPlayerName() : "")
                .seasonCode(card.getSeason() != null ? card.getSeason().getSeasonCode() : "")
                .imageUrl(card.getImageUrl())
                .ovr(card.getOvr())
                .authorUsername(r.getUser().getUsername())
                .content(r.getContent())
                .ingameRank(r.getIngameRank())
                .ngonCount(ngon)
                .pheCount(phe)
                .createdAt(r.getCreatedAt())
                .build();
    }

    private List<String> parseJsonArray(String json) {
        try {
            return objectMapper.readValue(json, new TypeReference<>() {});
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }
}
