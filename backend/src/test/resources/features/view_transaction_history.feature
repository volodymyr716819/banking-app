@history
Feature: View Transaction History
  As a bank customer
  I want to view my transaction history
  So that I can track my financial activities

  Background:
    Given the banking system is running
    And there are approved customers with accounts and transaction history:
      | customer_name | email            | account_id | account_type | balance | iban               |
      | Alaa Aldrobe  | drobe@example.com | 1          | CHECKING     | 1000.00 | NL91ABNA0417164300 |
      | Pan           | pan@example.com   | 2          | SAVINGS      | 500.00  | NL91ABNA0417164301 |

  Scenario: Successfully viewing transaction history
    Given I am logged in as "drobe@example.com"
    When I request my transaction history
    Then I should see all transactions involving my accounts
    And the transactions should be sorted by timestamp (newest first)