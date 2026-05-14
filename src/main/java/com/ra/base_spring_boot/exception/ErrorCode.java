package com.ra.base_spring_boot.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ErrorCode {
    AUTH_LOCKED("AUTH_001", "Tài khoản của bạn đã bị khóa. Vui lòng liên hệ quản trị viên.", HttpStatus.FORBIDDEN),
    AUTH_INVALID_CREDENTIALS("AUTH_002", "Tài khoản hoặc mật khẩu không chính xác.", HttpStatus.UNAUTHORIZED),
    AUTH_ACCESS_DENIED("AUTH_003", "Bạn không có quyền truy cập tài nguyên này.", HttpStatus.FORBIDDEN),
    AUTH_UNAUTHORIZED("AUTH_004", "Vui lòng đăng nhập để tiếp tục.", HttpStatus.UNAUTHORIZED),
    
    USER_VALIDATION_ERROR("USER_001", "Dữ liệu đầu vào không hợp lệ.", HttpStatus.BAD_REQUEST),
    USER_NOT_FOUND("USER_002", "Không tìm thấy người dùng yêu cầu.", HttpStatus.NOT_FOUND),
    
    PLAY_SYNC_ERROR("PLAY_001", "Lỗi đồng bộ dữ liệu cầu thủ.", HttpStatus.INTERNAL_SERVER_ERROR),
    
    SYST_ERROR("SYST_001", "Đã có lỗi hệ thống xảy ra. Vui lòng thử lại sau.", HttpStatus.INTERNAL_SERVER_ERROR);

    private final String code;
    private final String message;
    private final HttpStatus status;

    ErrorCode(String code, String message, HttpStatus status) {
        this.code = code;
        this.message = message;
        this.status = status;
    }
}
