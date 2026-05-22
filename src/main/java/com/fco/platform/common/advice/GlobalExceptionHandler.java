package com.fco.platform.common.advice;

import com.fco.platform.common.exception.ApiProblemDetails;
import com.fco.platform.common.exception.BusinessException;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.common.exception.HttpBadRequest;
import com.fco.platform.common.exception.HttpConflict;
import com.fco.platform.common.exception.HttpForbiden;
import com.fco.platform.common.exception.HttpNotFound;
import com.fco.platform.common.exception.HttpUnAuthorized;
import com.fco.platform.common.exception.ResourceNotFoundException;
import com.fco.platform.common.exception.UserNotFoundException;
import com.fco.platform.common.web.HttpRequestUris;
import com.fco.platform.common.application.AlertService;
import jakarta.servlet.http.HttpServletRequest;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * RFC 7807 ({@code application/problem+json}) with stable {@link ApiProblemDetails#PROPERTY_ERROR_CODE} for
 * contract-first client handling (B1/B3).
 */
@RestControllerAdvice
@Slf4j
@RequiredArgsConstructor
public class GlobalExceptionHandler {

    private final AlertService alertService;

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ProblemDetail> handleBusiness(BusinessException ex, HttpServletRequest request) {
        ErrorCode ec = ex.getErrorCode();
        log.warn("Business error at {}: {} — {}", request.getRequestURI(), ec.getCode(), ex.getMessage());
        ProblemDetail pd = ex.getExtensions().isEmpty()
                ? ApiProblemDetails.from(ec, ex.getMessage(), HttpRequestUris.currentRequestUri(request))
                : ApiProblemDetails.from(ec, ex.getMessage(), HttpRequestUris.currentRequestUri(request), ex.getExtensions());
        return problem(ec.getStatus().value(), pd);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ProblemDetail> handleValidation(MethodArgumentNotValidException ex, HttpServletRequest request) {
        Map<String, String> errors = ex.getBindingResult().getFieldErrors().stream()
                .collect(Collectors.toMap(
                        fe -> fe.getField(),
                        fe -> fe.getDefaultMessage() != null ? fe.getDefaultMessage() : "Invalid value",
                        (a, b) -> a));
        log.warn("Validation failed at {}: {}", request.getRequestURI(), errors);
        ProblemDetail pd = ApiProblemDetails.fromValidation(ErrorCode.USER_VALIDATION_ERROR, HttpRequestUris.currentRequestUri(request), errors);
        return problem(HttpStatus.BAD_REQUEST.value(), pd);
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ProblemDetail> handleBadCredentials(BadCredentialsException ex, HttpServletRequest request) {
        log.warn("Login failed at {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.AUTH_INVALID_CREDENTIALS, ErrorCode.AUTH_INVALID_CREDENTIALS.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.UNAUTHORIZED.value(), pd);
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ProblemDetail> handleAccessDenied(AccessDeniedException ex, HttpServletRequest request) {
        log.warn("Access denied at {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.AUTH_ACCESS_DENIED, ErrorCode.AUTH_ACCESS_DENIED.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.FORBIDDEN.value(), pd);
    }

    @ExceptionHandler({DisabledException.class, LockedException.class})
    public ResponseEntity<ProblemDetail> handleDisabledOrLocked(Exception ex, HttpServletRequest request) {
        log.warn("Account disabled or locked access attempt at {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.AUTH_LOCKED, ErrorCode.AUTH_LOCKED.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.FORBIDDEN.value(), pd);
    }

    @ExceptionHandler(AuthenticationException.class)
    public ResponseEntity<ProblemDetail> handleAuthentication(AuthenticationException ex, HttpServletRequest request) {
        log.warn("Authentication failed at {}: {}", request.getRequestURI(), ex.getMessage());
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.AUTH_UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.UNAUTHORIZED.value(), pd);
    }

    @ExceptionHandler(HttpBadRequest.class)
    public ResponseEntity<ProblemDetail> handleHttpBadRequest(HttpBadRequest ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.VAL_BAD_REQUEST, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.BAD_REQUEST.value(), pd);
    }

    @ExceptionHandler(HttpNotFound.class)
    public ResponseEntity<ProblemDetail> handleHttpNotFound(HttpNotFound ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.RES_NOT_FOUND, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.NOT_FOUND.value(), pd);
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleResourceNotFound(ResourceNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.RES_NOT_FOUND, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.NOT_FOUND.value(), pd);
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ProblemDetail> handleUserNotFound(UserNotFoundException ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.USER_NOT_FOUND, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.NOT_FOUND.value(), pd);
    }

    @ExceptionHandler(HttpConflict.class)
    public ResponseEntity<ProblemDetail> handleHttpConflict(HttpConflict ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.RES_CONFLICT, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.CONFLICT.value(), pd);
    }

    @ExceptionHandler(HttpForbiden.class)
    public ResponseEntity<ProblemDetail> handleHttpForbidden(HttpForbiden ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.RES_FORBIDDEN, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.FORBIDDEN.value(), pd);
    }

    @ExceptionHandler(HttpUnAuthorized.class)
    public ResponseEntity<ProblemDetail> handleHttpUnauthorized(HttpUnAuthorized ex, HttpServletRequest request) {
        ProblemDetail pd = ApiProblemDetails.from(ErrorCode.AUTH_UNAUTHORIZED, ex.getMessage(), HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.UNAUTHORIZED.value(), pd);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ProblemDetail> handleUnhandled(Exception ex, HttpServletRequest request) {
        return handleSystem(ex, request);
    }

    private ResponseEntity<ProblemDetail> handleSystem(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at {}", request.getRequestURI(), ex);
        alertService.sendCriticalAlert("Unhandled Exception at " + request.getRequestURI() + ": " + ex.getMessage(), ex);
        ProblemDetail pd = ApiProblemDetails.fallback(ex, HttpRequestUris.currentRequestUri(request));
        return problem(HttpStatus.INTERNAL_SERVER_ERROR.value(), pd);
    }

    private static ResponseEntity<ProblemDetail> problem(int status, ProblemDetail body) {
        return ResponseEntity.status(status).contentType(MediaType.APPLICATION_PROBLEM_JSON).body(body);
    }
}
