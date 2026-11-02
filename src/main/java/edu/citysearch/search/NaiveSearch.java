package edu.citysearch.search;

import edu.citysearch.metrics.Counters;
import edu.citysearch.metrics.Measurements;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class NaiveSearch implements StringSearch {
    private final Counters counts = new Counters();

    @Override public List<Integer> search(String text, String pattern) {
        Objects.requireNonNull(text, "text"); Objects.requireNonNull(pattern, "pattern");
        counts.reset();
        if (pattern.isEmpty()) throw new IllegalArgumentException("Pattern must not be empty");
        List<Integer> matches = new ArrayList<>();
        for (int start = 0; start <= text.length() - pattern.length(); start++) {
            int j = 0;
            while (j < pattern.length()) {
                counts.compare();
                if (text.charAt(start + j) != pattern.charAt(j)) break;
                j++;
            }
            if (j == pattern.length()) matches.add(start);
        }
        return matches;
    }

    @Override public Measurements measurements() { return counts.snapshot(); }
    @Override public String name() { return "naive"; }
}
