-- ==========================================
-- V7: Bình chọn NGON/PHẾ trực tiếp trên Thẻ Cầu thủ
-- ==========================================

CREATE TABLE fco_card_votes (
    card_id    BIGINT      NOT NULL COMMENT 'Khóa ngoại trỏ vào thẻ cầu thủ',
    user_id    BIGINT      NOT NULL COMMENT 'Khóa ngoại trỏ vào tài khoản người dùng',
    vote_type  VARCHAR(10) NOT NULL COMMENT 'NGON | PHE',
    created_at DATETIME    NOT NULL DEFAULT CURRENT_TIMESTAMP,

    PRIMARY KEY (card_id, user_id),
    CONSTRAINT fk_card_vote_card FOREIGN KEY (card_id) REFERENCES fco_player_cards (id) ON DELETE CASCADE,
    CONSTRAINT fk_card_vote_user FOREIGN KEY (user_id) REFERENCES users (id)
);

-- Hỗ trợ truy vấn đếm nhanh số NGON/PHE theo card
CREATE INDEX idx_fco_card_votes_card ON fco_card_votes (card_id, vote_type);
