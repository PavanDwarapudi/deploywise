package com.deploywise.service;

import com.deploywise.model.AnalysisResult;
import com.deploywise.model.FailureCategory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

/** In-memory store for past analyses (keeps the latest 100). Swap for a database later. */
@Component
public class AnalysisStore {

    private static final int MAX_ENTRIES = 100;

    private final AtomicLong ids = new AtomicLong();
    private final LinkedList<AnalysisResult> results = new LinkedList<>();

    public long nextId() {
        return ids.incrementAndGet();
    }

    public synchronized void save(AnalysisResult result) {
        results.addFirst(result);
        if (results.size() > MAX_ENTRIES) {
            results.removeLast();
        }
    }

    /** Newest first. */
    public synchronized List<AnalysisResult> findAll() {
        return Collections.unmodifiableList(new ArrayList<>(results));
    }

    /** How many analyses had each primary category. */
    public synchronized Map<FailureCategory, Integer> countByPrimaryCategory() {
        Map<FailureCategory, Integer> counts = new EnumMap<>(FailureCategory.class);
        for (AnalysisResult r : results) {
            counts.merge(r.primaryCategory(), 1, Integer::sum);
        }
        return counts;
    }
}
