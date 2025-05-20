package com.bankapp.exception;

import com.fasterxml.jackson.annotation.JsonFormat;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Standardized API error response format
 */
public class ApiError {
    
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss")
    private LocalDateTime timestamp;
    
    private int status;
    private String error;
    private String message;
    private String path;
    private List<String> details = new ArrayList<>();
    
    // Default constructor
    public ApiError() {
        timestamp = LocalDateTime.now();
    }
    
    // Constructor with basic error info
    public ApiError(int status, String error, String message, String path) {
        this();
        this.status = status;
        this.error = error;
        this.message = message;
        this.path = path;
    }
    
    // Add a detail message to the error
    public void addDetail(String detail) {
        this.details.add(detail);
    }
    
    // Getters and setters
    public LocalDateTime getTimestamp() {
        return timestamp;
    }
    
    public int getStatus() {
        return status;
    }
    
    public void setStatus(int status) {
        this.status = status;
    }
    
    public String getError() {
        return error;
    }
    
    public void setError(String error) {
        this.error = error;
    }
    
    public String getMessage() {
        return message;
    }
    
    public void setMessage(String message) {
        this.message = message;
    }
    
    public String getPath() {
        return path;
    }
    
    public void setPath(String path) {
        this.path = path;
    }
    
    public List<String> getDetails() {
        return details;
    }
    
    public void setDetails(List<String> details) {
        this.details = details;
    }
}