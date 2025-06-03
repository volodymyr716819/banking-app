Feature: Customer Approval
  As a bank employee
  I want to approve new customer registrations
  So that they can start using banking services

  Background:
    Given there is a pending user with name "Jane Doe" and email "jane.doe@example.com"

  Scenario: Employee approves a customer
    Given I am logged in as an employee
    When I approve the user "jane.doe@example.com"
    Then the user should be approved
    And the user should be able to login

  Scenario: Customer cannot approve other customers
    Given I am logged in as "john.smith@example.com"
    When I try to approve the user "jane.doe@example.com"
    Then I should receive an access denied error