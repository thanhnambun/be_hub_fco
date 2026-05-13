package com.ra.base_spring_boot.advice;

import com.ra.base_spring_boot.dto.ResponseWrapper;
import com.ra.base_spring_boot.exception.*;
import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;

@RestControllerAdvice
public class GlobalHandleException {

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<?> handleValidException(MethodArgumentNotValidException ex) {
        Map<String, String> errors = new java.util.LinkedHashMap<>();
        ex.getBindingResult().getFieldErrors()
                .forEach(fieldError -> errors.put(fieldError.getField(), fieldError.getDefaultMessage()));

        return wrap(HttpStatus.BAD_REQUEST, "Dữ liệu đầu vào không hợp lệ", errors);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<?> handleMaxUploadSizeExceededException(MaxUploadSizeExceededException ex) {
        return wrap(HttpStatus.BAD_REQUEST, "File upload size exceeded", ex.getMessage());
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<?> handleNoResourceFoundException(NoResourceFoundException ex) {
        return wrap(HttpStatus.NOT_FOUND, "Resource not found", ex.getMessage());
    }

    @ExceptionHandler({UsernameNotFoundException.class, UserNotFoundException.class, ResourceNotFoundException.class})
    public ResponseEntity<?> handleUserNotFound(RuntimeException ex) {
        return wrap(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<?> handleBusinessException(BusinessException ex) {
        return wrap(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(AccountLockedException.class)
    public ResponseEntity<?> handleAccountLockedException(AccountLockedException ex) {
        return wrap(HttpStatus.FORBIDDEN, "ACCOUNT_LOCKED", null);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<?> handleBadCredentials(BadCredentialsException ex) {
        return wrap(HttpStatus.UNAUTHORIZED, "Tài khoản hoặc mật khẩu không chính xác", null);
    }

    @ExceptionHandler(TooManyRequestsException.class)
    public ResponseEntity<?> handleTooManyRequests(TooManyRequestsException ex) {
        return wrap(HttpStatus.TOO_MANY_REQUESTS, "Too many requests", ex.getMessage());
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public ResponseEntity<?> handleExpiredJwt(ExpiredJwtException ex) {
        return wrap(HttpStatus.UNAUTHORIZED, "Access token has expired", null);
    }

    @ExceptionHandler(TokenBlacklistedException.class)
    public ResponseEntity<?> handleBlacklistedToken(TokenBlacklistedException ex) {
        return wrap(HttpStatus.UNAUTHORIZED, "Token is blacklisted", ex.getMessage());
    }

    @ExceptionHandler(HttpBadRequest.class)
    public ResponseEntity<?> handleHttpBadReqeust(HttpBadRequest ex) {
        return wrap(HttpStatus.BAD_REQUEST, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpUnAuthorized.class)
    public ResponseEntity<?> handleHttpUnAuthorized(HttpUnAuthorized ex) {
        return wrap(HttpStatus.UNAUTHORIZED, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpForbiden.class)
    public ResponseEntity<?> handleHttpForbiden(HttpForbiden ex) {
        return wrap(HttpStatus.FORBIDDEN, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpNotFound.class)
    public ResponseEntity<?> handleHttpNotFound(HttpNotFound ex) {
        return wrap(HttpStatus.NOT_FOUND, ex.getMessage(), null);
    }

    @ExceptionHandler(HttpConflict.class)
    public ResponseEntity<?> handleHttpConflict(HttpConflict ex) {
        return wrap(HttpStatus.CONFLICT, ex.getMessage(), null);
    }

    @ExceptionHandler(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException.class)
    public ResponseEntity<?> handleTypeMismatch(org.springframework.web.method.annotation.MethodArgumentTypeMismatchException ex) {
        return wrap(HttpStatus.BAD_REQUEST, "Dữ liệu truyền vào không đúng định dạng", ex.getMessage());
    }

    @ExceptionHandler(org.springframework.http.converter.HttpMessageNotReadableException.class)
    public ResponseEntity<?> handleMessageNotReadable(org.springframework.http.converter.HttpMessageNotReadableException ex) {
        return wrap(HttpStatus.BAD_REQUEST, "Dữ liệu JSON không hợp lệ", ex.getMessage());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleUnknown(Exception ex) {
        return wrap(HttpStatus.INTERNAL_SERVER_ERROR, "An internal server error occurred", ex.getMessage());
    }

    private ResponseEntity<ResponseWrapper<Object>> wrap(HttpStatus status, String message, Object data) {
        return ResponseEntity.status(status).body(
                ResponseWrapper.builder()
                        .status(status)
                        .code(status.value())
                        .message(message)
                        .data(data)
                        .build()
        );
    }
}
