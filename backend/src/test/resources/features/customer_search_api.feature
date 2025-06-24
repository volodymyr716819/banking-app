Feature: Customer Search API
  As an employee of the banking system
  I want to search for customers via the REST API endpoint /api/users/search
  So that I can find customer information and their account details

  Background:
    Given there are test customers in the system:
      | name        | email                   | role     | status   | account_type | balance | iban               | account_approved |
      | Trimpakkiros| trimpakkiros@bank.com   | CUSTOMER | APPROVED | CHECKING     | 1000.00 | NL12TEST0000000001 | true             |
      | Panagiotis  | panagiotis@bank.com     | CUSTOMER | APPROVED | SAVINGS      | 500.00  | NL12TEST0000000002 | false            |
      | Alaa Aldrobe| alaa.aldrobe@bank.com   | CUSTOMER | APPROVED | CHECKING     | 1500.00 | NL12TEST0000000003 | true             |

  Scenario: Search with empty name parameter returns error
    Given I am logged in as an employee
    When I search for customers with empty name
    Then the search should fail with status 400

  Scenario: Search for customer with unapproved account returns no IBANs
    Given I am logged in as an employee
    When I search for customers by name "Panagiotis"
    Then the search should be successful
    And the search results should contain customer "Panagiotis"
    And the search results should not include any IBANs