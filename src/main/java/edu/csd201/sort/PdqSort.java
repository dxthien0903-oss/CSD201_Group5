package edu.csd201.sort;

import java.util.Objects;

public final class PdqSort {
    private static final int INSERTION_SORT_THRESHOLD = 24;
    private static final int NINTHER_THRESHOLD = 128;
    private static final int PARTIAL_INSERTION_LIMIT = 8;

    private PdqSort() {
    }

    public static void sort(int[] values) {
        sort(values, true, true, new SortMetrics());
    }

    public static void sortWithoutMechanismA(int[] values) {
        sort(values, false, true, new SortMetrics());
    }

    public static void sortWithoutMechanismB(int[] values) {
        sort(values, true, false, new SortMetrics());
    }

    public static void sort(int[] values, boolean mechanismAEnabled, SortMetrics metrics) {
        sort(values, mechanismAEnabled, true, metrics);
    }

    public static void sort(int[] values, boolean mechanismAEnabled, boolean mechanismBEnabled, SortMetrics metrics) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(metrics, "metrics");
        if (values.length < 2) {
            return;
        }

        int log2Length = 31 - Integer.numberOfLeadingZeros(values.length);
        int badPartitionBudget = log2Length;
        int depthLimit = 2 * log2Length;
        sortRange(values, 0, values.length, badPartitionBudget, 1, depthLimit, false,
            true, mechanismAEnabled, mechanismBEnabled, metrics);
    }

    private static void sortRange(
            int[] values,
            int low,
            int high,
            int badPartitionBudget,
            int depth,
            int depthLimit,
            boolean breakPatternsBeforePartition,
            boolean leftmost,
            boolean mechanismAEnabled,
            boolean mechanismBEnabled,
            SortMetrics metrics) {
        metrics.recordDepth(depth);
        boolean breakPatternsOnNextPartition = breakPatternsBeforePartition;

        while (high - low >= INSERTION_SORT_THRESHOLD) {
            if (depth > depthLimit || badPartitionBudget <= 0) {
                HeapSort.sort(values, low, high, metrics);
                metrics.recordHeapFallback();
                return;
            }

            if (breakPatternsOnNextPartition) {
                breakPatterns(values, low, high);
                breakPatternsOnNextPartition = false;
            }
            int pivotIndex = choosePivot(values, low, high, metrics);
            int pivot = values[pivotIndex];

            if (mechanismBEnabled && !leftmost) {
                metrics.recordComparison();
                if (values[low - 1] == pivot) {
                    int equalEnd = partitionLeft(values, low, high, pivot, metrics);
                    metrics.recordPartition();
                    low = equalEnd;
                    continue;
                }
            }

            Partition partition = partitionRight(values, low, high, pivotIndex, pivot, metrics);
            metrics.recordPartition();

            int leftSize = partition.pivotIndex - low;
            int rightSize = high - (partition.pivotIndex + 1);
            int length = high - low;
            boolean balanced = Math.max(leftSize, rightSize) <= length - length / 8;
            if (!balanced) {
                badPartitionBudget--;
                breakPatternsOnNextPartition = true;
            }

            if (mechanismAEnabled && balanced && partition.alreadyPartitioned) {
                if (partialInsertionSort(values, low, partition.pivotIndex, metrics)
                        && partialInsertionSort(values, partition.pivotIndex + 1, high, metrics)) {
                    metrics.recordPartialInsertion();
                    return;
                }
            }

            if (leftSize < rightSize) {
                sortRange(values, low, partition.pivotIndex, badPartitionBudget, depth + 1, depthLimit,
                    breakPatternsOnNextPartition, leftmost, mechanismAEnabled, mechanismBEnabled, metrics);
                low = partition.pivotIndex + 1;
                leftmost = false;
            } else {
                sortRange(values, partition.pivotIndex + 1, high, badPartitionBudget, depth + 1, depthLimit,
                    breakPatternsOnNextPartition, false, mechanismAEnabled, mechanismBEnabled, metrics);
                high = partition.pivotIndex;
            }
        }

        insertionSort(values, low, high, metrics);
    }

    private static int choosePivot(int[] values, int low, int high, SortMetrics metrics) {
        int length = high - low;
        int middle = low + length / 2;
        if (length <= NINTHER_THRESHOLD) {
            return medianIndex(values, low, middle, high - 1, metrics);
        }

        int step = length / 8;
        int first = medianIndex(values, low, low + step, low + 2 * step, metrics);
        int second = medianIndex(values, middle - step, middle, middle + step, metrics);
        int third = medianIndex(values, high - 1 - 2 * step, high - 1 - step, high - 1, metrics);
        return medianIndex(values, first, second, third, metrics);
    }

    private static int medianIndex(int[] values, int first, int second, int third, SortMetrics metrics) {
        metrics.recordComparison();
        if (values[first] < values[second]) {
            metrics.recordComparison();
            if (values[second] < values[third]) {
                return second;
            }
            metrics.recordComparison();
            return values[first] < values[third] ? third : first;
        }
        metrics.recordComparison();
        if (values[first] < values[third]) {
            return first;
        }
        metrics.recordComparison();
        return values[second] < values[third] ? third : second;
    }

    private static Partition partitionRight(
            int[] values, int low, int high, int pivotIndex, int pivot, SortMetrics metrics) {
        swap(values, pivotIndex, high - 1);

        int first = low;
        int last = high - 1;
        boolean alreadyPartitioned = true;
        while (first < last) {
            metrics.recordComparison();
            if (values[first] < pivot) {
                first++;
            } else {
                metrics.recordComparison();
                if (values[last - 1] >= pivot) {
                    last--;
                } else {
                    swap(values, first, last - 1);
                    first++;
                    last--;
                    alreadyPartitioned = false;
                }
            }
        }
        swap(values, first, high - 1);
        return new Partition(first, alreadyPartitioned);
    }

    private static int partitionLeft(int[] values, int low, int high, int pivot, SortMetrics metrics) {
        int first = low;
        int last = high;
        while (first < last) {
            metrics.recordComparison();
            if (values[first] <= pivot) {
                first++;
            } else {
                metrics.recordComparison();
                if (values[last - 1] > pivot) {
                    last--;
                } else {
                    swap(values, first, last - 1);
                    first++;
                    last--;
                }
            }
        }
        return first;
    }

    private static boolean partialInsertionSort(int[] values, int low, int high, SortMetrics metrics) {
        int moves = 0;
        for (int index = low + 1; index < high; index++) {
            int value = values[index];
            int destination = index;
            while (destination > low) {
                metrics.recordComparison();
                if (value >= values[destination - 1]) {
                    break;
                }
                values[destination] = values[destination - 1];
                destination--;
            }
            if (destination != index) {
                values[destination] = value;
                moves += index - destination;
                if (moves > PARTIAL_INSERTION_LIMIT) {
                    return false;
                }
            }
        }
        return true;
    }

    private static void breakPatterns(int[] values, int low, int high) {
        int length = high - low;
        if (length < 8) {
            return;
        }
        int middle = low + length / 2;
        int quarter = length / 4;
        swap(values, low + quarter, middle);
        swap(values, middle, high - 1 - quarter);
    }

    private static void insertionSort(int[] values, int low, int high, SortMetrics metrics) {
        for (int index = low + 1; index < high; index++) {
            int value = values[index];
            int destination = index;
            while (destination > low) {
                metrics.recordComparison();
                if (value >= values[destination - 1]) {
                    break;
                }
                values[destination] = values[destination - 1];
                destination--;
            }
            values[destination] = value;
        }
    }

    private static void swap(int[] values, int first, int second) {
        int value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private record Partition(int pivotIndex, boolean alreadyPartitioned) {
    }
}