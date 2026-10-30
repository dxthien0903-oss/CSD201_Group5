package edu.csd201.sort;

public final class SortMetrics {
    private final boolean collectionEnabled;
    private int maxDepth;
    private long comparisonCount;
    private long partitionCount;
    private long heapFallbackCount;
    private long partialInsertionCount;

    public SortMetrics() {
        this(true);
    }

    SortMetrics(boolean collectionEnabled) {
        this.collectionEnabled = collectionEnabled;
    }

    void recordComparison() {
        if (collectionEnabled) {
            comparisonCount++;
        }
    }

    void recordDepth(int depth) {
        if (collectionEnabled) {
            maxDepth = Math.max(maxDepth, depth);
        }
    }

    void recordPartition() {
        if (collectionEnabled) {
            partitionCount++;
        }
    }

    void recordHeapFallback() {
        if (collectionEnabled) {
            heapFallbackCount++;
        }
    }

    void recordPartialInsertion() {
        if (collectionEnabled) {
            partialInsertionCount++;
        }
    }

    public int maxDepth() {
        return maxDepth;
    }

    public long comparisonCount() {
        return comparisonCount;
    }

    public long partitionCount() {
        return partitionCount;
    }

    public long heapFallbackCount() {
        return heapFallbackCount;
    }

    public long partialInsertionCount() {
        return partialInsertionCount;
    }
}