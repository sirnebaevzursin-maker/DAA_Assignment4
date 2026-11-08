package edu.citysearch.edit;

import edu.citysearch.metrics.Counters;
import edu.citysearch.metrics.Measurements;
import java.util.Objects;

public final class RecursiveEditDistance implements EditDistance {
    private final Counters counts = new Counters();

    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a"); Objects.requireNonNull(b, "b");
        counts.reset();
        return solve(a, b, a.length(), b.length(), 1);
    }

    private int solve(String a, String b, int i, int j, int depth) {
        counts.recursiveCall(depth);
        if (i == 0) return j;
        if (j == 0) return i;
        counts.compare();
        if (a.charAt(i - 1) == b.charAt(j - 1))
            return solve(a, b, i - 1, j - 1, depth + 1);
        int deletion = solve(a, b, i - 1, j, depth + 1);
        int insertion = solve(a, b, i, j - 1, depth + 1);
        int replacement = solve(a, b, i - 1, j - 1, depth + 1);
        return 1 + Math.min(deletion, Math.min(insertion, replacement));
    }

    @Override public Measurements measurements() { return counts.snapshot(); }
    @Override public String name() { return "recursive"; }
}
