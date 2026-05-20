package com.fco.platform.player.interfaces.web;

import com.fco.platform.card.domain.FcoCardReview;
import com.fco.platform.card.infrastructure.persistence.IFcoCardReviewRepository;
import com.fco.platform.common.dto.ResponseWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/admin/reviews")
@RequiredArgsConstructor
@PreAuthorize("hasAnyRole('ADMIN', 'STAFF')")
public class AdminReviewController {

    private final IFcoCardReviewRepository reviewRepo;

    /** Ẩn một đánh giá vi phạm (chuyển status → REJECTED). */
    @PutMapping("/{reviewId}/hide")
    public ResponseEntity<ResponseWrapper<Void>> hideReview(@PathVariable Long reviewId) {
        reviewRepo.findById(reviewId).ifPresent(r -> {
            r.setStatus("REJECTED");
            r.setIsAiChecked(true);
            reviewRepo.save(r);
            log.info("Admin ẩn review {}", reviewId);
        });
        return ResponseEntity.ok(ResponseWrapper.<Void>builder()
                .status(HttpStatus.OK).code(200)
                .message("Đã ẩn đánh giá " + reviewId + ".")
                .build());
    }

    /** Khôi phục đánh giá đã ẩn (chuyển status → ACTIVE). */
    @PutMapping("/{reviewId}/restore")
    public ResponseEntity<ResponseWrapper<Void>> restoreReview(@PathVariable Long reviewId) {
        reviewRepo.findById(reviewId).ifPresent(r -> {
            r.setStatus("ACTIVE");
            reviewRepo.save(r);
            log.info("Admin khôi phục review {}", reviewId);
        });
        return ResponseEntity.ok(ResponseWrapper.<Void>builder()
                .status(HttpStatus.OK).code(200)
                .message("Đã khôi phục đánh giá " + reviewId + ".")
                .build());
    }

    /** Lấy danh sách đánh giá chờ kiểm duyệt (is_ai_checked = false). */
    @GetMapping("/pending")
    public ResponseEntity<ResponseWrapper<java.util.List<Long>>> getPendingReviews() {
        java.util.List<Long> ids = reviewRepo.findAllPendingAiCheck()
                .stream().map(FcoCardReview::getId).toList();
        return ResponseEntity.ok(ResponseWrapper.<java.util.List<Long>>builder()
                .status(HttpStatus.OK).code(200).data(ids).build());
    }
}
