package com.washer.backend.cloudbase;

public class CloudBasePgException extends RuntimeException {

    public CloudBasePgException(String message) {
        super(message);
    }

    public CloudBasePgException(String message, Throwable cause) {
        super(message, cause);
    }
}
