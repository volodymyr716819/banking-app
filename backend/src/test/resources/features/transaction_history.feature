Feature: Transaction History
  As a bank customer or employee
  I want to view transaction history
  So that I can track financial activities

  Background:
    Given the banking system is running
    And there are approved customers with accounts and transaction history:
      | customer_name | email            | account_id | account_type | balance | iban               |
      | John Doe      | john@example.com | 1          | CHECKING     | 1000.00 | NL91ABNA0417164300 |
      | Jane Smith    | jane@example.com | 2          | SAVINGS      | 500.00  | NL91ABNA0417164301 |
    And there are historical transactions:
      | from_account_id | to_account_id | amount | description    | timestamp           |
      | 1               | 2             | 100.00 | Transfer to Jane | 2024-01-15T10:00:00 |
      | 2               | 1             | 50.00  | Refund payment   | 2024-01-16T14:30:00 |
    And there are ATM operations:
      | account_id | operation_type | amount | timestamp           |
      | 1          | WITHDRAW       | 200.00 | 2024-01-17T09:15:00 |
      | 1          | DEPOSIT        | 150.00 | 2024-01-18T16:45:00 |

  Scenario: Customer views their own transaction history
    Given I am logged in as "john@example.com"
    When I request my transaction history
    Then I should see all transactions involving my accounts
    And the history should include both regular transfers and ATM operations
    And the transactions should be sorted by timestamp (newest first)

  Scenario: Customer views specific account transaction history by account ID
    Given I am logged in as "john@example.com"
    When I request transaction history for my account ID 1
    Then I should see all transactions for that specific account
    And the history should include both incoming and outgoing transactions
    And ATM operations for that account should be included

  Scenario: Customer views transaction history by IBAN
    Given I am logged in as "john@example.com"
    When I request transaction history for IBAN "NL91ABNA0417164300"
    Then I should see all transactions for that IBAN
    And the response should match the account ID based history

  Scenario: Customer cannot view other customer's transaction history
    Given I am logged in as "john@example.com"
    When I try to view transaction history for user ID 2
    Then I should receive an access denied error
    And no transaction data should be returned

  Scenario: Customer cannot view other customer's account transactions
    Given I am logged in as "john@example.com"
    When I try to view transaction history for account ID 2
    Then I should receive an access denied error
    And no transaction data should be returned

  Scenario: Customer cannot view transactions for other customer's IBAN
    Given I am logged in as "john@example.com"
    When I try to view transaction history for IBAN "NL91ABNA0417164301"
    Then I should receive an access denied error
    And no transaction data should be returned

  Scenario: Employee can view any customer's transaction history
    Given I am logged in as an employee
    When I request transaction history for user ID 1
    Then I should see all transactions for that user
    And the history should include all account types and operations

  Scenario: Employee can view any account's transaction history
    Given I am logged in as an employee
    When I request transaction history for account ID 2
    Then I should see all transactions for that account
    And access should be granted without restrictions

  Scenario: Employee can view transactions by any IBAN
    Given I am logged in as an employee
    When I request transaction history for IBAN "NL91ABNA0417164301"
    Then I should see all transactions for that IBAN
    And access should be granted without restrictions

  Scenario: Transaction history includes all required information
    Given I am logged in as "john@example.com"
    When I request my transaction history
    Then each transaction should include:
      | field         | description                           |
      | amount        | Transaction amount                    |
      | description   | Transaction description               |
      | timestamp     | When the transaction occurred         |
      | type          | Type of transaction (TRANSFER, etc.) |
    And ATM operations should include operation type (DEPOSIT/WITHDRAW)

  Scenario: Empty transaction history for new account
    Given there is a new account with no transactions
    And I am the owner of that account
    When I request transaction history for the new account
    Then I should receive an empty transaction list
    And the response should be successful with no errors

  Scenario: Transaction history with invalid account ID
    Given I am logged in as "john@example.com"
    When I request transaction history for non-existent account ID 999
    Then I should receive an error about account not found
    And no transaction data should be returned

  Scenario: Transaction history with invalid IBAN
    Given I am logged in as "john@example.com"
    When I request transaction history for invalid IBAN "INVALID_IBAN"
    Then I should receive an error about invalid IBAN
    And no transaction data should be returned

  Scenario: Mixed transaction types in chronological order
    Given I am logged in as "john@example.com"
    And there are mixed transactions on my account:
      | type        | amount | timestamp           |
      | TRANSFER    | 100.00 | 2024-01-15T10:00:00 |
      | ATM_DEPOSIT | 50.00  | 2024-01-16T11:00:00 |
      | TRANSFER    | 75.00  | 2024-01-17T12:00:00 |
      | ATM_WITHDRAW| 25.00  | 2024-01-18T13:00:00 |
    When I request my transaction history
    Then the transactions should be ordered by timestamp (newest first)
    And all transaction types should be included

  Scenario: Large transaction history pagination
    Given I am logged in as "john@example.com"
    And my account has 100 historical transactions
    When I request my transaction history
    Then all transactions should be returned
    And the response should handle large datasets efficiently

  Scenario: Transaction history with special characters in descriptions
    Given I am logged in as "john@example.com"
    And there are transactions with special character descriptions:
      | description                    |
      | Payment for café & restaurant  |
      | Transfer to José's account     |
      | Refund für Überweisungsgebühr |
    When I request my transaction history
    Then all descriptions should be properly encoded
    And special characters should be preserved

  Scenario: Authentication required for transaction history
    Given I am not logged in
    When I try to request transaction history for any account
    Then I should receive an authentication error
    And no transaction data should be accessible

  Scenario: Transaction history error handling
    Given I am logged in as "john@example.com"
    And there is a database connection issue
    When I request my transaction history
    Then I should receive an appropriate error message
    And the system should handle the error gracefully