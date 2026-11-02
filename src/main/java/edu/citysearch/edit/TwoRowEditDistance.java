package edu.citysearch.edit;

import edu.citysearch.metrics.Counters;
import edu.citysearch.metrics.Measurements;
import java.util.Objects;

public final class TwoRowEditDistance implements EditDistance {
    private final Counters counts = new Counters();

    // State: previous[j]=D(i-1,j), current[j]=D(i,j), distances between the corresponding prefixes.
    // Transition: equal -> previous[j-1]; else 1+min(previous[j],current[j-1],previous[j-1]); swap rows.
    // Base: previous[j]=j and current[0]=i; O(n*m) time, exactly two rows of O(m) extra space.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a"); Objects.requireNonNull(b, "b");
        counts.reset();
        int m = b.length();
        int[] previous = new int[m + 1]; counts.allocateInts(previous.length);
        int[] current = new int[m + 1]; counts.allocateInts(current.length);
        for (int j = 0; j <= m; j++) { previous[j] = j; counts.cell(); }
        for (int i = 1; i <= a.length(); i++) {
            current[0] = i; counts.cell();
            for (int j = 1; j <= m; j++) {
                counts.cell(); counts.compare();
                current[j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? previous[j - 1]
                        : 1 + Math.min(previous[j], Math.min(current[j - 1], previous[j - 1]));
            }
            int[] temporary = previous; previous = current; current = temporary;
        }
        return previous[m];
    }

    @Override public Measurements measurements() { return counts.snapshot(); }
    @Override public String name() { return "two_rows"; }
}
