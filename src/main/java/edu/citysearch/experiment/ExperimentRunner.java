package edu.citysearch.experiment;

import edu.citysearch.edit.*;
import edu.citysearch.search.*;
import edu.citysearch.metrics.Measurements;
import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.*;
import java.util.*;
import java.util.function.*;

public final class ExperimentRunner {
    public static final int[] SMALL_ED = {2, 4, 6, 8, 10, 12};
    public static final int[] LARGE_ED = {64, 128, 256, 512, 1024};
    public static final int[] SEARCH = {4096, 8192, 16384, 32768, 65536};
    public static final int SAMPLES = 5, WARMUPS = 3;
    public static final long MIN_BATCH_NS = 20_000_000L;
    private static volatile long blackhole;
    private ExperimentRunner() {}

    public static void run(Path directory) throws IOException {
        Files.createDirectories(directory);
        writeEnvironment(directory);
        try (PrintWriter csv = new PrintWriter(Files.newBufferedWriter(directory.resolve("results.csv"), StandardCharsets.UTF_8))) {
            csv.println("family,algorithm,input,n,m,run,seed,input_sha256,result,batch_iterations,batch_elapsed_ms,time_ms,recursive_calls,dp_cells,char_comparisons,lps_comparisons,total_comparisons,allocated_int_slots,max_recursion_depth");
            for (String type : List.of("random", "disjoint")) {
                for (int n : SMALL_ED) editCase(csv, type, n, true);
                for (int n : LARGE_ED) editCase(csv, type, n, false);
            }
            for (String type : List.of("random", "late_mismatch", "early_mismatch")) {
                for (int n : SEARCH) searchCase(csv, type, n, n / 64);
            }
            if (csv.checkError()) throw new IOException("Could not write results.csv");
        }
        System.out.println("Saved " + directory.resolve("results.csv") + " (430 timed samples).");
    }

    private static void editCase(PrintWriter csv, String type, int n, boolean includeRecursion) {
        InputGenerator.Pair input = InputGenerator.edit(type, n);
        List<EditDistance> algorithms = new ArrayList<>();
        if (includeRecursion) algorithms.add(new RecursiveEditDistance());
        algorithms.add(new TableEditDistance()); algorithms.add(new TwoRowEditDistance());
        int expected = new TableEditDistance().distance(input.first(), input.second());
        for (EditDistance algorithm : algorithms) {
            measure(csv, "edit", algorithm.name(), type, n, n, input, expected,
                    () -> algorithm.distance(input.first(), input.second()), algorithm::measurements);
        }
    }

    private static void searchCase(PrintWriter csv, String type, int n, int m) {
        InputGenerator.Pair input = InputGenerator.search(type, n, m);
        List<Integer> expectedMatches = new NaiveSearch().search(input.first(), input.second());
        for (StringSearch algorithm : List.of(new NaiveSearch(), new KmpSearch())) {
            if (!expectedMatches.equals(algorithm.search(input.first(), input.second())))
                throw new IllegalStateException("Search lists disagree");
            measure(csv, "search", algorithm.name(), type, n, m, input, expectedMatches.size(),
                    () -> {
                        List<Integer> matches = algorithm.search(input.first(), input.second());
                        blackhole ^= matches.hashCode();
                        return matches.size();
                    }, algorithm::measurements);
        }
    }

    private static void measure(PrintWriter csv, String family, String algorithm, String type,
                                int n, int m, InputGenerator.Pair input, int expected,
                                IntSupplier operation, Supplier<Measurements> counters) {
        for (int i = 0; i < WARMUPS; i++) checkedCall(operation, expected);
        Measurements canonical = counters.get();
        String fingerprint = InputGenerator.fingerprint(input);
        for (int run = 1; run <= SAMPLES; run++) {
            long iterations = 0, elapsed;
            long start = System.nanoTime();
            do {
                checkedCall(operation, expected);
                iterations++;
                elapsed = System.nanoTime() - start;
            } while (elapsed < MIN_BATCH_NS);
            Measurements measured = counters.get();
            if (!canonical.equals(measured)) throw new IllegalStateException("Counters changed on identical input");
            double batchMs = elapsed / 1_000_000.0;
            csv.printf(Locale.ROOT,
                    "%s,%s,%s,%d,%d,%d,%d,%s,%d,%d,%.9f,%.9f,%d,%d,%d,%d,%d,%d,%d%n",
                    family, algorithm, type, n, m, run, InputGenerator.SEED, fingerprint, expected,
                    iterations, batchMs, batchMs / iterations, measured.recursiveCalls(), measured.dpCells(),
                    measured.comparisons(), measured.lpsComparisons(), measured.totalComparisons(),
                    measured.allocatedIntSlots(), measured.maxRecursionDepth());
        }
        csv.flush();
        System.out.printf(Locale.ROOT, "%-6s %-10s %-14s n=%-6d states=%-12d comparisons=%d%n",
                family, algorithm, type, n, canonical.states(), canonical.totalComparisons());
    }

    private static void checkedCall(IntSupplier operation, int expected) {
        int value = operation.getAsInt();
        if (value != expected) throw new IllegalStateException("Incorrect result during experiment");
        blackhole ^= value;
    }

    private static void writeEnvironment(Path directory) throws IOException {
        String text = "# Measurement environment\n\n"
                + "- Java: " + System.getProperty("java.runtime.version") + "\n"
                + "- VM: " + System.getProperty("java.vm.name") + "\n"
                + "- OS: " + System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " / " + System.getProperty("os.arch") + "\n"
                + "- Available processors: " + Runtime.getRuntime().availableProcessors() + "\n"
                + "- Maximum heap bytes: " + Runtime.getRuntime().maxMemory() + "\n"
                + "- Seed: " + InputGenerator.SEED + "\n"
                + "- Warm-ups: " + WARMUPS + " per case; samples: " + SAMPLES + "; minimum batch: 20 ms\n"
                + "- Serial execution; all algorithms instrumented; KMP timing includes LPS.\n"
                + "- CPU model: record separately in README; no personal machine identifiers collected.\n";
        Files.writeString(directory.resolve("environment.md"), text, StandardCharsets.UTF_8);
    }
}
