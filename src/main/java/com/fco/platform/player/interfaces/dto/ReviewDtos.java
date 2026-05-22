package com.fco.platform.player.interfaces.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;

/**
 * DTOs cho module Community Insight (Review, Vote, Reply, AI Summary).
 */
public final class ReviewDtos {
    private ReviewDtos() {}

    // ── Requests ───────────────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewRequest {
        @NotBlank(message = "Nội dung đánh giá không được để trống")
        @Size(min = 10, max = 280, message = "Nội dung đánh giá phải từ 10 đến 280 ký tự")
        private String content;

        @Size(max = 50, message = "Rank in-game tối đa 50 ký tự")
        private String ingameRank;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyRequest {
        @NotBlank(message = "Nội dung phản hồi không được để trống")
        @Size(min = 2, max = 280, message = "Nội dung phản hồi phải từ 2 đến 280 ký tự")
        private String content;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteRequest {
        /** Giá trị hợp lệ: "NGON" hoặc "PHE" */
        @NotBlank(message = "Loại vote không được để trống")
        private String voteType;
    }

    // ── Responses ──────────────────────────────────────────────────────────────

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewResponse {
        private Long id;
        private Long cardId;
        private String authorUsername;
        private String authorFullName;
        private String ingameRank;
        private String content;
        private String status;
        private long ngonCount;
        private long pheCount;
        /** Loại vote của user hiện tại (null = chưa vote, "NGON"/"PHE" = đã vote) */
        private String currentUserVote;
        private int replyCount;
        private List<ReplyResponse> replies;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReplyResponse {
        private Long id;
        private String authorUsername;
        private String authorFullName;
        private String content;
        private LocalDateTime createdAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class AiSummaryResponse {
        private Long cardId;
        /** JSON string array: ["Tốc độ", "Dứt điểm sắc"] */
        private List<String> positiveTags;
        /** JSON string array: ["Yếu tranh chấp"] */
        private List<String> negativeTags;
        private String summary;
        private LocalDateTime updatedAt;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VoteResponse {
        private Long reviewId;
        private long ngonCount;
        private long pheCount;
        /** Loại vote mới nhất của user (null = đã hủy vote) */
        private String currentUserVote;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecentReviewResponse {
        private Long reviewId;
        private Long cardId;
        private String playerName;
        private String seasonCode;
        private String imageUrl;
        private Integer ovr;
        private String authorUsername;
        private String content;
        private String ingameRank;
        private long ngonCount;
        private long pheCount;
        private LocalDateTime createdAt;
    }

    // ── Card-level Vote DTOs ────────────────────────────────────────────────────

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardVoteRequest {
        /** Giá trị hợp lệ: "NGON" hoặc "PHE" */
        @NotBlank(message = "Loại vote không được để trống")
        private String voteType;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CardVoteResponse {
        private Long cardId;
        private long ngonCount;
        private long pheCount;
        /** null = chưa vote, "NGON" / "PHE" = đang vote loại này */
        private String currentUserVote;
    }
}
