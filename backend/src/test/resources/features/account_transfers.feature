@transfers
Feature: Transfer Between Accounts
  As a bank customer
  I want to transfer money between accounts
  So that I can manage my finances

  Background:
    Given there are approved customers with accounts:
      | name       | email                  | account_type | balance | iban               |
      | Alaad|Alaad@example.com | CHECKING     | 1000.00 | NL91ABNA0417164300 |
      | Vol  | vol@example.com   | SAVINGS      | 500.00  | NL91ABNA0417164301 |

  Scenario: Successful transfer between accounts
    Given I am logged in as "Alaad@example.com"
    When I transfer 200.00 from my account to account ID 2 with description "Test transfer"
    Then the transfer should be successful
    And my account balance should be 800.00
    And the receiver's account balance should be 700.00
    And a transaction record should be created

  Scenario: Transfer with insufficient funds
    Given I am logged in as "Alaad@example.com"
    When I try to transfer 1500.00 from my account to account ID 2
    Then the transfer should fail
    And I should receive an error "Sender has insufficient balance"
    And no money should be deducted from my account