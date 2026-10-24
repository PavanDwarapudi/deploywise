package com.deploywise.service;

import com.deploywise.model.FailureCategory;

import java.util.regex.Pattern;

/** A rule maps a log pattern to a failure category, a likely cause and a debugging step. */
record Rule(FailureCategory category, Pattern pattern, String cause, String suggestion) {

    static Rule of(FailureCategory category, String regex, String cause, String suggestion) {
        return new Rule(category, Pattern.compile(regex, Pattern.CASE_INSENSITIVE), cause, suggestion);
    }
}
