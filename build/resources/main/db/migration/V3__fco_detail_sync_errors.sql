CREATE TABLE fco_detail_sync_errors (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    external_id VARCHAR(100) NULL,
    payload LONGTEXT NOT NULL,
    error_message TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_detail_sync_errors_created_at ON fco_detail_sync_errors(created_at);
CREATE INDEX idx_detail_sync_errors_external_id ON fco_detail_sync_errors(external_id);
