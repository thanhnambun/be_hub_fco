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
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.web.AuthenticationEntryPoint;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtEntryPoint implements AuthenticationEntryPoint {

    private final ObjectMapper objectMapper;

    @Override
    public void commence(
            HttpServletRequest request,
            HttpServletResponse response,
            AuthenticationException authException)
            throws IOException, ServletException {
        log.warn("{\"event\":\"AUTH_UNAUTHORIZED\", \"reason\":\"{}\", \"path\":\"{}\", \"ip\":\"{}\", \"correlationId\":\"{}\"}",
                authException.getMessage(),
                request.getRequestURI(),
                request.getRemoteAddr(),
                MDC.get(CorrelationIdFilter.MDC_KEY));
        ProblemDetail problemDetail =
                ApiProblemDetails.from(ErrorCode.AUTH_UNAUTHORIZED, ErrorCode.AUTH_UNAUTHORIZED.getMessage(), HttpRequestUris.currentRequestUri(request));
        ProblemDetailHttpSerializer.write(response, objectMapper, problemDetail);
    }
}
