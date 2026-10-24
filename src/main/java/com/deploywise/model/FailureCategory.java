package com.deploywise.model;

/** Categories of CI/CD failures. Declaration order is the priority used to pick the primary cause. */
public enum FailureCategory {
    COMPILATION_ERROR("Compilation error"),
    DEPENDENCY_ISSUE("Dependency issue"),
    TEST_FAILURE("Test failure"),
    OUT_OF_MEMORY("Out of memory"),
    PERMISSION_ERROR("Permission error"),
    TIMEOUT("Timeout"),
    UNKNOWN("Unknown");

    private final String label;

    FailureCategory(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
