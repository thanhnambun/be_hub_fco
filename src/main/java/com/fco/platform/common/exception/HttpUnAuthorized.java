package com.fco.platform.common.exception;

public class HttpUnAuthorized extends RuntimeException {

    public HttpUnAuthorized(String message) {
        super(message);
    }
}
