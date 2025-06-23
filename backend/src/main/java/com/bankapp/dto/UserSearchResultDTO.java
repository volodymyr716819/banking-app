package com.bankapp.dto;

import java.util.ArrayList;
import java.util.List;

public class UserSearchResultDTO {
    private Long id;
    private String name;
    private List<String> ibans = new ArrayList<>();

    public UserSearchResultDTO() {
    }

    public UserSearchResultDTO(Long id, String name, List<String> ibans) {
        this.id = id;
        this.name = name;
        this.ibans = ibans;
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
// test for merge