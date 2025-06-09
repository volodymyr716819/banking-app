package com.bankapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Utility for securely handling ATM PIN operations
 * 
 * This class provides methods to safely hash and verify 4-digit PINs
 * using industry-standard BCrypt algorithm.
 */
@Component
public class PinHashUtil {
    
    // Logger for security-related events
    private static final Logger logger = LoggerFactory.getLogger(PinHashUtil.class);
    
    // BCrypt encoder for secure one-way hashing
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Securely hash a PIN using BCrypt
     * 
     * @param pin Plain text PIN to hash
     * @return Securely hashed PIN string or empty string if input is invalid
     */
    public String hashPin(String pin) {
        // Safety check - don't process empty PINs
        if (!isPinValid(pin)) {
            logWarning("Attempted to hash an invalid PIN");
            return "";
        }
        
        // Use BCrypt to create a secure hash
        return encoder.encode(pin);
    }
    
    /**
     * Verify if a plaintext PIN matches a stored hashed PIN
     * 
     * @param plainPin Plain text PIN entered by user
     * @param hashedPin Previously hashed PIN from database
     * @return true if the PIN matches, false otherwise
     */
    public boolean verifyPin(String plainPin, String hashedPin) {
        // Safety check - don't process empty PINs or hashes
        if (!isPinValid(plainPin) || !isHashValid(hashedPin)) {
            logWarning("Attempted to verify with invalid PIN or hash");
            return false;
        }
        
        // Use BCrypt to safely compare the plain PIN against the hash
        return encoder.matches(plainPin, hashedPin);
    }
    
    /**
     * Check if a PIN is valid for processing
     * 
     * @param pin PIN to validate
     * @return true if PIN is valid
     */
    private boolean isPinValid(String pin) {
        return pin != null && !pin.isEmpty();
    }
    
    /**
     * Check if a hash is valid for verification
     * 
     * @param hash Hash to validate
     * @return true if hash is valid
     */
    private boolean isHashValid(String hash) {
        return hash != null && !hash.isEmpty();
    }
    
    /**
     * Log a security warning message
     * 
     * @param message Warning message to log
     */
    private void logWarning(String message) {
        logger.warn(message);
    }
}