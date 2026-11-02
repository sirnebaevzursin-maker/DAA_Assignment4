package edu.citysearch.edit;

import edu.citysearch.metrics.Counters;
import edu.citysearch.metrics.Measurements;
import java.util.Objects;

public final class TableEditDistance implements EditDistance {
    private final Counters counts = new Counters();

    // State: dp[i][j] is the distance between prefixes a[0..i) and b[0..j).
    // Transition: equal -> dp[i-1][j-1]; else 1+min(dp[i-1][j],dp[i][j-1],dp[i-1][j-1]).
    // Base: dp[0][j]=j, dp[i][0]=i; compute rows from top to bottom in O(n*m) time/space.
    @Override public int distance(String a, String b) {
        Objects.requireNonNull(a, "a"); Objects.requireNonNull(b, "b");
        counts.reset();
        int n = a.length(), m = b.length();
        int[][] dp = new int[n + 1][];
        for (int i = 0; i <= n; i++) {
            dp[i] = new int[m + 1];
            counts.allocateInts(dp[i].length);
        }
        for (int j = 0; j <= m; j++) { dp[0][j] = j; counts.cell(); }
        for (int i = 1; i <= n; i++) {
            dp[i][0] = i; counts.cell();
            for (int j = 1; j <= m; j++) {
                counts.cell(); counts.compare();
                dp[i][j] = a.charAt(i - 1) == b.charAt(j - 1)
                        ? dp[i - 1][j - 1]
                        : 1 + Math.min(dp[i - 1][j], Math.min(dp[i][j - 1], dp[i - 1][j - 1]));
            }
        }
        return dp[n][m];
    }

    @Override public Measurements measurements() { return counts.snapshot(); }
    @Override public String name() { return "table"; }
}
