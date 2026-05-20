package com.fco.platform.common.application;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;

/**
 * Service giao tiếp với Google Gemini API.
 * Xử lý 2 tác vụ:
 *   1. Kiểm duyệt mẻ (Batch Moderation)
 *   2. Tổng hợp lối chơi theo thẻ (Gameplay Summarization)
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class GeminiService {

    private static final String GEMINI_API_URL =
            "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.0-flash:generateContent";

    @Value("${app.gemini.api-key:}")
    private String apiKey;

    private final WebClient.Builder webClientBuilder;
    private final ObjectMapper objectMapper;

    /**
     * Kết quả kiểm duyệt cho 1 review.
     */
    public record ModerationResult(Long reviewId, boolean isViolating, String reason) {}

    /**
     * Kết quả tổng hợp lối chơi cho 1 thẻ cầu thủ.
     */
    public record SummaryResult(Long cardId, List<String> positiveTags, List<String> negativeTags, String summary) {}

    /**
     * Gửi mẻ review để kiểm duyệt VÀ tổng hợp lối chơi trong 1 lần gọi API.
     * Prompt được thiết kế để Gemini trả về JSON chuẩn.
     */
    public BatchResult processBatch(Long cardId, String playerName, List<Map<String, Object>> reviews) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GeminiService: GEMINI_API_KEY chưa được cấu hình — bỏ qua batch cardId={}", cardId);
            return null;
        }

        String prompt = buildBatchPrompt(playerName, reviews);
        try {
            String rawResponse = callGeminiApi(prompt);
            return parseBatchResponse(cardId, rawResponse);
        } catch (Exception e) {
            log.error("GeminiService: Lỗi khi xử lý batch cardId={}: {}", cardId, e.getMessage());
            return null;
        }
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private String buildBatchPrompt(String playerName, List<Map<String, Object>> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là AI kiểm duyệt nội dung cho nền tảng game bóng đá FC Online tại Việt Nam.\n");
        sb.append("Nhiệm vụ: Phân tích các bình luận dưới đây về cầu thủ '").append(playerName).append("'.\n\n");
        sb.append("Hãy thực hiện 2 việc:\n");
        sb.append("1. Với mỗi bình luận: xác định có vi phạm ngôn từ không (tục tĩu, spam, toxic) — ");
        sb.append("kể cả từ lóng, từ viết tắt hoặc mỉa mai tinh vi.\n");
        sb.append("2. Từ toàn bộ bình luận hợp lệ: tổng hợp lối chơi (positiveTags, negativeTags, summary).\n\n");
        sb.append("Danh sách bình luận (JSON):\n");
        try {
            sb.append(objectMapper.writeValueAsString(reviews));
        } catch (Exception e) {
            sb.append("[]");
        }
        sb.append("\n\nTRẢ VỀ DUY NHẤT JSON (không có markdown, không có text ngoài JSON):\n");
        sb.append("{\n");
        sb.append("  \"moderation\": [{\"reviewId\": <id>, \"isViolating\": <bool>, \"reason\": \"<lý do nếu vi phạm>\"}],\n");
        sb.append("  \"summary\": {\n");
        sb.append("    \"positiveTags\": [\"<tag1>\", \"<tag2>\"],\n");
        sb.append("    \"negativeTags\": [\"<tag1>\"],\n");
        sb.append("    \"summary\": \"<đoạn tóm tắt ngắn không quá 2 câu>\"\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String callGeminiApi(String prompt) {
        WebClient client = webClientBuilder.build();
        String requestBody = """
                {
                  "contents": [{"parts": [{"text": "%s"}]}]
                }
                """.formatted(prompt.replace("\"", "\\\"").replace("\n", "\\n"));

        return client.post()
                .uri(GEMINI_API_URL + "?key=" + apiKey)
                .header("Content-Type", "application/json")
                .bodyValue(requestBody)
                .retrieve()
                .bodyToMono(String.class)
                .block();
    }

    private BatchResult parseBatchResponse(Long cardId, String rawResponse) throws Exception {
        // Trích xuất nội dung text từ phản hồi Gemini
        JsonNode root = objectMapper.readTree(rawResponse);
        String text = root.at("/candidates/0/content/parts/0/text").asText("");

        // Bóc tách JSON: tìm cặp {} đầu tiên và cuối cùng để bỏ markdown wrapper nếu có
        int start = text.indexOf('{');
        int end   = text.lastIndexOf('}');
        if (start == -1 || end == -1 || start >= end) {
            log.warn("GeminiService: Không tìm thấy JSON hợp lệ trong response, cardId={}", cardId);
            return null;
        }
        String jsonText = text.substring(start, end + 1);
        JsonNode parsed = objectMapper.readTree(jsonText);

        // -- Moderation results --
        List<ModerationResult> moderations = new java.util.ArrayList<>();
        JsonNode modArr = parsed.get("moderation");
        if (modArr != null && modArr.isArray()) {
            for (JsonNode item : modArr) {
                moderations.add(new ModerationResult(
                        item.get("reviewId").asLong(),
                        item.get("isViolating").asBoolean(),
                        item.has("reason") ? item.get("reason").asText("") : ""
                ));
            }
        }

        // -- Summary result --
        SummaryResult summaryResult = null;
        JsonNode summaryNode = parsed.get("summary");
        if (summaryNode != null) {
            List<String> positiveTags = new java.util.ArrayList<>();
            List<String> negativeTags = new java.util.ArrayList<>();
            JsonNode posArr = summaryNode.get("positiveTags");
            JsonNode negArr = summaryNode.get("negativeTags");
            if (posArr != null && posArr.isArray()) posArr.forEach(n -> positiveTags.add(n.asText()));
            if (negArr != null && negArr.isArray()) negArr.forEach(n -> negativeTags.add(n.asText()));
            String summary = summaryNode.has("summary") ? summaryNode.get("summary").asText("") : "";
            summaryResult = new SummaryResult(cardId, positiveTags, negativeTags, summary);
        }

        return new BatchResult(moderations, summaryResult);
    }

    public record BatchResult(List<ModerationResult> moderations, SummaryResult summary) {}
}
