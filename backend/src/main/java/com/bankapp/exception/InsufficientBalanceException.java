package com.bankapp.exception;

/**
 * Exception thrown when account has insufficient balance for a transaction
 */
public class InsufficientBalanceException extends RuntimeException {
    
    public InsufficientBalanceException(String message) {
        super(message);
    }
}