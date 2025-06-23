package com.bankapp.cucumber;

import org.junit.platform.suite.api.ConfigurationParameter;
import org.junit.platform.suite.api.IncludeEngines;
import org.junit.platform.suite.api.SelectClasspathResource;
import org.junit.platform.suite.api.Suite;

import static io.cucumber.junit.platform.engine.Constants.GLUE_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.PLUGIN_PROPERTY_NAME;
import static io.cucumber.junit.platform.engine.Constants.FILTER_TAGS_PROPERTY_NAME;

/**
 * Runner class for Cucumber tests using JUnit 5.
 * This configures and runs the Cucumber features found in the classpath resources.
 * You can filter which scenarios to run using -Dcucumber.filter.tags=@tagname
 */
@Suite
@IncludeEngines("cucumber")
@SelectClasspathResource("features")
@ConfigurationParameter(key = GLUE_PROPERTY_NAME, value = "com.bankapp.cucumber")
@ConfigurationParameter(
    key = PLUGIN_PROPERTY_NAME,
    value = "pretty, html:target/cucumber-reports/report.html, json:target/cucumber-reports/Cucumber.json, junit:target/cucumber-reports/Cucumber.xml"
)
// Default tag filter is empty. You can override with: -Dcucumber.filter.tags=@yourtag
@ConfigurationParameter(key = FILTER_TAGS_PROPERTY_NAME, value = "")
public class CucumberRunner {
    // Entry point for running Cucumber tests with JUnit 5 and tag filtering support
}