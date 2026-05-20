package com.fco.platform.common.exception;

import java.net.URI;
import java.time.Instant;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ProblemDetail;

/**
 * Builds RFC 7807 {@link ProblemDetail} bodies with stable {@code errorCode} for client routing.
 */
public final class ApiProblemDetails {

    public static final String PROPERTY_ERROR_CODE = "errorCode";
    public static final String PROPERTY_TIMESTAMP = "timestamp";
    public static final String PROPERTY_ERRORS = "errors";

    private ApiProblemDetails() {}

    public static ProblemDetail from(ErrorCode errorCode, String detail, URI instance) {
        return from(errorCode, detail, instance, Map.of());
    }

    public static ProblemDetail from(ErrorCode errorCode, String detail, URI instance, Map<String, ?> extensions) {
        String resolvedDetail = detail != null && !detail.isBlank() ? detail : errorCode.getMessage();
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(errorCode.getStatus(), resolvedDetail);
        pd.setType(errorCode.getTypeUri());
        pd.setTitle(errorCode.name());
        pd.setInstance(instance);
        pd.setProperty(PROPERTY_ERROR_CODE, errorCode.getCode());
        pd.setProperty(PROPERTY_TIMESTAMP, Instant.now().toString());
        if (extensions != null) {
            extensions.forEach(pd::setProperty);
        }
        return pd;
    }

    public static ProblemDetail fromValidation(ErrorCode errorCode, URI instance, Map<String, String> fieldErrors) {
        ProblemDetail pd = from(errorCode, errorCode.getMessage(), instance);
        if (fieldErrors != null && !fieldErrors.isEmpty()) {
            pd.setProperty(PROPERTY_ERRORS, Map.copyOf(fieldErrors));
        }
        return pd;
    }

    public static ProblemDetail fallback(Throwable ex, URI instance) {
        String originalMsg = ex.getMessage();
        String detailMessage = ErrorCode.SYST_ERROR.getMessage() + (originalMsg != null ? " (Chi tiết: " + originalMsg + ")" : "");
        ProblemDetail pd = ProblemDetail.forStatusAndDetail(HttpStatus.INTERNAL_SERVER_ERROR, detailMessage);
        pd.setType(ErrorCode.SYST_ERROR.getTypeUri());
        pd.setTitle(ErrorCode.SYST_ERROR.name());
        pd.setInstance(instance);
        pd.setProperty(PROPERTY_ERROR_CODE, ErrorCode.SYST_ERROR.getCode());
        pd.setProperty(PROPERTY_TIMESTAMP, Instant.now().toString());
        pd.setProperty("exceptionType", ex.getClass().getName());
        return pd;
    }
}
