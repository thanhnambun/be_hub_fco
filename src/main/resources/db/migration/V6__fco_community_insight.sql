-- ==========================================
-- V6: Community Insight — Review, Vote & AI
-- ==========================================

-- 1. Mở rộng bảng fco_card_reviews đã có từ V1
--    Thêm cột is_ai_checked để theo dõi trạng thái kiểm duyệt AI
ALTER TABLE fco_card_reviews
    ADD COLUMN is_ai_checked TINYINT(1) NOT NULL DEFAULT 0 COMMENT '0 = chờ AI kiểm duyệt, 1 = đã kiểm duyệt';

-- Ràng buộc: mỗi user chỉ được viết 1 đánh giá cho 1 thẻ cầu thủ
ALTER TABLE fco_card_reviews
    ADD CONSTRAINT uq_review_card_user UNIQUE (card_id, user_id);

-- Index hỗ trợ tác vụ Scheduled Job quét các review chưa AI kiểm duyệt
CREATE INDEX idx_fco_reviews_ai_pending ON fco_card_reviews (is_ai_checked, status);

-- ==========================================
-- 2. Bảng phản hồi 1 cấp trên đánh giá
-- ==========================================
CREATE TABLE fco_card_review_replies (
    id         BIGINT AUTO_INCREMENT PRIMARY KEY,
    review_id  BIGINT NOT NULL,
    user_id    BIGINT NOT NULL,
    content    VARCHAR(280) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_fco_reply_review FOREIGN KEY (review_id) REFERENCES fco_card_reviews (id) ON DELETE CASCADE,
    CONSTRAINT fk_fco_reply_user   FOREIGN KEY (user_id)   REFERENCES users (id)
);

CREATE INDEX idx_fco_replies_review ON fco_card_review_replies (review_id);

-- ==========================================
-- 3. Bảng kết quả tổng hợp AI theo thẻ cầu thủ
-- ==========================================
CREATE TABLE fco_card_ai_summaries (
    card_id        BIGINT PRIMARY KEY,
    positive_tags  TEXT NOT NULL DEFAULT '[]' COMMENT 'JSON array chuỗi: ["Tốc độ", "Dứt điểm sắc"]',
    negative_tags  TEXT NOT NULL DEFAULT '[]' COMMENT 'JSON array chuỗi: ["Yếu tranh chấp"]',
    summary        TEXT NOT NULL               COMMENT 'Đoạn tóm tắt lối chơi ngắn gọn từ AI',
    updated_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_fco_ai_summary_card FOREIGN KEY (card_id) REFERENCES fco_player_cards (id) ON DELETE CASCADE
);
