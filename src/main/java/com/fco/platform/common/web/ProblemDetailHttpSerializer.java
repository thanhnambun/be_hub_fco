package com.fco.platform.common.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import org.springframework.http.MediaType;
import org.springframework.http.ProblemDetail;

/** Writes RFC 7807 {@link ProblemDetail} as {@code application/problem+json} on servlet response. */
public final class ProblemDetailHttpSerializer {

    private ProblemDetailHttpSerializer() {}

    public static void write(HttpServletResponse response, ObjectMapper objectMapper, ProblemDetail problemDetail)
            throws IOException {
        response.setStatus(problemDetail.getStatus());
        response.setContentType(MediaType.APPLICATION_PROBLEM_JSON_VALUE);
        response.setCharacterEncoding(StandardCharsets.UTF_8.name());
        objectMapper.writeValue(response.getOutputStream(), problemDetail);
    }
}
