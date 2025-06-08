package com.bankapp.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.Arrays;

/**
 * Global exception handler to standardize error responses across the application
 */
@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
    
    /**
     * Handle ResourceNotFoundException
     */
    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ApiError> handleResourceNotFoundException(
            ResourceNotFoundException ex, HttpServletRequest request) {
        
        ApiError apiError = new ApiError(
                HttpStatus.NOT_FOUND.value(),
                "Resource Not Found",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(apiError, HttpStatus.NOT_FOUND);
    }
    
    /**
     * Handle InvalidPinException
     */
    @ExceptionHandler(InvalidPinException.class)
    public ResponseEntity<ApiError> handleInvalidPinException(
            InvalidPinException ex, HttpServletRequest request) {
        
        ApiError apiError = new ApiError(
                HttpStatus.UNAUTHORIZED.value(),
                "Unauthorized",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(apiError, HttpStatus.UNAUTHORIZED);
    }
    
    /**
     * Handle InsufficientBalanceException
     */
    @ExceptionHandler(InsufficientBalanceException.class)
    public ResponseEntity<ApiError> handleInsufficientBalanceException(
            InsufficientBalanceException ex, HttpServletRequest request) {
        
        ApiError apiError = new ApiError(
                HttpStatus.BAD_REQUEST.value(),
                "Transaction Failed",
                ex.getMessage(),
                request.getRequestURI()
        );
        
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(UnapprovedAccountException.class)
    public ResponseEntity<ApiError> handleUnapprovedAccountException(
        UnapprovedAccountException ex, HttpServletRequest request) {
    
        ApiError apiError = new ApiError(
            HttpStatus.BAD_REQUEST.value(),
            "Account Not Approved",
            ex.getMessage(),
            request.getRequestURI()
        );
    
         return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    } 

    /**
     * Handle general exceptions
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiError> handleGeneralException(
            Exception ex, HttpServletRequest request) {
        
        ApiError apiError = new ApiError(
                HttpStatus.INTERNAL_SERVER_ERROR.value(),
                "Internal Server Error",
                "An unexpected error occurred",
                request.getRequestURI()
        );
        
        // Add stack trace details only in development environments
        apiError.addDetail(ex.getMessage());
        apiError.addDetail(Arrays.toString(ex.getStackTrace()).substring(0, 200) + "...");
        
        return new ResponseEntity<>(apiError, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ApiError> handleIllegalArgumentException(
        IllegalArgumentException ex, HttpServletRequest request) {

        ApiError apiError = new ApiError(
           HttpStatus.BAD_REQUEST.value(),
           "Invalid Request",
           ex.getMessage(),
           request.getRequestURI()
        );
        return new ResponseEntity<>(apiError, HttpStatus.BAD_REQUEST);
    }
}