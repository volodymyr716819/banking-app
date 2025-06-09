package com.bankapp.controller;

import com.bankapp.dto.AtmRequest;
import com.bankapp.service.AtmService;
import com.bankapp.model.Account;
import com.bankapp.model.AtmOperation;
import com.bankapp.model.CardDetails;
import com.bankapp.repository.AccountRepository;
import com.bankapp.repository.AtmOperationRepository;
import com.bankapp.repository.CardDetailsRepository;
import com.bankapp.util.PinHashUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/atm")
public class AtmOperationController {

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private AtmOperationRepository atmOperationRepository;
    
    @Autowired
    private CardDetailsRepository cardDetailsRepository;

    @Autowired
    private AtmService atmService;


    @Autowired
    private PinHashUtil pinHashUtil;

    /**
     * Endpoint to deposit money into an account via ATM.
     *
     * @param atmRequest contains account ID, amount to deposit, and PIN
     * @return ResponseEntity with success message
    */ 
    @PostMapping("/deposit")
    public ResponseEntity<?> deposit(@RequestBody AtmRequest atmRequest) {
        atmService.processDeposit(
        atmRequest.getAccountId(),
        atmRequest.getAmount(),
        atmRequest.getPin()
    );
    return ResponseEntity.ok("Deposit successful");
    }

    /**
     * Endpoint to withdraw money from an account via ATM.
     *
     * @param atmRequest contains account ID, amount to withdraw, and PIN
     * @return ResponseEntity with success message
    */
    @PostMapping("/withdraw")
    public ResponseEntity<?> withdraw(@RequestBody AtmRequest atmRequest) {
        atmService.processWithdrawal(
           atmRequest.getAccountId(),
           atmRequest.getAmount(),
           atmRequest.getPin()
        );
        return ResponseEntity.ok("Withdrawal successful");
    }

    /**
     * Checks the balance of an account by account ID.
     *
     * @param accountId the ID of the account
     * @return ResponseEntity with balance or error if not found    
    */
    @GetMapping("/balance")
    public ResponseEntity<?> getBalance(@RequestParam Long accountId) {
        return atmService.getBalance(accountId);
    }

    /**
     * Checks whether a PIN has been created for an account.
     *
     * @param accountId the ID of the account
     * @return ResponseEntity with pinCreated status (true/false)
    */
    @GetMapping("/pinStatus")
    public ResponseEntity<?> getPinStatus(@RequestParam Long accountId) {
        return atmService.getPinStatus(accountId);
    }
}