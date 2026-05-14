package com.ra.base_spring_boot.dto;

import java.time.Instant;

public record ErrorResponse(
    String code,
    String message,
    Instant timestamp,
    String path,
    Object details
) {
    public static ErrorResponse of(String code, String message, String path) {
        return new ErrorResponse(code, message, Instant.now(), path, null);
    }

    public static ErrorResponse of(String code, String message, String path, Object details) {
        return new ErrorResponse(code, message, Instant.now(), path, details);
    }
}
