package com.fco.platform.common.application;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.HashSet;
import java.util.Set;

/**
 * Bộ lọc từ ngữ thô cục bộ — chạy tức thì (0ms), không cần gọi API ngoài.
 * Tải danh sách từ cấm từ file resources/data/bad-words.txt khi khởi động.
 */
@Slf4j
@Service
public class BadWordFilterService {

    private static final String BAD_WORDS_FILE = "data/bad-words.txt";

    private final Set<String> badWords = new HashSet<>();

    @PostConstruct
    public void init() {
        try {
            ClassPathResource resource = new ClassPathResource(BAD_WORDS_FILE);
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(resource.getInputStream(), StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    line = line.trim().toLowerCase();
                    // Bỏ qua dòng trống và comment (#)
                    if (!line.isEmpty() && !line.startsWith("#")) {
                        badWords.add(line);
                    }
                }
            }
            log.info("BadWordFilterService: Đã nạp {} từ bị cấm.", badWords.size());
        } catch (Exception e) {
            log.warn("BadWordFilterService: Không thể đọc file bad-words.txt — {}", e.getMessage());
        }
    }

    /**
     * Kiểm tra xem nội dung có chứa từ ngữ bị cấm không.
     *
     * @param content nội dung cần kiểm tra
     * @return true nếu phát hiện từ cấm
     */
    public boolean containsBadWord(String content) {
        if (content == null || content.isBlank()) return false;
        String lower = content.toLowerCase();
        return badWords.stream().anyMatch(lower::contains);
    }
}
