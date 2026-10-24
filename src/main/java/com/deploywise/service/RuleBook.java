package com.deploywise.service;

import com.deploywise.model.FailureCategory;

import java.util.List;

/** The built-in rules. Add a line here to teach DeployWise a new failure pattern. */
final class RuleBook {

    private RuleBook() {
    }

    static final List<Rule> RULES = List.of(
            // Compilation
            Rule.of(FailureCategory.COMPILATION_ERROR, "COMPILATION ERROR|compilation failure|error: cannot find symbol|cannot find symbol",
                    "The code does not compile; a symbol, class or method is missing or misspelled.",
                    "Open the file and line shown above the error, check imports and spelling, then rebuild locally."),
            Rule.of(FailureCategory.COMPILATION_ERROR, "error: incompatible types|incompatible types",
                    "A value is assigned to a variable of a different type.",
                    "Check the types on both sides of the assignment or add an explicit conversion."),
            Rule.of(FailureCategory.COMPILATION_ERROR, "error: ';' expected|illegal start of expression|class, interface, or enum expected|SyntaxError",
                    "A syntax error such as a missing semicolon, bracket or stray character.",
                    "Look at the line before the reported one for a missing ';' or '}'."),
            Rule.of(FailureCategory.COMPILATION_ERROR, "error TS\\d+|error CS\\d+|undefined reference to",
                    "The compiler or linker reported an error.",
                    "Read the first error only; later ones are often caused by it."),

            // Dependencies
            Rule.of(FailureCategory.DEPENDENCY_ISSUE, "could not resolve dependencies|could not find artifact|failed to collect dependencies|dependency resolution failed",
                    "Maven or Gradle could not download a required dependency.",
                    "Check the artifact name and version in pom.xml, then check the repository URL and network access."),
            Rule.of(FailureCategory.DEPENDENCY_ISSUE, "npm ERR! 404|npm ERR! code E404|npm ERR! code ERESOLVE|unable to resolve dependency tree",
                    "npm could not find a package or the versions conflict.",
                    "Verify the package name and version in package.json, or reinstall with a clean lock file."),
            Rule.of(FailureCategory.DEPENDENCY_ISSUE, "ModuleNotFoundError|No module named|ImportError|Could not find a version that satisfies",
                    "A Python package is missing or the wrong version is requested.",
                    "Add the package to requirements.txt and pin a version that exists."),
            Rule.of(FailureCategory.DEPENDENCY_ISSUE, "package .* does not exist|NoClassDefFoundError|ClassNotFoundException",
                    "A class or package is missing at compile or run time.",
                    "Check that the dependency is declared with the right scope and that the version is compatible."),

            // Tests
            Rule.of(FailureCategory.TEST_FAILURE, "Tests run:.*Failures: [1-9]|Tests run:.*Errors: [1-9]|There are test failures|\\d+ tests? failed|FAILED \\(failures",
                    "One or more automated tests failed.",
                    "Open the surefire/test report, find the first failing test, and reproduce it locally."),
            Rule.of(FailureCategory.TEST_FAILURE, "AssertionError|AssertionFailedError|expected:<.*> but was:<.*>|ComparisonFailure",
                    "An assertion did not hold: the actual result differs from the expected one.",
                    "Compare the expected and actual values in the message and check whether the code or the test is wrong."),
            Rule.of(FailureCategory.TEST_FAILURE, "FAIL\\s+\\S+\\.(test|spec)\\.|Test failed|test suite failed",
                    "A test suite reported a failure.",
                    "Run the failing suite locally with verbose output."),

            // Memory
            Rule.of(FailureCategory.OUT_OF_MEMORY, "OutOfMemoryError|Java heap space|JavaScript heap out of memory|Killed process|exit code 137",
                    "The build or test process ran out of memory.",
                    "Raise the heap limit (for example -Xmx) or the runner's memory, and look for memory-heavy tests."),

            // Permissions
            Rule.of(FailureCategory.PERMISSION_ERROR, "permission denied|access denied|403 forbidden|401 unauthorized|authentication failed",
                    "The job lacks permission or credentials for a file, registry or service.",
                    "Check the secrets, token scopes and file permissions used by this step."),

            // Timeouts
            Rule.of(FailureCategory.TIMEOUT, "timed out|timeout exceeded|deadline exceeded|connection timed out|ETIMEDOUT",
                    "A step took longer than its allowed time or a network call hung.",
                    "Check network access, increase the step timeout, or find the slow step in the log timestamps.")
    );
}
