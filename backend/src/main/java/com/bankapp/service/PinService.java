package com.bankapp.service;

import com.bankapp.exception.InvalidPinException;
import com.bankapp.exception.ResourceNotFoundException;
import com.bankapp.model.Account;
import com.bankapp.model.CardDetails;
import com.bankapp.dto.PinRequest;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.time.LocalDateTime;
import java.util.Arrays;

@Service
public class PinService {

    @Autowired private AccountRepository accountRepository;
    @Autowired private CardDetailsRepository cardDetailsRepository;
    @Autowired private PinHashUtil pinHashUtil;

    public boolean checkPinStatus(Long accountId) {
        return cardDetailsRepository.findByAccountId(accountId)
                .map(CardDetails::isPinCreated).orElse(false);
    }

    public void createPin(PinRequest pinRequest) {
        char[] pinChars = pinRequest.getPin();

        if (pinChars == null || pinChars.length != 4) {
            throw new IllegalArgumentException("PIN must be 4 digits");
        }

        Account account = accountRepository.findById(pinRequest.getAccountId())
                .orElseThrow(() -> new RuntimeException("Account not found"));

        cardDetailsRepository.deleteByAccountId(account.getId());

        String hashedPin = pinHashUtil.hashPin(pinChars);

        CardDetails cardDetails = new CardDetails(account, hashedPin);
        cardDetails.setPinCreated(true);
        cardDetails.setLastPinChanged(LocalDateTime.now());

        cardDetailsRepository.save(cardDetails);
    }

    public boolean verifyPin(PinRequest pinRequest) {
        CardDetails cardDetails = cardDetailsRepository.findByAccountId(pinRequest.getAccountId())
                .filter(CardDetails::isPinCreated)
                .orElseThrow(() -> new RuntimeException("PIN not set for this account"));

        char[] pinChars = pinRequest.getPin();
        return pinHashUtil.verifyPin(pinChars, cardDetails.getHashedPin());
    }

    public void changePin(PinRequest pinRequest) {
        char[] oldPinChars = pinRequest.getPin();
        char[] newPinChars = pinRequest.getNewPin();

        if (newPinChars == null || newPinChars.length != 4) {
            throw new IllegalArgumentException("New PIN must be exactly 4 digits");
        }

        CardDetails cardDetails = cardDetailsRepository.findByAccountId(pinRequest.getAccountId())
                .filter(CardDetails::isPinCreated)
                .orElseThrow(() -> new ResourceNotFoundException("PIN", "accountId", pinRequest.getAccountId()));

        if (!pinHashUtil.verifyPin(oldPinChars, cardDetails.getHashedPin())) {
            throw new InvalidPinException("Current PIN is incorrect");
        }

        String newHashedPin = pinHashUtil.hashPin(newPinChars);
        cardDetails.setHashedPin(newHashedPin);
        cardDetails.setLastPinChanged(LocalDateTime.now());

        cardDetailsRepository.save(cardDetails);
    }
}
