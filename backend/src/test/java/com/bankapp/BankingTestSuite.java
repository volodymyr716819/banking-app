package com.bankapp;

import org.junit.platform.suite.api.SelectClasses;
import org.junit.platform.suite.api.Suite;

import com.bankapp.controller.AccountControllerTest;
import com.bankapp.integration.BankingApplicationIntegrationTest;
// import com.bankapp.controller.TransactionControllerTest;
// import com.bankapp.controller.AuthControllerTest;
// import com.bankapp.controller.UserControllerTest;
// import com.bankapp.controller.AtmOperationControllerTest;

/**
 * Test suite that groups all integration level tests.
 */
@Suite
@SelectClasses({
    BankingApplicationIntegrationTest.class,
    AccountControllerTest.class,
    // TransactionControllerTest.class,
    // AuthControllerTest.class,
    // UserControllerTest.class,
    // AtmOperationControllerTest.class
})
public class BankingTestSuite {
    // this class remains empty, it is used only as a holder for the above annotations
}