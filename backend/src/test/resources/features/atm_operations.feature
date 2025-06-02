Feature: ATM Operations
  As a bank customer
  I want to use ATM services
  So that I can deposit and withdraw money from my account

  Background:
    Given the banking system is running
    And there is an approved customer "john@example.com" with an approved account ID 1
    And the account has a balance of 1000.00
    And the account has a PIN set to "1234"

  Scenario: Successful cash deposit
    Given I am at the ATM
    When I deposit 200.00 into account ID 1 with PIN "1234"
    Then the deposit should be successful
    And the account balance should be 1200.00
    And an ATM operation record should be created for DEPOSIT

  Scenario: Successful cash withdrawal
    Given I am at the ATM
    When I withdraw 300.00 from account ID 1 with PIN "1234"
    Then the withdrawal should be successful
    And the account balance should be 700.00
    And an ATM operation record should be created for WITHDRAW

  Scenario: Withdrawal fails with insufficient balance
    Given I am at the ATM
    When I try to withdraw 1500.00 from account ID 1 with PIN "1234"
    Then the withdrawal should fail
    And I should receive an error "Insufficient balance"
    And the account balance should remain 1000.00

  Scenario: ATM operation fails with incorrect PIN
    Given I am at the ATM
    When I try to deposit 100.00 into account ID 1 with PIN "9999"
    Then the operation should fail
    And I should receive an error "Invalid PIN"
    And no money should be deposited

  Scenario: ATM operation fails with no PIN set
    Given there is an account ID 2 with no PIN set
    When I try to deposit 100.00 into account ID 2 with PIN "1234"
    Then the operation should fail
    And I should receive an error "PIN not set for this account"

  Scenario: ATM operation fails with null PIN
    Given I am at the ATM
    When I try to deposit 100.00 into account ID 1 with no PIN
    Then the operation should fail
    And I should receive an error "PIN is required"

  Scenario: ATM operation fails for non-existent account
    Given I am at the ATM
    When I try to deposit 100.00 into non-existent account ID 999 with PIN "1234"
    Then the operation should fail
    And I should receive an error "Account not found"

  Scenario: ATM operation fails for unapproved account
    Given there is an unapproved account ID 3 with PIN set
    When I try to deposit 100.00 into account ID 3 with correct PIN
    Then the operation should fail
    And I should receive an error "Account is not approved for ATM operations"

  Scenario: ATM operation fails for closed account
    Given there is a closed account ID 4 with PIN set
    When I try to deposit 100.00 into account ID 4 with correct PIN
    Then the operation should fail
    And I should receive an error "Account is closed and cannot perform ATM operations"

  Scenario: Check account balance
    Given I am at the ATM
    When I check the balance for account ID 1
    Then I should see the balance 1000.00

  Scenario: Check balance for non-existent account
    Given I am at the ATM
    When I try to check the balance for non-existent account ID 999
    Then I should receive an error "Account not found"

  Scenario: Check PIN status for account with PIN
    Given I am at the ATM
    When I check the PIN status for account ID 1
    Then the PIN status should show "PIN created: true"

  Scenario: Check PIN status for account without PIN
    Given there is an account ID 5 with no PIN set
    When I check the PIN status for account ID 5
    Then the PIN status should show "PIN created: false"

  Scenario: Exact balance withdrawal
    Given I am at the ATM
    And the account ID 1 has exactly 500.00 balance
    When I withdraw 500.00 from account ID 1 with PIN "1234"
    Then the withdrawal should be successful
    And the account balance should be 0.00

  Scenario: Very large deposit
    Given I am at the ATM
    When I deposit 999999.99 into account ID 1 with PIN "1234"
    Then the deposit should be successful
    And the account balance should be 1000999.99

  Scenario: Small amount operations
    Given I am at the ATM
    When I deposit 0.01 into account ID 1 with PIN "1234"
    Then the deposit should be successful
    And the account balance should be 1000.01

  Scenario: Zero amount deposit
    Given I am at the ATM
    When I try to deposit 0.00 into account ID 1 with PIN "1234"
    Then the deposit should be successful
    And the account balance should remain 1000.00

  Scenario: Negative amount handling
    Given I am at the ATM
    When I try to deposit -100.00 into account ID 1 with PIN "1234"
    Then the operation should be handled appropriately
    And the system should manage negative amounts according to business rules

  Scenario: Multiple consecutive operations
    Given I am at the ATM
    When I perform the following operations on account ID 1 with PIN "1234":
      | operation | amount |
      | deposit   | 100.00 |
      | withdraw  | 50.00  |
      | deposit   | 25.00  |
    Then all operations should be successful
    And the final account balance should be 1075.00
    And 3 ATM operation records should be created

  Scenario: PIN verification for different operations
    Given I am at the ATM
    When I try multiple operations with different PINs:
      | operation | amount | pin  | expected_result |
      | deposit   | 100.00 | 1234 | success         |
      | withdraw  | 50.00  | 9999 | fail            |
      | deposit   | 25.00  | 1234 | success         |
    Then the results should match expectations
    And only successful operations should affect the balance