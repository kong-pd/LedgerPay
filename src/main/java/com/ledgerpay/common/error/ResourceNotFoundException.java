package com.ledgerpay.common.error;

public class ResourceNotFoundException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    private final String code;

    public ResourceNotFoundException(String code, String message) {
        super(message);
        this.code = code;
    }

    public String getCode() {
        return code;
    }
}