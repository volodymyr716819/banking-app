Feature: Account Management
  As a bank customer or employee
  I want to manage bank accounts
  So that I can create, approve, and manage account settings

  Background:
    Given the banking system is running
    And there is an approved customer with name "John Customer" and email "john@example.com"
    And there is an employee with name "Jane Employee" and email "jane@bank.com"

  Scenario: Customer creates a new checking account
    Given I am logged in as "john@example.com"
    When I create a new account with type "CHECKING"
    Then the account should be created successfully
    And the account should be pending approval
    And the account balance should be 0.00

  Scenario: Customer creates a new savings account
    Given I am logged in as "john@example.com"
    When I create a new account with type "SAVINGS"
    Then the account should be created successfully
    And the account should be pending approval
    And the account balance should be 0.00

  Scenario: Employee approves pending account
    Given there is a pending account for user "john@example.com" with type "CHECKING"
    And I am logged in as an employee "jane@bank.com"
    When I approve the account
    Then the account should be approved
    And the customer should be able to use the account for transactions

  Scenario: Employee views all pending accounts
    Given there are multiple pending accounts:
      | user_email        | account_type |
      | user1@example.com | CHECKING     |
      | user2@example.com | SAVINGS      |
      | user3@example.com | CHECKING     |
    And I am logged in as an employee "jane@bank.com"
    When I request the list of pending accounts
    Then I should see 3 pending accounts
    And the accounts should have correct types and users

  Scenario: Customer views their own accounts
    Given the customer "john@example.com" has approved accounts:
      | account_type | balance |
      | CHECKING     | 1000.00 |
      | SAVINGS      | 2500.00 |
    And I am logged in as "john@example.com"
    When I request my account list
    Then I should see 2 accounts
    And the accounts should show correct balances and types

  Scenario: Customer cannot view other customer's accounts
    Given there is another customer "other@example.com" with accounts
    And I am logged in as "john@example.com"
    When I try to view accounts for user "other@example.com"
    Then I should receive an access denied error

  Scenario: Employee can view any customer's accounts
    Given there is a customer "any@example.com" with accounts
    And I am logged in as an employee "jane@bank.com"
    When I view accounts for user "any@example.com"
    Then I should see the customer's accounts

  Scenario: Employee updates account limits
    Given there is an approved account with ID 1
    And I am logged in as an employee "jane@bank.com"
    When I update the account limits:
      | daily_limit    | 5000.00 |
      | absolute_limit | 1000.00 |
    Then the account limits should be updated successfully

  Scenario: Employee closes an account
    Given there is an approved account with ID 1
    And I am logged in as an employee "jane@bank.com"
    When I close the account with ID 1
    Then the account should be marked as closed
    And the account should not appear in active account lists

  Scenario: Cannot close already closed account
    Given there is a closed account with ID 1
    And I am logged in as an employee "jane@bank.com"
    When I try to close the account with ID 1
    Then I should receive an error "Account already closed"

  Scenario: Invalid account type rejection
    Given I am logged in as "john@example.com"
    When I try to create an account with invalid type "INVALID_TYPE"
    Then the account creation should fail
    And I should receive an error about invalid account type

  Scenario: Account filtering excludes closed accounts
    Given the customer "john@example.com" has accounts:
      | account_type | balance | status |
      | CHECKING     | 1000.00 | active |
      | SAVINGS      | 2500.00 | closed |
    And I am logged in as "john@example.com"
    When I request my account list
    Then I should see 1 account
    And the closed account should not be included