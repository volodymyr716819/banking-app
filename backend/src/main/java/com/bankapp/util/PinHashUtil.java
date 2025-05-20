package com.bankapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Utility for securely hashing and verifying PINs
 */
@Component
public class PinHashUtil {
    
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /**
     * Hash a PIN using BCrypt algorithm
     * 
     * @param pin Plain text PIN to hash
     * @return Hashed PIN
     */
    public String hashPin(String pin) {
        return encoder.encode(pin);
    }
    
    /**
     * Verify if a plaintext PIN matches a hashed PIN
     * 
     * @param plainPin Plain text PIN to verify
     * @param hashedPin Hashed PIN to compare against
     * @return true if the PIN matches, false otherwise
     */
    public boolean verifyPin(String plainPin, String hashedPin) {
        return encoder.matches(plainPin, hashedPin);
    }
}