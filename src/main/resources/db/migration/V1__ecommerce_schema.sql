CREATE DATABASE IF NOT EXISTS ecommer_fco CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE ecommer_fco;

SET NAMES utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS support_tickets;
DROP TABLE IF EXISTS support_ticket_histories;
DROP TABLE IF EXISTS account_deliveries;
DROP TABLE IF EXISTS payments;
DROP TABLE IF EXISTS order_items;
DROP TABLE IF EXISTS order_histories;
DROP TABLE IF EXISTS orders;
DROP TABLE IF EXISTS account_images;
DROP TABLE IF EXISTS account_attributes;
DROP TABLE IF EXISTS game_accounts;
DROP TABLE IF EXISTS game_titles;
DROP TABLE IF EXISTS fco_review_votes;
DROP TABLE IF EXISTS fco_card_reviews;
DROP TABLE IF EXISTS fco_user_squad_players;
DROP TABLE IF EXISTS fco_user_squads;
DROP TABLE IF EXISTS fco_formation_fits;
DROP TABLE IF EXISTS fco_card_tags;
DROP TABLE IF EXISTS fco_tags;
DROP TABLE IF EXISTS fco_player_team_colors;
DROP TABLE IF EXISTS fco_card_prices;
DROP TABLE IF EXISTS fco_player_card_traits;
DROP TABLE IF EXISTS fco_traits;
DROP TABLE IF EXISTS fco_player_cards;
DROP TABLE IF EXISTS fco_seasons;
DROP TABLE IF EXISTS fco_players;
DROP TABLE IF EXISTS fco_clubs;
DROP TABLE IF EXISTS fco_leagues;
DROP TABLE IF EXISTS fco_nations;
DROP TABLE IF EXISTS payment_gateways;
DROP TABLE IF EXISTS user_role;
DROP TABLE IF EXISTS users;
DROP TABLE IF EXISTS roles;
DROP TABLE IF EXISTS fco_sync_errors;

SET FOREIGN_KEY_CHECKS = 1;

CREATE TABLE roles (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       role_name VARCHAR(30) NOT NULL UNIQUE
);

CREATE TABLE users (
                       id BIGINT AUTO_INCREMENT PRIMARY KEY,
                       full_name VARCHAR(120) NOT NULL,
                       username VARCHAR(100) NOT NULL UNIQUE,
                       email VARCHAR(150) NOT NULL UNIQUE,
                       phone VARCHAR(20) NULL,
                       password_hash VARCHAR(255) NOT NULL,
                       balance DECIMAL(15,2) NOT NULL DEFAULT 0,
                       status TINYINT(1) NOT NULL DEFAULT 1,
                       created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE user_role (
                           user_id BIGINT NOT NULL,
                           role_id BIGINT NOT NULL,
                           PRIMARY KEY (user_id, role_id),
                           CONSTRAINT fk_user_role_user FOREIGN KEY (user_id) REFERENCES users(id),
                           CONSTRAINT fk_user_role_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE game_titles (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             name VARCHAR(150) NOT NULL UNIQUE,
                             slug VARCHAR(180) NOT NULL UNIQUE,
                             publisher VARCHAR(150) NULL,
                             icon_url VARCHAR(500) NULL,
                             status VARCHAR(30) NOT NULL DEFAULT 'ACTIVE',
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);


CREATE TABLE game_accounts (
                               id BIGINT AUTO_INCREMENT PRIMARY KEY,
                               game_title_id BIGINT NOT NULL,
                               account_code VARCHAR(40) NOT NULL UNIQUE,
                               title VARCHAR(255) NOT NULL,
                               description LONGTEXT NULL,
                               account_level INT NULL,
                               rank_name VARCHAR(120) NULL,
                               hero_count INT NULL,
                               skin_count INT NULL,
                               price DECIMAL(15,2) NOT NULL,
                               login_method VARCHAR(50) NULL,
                               source_note VARCHAR(255) NULL,
                               account_status VARCHAR(30) NOT NULL DEFAULT 'DRAFT',
                               reserved_at DATETIME NULL,
                               reserve_expires_at DATETIME NULL,
                               is_featured TINYINT(1) NOT NULL DEFAULT 0,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                               published_at DATETIME NULL,
                               sold_at DATETIME NULL,
                               CONSTRAINT fk_game_accounts_title FOREIGN KEY (game_title_id) REFERENCES game_titles(id)
);

CREATE TABLE account_attributes (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    game_account_id BIGINT NOT NULL,
                                    attr_key VARCHAR(100) NOT NULL,
                                    attr_value VARCHAR(255) NOT NULL,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_account_attributes_account FOREIGN KEY (game_account_id) REFERENCES game_accounts(id)
);

CREATE TABLE account_images (
                                id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                game_account_id BIGINT NOT NULL,
                                image_url VARCHAR(500) NOT NULL,
                                is_primary TINYINT(1) NOT NULL DEFAULT 0,
                                sort_order INT NOT NULL DEFAULT 0,
                                created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT fk_account_images_account FOREIGN KEY (game_account_id) REFERENCES game_accounts(id)
);

CREATE TABLE orders (
                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                        buyer_id BIGINT NOT NULL,
                        order_code VARCHAR(40) NOT NULL UNIQUE,
                        subtotal DECIMAL(15,2) NOT NULL,
                        discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
                        total_amount DECIMAL(15,2) NOT NULL,
                        payment_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        order_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                        note VARCHAR(500) NULL,
                        placed_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                        updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                        CONSTRAINT fk_orders_buyer FOREIGN KEY (buyer_id) REFERENCES users(id)
);

CREATE TABLE order_items (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             order_id BIGINT NOT NULL,
                             game_account_id BIGINT NOT NULL,
                             item_title VARCHAR(255) NOT NULL,
                             unit_price DECIMAL(15,2) NOT NULL,
                             discount_amount DECIMAL(15,2) NOT NULL DEFAULT 0,
                             final_price DECIMAL(15,2) NOT NULL,
                             item_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
                             CONSTRAINT fk_order_items_account FOREIGN KEY (game_account_id) REFERENCES game_accounts(id)
);

CREATE TABLE order_histories (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 order_id BIGINT NOT NULL,
                                 changed_by_user_id BIGINT NULL,
                                 from_status VARCHAR(30) NULL,
                                 to_status VARCHAR(30) NOT NULL,
                                 reason VARCHAR(255) NULL,
                                 note TEXT NULL,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_order_histories_order FOREIGN KEY (order_id) REFERENCES orders(id),
                                 CONSTRAINT fk_order_histories_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);

CREATE TABLE payment_gateways (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  code VARCHAR(30) NOT NULL UNIQUE,
                                  name VARCHAR(100) NOT NULL,
                                  is_active TINYINT(1) NOT NULL DEFAULT 1,
                                  config_json JSON NULL,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE payments (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          order_id BIGINT NOT NULL,
                          gateway_id BIGINT NOT NULL,
                          method VARCHAR(30) NOT NULL,
                          provider VARCHAR(50) NULL,
                          transaction_code VARCHAR(150) NULL,
                          qr_content VARCHAR(500) NULL,
                          checkout_url VARCHAR(500) NULL,
                          amount DECIMAL(15,2) NOT NULL,
                          status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                          requested_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          paid_at DATETIME NULL,
                          raw_response JSON NULL,
                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                          CONSTRAINT fk_payments_order FOREIGN KEY (order_id) REFERENCES orders(id),
                          CONSTRAINT fk_payments_gateway FOREIGN KEY (gateway_id) REFERENCES payment_gateways(id)
);

CREATE TABLE account_deliveries (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    order_item_id BIGINT NOT NULL UNIQUE,
                                    delivery_method VARCHAR(30) NOT NULL DEFAULT 'AUTO',
                                    account_login VARCHAR(150) NULL,
                                    account_password VARCHAR(255) NULL,
                                    email_recovery VARCHAR(150) NULL,
                                    email_password VARCHAR(255) NULL,
                                    two_factor_backup_code VARCHAR(255) NULL,
                                    delivered_at DATETIME NULL,
                                    confirmed_at DATETIME NULL,
                                    auto_delivery_attempts INT NOT NULL DEFAULT 0,
                                    last_attempt_at DATETIME NULL,
                                    error_message VARCHAR(500) NULL,
                                    delivery_status VARCHAR(30) NOT NULL DEFAULT 'PENDING',
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                    CONSTRAINT fk_account_deliveries_item FOREIGN KEY (order_item_id) REFERENCES order_items(id)
);

CREATE TABLE support_tickets (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 order_item_id BIGINT NOT NULL,
                                 user_id BIGINT NOT NULL,
                                 subject VARCHAR(255) NOT NULL,
                                 detail TEXT NULL,
                                 evidence_urls JSON NULL,
                                 ticket_status VARCHAR(30) NOT NULL DEFAULT 'OPEN',
                                 admin_note TEXT NULL,
                                 resolved_at DATETIME NULL,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_support_tickets_item FOREIGN KEY (order_item_id) REFERENCES order_items(id),
                                 CONSTRAINT fk_support_tickets_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE support_ticket_histories (
                                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                          support_ticket_id BIGINT NOT NULL,
                                          changed_by_user_id BIGINT NULL,
                                          from_status VARCHAR(30) NULL,
                                          to_status VARCHAR(30) NOT NULL,
                                          note TEXT NULL,
                                          created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                          CONSTRAINT fk_ticket_histories_ticket FOREIGN KEY (support_ticket_id) REFERENCES support_tickets(id),
                                          CONSTRAINT fk_ticket_histories_user FOREIGN KEY (changed_by_user_id) REFERENCES users(id)
);

-- ==========================================
-- Module A: Player Intelligence
-- ==========================================

CREATE TABLE fco_nations (
                             id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                             nation_name VARCHAR(100) NOT NULL UNIQUE
                                 COMMENT 'Tên quốc gia (VD: Georgia, Brazil)',
                             nation_slug VARCHAR(120) NULL
                                 COMMENT 'Slug URL từ FIFAAddict (VD: georgia)',
                             flag_url    VARCHAR(500) NULL
                                 COMMENT 'URL ảnh cờ quốc gia',
                             created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE fco_leagues (
                             id           BIGINT AUTO_INCREMENT PRIMARY KEY,
                             league_name  VARCHAR(150) NOT NULL UNIQUE
                                 COMMENT 'Tên giải đấu (VD: France Ligue 1)',
                             league_slug  VARCHAR(180) NULL
                                 COMMENT 'Slug URL từ FIFAAddict (VD: france-ligue-1)',
                             logo_url     VARCHAR(500) NULL
                                 COMMENT 'URL logo giải đấu',
                             created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE fco_clubs (
                           id              BIGINT AUTO_INCREMENT PRIMARY KEY,
                           club_name       VARCHAR(150) NOT NULL
                               COMMENT 'Tên CLB (VD: Paris Saint-Germain)',
                           club_slug       VARCHAR(180) NULL
                               COMMENT 'Slug URL từ FIFAAddict (VD: paris-saint-germain)',
                           fifaaddict_id   INT NULL
                               COMMENT 'ID CLB từ FIFAAddict — dùng để build URL crest',
                           crest_url       VARCHAR(500) NULL
                               COMMENT 'URL logo CLB (VD: https://s1.fifaaddict.com/fo4/crests/dark/d73.png)',
                           league_id       BIGINT NULL
                               COMMENT 'FK → fco_leagues.id',
                           created_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at      DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                           CONSTRAINT fk_clubs_league FOREIGN KEY (league_id) REFERENCES fco_leagues(id) ON DELETE SET NULL
);

CREATE TABLE fco_players (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             player_name VARCHAR(150) NOT NULL,
                             external_id VARCHAR(100) NOT NULL UNIQUE,
                             height TINYINT UNSIGNED NULL COMMENT 'Chiều cao (cm)',
                             weight TINYINT UNSIGNED NULL COMMENT 'Cân nặng (kg)',
                             birthdate VARCHAR(20) NULL COMMENT 'Ngày sinh dạng dd.mm.yyyy',
                             preferred_foot VARCHAR(10) NULL COMMENT 'Chân thuận: right / left / both',
                             weak_foot TINYINT NULL COMMENT 'Điểm chân không thuận (1-5)',
                             nation_id BIGINT NULL COMMENT 'FK → fco_nations.id',
                             club_id BIGINT NULL COMMENT 'FK → fco_clubs.id',
                             league_id BIGINT NULL COMMENT 'FK → fco_leagues.id (giải đấu hiện tại của cầu thủ)',
                             nation_name VARCHAR(100) NULL COMMENT 'Cache tên QG (đồng bộ từ fco_nations)',
                             club_name VARCHAR(150) NULL COMMENT 'Cache tên CLB (đồng bộ từ fco_clubs)',
                             league_name VARCHAR(150) NULL COMMENT 'Cache tên giải đấu (đồng bộ từ fco_leagues)',
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                             CONSTRAINT fk_players_nation FOREIGN KEY (nation_id) REFERENCES fco_nations(id) ON DELETE SET NULL,
                             CONSTRAINT fk_players_club FOREIGN KEY (club_id) REFERENCES fco_clubs(id) ON DELETE SET NULL,
                             CONSTRAINT fk_players_league FOREIGN KEY (league_id) REFERENCES fco_leagues(id) ON DELETE SET NULL
);

CREATE TABLE fco_seasons (
                             id BIGINT AUTO_INCREMENT PRIMARY KEY,
                             season_code VARCHAR(20) NOT NULL UNIQUE,
                             season_name VARCHAR(100) NOT NULL,
                             is_core TINYINT(1) NOT NULL DEFAULT 1,
                             is_active TINYINT(1) NOT NULL DEFAULT 1,
                             created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE fco_player_cards (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  player_id BIGINT NOT NULL,
                                  season_id BIGINT NOT NULL,
                                  enhance_level INT NOT NULL DEFAULT 1,
                                  ovr INT NOT NULL,
                                  liveperf TINYINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Số lần cập nhật LivePerf (0 = không có)',
                                  ovr_by_pos_json JSON NULL COMMENT 'OVR theo từng vị trí dạng JSON',
                                  image_url VARCHAR(500) NULL,
                                  salary INT NOT NULL DEFAULT 0,
                                  preferred_position VARCHAR(10) NULL,
                                  secondary_position VARCHAR(10) NULL COMMENT 'Vị trí phụ (VD: RW)',
                                  skill_level TINYINT UNSIGNED NULL COMMENT 'Số sao kỹ thuật (1-5)',
                                  workrate_att VARCHAR(10) NULL COMMENT 'Công suất tấn công: low / mid / high',
                                  workrate_def VARCHAR(10) NULL COMMENT 'Công suất phòng thủ: low / mid / high',
                                  bodytype VARCHAR(20) NULL COMMENT 'Thể hình: TB / Lean / Stocky...',
                                  reputation VARCHAR(50) NULL COMMENT 'Danh tiếng: Top Class / World Class...',
                                  pace INT NULL,
                                  shooting INT NULL,
                                  passing INT NULL,
                                  dribbling INT NULL,
                                  defending INT NULL,
                                  physicality INT NULL,
                                  market_price_bp BIGINT NULL,
                                  price_updated_at DATE NULL COMMENT 'Ngày cập nhật giá gần nhất từ FIFAAddict',
                                  is_active TINYINT(1) NOT NULL DEFAULT 1,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  UNIQUE KEY uq_player_card_level (player_id, season_id, enhance_level),
                                  CONSTRAINT fk_fco_cards_player FOREIGN KEY (player_id) REFERENCES fco_players(id),
                                  CONSTRAINT fk_fco_cards_season FOREIGN KEY (season_id) REFERENCES fco_seasons(id)
);

CREATE TABLE fco_traits (
                            id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                            trait_code  VARCHAR(60)  NOT NULL UNIQUE
                                COMMENT 'Mã trait từ FIFAAddict (VD: speedster, finesse-shot)',
                            trait_name  VARCHAR(120) NOT NULL
                                COMMENT 'Tên hiển thị (VD: Sát thủ băng cắt)',
                            description VARCHAR(500) NULL
                                COMMENT 'Mô tả chi tiết',
                            icon_id     VARCHAR(10)  NULL
                                COMMENT 'ID icon từ FIFAAddict',
                            icon_url    VARCHAR(500) NULL
                                COMMENT 'URL icon đầy đủ',
                            created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP
);

CREATE TABLE fco_player_card_traits (
                                        card_id     BIGINT NOT NULL,
                                        trait_id    BIGINT NOT NULL,
                                        PRIMARY KEY (card_id, trait_id),
                                        CONSTRAINT fk_card_traits_card  FOREIGN KEY (card_id)  REFERENCES fco_player_cards(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_card_traits_trait FOREIGN KEY (trait_id) REFERENCES fco_traits(id)
);

CREATE TABLE fco_card_prices (
                                 id          BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 card_id     BIGINT           NOT NULL
                                     COMMENT 'FK → fco_player_cards.id',
                                 grade       TINYINT UNSIGNED NOT NULL
                                     COMMENT 'Mức nâng cấp: 1-13 (tương đương +1 đến +13)',
                                 price_bp    BIGINT           NOT NULL
                                     COMMENT 'Giá tính theo đơn vị BP',
                                 price_raw   VARCHAR(30)      NULL
                                     COMMENT 'Giá dạng text gốc từ FIFAAddict (VD: 33,800B)',
                                 price_date  DATE             NOT NULL
                                     COMMENT 'Ngày cập nhật giá (từ FIFAAddict updatetime)',
                                 created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,

                                 UNIQUE KEY uq_card_grade_date (card_id, grade, price_date),
                                 CONSTRAINT fk_card_prices_card
                                     FOREIGN KEY (card_id) REFERENCES fco_player_cards(id) ON DELETE CASCADE
);

CREATE TABLE fco_player_team_colors (
                                        player_id   BIGINT NOT NULL
                                            COMMENT 'FK → fco_players.id',
                                        club_id     BIGINT NOT NULL
                                            COMMENT 'FK → fco_clubs.id',
                                        sort_order  TINYINT UNSIGNED NOT NULL DEFAULT 0
                                            COMMENT 'Thứ tự hiển thị (0 = CLB hiện tại/gần nhất)',
                                        PRIMARY KEY (player_id, club_id),
                                        CONSTRAINT fk_teamcolor_player FOREIGN KEY (player_id) REFERENCES fco_players(id) ON DELETE CASCADE,
                                        CONSTRAINT fk_teamcolor_club   FOREIGN KEY (club_id)   REFERENCES fco_clubs(id)   ON DELETE CASCADE
);

CREATE TABLE fco_tags (
                          id BIGINT AUTO_INCREMENT PRIMARY KEY,
                          tag_code VARCHAR(50) NOT NULL UNIQUE,
                          tag_name VARCHAR(100) NOT NULL,
                          description VARCHAR(255) NULL
);

CREATE TABLE fco_card_tags (
                               card_id BIGINT NOT NULL,
                               tag_id BIGINT NOT NULL,
                               score DECIMAL(5,2) NOT NULL DEFAULT 1.00,
                               created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               PRIMARY KEY (card_id, tag_id),
                               CONSTRAINT fk_fco_card_tags_card FOREIGN KEY (card_id) REFERENCES fco_player_cards(id),
                               CONSTRAINT fk_fco_card_tags_tag FOREIGN KEY (tag_id) REFERENCES fco_tags(id)
);

CREATE TABLE fco_formation_fits (
                                    id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                    card_id BIGINT NOT NULL,
                                    formation_code VARCHAR(20) NOT NULL,
                                    fit_score DECIMAL(5,2) NOT NULL DEFAULT 0.00,
                                    note VARCHAR(255) NULL,
                                    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                    UNIQUE KEY uq_card_formation (card_id, formation_code),
                                    CONSTRAINT fk_fco_formation_fit_card FOREIGN KEY (card_id) REFERENCES fco_player_cards(id)
);

-- ==========================================
-- Module B: Squad Building & Team Optimization
-- ==========================================
CREATE TABLE fco_user_squads (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 user_id BIGINT NOT NULL,
                                 squad_name VARCHAR(120) NOT NULL,
                                 formation_code VARCHAR(20) NOT NULL,
                                 budget_bp BIGINT NOT NULL DEFAULT 0,
                                 total_salary INT NOT NULL DEFAULT 0,
                                 chemistry_score DECIMAL(6,2) NOT NULL DEFAULT 0,
                                 is_public TINYINT(1) NOT NULL DEFAULT 0,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                 CONSTRAINT fk_fco_squads_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE fco_user_squad_players (
                                        id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                        squad_id BIGINT NOT NULL,
                                        card_id BIGINT NOT NULL,
                                        slot_no INT NOT NULL,
                                        role_code VARCHAR(20) NULL,
                                        created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                        UNIQUE KEY uq_squad_slot (squad_id, slot_no),
                                        CONSTRAINT fk_fco_squad_players_squad FOREIGN KEY (squad_id) REFERENCES fco_user_squads(id),
                                        CONSTRAINT fk_fco_squad_players_card FOREIGN KEY (card_id) REFERENCES fco_player_cards(id)
);

-- ==========================================
-- Module C: Community Insight
-- ==========================================
CREATE TABLE fco_card_reviews (
                                  id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                  card_id BIGINT NOT NULL,
                                  user_id BIGINT NOT NULL,
                                  content VARCHAR(280) NOT NULL,
                                  ingame_rank VARCHAR(50) NULL,
                                  trust_weight DECIMAL(5,2) NOT NULL DEFAULT 1.00,
                                  status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
                                  CONSTRAINT fk_fco_reviews_card FOREIGN KEY (card_id) REFERENCES fco_player_cards(id),
                                  CONSTRAINT fk_fco_reviews_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE fco_review_votes (
                                  review_id BIGINT NOT NULL,
                                  user_id BIGINT NOT NULL,
                                  vote_type ENUM('NGON', 'PHE') NOT NULL,
                                  created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  PRIMARY KEY (review_id, user_id),
                                  CONSTRAINT fk_fco_review_votes_review FOREIGN KEY (review_id) REFERENCES fco_card_reviews(id),
                                  CONSTRAINT fk_fco_review_votes_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE fco_sync_errors (
                                 id BIGINT AUTO_INCREMENT PRIMARY KEY,
                                 external_id VARCHAR(100) NULL,
                                 payload LONGTEXT NOT NULL,
                                 error_message TEXT NOT NULL,
                                 created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_game_accounts_title ON game_accounts(game_title_id);
CREATE INDEX idx_game_accounts_status ON game_accounts(account_status);
CREATE INDEX idx_game_accounts_reserve_expires ON game_accounts(reserve_expires_at);
CREATE INDEX idx_orders_buyer ON orders(buyer_id);
CREATE INDEX idx_orders_status ON orders(order_status);
CREATE INDEX idx_order_items_account ON order_items(game_account_id);
CREATE INDEX idx_order_histories_order ON order_histories(order_id);
CREATE INDEX idx_payments_status ON payments(status);
CREATE INDEX idx_payments_gateway ON payments(gateway_id);
CREATE INDEX idx_tickets_status ON support_tickets(ticket_status);
CREATE INDEX idx_ticket_histories_ticket ON support_ticket_histories(support_ticket_id);
CREATE INDEX idx_fco_cards_player ON fco_player_cards(player_id);
CREATE INDEX idx_fco_cards_season ON fco_player_cards(season_id);
CREATE INDEX idx_fco_cards_ovr ON fco_player_cards(ovr);
CREATE INDEX idx_cards_level_price ON fco_player_cards(enhance_level, market_price_bp DESC);
CREATE INDEX idx_fco_formation_code ON fco_formation_fits(formation_code);
CREATE INDEX idx_fco_squad_user ON fco_user_squads(user_id);
CREATE INDEX idx_fco_reviews_card ON fco_card_reviews(card_id);
CREATE INDEX idx_fco_reviews_user ON fco_card_reviews(user_id);
CREATE INDEX idx_sync_errors_created_at ON fco_sync_errors(created_at);
CREATE INDEX idx_sync_errors_external_id ON fco_sync_errors(external_id);

CREATE UNIQUE INDEX uq_club_name_league ON fco_clubs(club_name, league_id);
CREATE INDEX idx_clubs_league         ON fco_clubs(league_id);
CREATE INDEX idx_clubs_fifaaddict_id  ON fco_clubs(fifaaddict_id);
CREATE INDEX idx_nations_slug         ON fco_nations(nation_slug);
CREATE INDEX idx_leagues_slug         ON fco_leagues(league_slug);

CREATE INDEX idx_players_nation ON fco_players(nation_id);
CREATE INDEX idx_players_club   ON fco_players(club_id);
CREATE INDEX idx_players_league ON fco_players(league_id);

CREATE INDEX idx_fco_cards_liveperf ON fco_player_cards(liveperf);

CREATE INDEX idx_card_traits_trait ON fco_player_card_traits(trait_id);

CREATE INDEX idx_card_prices_card_grade ON fco_card_prices(card_id, grade);
CREATE INDEX idx_card_prices_date       ON fco_card_prices(price_date);

CREATE INDEX idx_teamcolor_club ON fco_player_team_colors(club_id);


-- Security note:
-- account_password, email_password, two_factor_backup_code must be encrypted by backend (AES-256)
-- before storing into DB. Do not store plaintext secrets in production.

INSERT INTO roles (role_name) VALUES ('ROLE_ADMIN'), ('ROLE_STAFF'), ('ROLE_CUSTOMER');

INSERT INTO payment_gateways (code, name, is_active) VALUES
                                                         ('MOMO', 'MoMo E-Wallet', 1),
                                                         ('VNPAY', 'VNPay Gateway', 1),
                                                         ('BANK_QR', 'Bank Transfer QR', 1),
                                                         ('CARD', 'Prepaid Card', 0);
# DROP DATABASE ecommer_fco;
# CREATE DATABASE ecommer_fco CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
