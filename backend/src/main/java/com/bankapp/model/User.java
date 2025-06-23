package com.bankapp.model;

import java.util.List;

import com.bankapp.model.enums.RegistrationStatus;
import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import jakarta.persistence.EnumType;
import com.bankapp.model.enums.RegistrationStatus;

@Entity
@Table(name = "app_user")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToMany(mappedBy = "user")
    @JsonIgnore
    private List<Account> accounts;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private String password;

    private String name;

    @Column(unique = true)
    private String bsn;

    private String role = "customer"; // default role

     @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private RegistrationStatus registrationStatus = RegistrationStatus.PENDING;
    
    @Column(name = "registration_date")
    private java.util.Date registrationDate = new java.util.Date();

    public User() {
    }

    public User(String name, String email, String password, String bsn, RegistrationStatus registrationStatus) {
        this.name = name;
        this.email = email;
        this.password = password;
        this.bsn = bsn;
        this.registrationStatus = RegistrationStatus.PENDING;
        this.registrationDate = new java.util.Date();
    }

    // Getters
    public Long getId() {
        return id;
    }

    public String getEmail() {
        return email;
    }

    public String getPassword() {
        return password;
    }

    public String getName() {
        return name;
    }
    
    public String getBsn() {
        return bsn;
    }

    public String getRole() {
        return role;
    }

    public RegistrationStatus getRegistrationStatus() {
        return registrationStatus;
    }
    
    public java.util.Date getRegistrationDate() {
        return registrationDate;
    }

    public boolean isApproved(){
        if (registrationStatus == RegistrationStatus.APPROVED) {
            return true;
        }
        else{
            return false;
        }
    }

    // Setters
    public void setId(Long id) {
        this.id = id;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public void setName(String name) {
        this.name = name;
    }
    
    public void setBsn(String bsn) {
        this.bsn = bsn;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public void setRegistrationStatus(RegistrationStatus status) {
        this.registrationStatus = status;
    }
    
    public void setRegistrationDate(java.util.Date registrationDate) {
        this.registrationDate = registrationDate;
    }

    public void setApproved(boolean approved) {
        this.registrationStatus = approved ? RegistrationStatus.APPROVED : RegistrationStatus.PENDING;
    }
        
    public void approve() {
        this.registrationStatus = RegistrationStatus.APPROVED;
    }
 
    public void decline() {
        this.registrationStatus = RegistrationStatus.DECLINED;
    }
}