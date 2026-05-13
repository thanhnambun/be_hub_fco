-- MySQL strict mode (1364): INSERT must supply NOT NULL columns without DEFAULT.
-- Some environments had liveperf without DEFAULT; Pha A INSERT must still work if Java omits the column.
ALTER TABLE fco_player_cards
    MODIFY COLUMN liveperf TINYINT UNSIGNED NOT NULL DEFAULT 0
        COMMENT 'Số lần cập nhật LivePerf (0 = không có)';
