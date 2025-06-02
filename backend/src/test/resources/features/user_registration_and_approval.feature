Feature: User Registration and Approval
  As a potential bank customer
  I want to register for an account and get approved by an employee
  So that I can use the banking services

  Background:
    Given the banking system is running
    And there is an employee user with email "employee@bank.com" and password "employee123"

  Scenario: Successful user registration
    Given I am a new user
    When I register with name "John Doe", email "john@example.com", and password "password123"
    Then my registration should be successful
    And my account should be pending approval
    And I should receive a message "Registration successful. Your account is pending approval."

  Scenario: Duplicate email registration
    Given there is already a user with email "existing@example.com"
    When I try to register with email "existing@example.com"
    Then my registration should fail
    And I should receive an error message "Email is already registered."

  Scenario: Employee approves pending user
    Given there is a pending user with name "Jane Smith" and email "jane@example.com"
    And I am logged in as an employee
    When I approve the user "jane@example.com"
    Then the user should be approved
    And the user should be able to login

  Scenario: Customer cannot approve users
    Given there is a pending user with name "Bob Wilson" and email "bob@example.com"
    And I am logged in as a customer with email "customer@example.com"
    When I try to approve the user "bob@example.com"
    Then I should receive an access denied error

  Scenario: Employee views pending users
    Given there are multiple pending users:
      | name        | email             |
      | Alice Brown | alice@example.com |
      | Charlie Fox | charlie@example.com |
    And I am logged in as an employee
    When I request the list of pending users
    Then I should see 2 pending users
    And the list should contain "Alice Brown" and "Charlie Fox"

  Scenario: Unapproved user cannot login
    Given there is a pending user with name "David Lee" and email "david@example.com"
    When I try to login with email "david@example.com" and password "password123"
    Then my login should fail
    And I should receive an error message "Your account is pending approval by an employee."

  Scenario: Approved user can login successfully
    Given there is an approved user with name "Emma Davis" and email "emma@example.com"
    When I login with email "emma@example.com" and password "password123"
    Then my login should be successful
    And I should receive a valid JWT token
    And the token should contain my user information