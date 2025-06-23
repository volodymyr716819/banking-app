@registration
Feature: Customer Registration Flow
  As a potential bank customer
  I want to register for a new account
  So that I can use banking services

  Scenario: Successful customer registration
    Given I am a new user
    When I register with name "Alaa Aldrobe ", email "drobe@example.com", and password "SecurePassword123"
    Then my registration should be successful
    And my account should be pending approval
    And I should receive a message "Registration successful. Your account is pending approval."

