package com.fco.platform.common.web;

import jakarta.servlet.http.HttpServletRequest;
import java.net.URI;
import java.util.Optional;

/** Stable {@link URI} for RFC 7807 {@code instance} from servlet request. */
public final class HttpRequestUris {

    private HttpRequestUris() {}

    public static URI currentRequestUri(HttpServletRequest request) {
        try {
            return URI.create(request.getRequestURL().toString());
        } catch (Exception e) {
            return URI.create(Optional.ofNullable(request.getRequestURI()).orElse("/"));
        }
    }
}
