package com.bankapp.dto;

import com.bankapp.model.Account;
import com.bankapp.model.User;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

public class UserSearchResultDTO {
    private Long id;
    private String name;
    private List<String> ibans = new ArrayList<>();

    public UserSearchResultDTO(User user, List<Account> accounts) {
        this.id = user.getId();
        this.name = user.getName();
        if (accounts != null && !accounts.isEmpty()) {
            this.ibans = accounts.stream()
                    .filter(Account::isApproved)
                    .filter(account -> !account.isClosed())
                    .map(UserSearchResultDTO::generateIban)
                    .collect(Collectors.toList());
        }
    }

    // Generate a pseudo-IBAN from account ID for display purposes
    private static String generateIban(Account account) {
        String countryCode = "NL";
        String bankCode = "BANK";
        String paddedId = String.format("%010d", account.getId());
        
        return countryCode + bankCode + paddedId;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public List<String> getIbans() {
        return ibans;
    }

    public void setIbans(List<String> ibans) {
        this.ibans = ibans;
    }
}