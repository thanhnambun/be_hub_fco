package com.fco.platform.auth.infrastructure.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fco.platform.common.exception.ApiProblemDetails;
import com.fco.platform.common.exception.ErrorCode;
import com.fco.platform.common.web.CorrelationIdFilter;
import com.fco.platform.common.web.HttpRequestUris;
import com.fco.platform.common.web.ProblemDetailHttpSerializer;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.MDC;
import org.springframework.http.ProblemDetail;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.web.access.AccessDeniedHandler;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class AccessDenied implements AccessDeniedHandler {

    private final ObjectMapper objectMapper;

    @Override
    public void handle(
            HttpServletRequest request,
            HttpServletResponse response,
            AccessDeniedException accessDeniedException)
            throws IOException, ServletException {
        log.warn("{\"event\":\"AUTH_ACCESS_DENIED\", \"reason\":\"{}\", \"path\":\"{}\", \"ip\":\"{}\", \"correlationId\":\"{}\"}",
                accessDeniedException.getMessage(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
        ProblemDetail problemDetail =
                ApiProblemDetails.from(ErrorCode.AUTH_ACCESS_DENIED, ErrorCode.AUTH_ACCESS_DENIED.getMessage(), HttpRequestUris.currentRequestUri(request));
        ProblemDetailHttpSerializer.write(response, objectMapper, problemDetail);
    }
}
