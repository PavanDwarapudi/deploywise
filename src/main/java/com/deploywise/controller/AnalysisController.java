package com.deploywise.controller;

import com.deploywise.model.AnalysisResult;
import com.deploywise.model.FailureCategory;
import com.deploywise.service.AnalysisStore;
import com.deploywise.service.LogAnalyzer;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class AnalysisController {

    private final LogAnalyzer analyzer;
    private final AnalysisStore store;

    public AnalysisController(LogAnalyzer analyzer, AnalysisStore store) {
        this.analyzer = analyzer;
        this.store = store;
    }

    /** Analyse a build log and remember the result. */
    @PostMapping("/analyze")
    public ResponseEntity<AnalysisResult> analyze(@Valid @RequestBody AnalyzeRequest request) {
        AnalysisResult result = analyzer.analyze(store.nextId(), request.log());
        store.save(result);
        return ResponseEntity.status(HttpStatus.CREATED).body(result);
    }

    /** Past analyses, newest first. */
    @GetMapping("/analyses")
    public List<AnalysisResult> list() {
        return store.findAll();
    }

    @GetMapping("/analyses/{id}")
    public ResponseEntity<AnalysisResult> get(@PathVariable long id) {
        return store.findAll().stream()
                .filter(r -> r.id() == id)
                .findFirst()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    /** Failure counts per category, for the dashboard. */
    @GetMapping("/stats")
    public Map<String, Object> stats() {
        Map<String, Integer> byCategory = new LinkedHashMap<>();
        for (FailureCategory c : FailureCategory.values()) {
            byCategory.put(c.name(), 0);
        }
        store.countByPrimaryCategory().forEach((c, n) -> byCategory.put(c.name(), n));
        int total = byCategory.values().stream().mapToInt(Integer::intValue).sum();
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("totalAnalyses", total);
        body.put("byCategory", byCategory);
        return body;
    }
}
