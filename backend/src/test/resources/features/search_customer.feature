Feature: Customer Search Functionality
  As a bank employee or customer
  I want to search for customer accounts
  So that I can find accounts by name, email, or IBAN

  Background:
    Given the banking system is running
    And there are approved customers with the following details:
      | name                | email                | account_type | balance | iban               |
      | Alaa Aldrobe        | alaa@example.com     | CHECKING     | 1000.00 | NL00BANK0000000001 |
      | Volodymyr Gulchenko | volodymyr@example.com| SAVINGS      | 2500.00 | NL00BANK0000000002 |
      | Dan Breczinski      | dan@example.com      | CHECKING     | 3000.00 | NL00BANK0000000003 |

  Scenario: Employee searches for customer by name
    Given I am logged in as an employee with email "employee@bank.com"
    When I search for customers with term "Alaa"
    Then the search should be successful
    And the search results should contain 1 customer
    And the results should include a customer with name "Alaa Aldrobe"
    And the customer should have the IBAN "NL00BANK0000000001"

  Scenario: Employee searches for customer by email
    Given I am logged in as an employee with email "employee@bank.com"
    When I search for customers with email "volodymyr@example.com"
    Then the search should be successful
    And the search results should contain 1 customer
    And the results should include a customer with name "Volodymyr Gulchenko"

  Scenario: Employee searches for customer by IBAN
    Given I am logged in as an employee with email "employee@bank.com"
    When I search for customers with IBAN "NL00BANK0000000003"
    Then the search should be successful
    And the search results should contain 1 customer
    And the results should include a customer with name "Dan Breczinski"

  Scenario: Employee searches with partial match
    Given I am logged in as an employee with email "employee@bank.com"
    When I search for customers with term "Al"
    Then the search should be successful
    And the search results should contain 1 customer
    And the results should include a customer with name "Alaa Aldrobe"

  Scenario: Employee searches with no results
    Given I am logged in as an employee with email "employee@bank.com"
    When I search for customers with term "Nonexistent"
    Then the search should be successful
    And the search results should be empty

  Scenario: Search with no authentication fails
    When I try to search for customers without authentication
    Then the search should fail
    And I should receive an error message about authentication for search

  Scenario: Search with no parameters fails
    Given I am logged in as an employee with email "employee@bank.com"
    When I try to search for customers without any search parameters
    Then the search should fail
    And I should receive a search error message "At least one search parameter is required"

  Scenario: Customer searches for own account
    Given I am logged in as a customer with email "alaa@example.com"
    When I search for customers with term "Alaa"
    Then the search should be successful
    And the search results should contain 1 customer
    And the results should include a customer with name "Alaa Aldrobe"

  Scenario: Search for customer with multiple accounts shows all IBANs
    Given I am logged in as an employee with email "employee@bank.com"
    And customer "Alaa Aldrobe" has multiple accounts:
      | account_type | balance | iban               |
      | CHECKING     | 1000.00 | NL00BANK0000000001 |
      | SAVINGS      | 5000.00 | NL00BANK0000000004 |
    When I search for customers with term "Alaa"
    Then the search should be successful
    And the results should include a customer with name "Alaa Aldrobe"
    And the customer should have 2 IBANs