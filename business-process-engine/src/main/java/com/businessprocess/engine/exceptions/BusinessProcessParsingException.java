package com.businessprocess.engine.exceptions;

public class BusinessProcessParsingException extends RuntimeException {
    public BusinessProcessParsingException(String message) {
        super(message);
    }

    public BusinessProcessParsingException(String message, Throwable cause) {
        super(message, cause);
    }
}