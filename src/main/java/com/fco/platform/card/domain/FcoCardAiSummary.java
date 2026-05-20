package com.fco.platform.card.domain;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
@Table(name = "fco_card_ai_summaries")
public class FcoCardAiSummary {

    /** Khóa chính đồng thời là khóa ngoại trỏ vào fco_player_cards */
    @Id
    @Column(name = "card_id")
    private Long cardId;

    @OneToOne(fetch = FetchType.LAZY)
    @MapsId
    @JoinColumn(name = "card_id")
    private PlayerCard card;

    /** JSON array dạng chuỗi: ["Tốc độ", "Dứt điểm sắc", "Kéo biên tốt"] */
    @Column(name = "positive_tags", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String positiveTags = "[]";

    /** JSON array dạng chuỗi: ["Yếu tranh chấp", "Lười di chuyển"] */
    @Column(name = "negative_tags", nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String negativeTags = "[]";

    /** Đoạn tóm tắt lối chơi ngắn gọn do AI tổng hợp từ cộng đồng */
    @Column(nullable = false, columnDefinition = "TEXT")
    @Builder.Default
    private String summary = "";

    @Column(name = "updated_at", nullable = false)
    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
}
