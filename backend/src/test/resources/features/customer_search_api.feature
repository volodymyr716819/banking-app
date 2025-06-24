Feature: Customer Search API
  As an employee of the banking system
  I want to search for customers via the REST API endpoint /api/users/search
  So that I can find customer information based on their approval status

  Background:
    Given there are test customers in the system:
      | name        | email                   | role     | registration_status | account_type | balance | iban               | account_approved |
      | Alaa Aldrobe| alaa.aldrobe@bank.com   | CUSTOMER | PENDING            | CHECKING     | 1000.00 | NL12TEST0000000001 | false            |
      | Trimpakkiros| trimpakkiros@bank.com   | CUSTOMER | APPROVED           | CHECKING     | 2000.00 | NL12TEST0000000002 | false            |
      | Panagiotis  | panagiotis@bank.com     | CUSTOMER | APPROVED           | SAVINGS      | 1500.00 | NL12TEST0000000003 | true             |

  Scenario: Search for unapproved customer returns empty results
    Given I am logged in as an employee
    When I search for customers by name "Alaa"
    Then the search should be successful
    And the search results should be empty

  Scenario: Search for approved customer with pending account returns customer without IBANs
    Given I am logged in as an employee
    When I search for customers by name "Trimpakkiros"
    Then the search should be successful
    And the search results should contain customer "Trimpakkiros"
    And the search results should not include any IBANs

  Scenario: Search for approved customer with approved account returns customer with IBANs
    Given I am logged in as an employee
    When I search for customers by name "Panagiotis"
    Then the search should be successful
    And the search results should contain customer "Panagiotis"
    And the search results should include IBAN "NL12TEST0000000003"