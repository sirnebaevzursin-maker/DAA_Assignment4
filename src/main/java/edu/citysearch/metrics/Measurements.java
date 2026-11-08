package edu.citysearch.metrics;

public record Measurements(long recursiveCalls, long dpCells, long comparisons,
                           long lpsComparisons, long allocatedIntSlots,
                           long maxRecursionDepth) {
    public long states() { return recursiveCalls + dpCells; }
    public long totalComparisons() { return comparisons + lpsComparisons; }
}
