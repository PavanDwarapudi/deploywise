package com.deploywise.service;

import com.deploywise.model.AnalysisResult;
import com.deploywise.model.FailureCategory;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LogAnalyzerTest {

    private final LogAnalyzer analyzer = new LogAnalyzer();

    @Test
    void detectsCompilationError() {
        AnalysisResult r = analyzer.analyze(1, "[ERROR] COMPILATION ERROR :\n[ERROR] cannot find symbol");
        assertEquals(FailureCategory.COMPILATION_ERROR, r.primaryCategory());
        assertEquals(2, r.findings().size());
        assertEquals(1, r.findings().get(0).lineNumber());
    }

    @Test
    void detectsDependencyIssue() {
        AnalysisResult r = analyzer.analyze(2, "[ERROR] Could not find artifact org.x:y:jar:1.0 in central");
        assertEquals(FailureCategory.DEPENDENCY_ISSUE, r.primaryCategory());
    }

    @Test
    void detectsTestFailure() {
        AnalysisResult r = analyzer.analyze(3, "[ERROR] Tests run: 5, Failures: 2, Errors: 0, Skipped: 0");
        assertEquals(FailureCategory.TEST_FAILURE, r.primaryCategory());
    }

    @Test
    void doesNotFlagPassingTests() {
        AnalysisResult r = analyzer.analyze(4, "[INFO] Tests run: 5, Failures: 0, Errors: 0, Skipped: 0\n[INFO] BUILD SUCCESS");
        assertEquals(FailureCategory.UNKNOWN, r.primaryCategory());
        assertTrue(r.findings().isEmpty());
    }

    @Test
    void earlierStageWinsWhenSeveralCategoriesAppear() {
        String log = "[ERROR] cannot find symbol\n[ERROR] Tests run: 3, Failures: 1, Errors: 0";
        AnalysisResult r = analyzer.analyze(5, log);
        assertEquals(FailureCategory.COMPILATION_ERROR, r.primaryCategory());
        assertEquals(1, r.categoryCounts().get(FailureCategory.TEST_FAILURE));
    }

    @Test
    void unknownLogGivesGuidance() {
        AnalysisResult r = analyzer.analyze(6, "all good\nnothing to see");
        assertEquals(FailureCategory.UNKNOWN, r.primaryCategory());
        assertTrue(r.summary().contains("No known failure pattern"));
    }

    @Test
    void handlesNullAndEmptyLog() {
        assertEquals(0, analyzer.analyze(7, null).totalLines());
        assertEquals(FailureCategory.UNKNOWN, analyzer.analyze(8, "").primaryCategory());
    }
}
