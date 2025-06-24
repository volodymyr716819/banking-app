package com.bankapp.dto;

import com.fasterxml.jackson.annotation.JsonGetter;
import com.fasterxml.jackson.annotation.JsonSetter;

public class PinRequest {

    private Long accountId;
    private char[] pin;
    private char[] newPin;

    public Long getAccountId() {
        return accountId;
    }

    public void setAccountId(Long accountId) {
        this.accountId = accountId;
    }

    public char[] getPin() {
        return pin;
    }

    @JsonSetter("pin")
    public void setPin(String pinStr) {
        this.pin = pinStr != null ? pinStr.toCharArray() : null;
    }

    @JsonGetter("pin")
    public String getPinAsString() {
        return pin != null ? new String(pin) : null;
    }

    public char[] getNewPin() {
        return newPin;
    }

    @JsonSetter("newPin")
    public void setNewPin(String newPinStr) {
        this.newPin = newPinStr != null ? newPinStr.toCharArray() : null;
    }

    @JsonGetter("newPin")
    public String getNewPinAsString() {
        return newPin != null ? new String(newPin) : null;
    }
}
