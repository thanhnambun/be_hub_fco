package com.ra.base_spring_boot.advice;

import com.ra.base_spring_boot.dto.ErrorResponse;
import com.ra.base_spring_boot.exception.BusinessException;
import com.ra.base_spring_boot.exception.ErrorCode;
import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.Map;
import java.util.stream.Collectors;

@RestControllerAdvice
@Slf4j
@lombok.RequiredArgsConstructor
public class GlobalHandleException {

    private final com.ra.base_spring_boot.services.AlertService alertService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ErrorResponse> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business error at {}: {} - {}", request.getRequestURI(), ex.getErrorCode().getCode(), ex.getMessage());
        return buildResponse(ex.getErrorCode(), ex.getMessage(), request);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        org.springframework.validation.FieldError::getField,
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (existing, replacement) -> existing
                ));
        
        log.warn("Validation failed at {}: {}", request.getRequestURI(), errors);
        return buildResponse(ErrorCode.USER_VALIDATION_ERROR, ErrorCode.USER_VALIDATION_ERROR.getMessage(), request, errors);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Login failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.AUTH_INVALID_CREDENTIALS, ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage(), request);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.AUTH_ACCESS_DENIED, ErrorCode.AUTH_ACCESS_DENIED.getMessage(), request);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        return buildResponse(ErrorCode.AUTH_UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED.getMessage(), request);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleAll(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at " + request.getRequestURI(), ex);
        
        // Alerting for 500 errors
        alertService.sendCriticalAlert("Unhandled Exception at " + request.getRequestURI() + ": " + ex.getMessage(), ex);
        
        return buildResponse(ErrorCode.SYST_ERROR, ErrorCode.SYST_ERROR.getMessage(), request);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, String message, HttpServletRequest request) {
        return buildResponse(errorCode, message, request, null);
    }

    private ResponseEntity<ErrorResponse> buildResponse(ErrorCode errorCode, String message, HttpServletRequest request, Object details) {
        ErrorResponse response = ErrorResponse.of(
                errorCode.getCode(),
                message,
                request.getRequestURI(),
                details
        );
        return new ResponseEntity<>(response, errorCode.getStatus());
    }
}
