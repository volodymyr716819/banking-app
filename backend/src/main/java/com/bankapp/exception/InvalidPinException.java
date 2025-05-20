package com.bankapp.exception;

/**
 * Exception thrown when PIN validation fails
 */
public class InvalidPinException extends RuntimeException {
    
    public InvalidPinException(String message) {
        super(message);
    }
}