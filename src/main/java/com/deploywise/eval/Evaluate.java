package com.deploywise.eval;

import com.deploywise.model.AnalysisResult;
import com.deploywise.model.FailureCategory;
import com.deploywise.service.LogAnalyzer;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

/**
 * Command-line tool to measure how accurate the analyzer is on real logs.
 *
 * <pre>
 *   java -cp target/classes com.deploywise.eval.Evaluate init logs labels.csv   (create or extend the labels file)
 *   java -cp target/classes com.deploywise.eval.Evaluate eval logs labels.csv   (measure accuracy)
 * </pre>
 * labels.csv has the columns: file,true_category,set. Fill true_category by reading each log yourself.
 * The set column is "tune" (logs you may use to improve the rules) or "test" (logs you must not tune on).
 */
public class Evaluate {

    private static final int MIN_BYTES = 300;

    public static void main(String[] args) throws IOException {
        if (args.length != 3 || !(args[0].equals("init") || args[0].equals("eval"))) {
            System.out.println("Usage: Evaluate <init|eval> <logsDir> <labelsFile>");
            return;
        }
        Path logsDir = Path.of(args[1]);
        Path labelsFile = Path.of(args[2]);
        if (args[0].equals("init")) {
            init(logsDir, labelsFile);
        } else {
            evaluate(logsDir, labelsFile);
        }
    }

    private static void init(Path logsDir, Path labelsFile) throws IOException {
        Map<String, String[]> existing = readLabels(labelsFile, false);
        List<Path> files = new ArrayList<>();
        try (Stream<Path> s = Files.list(logsDir)) {
            s.filter(Files::isRegularFile).sorted().forEach(files::add);
        }
        List<String> lines = new ArrayList<>();
        lines.add("file,true_category,set");
        int skipped = 0;
        int index = 0;
        for (Path f : files) {
            String name = f.getFileName().toString();
            if (Files.size(f) < MIN_BYTES) {
                skipped++;
                System.out.println("Skipped (too small, under " + MIN_BYTES + " bytes): " + name);
                continue;
            }
            String[] old = existing.get(name);
            String label = old != null ? old[1] : "";
            String set = old != null && !old[2].isEmpty() ? old[2] : (index % 2 == 0 ? "tune" : "test");
            lines.add(name + "," + label + "," + set);
            index++;
        }
        Files.write(labelsFile, lines, StandardCharsets.UTF_8);
        System.out.println("Wrote " + (lines.size() - 1) + " rows to " + labelsFile + " (" + skipped + " files skipped).");
        System.out.println("Now open it and fill true_category for every row. Valid values: " + validNames());
    }

    private static void evaluate(Path logsDir, Path labelsFile) throws IOException {
        Map<String, String[]> labels = readLabels(labelsFile, true);
        LogAnalyzer analyzer = new LogAnalyzer();
        List<String> out = new ArrayList<>();
        out.add("file,true_category,predicted,correct,set");
        Map<String, int[]> bySet = new LinkedHashMap<>();
        Map<String, int[]> byCategory = new LinkedHashMap<>();
        List<String> mistakes = new ArrayList<>();
        int total = 0;
        int correct = 0;

        for (Map.Entry<String, String[]> e : labels.entrySet()) {
            String file = e.getKey();
            String truth = e.getValue()[1];
            String set = e.getValue()[2].isEmpty() ? "all" : e.getValue()[2];
            if (truth.isEmpty()) {
                continue; // not labelled yet
            }
            Path p = logsDir.resolve(file);
            if (!Files.exists(p)) {
                System.out.println("Missing file, skipped: " + file);
                continue;
            }
            String predicted = analyzer.analyze(0, decode(Files.readAllBytes(p))).primaryCategory().name();
            boolean ok = predicted.equals(truth);
            total++;
            if (ok) {
                correct++;
            } else {
                mistakes.add(file + "  true=" + truth + "  predicted=" + predicted);
            }
            bump(bySet, set, ok);
            bump(byCategory, truth, ok);
            out.add(file + "," + truth + "," + predicted + "," + ok + "," + set);
        }

        Files.write(Path.of("eval-results.csv"), out, StandardCharsets.UTF_8);
        if (total == 0) {
            System.out.println("No labelled rows found. Fill true_category in " + labelsFile + " first.");
            return;
        }
        System.out.printf("%nOverall accuracy: %d / %d = %.1f%%%n", correct, total, 100.0 * correct / total);
        System.out.println("\nBy set (report the 'test' number as your honest result):");
        bySet.forEach((k, v) -> System.out.printf("  %-6s %d / %d = %.1f%%%n", k, v[0], v[1], 100.0 * v[0] / v[1]));
        System.out.println("\nBy true category (correct / total):");
        byCategory.forEach((k, v) -> System.out.printf("  %-18s %d / %d%n", k, v[0], v[1]));
        System.out.println("\nMistakes (" + mistakes.size() + "):");
        mistakes.forEach(m -> System.out.println("  " + m));
        System.out.println("\nDetails saved to eval-results.csv");
    }

    private static void bump(Map<String, int[]> map, String key, boolean ok) {
        int[] v = map.computeIfAbsent(key, k -> new int[2]);
        if (ok) {
            v[0]++;
        }
        v[1]++;
    }

    /** Reads labels.csv into file -> [file, true_category, set]. */
    private static Map<String, String[]> readLabels(Path file, boolean mustExist) throws IOException {
        Map<String, String[]> map = new LinkedHashMap<>();
        if (!Files.exists(file)) {
            if (mustExist) {
                throw new IOException("Labels file not found: " + file + " (run the init command first)");
            }
            return map;
        }
        List<String> lines = Files.readAllLines(file, StandardCharsets.UTF_8);
        for (int i = 1; i < lines.size(); i++) {
            String line = lines.get(i).trim();
            if (line.isEmpty()) {
                continue;
            }
            String[] parts = line.split(",", -1);
            String name = parts[0].trim();
            String truth = parts.length > 1 ? parts[1].trim().toUpperCase() : "";
            String set = parts.length > 2 ? parts[2].trim().toLowerCase() : "";
            if (!truth.isEmpty()) {
                try {
                    FailureCategory.valueOf(truth);
                } catch (IllegalArgumentException ex) {
                    throw new IOException("Line " + (i + 1) + ": '" + truth + "' is not a valid category. Use one of: " + validNames());
                }
            }
            map.put(name, new String[]{name, truth, set});
        }
        return map;
    }

    private static String validNames() {
        StringBuilder sb = new StringBuilder();
        for (FailureCategory c : FailureCategory.values()) {
            sb.append(sb.length() == 0 ? "" : ", ").append(c.name());
        }
        return sb.toString();
    }

    /** Windows PowerShell often saves redirected output as UTF-16; detect that so the log is read correctly. */
    static String decode(byte[] b) {
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFF && (b[1] & 0xFF) == 0xFE) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16LE);
        }
        if (b.length >= 2 && (b[0] & 0xFF) == 0xFE && (b[1] & 0xFF) == 0xFF) {
            return new String(b, 2, b.length - 2, StandardCharsets.UTF_16BE);
        }
        if (b.length >= 3 && (b[0] & 0xFF) == 0xEF && (b[1] & 0xFF) == 0xBB && (b[2] & 0xFF) == 0xBF) {
            return new String(b, 3, b.length - 3, StandardCharsets.UTF_8);
        }
        return new String(b, StandardCharsets.UTF_8);
    }
}
