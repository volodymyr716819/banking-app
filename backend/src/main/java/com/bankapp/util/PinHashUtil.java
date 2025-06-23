package com.bankapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** PIN hash utility */
@Component
public class PinHashUtil {
    
    private static final Logger logger = LoggerFactory.getLogger(PinHashUtil.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    
    /** Hash a PIN */
    public String hashPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            logger.warn("Attempt to hash null or empty PIN");
            return "";
        }
        return encoder.encode(pin);
    }
    
    /** Verify PIN match */
    public boolean verifyPin(String plainPin, String hashedPin) {
        if (plainPin == null || plainPin.isEmpty() || hashedPin == null || hashedPin.isEmpty()) {
            logger.warn("Attempt to verify null or empty PIN");
            return false;
        }
        return encoder.matches(plainPin, hashedPin);
    }
}