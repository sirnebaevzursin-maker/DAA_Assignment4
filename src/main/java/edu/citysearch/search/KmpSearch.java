package edu.citysearch.search;

import edu.citysearch.metrics.Counters;
import edu.citysearch.metrics.Measurements;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public final class KmpSearch implements StringSearch {
    private final Counters counts = new Counters();

    /** lps[i] is the longest proper prefix of p[0..i] that is also its suffix. */
    public int[] lps(String p) {
        Objects.requireNonNull(p, "pattern");
        counts.reset();
        return buildLps(p);
    }

    private int[] buildLps(String p) {
        int[] lps = new int[p.length()]; counts.allocateInts(lps.length);
        int i = 1, length = 0;
        while (i < p.length()) {
            counts.compareLps();
            if (p.charAt(i) == p.charAt(length)) {
                lps[i++] = ++length;
            } else if (length > 0) {
                length = lps[length - 1];
            } else {
                lps[i++] = 0;
            }
        }
        return lps;
    }

    @Override public List<Integer> search(String text, String pattern) {
        Objects.requireNonNull(text, "text"); Objects.requireNonNull(pattern, "pattern");
        counts.reset();
        if (pattern.isEmpty()) throw new IllegalArgumentException("Pattern must not be empty");
        List<Integer> matches = new ArrayList<>();
        if (pattern.length() > text.length()) return matches;
        int[] lps = buildLps(pattern);
        int i = 0, j = 0;
        while (i < text.length()) {
            counts.compare();
            if (text.charAt(i) == pattern.charAt(j)) {
                i++; j++;
                if (j == pattern.length()) {
                    matches.add(i - j);
                    j = lps[j - 1]; // Retain the border: overlapping matches remain possible.
                }
            } else if (j > 0) {
                j = lps[j - 1]; // Reuse the matched prefix; i never decreases.
            } else {
                i++;
            }
        }
        return matches;
    }

    @Override public Measurements measurements() { return counts.snapshot(); }
    @Override public String name() { return "kmp"; }
}
