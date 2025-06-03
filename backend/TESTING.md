# Banking Application Test Suite

This comprehensive test suite provides 90%+ code coverage for the Spring Boot banking application using JUnit 5, Cucumber.

## Test Structure

```
src/test/java/com/bankapp/
├── controller/                 # Controller integration tests
│   ├── AccountControllerTest.java
│   ├── AtmOperationControllerTest.java
│   ├── AuthControllerTest.java
│   ├── TransactionControllerTest.java
│   └── UserControllerTest.java
├── service/                    # Service unit tests
│   └── TransactionServiceTest.java
├── integration/                # Full integration tests
│   └── BankingApplicationIntegrationTest.java
├── cucumber/                   # BDD tests
│   ├── BankingStepDefinitions.java
│   └── CucumberTestRunner.java
├── config/                     # Test configuration
│   └── TestConfiguration.java
└── BankingTestSuite.java      # Test suite runner

src/test/resources/
├── features/                   # Cucumber feature files
│   ├── user_registration_and_approval.feature
│   ├── account_management.feature
│   ├── money_transfers.feature
│   ├── atm_operations.feature
│   └── transaction_history.feature
└── application-test.properties # Test configuration
```

## Test Categories

### 1. Unit Tests
- **TransactionServiceTest**: Comprehensive service layer testing with mocks
  - Transfer money functionality
  - Transfer by IBAN
  - Transaction history retrieval
  - Edge cases and boundary conditions
  - Error handling scenarios

### 2. Integration Tests

#### Controller Tests
Each controller test uses `@WebMvcTest` for focused testing:

- **AccountControllerTest**: Account management endpoints
- **AtmOperationControllerTest**: ATM operations (deposit/withdraw)
- **AuthControllerTest**: Authentication and authorization
- **TransactionControllerTest**: Money transfer operations
- **UserControllerTest**: User management and search

#### Full Integration Tests
- **BankingApplicationIntegrationTest**: End-to-end workflows
  - Complete user registration and approval
  - Account creation and management
  - Money transfer workflows
  - Security and authorization
  - Error handling and validation

### 3. Behavior-Driven Development (BDD) Tests

#### Cucumber Features
1. **User Registration and Approval**
   - New user registration
   - Employee approval process
   - Login restrictions for unapproved users

2. **Account Management**
   - Account creation (checking/savings)
   - Employee approval workflow
   - Account limits and status management

3. **Money Transfers**
   - Transfer by account ID and IBAN
   - Validation and error scenarios
   - Balance verification

4. **ATM Operations**
   - Deposit and withdrawal operations
   - PIN verification
   - Account status validation

5. **Transaction History**
   - Viewing transaction history
   - Authorization checks
   - Mixed transaction types

## Running Tests

### All Tests
```bash
# Run complete test suite
mvn test

# Run with coverage report
mvn clean test jacoco:report

# Run specific test suite
mvn test -Dtest=BankingTestSuite
```

### Unit Tests Only
```bash
mvn test -Dtest="**/*Test"
```

### Integration Tests Only
```bash
mvn test -Dtest="**/*IntegrationTest"
```

### Cucumber Tests Only
```bash
mvn test -Dtest=CucumberTestRunner
```

### Specific Test Class
```bash
mvn test -Dtest=TransactionServiceTest
mvn test -Dtest=TransactionControllerTest
```

## Test Configuration

### Test Database
- **H2 In-Memory Database**: Fast, isolated test execution
- **Auto-DDL**: Tables created/dropped per test
- **Test Data**: Programmatically created in tests

### Security Configuration
- **Test Authentication**: Simplified for testing
- **JWT Testing**: Mock tokens for authenticated endpoints
- **Role-Based Testing**: Employee vs Customer scenarios

### Test Properties
```properties
# Test Database
spring.datasource.url=jdbc:h2:mem:testdb
spring.jpa.hibernate.ddl-auto=create-drop

# JWT for Testing
jwt.secret=testSecretKeyForJWTTokenThatIsLongEnoughForHS256Algorithm
jwt.expiration=86400000

# Logging
logging.level.com.bankapp=DEBUG
```

## Coverage Goals

- **Line Coverage**: 90%+
- **Branch Coverage**: 85%+
- **Method Coverage**: 95%+

### Coverage Reports
Reports are generated in:
- `target/site/jacoco/index.html` - HTML coverage report
- `target/cucumber-reports/` - Cucumber test reports

## Test Scenarios Covered

### Happy Path Scenarios
- ✅ User registration and approval
- ✅ Account creation and management
- ✅ Successful money transfers
- ✅ ATM operations (deposit/withdraw)
- ✅ Transaction history viewing

### Error Conditions
- ✅ Invalid credentials
- ✅ Insufficient balance
- ✅ Non-existent accounts
- ✅ Unauthorized access attempts
- ✅ Invalid input validation
- ✅ Closed/unapproved accounts

### Security Tests
- ✅ Authentication required
- ✅ Role-based authorization
- ✅ Cross-user access prevention
- ✅ JWT token validation
- ✅ PIN verification (ATM)

### Edge Cases
- ✅ Zero and negative amounts
- ✅ Very large transactions
- ✅ Empty/null descriptions
- ✅ Invalid IBAN formats
- ✅ Concurrent operations
- ✅ Special characters in data

### Business Logic Validation
- ✅ Account approval workflow
- ✅ Transfer between account types
- ✅ Daily/absolute limits
- ✅ Account closure effects
- ✅ Transaction history ordering

## Best Practices Implemented

### Test Structure
- ✅ **AAA Pattern**: Arrange, Act, Assert
- ✅ **Nested Test Classes**: Logical grouping
- ✅ **Display Names**: Descriptive test names
- ✅ **Test Data Builders**: Reusable test objects

### Mocking Strategy
- ✅ **Service Layer Mocking**: Isolated unit tests
- ✅ **Repository Mocking**: Database abstraction
- ✅ **Security Context Mocking**: Authentication simulation
- ✅ **External Service Mocking**: PIN utilities, etc.

### Assertions
- ✅ **Specific Assertions**: Exact error messages
- ✅ **State Verification**: Database changes
- ✅ **Behavior Verification**: Method calls
- ✅ **Exception Testing**: Error conditions

### Test Isolation
- ✅ **Transaction Rollback**: Database cleanup
- ✅ **Independent Tests**: No test dependencies
- ✅ **Fresh Context**: Clean state per test
- ✅ **Deterministic Data**: Predictable test conditions

## Troubleshooting

### Common Issues

1. **Test Database Issues**
   ```bash
   # Clear target directory if tests fail
   mvn clean test
   ```

2. **Authentication Failures**
   - Check JWT secret configuration
   - Verify test user setup
   - Ensure proper role assignment

3. **Coverage Issues**
   ```bash
   # Generate fresh coverage report
   mvn clean test jacoco:report
   ```

4. **Cucumber Step Issues**
   - Verify step definition imports
   - Check feature file syntax
   - Ensure Spring context loading

### Running Specific Test Methods
```bash
# Run specific test method
mvn test -Dtest=TransactionServiceTest#shouldTransferMoneySuccessfully

# Run tests matching pattern
mvn test -Dtest=TransactionServiceTest#*Transfer*
```

## Performance Considerations

- **Fast Unit Tests**: < 100ms per test
- **Medium Integration Tests**: < 500ms per test
- **Comprehensive BDD Tests**: < 2s per scenario
- **Total Suite Runtime**: < 30 seconds

## Continuous Integration

### GitHub Actions / CI Pipeline
```yaml
- name: Run Tests
  run: mvn clean test

- name: Generate Coverage Report
  run: mvn jacoco:report

- name: Check Coverage Thresholds
  run: mvn jacoco:check
```

### Quality Gates
- All tests must pass
- Coverage thresholds must be met
- No test flakiness tolerated
- Security tests must pass

## Future Enhancements

- [ ] Performance tests with load simulation
- [ ] Contract testing with Pact
- [ ] Mutation testing with PIT
- [ ] Property-based testing with jqwik
- [ ] Database migration testing
- [ ] API documentation testing