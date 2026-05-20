package com.fco.platform.player.interfaces.web;

import com.fco.platform.auth.domain.User;
import com.fco.platform.common.dto.ResponseWrapper;
import com.fco.platform.common.dto.resp.PageResponse;
import com.fco.platform.player.application.IPlayerReviewService;
import com.fco.platform.player.interfaces.dto.ReviewDtos;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class PlayerReviewController {

    private final IPlayerReviewService reviewService;

    // ── GET reviews của 1 thẻ (Public) ────────────────────────────────────────

    @GetMapping("/api/v1/cards/{cardId}/reviews")
    public ResponseEntity<ResponseWrapper<PageResponse<ReviewDtos.ReviewResponse>>> getCardReviews(
            @PathVariable Long cardId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @AuthenticationPrincipal User currentUser
    ) {
        Long userId = currentUser != null ? currentUser.getId() : null;
        Page<ReviewDtos.ReviewResponse> result = reviewService.getCardReviews(cardId, page, size, userId);
        return ResponseEntity.ok(ResponseWrapper.<PageResponse<ReviewDtos.ReviewResponse>>builder()
                .status(HttpStatus.OK).code(200)
                .data(toPageResponse(result))
                .build());
    }

    // ── POST đánh giá mới (User đăng nhập) ────────────────────────────────────

    @PostMapping("/api/v1/cards/{cardId}/reviews")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseWrapper<ReviewDtos.ReviewResponse>> submitReview(
            @PathVariable Long cardId,
            @Valid @RequestBody ReviewDtos.ReviewRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReviewDtos.ReviewResponse response = reviewService.submitReview(cardId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.<ReviewDtos.ReviewResponse>builder()
                        .status(HttpStatus.CREATED).code(201)
                        .message("Đăng đánh giá thành công!")
                        .data(response)
                        .build());
    }

    // ── POST vote NGON/PHE (Toggle) ────────────────────────────────────────────

    @PostMapping("/api/v1/reviews/{reviewId}/vote")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseWrapper<ReviewDtos.VoteResponse>> toggleVote(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDtos.VoteRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReviewDtos.VoteResponse result = reviewService.toggleVote(reviewId, request, currentUser.getId());
        return ResponseEntity.ok(ResponseWrapper.<ReviewDtos.VoteResponse>builder()
                .status(HttpStatus.OK).code(200).data(result).build());
    }

    // ── DELETE review (Owner hoặc Admin) ──────────────────────────────────────

    @DeleteMapping("/api/v1/reviews/{reviewId}")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseWrapper<Void>> deleteReview(
            @PathVariable Long reviewId,
            @AuthenticationPrincipal User currentUser
    ) {
        boolean isAdmin = currentUser.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN") || a.getAuthority().equals("ROLE_STAFF"));
        reviewService.deleteReview(reviewId, currentUser.getId(), isAdmin);
        return ResponseEntity.ok(ResponseWrapper.<Void>builder()
                .status(HttpStatus.OK).code(200).message("Đã xóa đánh giá.").build());
    }

    // ── POST phản hồi 1 cấp ────────────────────────────────────────────────────

    @PostMapping("/api/v1/reviews/{reviewId}/replies")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<ResponseWrapper<ReviewDtos.ReplyResponse>> addReply(
            @PathVariable Long reviewId,
            @Valid @RequestBody ReviewDtos.ReplyRequest request,
            @AuthenticationPrincipal User currentUser
    ) {
        ReviewDtos.ReplyResponse result = reviewService.addReply(reviewId, request, currentUser.getId());
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ResponseWrapper.<ReviewDtos.ReplyResponse>builder()
                        .status(HttpStatus.CREATED).code(201)
                        .message("Phản hồi đã được gửi!")
                        .data(result)
                        .build());
    }

    // ── GET AI Summary của 1 thẻ (Public) ─────────────────────────────────────

    @GetMapping("/api/v1/cards/{cardId}/ai-summary")
    public ResponseEntity<ResponseWrapper<ReviewDtos.AiSummaryResponse>> getAiSummary(
            @PathVariable Long cardId
    ) {
        ReviewDtos.AiSummaryResponse result = reviewService.getAiSummary(cardId);
        return ResponseEntity.ok(ResponseWrapper.<ReviewDtos.AiSummaryResponse>builder()
                .status(HttpStatus.OK).code(200).data(result).build());
    }

    // ── GET Recent Reviews toàn hệ thống (Public) ─────────────────────────────

    @GetMapping("/api/v1/reviews/recent")
    public ResponseEntity<ResponseWrapper<PageResponse<ReviewDtos.RecentReviewResponse>>> getRecentReviews(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size
    ) {
        Page<ReviewDtos.RecentReviewResponse> result = reviewService.getRecentReviews(page, size);
        return ResponseEntity.ok(ResponseWrapper.<PageResponse<ReviewDtos.RecentReviewResponse>>builder()
                .status(HttpStatus.OK).code(200)
                .data(toPageResponse(result))
                .build());
    }

    // ── Helper ─────────────────────────────────────────────────────────────────

    private <T> PageResponse<T> toPageResponse(Page<T> page) {
        return PageResponse.<T>builder()
                .items(page.getContent())
                .page(page.getNumber())
                .size(page.getSize())
                .totalItems(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .hasNext(page.hasNext())
                .build();
    }
}
