package edu.citysearch.metrics;

/** Immutable, per-invocation measurements; counts refer to UTF-16 code units. */
public record Measurements(long recursiveCalls, long dpCells, long comparisons,
                           long lpsComparisons, long allocatedIntSlots,
                           long maxRecursionDepth) {
    public long states() { return recursiveCalls + dpCells; }
    public long totalComparisons() { return comparisons + lpsComparisons; }
}
