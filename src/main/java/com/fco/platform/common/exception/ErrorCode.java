package com.fco.platform.common.exception;

import java.net.URI;
import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * Contract-first error registry (B1). Stable {@code code} values are part of the public API for
 * frontend switch/case handling. {@link #getTypeUri()} maps to RFC 7807 {@code type}.
 */
@Getter
public enum ErrorCode {

    // —— Auth ——————————————————————————————————————————————————————————————
    AUTH_LOCKED("AUTH_001", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", HttpStatus.FORBIDDEN, "auth-locked"),
    AUTH_INVALID_CREDENTIALS("AUTH_002", "Tài khoản hoặc mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED, "auth-invalid-credentials"),
    AUTH_ACCESS_DENIED("AUTH_003", "Bạn không có quyền truy cập tài nguyên này.", HttpStatus.FORBIDDEN, "auth-access-denied"),
    AUTH_UNAUTHORIZED("AUTH_004", "Vui lòng đăng nhập để tiếp tục.", HttpStatus.UNAUTHORIZED, "auth-unauthorized"),
    AUTH_TOKEN_BLACKLISTED("AUTH_005", "Phiên đăng nhập đã bị thu hồi. Vui lòng đăng nhập lại.", HttpStatus.UNAUTHORIZED, "auth-token-blacklisted"),
    AUTH_RATE_LIMITED("AUTH_006", "Quá nhiều yêu cầu. Vui lòng thử lại sau.", HttpStatus.TOO_MANY_REQUESTS, "auth-rate-limited"),

    // —— User / identity ———————————————————————————————————————————————————
    USER_VALIDATION_ERROR("USER_001", "Dữ liệu đầu vào không hợp lệ.", HttpStatus.BAD_REQUEST, "user-validation-error"),
    USER_NOT_FOUND("USER_002", "Không tìm thấy người dùng yêu cầu.", HttpStatus.NOT_FOUND, "user-not-found"),

    // —— Domain (Phase 1 placeholders — extend before use) —————————————————————
    PLAYER_SYNC_ERROR("PLAY_001", "Lỗi đồng bộ dữ liệu cầu thủ.", HttpStatus.INTERNAL_SERVER_ERROR, "player-sync-error"),
    CARD_NOT_FOUND("CARD_001", "Không tìm thấy thẻ cầu thủ.", HttpStatus.NOT_FOUND, "card-not-found"),
    MARKET_QUOTE_INVALID("MKT_001", "Giá thị trường không hợp lệ.", HttpStatus.BAD_REQUEST, "market-quote-invalid"),
    SQUAD_VALIDATION("SQUAD_001", "Đội hình không hợp lệ.", HttpStatus.BAD_REQUEST, "squad-validation"),
    REVIEW_MODERATION("REV_001", "Đánh giá không được chấp nhận.", HttpStatus.BAD_REQUEST, "review-moderation"),
    CRAWLER_PAYLOAD_INVALID("CRAW_001", "Payload đồng bộ crawler không hợp lệ.", HttpStatus.BAD_REQUEST, "crawler-payload-invalid"),
    SYNC_JOB_FAILED("SYNC_001", "Tác vụ đồng bộ thất bại.", HttpStatus.INTERNAL_SERVER_ERROR, "sync-job-failed"),

    // —— Cross-cutting HTTP semantics (map legacy Http* exceptions) ————————————
    VAL_BAD_REQUEST("VAL_001", "Yêu cầu không hợp lệ.", HttpStatus.BAD_REQUEST, "validation-bad-request"),
    RES_NOT_FOUND("RES_001", "Không tìm thấy tài nguyên.", HttpStatus.NOT_FOUND, "resource-not-found"),
    RES_CONFLICT("RES_002", "Xung đột dữ liệu.", HttpStatus.CONFLICT, "resource-conflict"),
    RES_FORBIDDEN("RES_003", "Không được phép thực hiện thao tác này.", HttpStatus.FORBIDDEN, "resource-forbidden"),

    // —— System —————————————————————————————————————————————————————————————
    SYST_ERROR("SYST_001", "Đã có lỗi hệ thống xảy ra. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR, "system-error");

    public static final URI PROBLEM_TYPE_BASE = URI.create("https://fco.platform/problems/");

    private final String code;
    private final String message;
    private final HttpStatus status;
    private final String typeSlug;

    ErrorCode(String code, String message, HttpStatus status, String typeSlug) {
        this.code = code;
        this.message = message;
        this.status = status;
        this.typeSlug = typeSlug;
    }

    /** RFC 7807 problem type URI (stable, documentation-backed). */
    public URI getTypeUri() {
        return PROBLEM_TYPE_BASE.resolve(typeSlug);
    }
}
