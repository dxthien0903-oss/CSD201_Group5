package edu.csd201.sort;

public final class SortMetrics {
    private int maxDepth;
    private long partitionCount;
    private long heapFallbackCount;
    private long partialInsertionCount;

    void recordDepth(int depth) {
        maxDepth = Math.max(maxDepth, depth);
    }

    void recordPartition() {
        partitionCount++;
    }

    void recordHeapFallback() {
        heapFallbackCount++;
    }

    void recordPartialInsertion() {
        partialInsertionCount++;
    }

    public int maxDepth() {
        return maxDepth;
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