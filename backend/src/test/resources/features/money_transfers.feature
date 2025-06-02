Feature: Money Transfers
  As a bank customer
  I want to transfer money between accounts
  So that I can manage my finances

  Background:
    Given the banking system is running
    And there are approved customers with accounts:
      | name      | email            | account_type | balance | iban               |
      | John Doe  | john@example.com | CHECKING     | 1000.00 | NL91ABNA0417164300 |
      | Jane Smith| jane@example.com | SAVINGS      | 500.00  | NL91ABNA0417164301 |

  Scenario: Successful money transfer using account IDs
    Given I am logged in as "john@example.com"
    When I transfer 100.00 from my account to account ID 2 with description "Payment to Jane"
    Then the transfer should be successful
    And my account balance should be 900.00
    And the receiver's account balance should be 600.00
    And a transaction record should be created

  Scenario: Successful money transfer using IBANs
    Given I am logged in as "john@example.com"
    When I transfer 150.00 from IBAN "NL91ABNA0417164300" to IBAN "NL91ABNA0417164301" with description "IBAN transfer"
    Then the transfer should be successful
    And my account balance should be 850.00
    And the receiver's account balance should be 650.00
    And a transaction record should be created

  Scenario: Transfer fails with insufficient balance
    Given I am logged in as "john@example.com"
    When I try to transfer 1500.00 from my account to account ID 2
    Then the transfer should fail
    And I should receive an error "Sender has insufficient balance"
    And no money should be deducted from my account

  Scenario: Transfer fails with invalid amount
    Given I am logged in as "john@example.com"
    When I try to transfer 0.00 from my account to account ID 2
    Then the transfer should fail
    And I should receive an error "Amount must be greater than zero"

  Scenario: Transfer fails with negative amount
    Given I am logged in as "john@example.com"
    When I try to transfer -100.00 from my account to account ID 2
    Then the transfer should fail
    And I should receive an error "Amount must be greater than zero"

  Scenario: Transfer fails when sender account not found
    Given I am logged in as "john@example.com"
    When I try to transfer 100.00 from non-existent account ID 999 to account ID 2
    Then the transfer should fail
    And I should receive an error "Sender or receiver account not found"

  Scenario: Transfer fails when receiver account not found
    Given I am logged in as "john@example.com"
    When I try to transfer 100.00 from my account to non-existent account ID 999
    Then the transfer should fail
    And I should receive an error "Sender or receiver account not found"

  Scenario: Transfer fails when sender account not approved
    Given there is an unapproved account with sufficient balance
    When I try to transfer money from the unapproved account
    Then the transfer should fail
    And I should receive an error "Sender account is not approved for transactions"

  Scenario: Transfer fails when receiver account not approved
    Given I am logged in as "john@example.com"
    And there is an unapproved receiver account
    When I try to transfer 100.00 to the unapproved account
    Then the transfer should fail
    And I should receive an error "Receiver account is not approved for transactions"

  Scenario: Transfer fails when sender account is closed
    Given there is a closed account with sufficient balance
    When I try to transfer money from the closed account
    Then the transfer should fail
    And I should receive an error "Sender account is closed and cannot make transactions"

  Scenario: Transfer fails when receiver account is closed
    Given I am logged in as "john@example.com"
    And there is a closed receiver account
    When I try to transfer 100.00 to the closed account
    Then the transfer should fail
    And I should receive an error "Receiver account is closed and cannot receive transactions"

  Scenario: Transfer with invalid IBAN format
    Given I am logged in as "john@example.com"
    When I try to transfer 100.00 from IBAN "INVALID_IBAN" to IBAN "NL91ABNA0417164301"
    Then the transfer should fail
    And I should receive an error "Invalid sender IBAN"

  Scenario: Transfer to invalid IBAN format
    Given I am logged in as "john@example.com"
    When I try to transfer 100.00 from IBAN "NL91ABNA0417164300" to IBAN "INVALID_IBAN"
    Then the transfer should fail
    And I should receive an error "Invalid receiver IBAN"

  Scenario: Transfer with exact account balance
    Given I am logged in as "john@example.com"
    And my account has exactly 100.00 balance
    When I transfer 100.00 from my account to account ID 2
    Then the transfer should be successful
    And my account balance should be 0.00
    And the receiver's account balance should be increased by 100.00

  Scenario: Large amount transfer
    Given I am logged in as "john@example.com"
    And my account has 1000000.00 balance
    When I transfer 999999.99 from my account to account ID 2
    Then the transfer should be successful
    And the appropriate amounts should be transferred

  Scenario: Transfer with empty description
    Given I am logged in as "john@example.com"
    When I transfer 100.00 from my account to account ID 2 with description ""
    Then the transfer should be successful
    And the transaction should be recorded with empty description

  Scenario: Transfer with null description
    Given I am logged in as "john@example.com"
    When I transfer 100.00 from my account to account ID 2 with no description
    Then the transfer should be successful
    And the transaction should be recorded with null description