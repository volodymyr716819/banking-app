package com.bankapp.util;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class PinHashUtil {

    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public String hashPin(String pin) {
        if (pin == null || pin.isEmpty()) {
            return "";
        }
        return encoder.encode(pin);
    }

    public String hashPin(char[] pinChars) {
        return hashPin(new String(pinChars));
    }

    public boolean verifyPin(String plainPin, String hashedPin) {
        if (plainPin == null || plainPin.isEmpty() || hashedPin == null || hashedPin.isEmpty()) {
            return false;
        }
        return encoder.matches(plainPin, hashedPin);
    }

    public boolean verifyPin(char[] plainPinChars, String hashedPin) {
        return verifyPin(new String(plainPinChars), hashedPin);
    }
}
