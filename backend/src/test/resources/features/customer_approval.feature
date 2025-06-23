@approval
Feature: Customer Approval
  As a bank employee
  I want to approve new customer registrations
  So that they can start using banking services

  Background:
    Given there is a pending user with name "Dan" and email "dan@example.com"

  Scenario: Employee approves a customer
    Given I am logged in as an employee
    When I approve the user "dan@example.com"
    Then the user should be approved
    And the user should be able to login
