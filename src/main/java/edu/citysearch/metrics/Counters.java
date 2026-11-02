package edu.citysearch.metrics;

/** Reset at each public algorithm call. Algorithm instances are not thread-safe. */
public final class Counters {
    private long calls, cells, comparisons, lpsComparisons, ints, depth;
    public void reset() { calls = cells = comparisons = lpsComparisons = ints = depth = 0; }
    public void recursiveCall(int currentDepth) {
        calls++;
        depth = Math.max(depth, currentDepth);
    }
    public void cell() { cells++; }
    public void compare() { comparisons++; }
    public void compareLps() { lpsComparisons++; }
    public void allocateInts(int length) { ints += length; }
    public Measurements snapshot() {
        return new Measurements(calls, cells, comparisons, lpsComparisons, ints, depth);
    }
}
