package com.deploywise.service;

import com.deploywise.model.AnalysisResult;
import com.deploywise.model.FailureCategory;
import com.deploywise.model.Finding;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

/** Rule-based analyser that classifies the failures found in a CI/CD build log. */
@Service
public class LogAnalyzer {

    private static final int MAX_LINE_LENGTH = 300;

    private final List<Rule> rules;

    public LogAnalyzer() {
        this.rules = RuleBook.RULES;
    }

    /** Analyses the log; the id is assigned by the caller. */
    public AnalysisResult analyze(long id, String log) {
        String[] lines = log == null ? new String[0] : log.split("\\R");
        List<Finding> findings = new ArrayList<>();
        Map<FailureCategory, Integer> counts = new EnumMap<>(FailureCategory.class);

        for (int i = 0; i < lines.length; i++) {
            String line = lines[i];
            if (line.isBlank()) {
                continue;
            }
            for (Rule rule : rules) {
                if (rule.pattern().matcher(line).find()) {
                    findings.add(new Finding(i + 1, trim(line), rule.category(), rule.cause(), rule.suggestion()));
                    counts.merge(rule.category(), 1, Integer::sum);
                    break; // first matching rule wins for a line
                }
            }
        }

        FailureCategory primary = pickPrimary(counts);
        if (primary == FailureCategory.UNKNOWN) {
            counts.put(FailureCategory.UNKNOWN, 1);
        }
        return new AnalysisResult(id, Instant.now(), lines.length, primary, summarize(primary, findings),
                counts, findings);
    }

    /** The highest-priority category found (enum order), because an earlier stage failing causes later ones. */
    private FailureCategory pickPrimary(Map<FailureCategory, Integer> counts) {
        for (FailureCategory category : FailureCategory.values()) {
            if (category != FailureCategory.UNKNOWN && counts.containsKey(category)) {
                return category;
            }
        }
        return FailureCategory.UNKNOWN;
    }

    private String summarize(FailureCategory primary, List<Finding> findings) {
        if (primary == FailureCategory.UNKNOWN) {
            return "No known failure pattern was found. Check the last 50 lines of the log for the first error.";
        }
        Finding first = findings.stream().filter(f -> f.category() == primary).findFirst().orElseThrow();
        return primary.getLabel() + " at line " + first.lineNumber() + ". " + first.cause() + " " + first.suggestion();
    }

    private String trim(String line) {
        String t = line.strip();
        return t.length() > MAX_LINE_LENGTH ? t.substring(0, MAX_LINE_LENGTH) + "..." : t;
    }
}
