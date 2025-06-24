Feature: Transaction History
  As a user of the banking system
  I want to view transaction history
  So that I can track my financial activities

  Background:
    Given there are test users with transactions in the system:
      | name         | email                | role     | transaction_amount |
      | Alaa Aldrobe | alaa@example.com     | CUSTOMER | 100.00            |
      | Panagiotis   | panagiotis@example.com | CUSTOMER | 200.00            |

  Scenario: Customer views own transactions
    Given I am logged in as customer "alaa@example.com"
    When I request transaction history
    Then I should see my own transactions
    And I should not see other customers' transactions

  Scenario: Employee views specific customer transactions
    Given I am logged in as employee "employee@bank.com"  
    When I request transaction history for customer "Panagiotis"
    Then I should see that customer's transactions

  Scenario: Customer cannot view other customer transactions
    Given I am logged in as customer "alaa@example.com"
    When I try to request another customer's transaction history
    Then I should only see my own transactions