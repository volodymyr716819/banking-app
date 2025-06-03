package com.bankapp.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;

/**
 * Runner class for Cucumber tests using JUnit 5
 * This configures and runs the Cucumber features found in the classpath resources
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.bankapp.cucumber")
@ConfigurationParameter(key = PLUGIN_PROPERTY_NAME, value = "pretty, html:target/cucumber-reports/report.html, json:target/cucumber-reports/Cucumber.json, junit:target/cucumber-reports/Cucumber.xml")
public class CucumberRunner {
    // This class serves as the entry point for running Cucumber tests with JUnit 5
}