
Feature: Customer Search
  As a bank employee
  I want to search for customers by name, email, or IBAN
  So that I can find customer accounts quickly

  Scenario: Search customer by name
    Given I am logged in as an employee
    And there are approved customers with accounts:
      | name          | email                   | account_type | balance | iban              |
      | Alaa Aldrobe  | alaa.aldrobe@example.com   | CHECKING    | 1000.00 | NL91BANK0000000101 |
      | Dan Breczinski| dan.breczinski@example.com | SAVINGS     | 2000.00 | NL91BANK0000000102 |
    When I search for customers with name "Alaa"
    Then I should see customer "Alaa Aldrobe" in the search results
    And the search results should include customer's IBAN "NL91BANK0000000101"

  Scenario: Search customer by email
    Given I am logged in as an employee
    And there are approved customers with accounts:
      | name          | email                   | account_type | balance | iban              |
      | Alaa Aldrobe  | alaa.aldrobe@example.com   | CHECKING    | 1000.00 | NL91BANK0000000101 |
      | Dan Breczinski| dan.breczinski@example.com | SAVINGS     | 2000.00 | NL91BANK0000000102 |
    When I search for customers with email "dan.breczinski"
    Then I should see customer "Dan Breczinski" in the search results
    And the search results should include customer's IBAN "NL91BANK0000000102"

  Scenario: Search customer by IBAN
    Given I am logged in as an employee
    And there are approved customers with accounts:
      | name          | email                   | account_type | balance | iban              |
      | Alaa Aldrobe  | alaa.aldrobe@example.com   | CHECKING    | 1000.00 | NL91BANK0000000101 |
      | Dan Breczinski| dan.breczinski@example.com | SAVINGS     | 2000.00 | NL91BANK0000000102 |
    When I search for customers with IBAN "NL91BANK0000000101"
    Then I should see customer "Alaa Aldrobe" in the search results
    And the search results should include customer's IBAN "NL91BANK0000000101"

  Scenario: No results found when searching with non-existent term
    Given I am logged in as an employee
    And there are approved customers with accounts:
      | name          | email                   | account_type | balance | iban              |
      | Alaa Aldrobe  | alaa.aldrobe@example.com   | CHECKING    | 1000.00 | NL91BANK0000000101 |
      | Dan Breczinski| dan.breczinski@example.com | SAVINGS     | 2000.00 | NL91BANK0000000102 |
    When I search for customers with name "Volodymyr"
    Then I should receive empty search results

  Scenario: Customer is not found if their account is closed
    Given I am logged in as an employee
    And there is a customer with a closed account:
      | name          | email                       | account_type | balance | iban              |
      | Volodymyr Gulchenko | volodymyr.gulchenko@example.com | CHECKING    | 500.00  | NL91BANK0000000103 |
    When I search for customers with name "Volodymyr"
    Then I should receive empty search results

  Scenario: Regular customer cannot search for other customers
    Given I am logged in as "alaa.aldrobe@example.com"
    When I try to search for customers
    Then I should receive an access denied error