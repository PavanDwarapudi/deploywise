package com.deploywise.model;

/** One log line that matched a rule. */
public record Finding(int lineNumber, String line, FailureCategory category, String cause, String suggestion) {
}
