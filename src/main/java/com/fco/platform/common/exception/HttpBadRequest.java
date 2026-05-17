package com.fco.platform.common.exception;

public class HttpBadRequest extends RuntimeException {

    public HttpBadRequest(String message) {
        super(message);
    }
}
