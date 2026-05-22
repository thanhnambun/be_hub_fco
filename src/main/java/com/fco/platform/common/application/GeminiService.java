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
    /**
     * Gửi mẻ review để kiểm duyệt VÀ tổng hợp lối chơi trong 1 lần gọi API.
     * Prompt được thiết kế để Gemini trả về JSON chuẩn kết hợp chỉ số cầu thủ và review của user.
     */
    public BatchResult processBatch(Long cardId, Map<String, Object> cardSpecs, List<Map<String, Object>> reviews) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("GeminiService: GEMINI_API_KEY chưa được cấu hình — bỏ qua batch cardId={}", cardId);
            return null;
        }

        String prompt = buildBatchPrompt(cardSpecs, reviews);
        try {
            String rawResponse = callGeminiApi(prompt);
            return parseBatchResponse(cardId, rawResponse);
        } catch (Exception e) {
            log.error("GeminiService: Lỗi khi xử lý batch cardId={}: {}", cardId, e.getMessage());
            return null;
        }
    }

    // ── Private Helpers ────────────────────────────────────────────────────────

    private String buildBatchPrompt(Map<String, Object> cardSpecs, List<Map<String, Object>> reviews) {
        StringBuilder sb = new StringBuilder();
        sb.append("Bạn là AI chuyên gia phân tích dữ liệu và kiểm duyệt nội dung cho nền tảng game bóng đá FC Online tại Việt Nam.\n");
        sb.append("Nhiệm vụ của bạn là phân tích các bình luận thực tế của người chơi kết hợp với thông số kỹ thuật (chỉ số, mùa giải, lương...) của thẻ cầu thủ dưới đây.\n\n");
        
        sb.append("--- THÔNG TIN THẺ CẦU THỦ ---\n");
        sb.append("Tên cầu thủ: ").append(cardSpecs.getOrDefault("playerName", "Không rõ")).append("\n");
        sb.append("Mùa giải (Season): ").append(cardSpecs.getOrDefault("season", "Không rõ")).append("\n");
        sb.append("Chỉ số tổng quát (OVR): ").append(cardSpecs.getOrDefault("ovr", 0)).append("\n");
        sb.append("Mức lương (Salary): ").append(cardSpecs.getOrDefault("salary", 0)).append("\n");
        sb.append("Vị trí sở trường: ").append(cardSpecs.getOrDefault("position", "Không rõ")).append("\n");
        sb.append("Thể hình: Chiều cao ").append(cardSpecs.getOrDefault("height", 0)).append("cm, Nặng ").append(cardSpecs.getOrDefault("weight", 0)).append("kg\n");
        sb.append("Thuận chân (Preferred Foot/Weak Foot): ").append(cardSpecs.getOrDefault("preferredFoot", "Không rõ")).append(" (Chân không thuận: ").append(cardSpecs.getOrDefault("weakFoot", 5)).append("/5)\n");
        sb.append("Chỉ số thành phần chính (Hexagon Stats):\n");
        sb.append("  - Tốc độ (Pace): ").append(cardSpecs.getOrDefault("pace", 0)).append("\n");
        sb.append("  - Dứt điểm (Shooting): ").append(cardSpecs.getOrDefault("shooting", 0)).append("\n");
        sb.append("  - Chuyền bóng (Passing): ").append(cardSpecs.getOrDefault("passing", 0)).append("\n");
        sb.append("  - Rê bóng (Dribbling): ").append(cardSpecs.getOrDefault("dribbling", 0)).append("\n");
        sb.append("  - Phòng ngự (Defending): ").append(cardSpecs.getOrDefault("defending", 0)).append("\n");
        sb.append("  - Thể chất (Physicality): ").append(cardSpecs.getOrDefault("physicality", 0)).append("\n\n");

        sb.append("--- DANH SÁCH BÌNH LUẬN CỦA NGƯỜI CHƠI (JSON) ---\n");
        try {
            sb.append(objectMapper.writeValueAsString(reviews));
        } catch (Exception e) {
            sb.append("[]");
        }
        sb.append("\n\n");

        sb.append("YÊU CẦU PHÂN TÍCH:\n");
        sb.append("1. Kiểm duyệt (moderation): Xác định mỗi bình luận có vi phạm ngôn từ tục tĩu, spam, toxic hay không. Kể cả viết tắt (vd: cc, dkm), từ lóng bậy bạ của Việt Nam.\n");
        sb.append("2. Phân tích lối chơi (summary):\n");
        sb.append("  - Tổng hợp điểm mạnh (positiveTags) và điểm yếu (negativeTags) thực tế khi ingame. Chú ý đối chiếu chỉ số kỹ thuật với trải nghiệm thực tế trong bình luận (Ví dụ: OVR/Tốc độ cao nhưng bị bình luận chê chậm thì tag điểm yếu có thể là 'Chạy ảo' hoặc 'Gia tốc chậm'; hoặc Lương cao nhưng đá dở thì tag điểm yếu là 'Nặng lương').\n");
        sb.append("  - Viết 1 đoạn tóm tắt ngắn (summary) không quá 2-3 câu khuyên dùng cầu thủ này như thế nào (Ví dụ: đá vị trí nào hợp nhất, có nên mua hay không, đáng lương hay không).\n\n");
        
        sb.append("TRẢ VỀ DUY NHẤT JSON (không có markdown ```json, không có text ngoài JSON):\n");
        sb.append("{\n");
        sb.append("  \"moderation\": [{\"reviewId\": <id>, \"isViolating\": <bool>, \"reason\": \"<lý do nếu vi phạm>\"}],\n");
        sb.append("  \"summary\": {\n");
        sb.append("    \"positiveTags\": [\"<tag1>\", \"<tag2>\"],\n");
        sb.append("    \"negativeTags\": [\"<tag1>\"],\n");
        sb.append("    \"summary\": \"<đoạn tóm tắt ngắn khuyên dùng>\"\n");
        sb.append("  }\n");
        sb.append("}");
        return sb.toString();
    }

    private String callGeminiApi(String prompt) {
        WebClient client = webClientBuilder.build();
        
        // Tạo cấu trúc request an toàn bằng Map/List
        Map<String, Object> part = Map.of("text", prompt);
        Map<String, Object> content = Map.of("parts", List.of(part));
        Map<String, Object> payload = Map.of("contents", List.of(content));

        String requestBody;
        try {
            requestBody = objectMapper.writeValueAsString(payload);
        } catch (Exception e) {
            log.error("GeminiService: Lỗi serialize request payload: {}", e.getMessage());
            throw new RuntimeException("Failed to serialize request payload", e);
        }

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
