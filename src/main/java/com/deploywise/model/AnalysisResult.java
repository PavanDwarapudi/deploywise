package com.deploywise.model;

import java.time.Instant;
import java.util.List;
import java.util.Map;

/** Result of analysing one build log. */
public record AnalysisResult(
        long id,
        Instant analyzedAt,
        int totalLines,
        FailureCategory primaryCategory,
        String summary,
        Map<FailureCategory, Integer> categoryCounts,
        List<Finding> findings) {
}
