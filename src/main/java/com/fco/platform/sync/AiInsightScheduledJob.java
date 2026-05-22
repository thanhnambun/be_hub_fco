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
            
            // Lấy thông tin thẻ cầu thủ
            com.fco.platform.card.domain.PlayerCard card = cardReviews.get(0).getCard();
            Map<String, Object> cardSpecs = buildCardSpecs(card);

            // Xử lý theo từng mẻ BATCH_SIZE để bảo toàn quota
            for (int i = 0; i < cardReviews.size(); i += BATCH_SIZE) {
                List<FcoCardReview> batch = cardReviews.subList(i, Math.min(i + BATCH_SIZE, cardReviews.size()));
                processBatch(cardId, cardSpecs, batch);
            }
        }
    }

    private Map<String, Object> buildCardSpecs(com.fco.platform.card.domain.PlayerCard card) {
        java.util.HashMap<String, Object> specs = new java.util.HashMap<>();
        if (card == null) return specs;

        String playerName = "Unknown";
        int height = 0;
        int weight = 0;
        String preferredFoot = "Không rõ";
        int weakFoot = 5;

        if (card.getPlayer() != null) {
            playerName = card.getPlayer().getPlayerName();
            height = card.getPlayer().getHeight() != null ? card.getPlayer().getHeight() : 0;
            weight = card.getPlayer().getWeight() != null ? card.getPlayer().getWeight() : 0;
            preferredFoot = card.getPlayer().getPreferredFoot() != null ? card.getPlayer().getPreferredFoot() : "Không rõ";
            weakFoot = card.getPlayer().getWeakFoot() != null ? card.getPlayer().getWeakFoot() : 5;
        }

        specs.put("playerName", playerName);
        specs.put("season", card.getSeason() != null ? card.getSeason().getSeasonName() : "Unknown");
        specs.put("ovr", card.getOvr() != null ? card.getOvr() : 0);
        specs.put("salary", card.getSalary() != null ? card.getSalary() : 0);
        specs.put("position", card.getPreferredPosition() != null ? card.getPreferredPosition() : "Không rõ");
        specs.put("height", height);
        specs.put("weight", weight);
        specs.put("preferredFoot", preferredFoot);
        specs.put("weakFoot", weakFoot);

        // Hexagon stats
        specs.put("pace", card.getPace() != null ? card.getPace() : 0);
        specs.put("shooting", card.getShooting() != null ? card.getShooting() : 0);
        specs.put("passing", card.getPassing() != null ? card.getPassing() : 0);
        specs.put("dribbling", card.getDribbling() != null ? card.getDribbling() : 0);
        specs.put("defending", card.getDefending() != null ? card.getDefending() : 0);
        specs.put("physicality", card.getPhysicality() != null ? card.getPhysicality() : 0);

        return specs;
    }

    private void processBatch(Long cardId, Map<String, Object> cardSpecs, List<FcoCardReview> batch) {
        // Chuẩn bị dữ liệu gửi cho Gemini
        List<Map<String, Object>> reviewData = new ArrayList<>();
        for (FcoCardReview r : batch) {
            reviewData.add(Map.of(
                    "reviewId", r.getId(),
                    "content",  r.getContent()
            ));
        }

        GeminiService.BatchResult result = geminiService.processBatch(cardId, cardSpecs, reviewData);
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
