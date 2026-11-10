package com.l299l.newbedwars.panel.web;

public final class HttpError extends RuntimeException {
    final int status;
    final String code;

    public HttpError(int status, String code) {
        super(code, null, false, false);
        this.status = status;
        this.code = code;
    }
}
