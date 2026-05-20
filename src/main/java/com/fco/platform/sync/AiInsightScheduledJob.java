package com.fco.platform.sync;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fco.platform.card.domain.FcoCardAiSummary;
import com.fco.platform.card.domain.FcoCardReview;
import com.fco.platform.card.infrastructure.persistence.IFcoCardAiSummaryRepository;
import com.fco.platform.card.infrastructure.persistence.IFcoCardReviewRepository;
import com.fco.platform.common.application.GeminiService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * Tác vụ AI chạy định kỳ mỗi 15 phút:
 *   1. Quét tất cả review chưa AI kiểm duyệt (is_ai_checked = false)
 *   2. Nhóm theo card_id
 *   3. Gửi mẻ sang Gemini — vừa kiểm duyệt, vừa cập nhật AI Summary
 *   4. Xử lý kết quả: REJECTED nếu vi phạm, cập nhật fco_card_ai_summaries
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AiInsightScheduledJob {

    private final IFcoCardReviewRepository    reviewRepo;
    private final IFcoCardAiSummaryRepository aiSummaryRepo;
    private final GeminiService               geminiService;
    private final ObjectMapper                objectMapper;

    private static final int BATCH_SIZE = 30; // Tối đa 30 review / request

    @Scheduled(cron = "0 */15 * * * ?")
    @Transactional
    public void runAiInsightJob() {
        List<FcoCardReview> pending = reviewRepo.findAllPendingAiCheck();
        if (pending.isEmpty()) {
            log.debug("AiInsightJob: Không có review nào cần kiểm duyệt.");
            return;
        }
        log.info("AiInsightJob: Tìm thấy {} review cần xử lý.", pending.size());

        // Nhóm theo card_id
        Map<Long, List<FcoCardReview>> byCard = pending.stream()
                .collect(Collectors.groupingBy(r -> r.getCard().getId()));

        for (Map.Entry<Long, List<FcoCardReview>> entry : byCard.entrySet()) {
            Long cardId = entry.getKey();
            List<FcoCardReview> cardReviews = entry.getValue();
            String playerName = cardReviews.get(0).getCard().getPlayer() != null
                    ? cardReviews.get(0).getCard().getPlayer().getPlayerName() : "Unknown";

            // Xử lý theo từng mẻ BATCH_SIZE để bảo toàn quota
            for (int i = 0; i < cardReviews.size(); i += BATCH_SIZE) {
                List<FcoCardReview> batch = cardReviews.subList(i, Math.min(i + BATCH_SIZE, cardReviews.size()));
                processBatch(cardId, playerName, batch);
            }
        }
    }

    private void processBatch(Long cardId, String playerName, List<FcoCardReview> batch) {
        // Chuẩn bị dữ liệu gửi cho Gemini
        List<Map<String, Object>> reviewData = new ArrayList<>();
        for (FcoCardReview r : batch) {
            reviewData.add(Map.of(
                    "reviewId", r.getId(),
                    "content",  r.getContent()
            ));
        }

        GeminiService.BatchResult result = geminiService.processBatch(cardId, playerName, reviewData);
        if (result == null) {
            // Không có key hoặc lỗi API → đánh dấu checked để tránh lặp vô tận
            batch.forEach(r -> r.setIsAiChecked(true));
            reviewRepo.saveAll(batch);
            return;
        }

        // -- Áp dụng kết quả kiểm duyệt --
        Map<Long, GeminiService.ModerationResult> modMap = result.moderations().stream()
                .collect(Collectors.toMap(GeminiService.ModerationResult::reviewId, m -> m));

        for (FcoCardReview r : batch) {
            GeminiService.ModerationResult mod = modMap.get(r.getId());
            if (mod != null && mod.isViolating()) {
                r.setStatus("REJECTED");
                log.info("AiInsightJob: Review {} bị từ chối — {}", r.getId(), mod.reason());
            }
            r.setIsAiChecked(true);
        }
        reviewRepo.saveAll(batch);

        // -- Cập nhật AI Summary cho thẻ --
        GeminiService.SummaryResult summaryResult = result.summary();
        if (summaryResult != null) {
            try {
                FcoCardAiSummary summary = aiSummaryRepo.findByCardId(cardId)
                        .orElseGet(() -> FcoCardAiSummary.builder()
                                .cardId(cardId)
                                .card(batch.get(0).getCard())
                                .build());
                summary.setPositiveTags(objectMapper.writeValueAsString(summaryResult.positiveTags()));
                summary.setNegativeTags(objectMapper.writeValueAsString(summaryResult.negativeTags()));
                summary.setSummary(summaryResult.summary());
                summary.setUpdatedAt(LocalDateTime.now());
                aiSummaryRepo.save(summary);
                log.info("AiInsightJob: Cập nhật AI Summary cardId={}", cardId);
            } catch (Exception e) {
                log.error("AiInsightJob: Lỗi lưu AI Summary cardId={}: {}", cardId, e.getMessage());
            }
        }
    }
}
