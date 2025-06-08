package com.bankapp.exception;

public class UnapprovedAccountException extends RuntimeException {
    public UnapprovedAccountException(String message) {
        super(message);
    }
}
