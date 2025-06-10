package com.bankapp.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * Runner class specifically for search customer Cucumber tests
 * This configures and runs the search_customer.feature
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features/search_customer.feature")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.bankapp.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-reports/search-report.html, json:target/cucumber-reports/SearchCustomer.json, junit:target/cucumber-reports/SearchCustomer.xml")
public class SearchCustomerRunner {
    // This class serves as the entry point for running the search customer Cucumber tests
}