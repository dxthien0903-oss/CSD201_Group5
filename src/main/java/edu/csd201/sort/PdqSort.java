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

        int badPartitionBudget = 2 * (31 - Integer.numberOfLeadingZeros(values.length));
        sortRange(values, 0, values.length, badPartitionBudget, 1, false,
            mechanismAEnabled, mechanismBEnabled, metrics);
    }

    private static void sortRange(
            int[] values,
            int low,
            int high,
            int badPartitionBudget,
            int depth,
            boolean breakPatternsBeforePartition,
            boolean mechanismAEnabled,
            boolean mechanismBEnabled,
            SortMetrics metrics) {
        metrics.recordDepth(depth);
        boolean breakPatternsOnNextPartition = breakPatternsBeforePartition;

        while (high - low >= INSERTION_SORT_THRESHOLD) {
            if (badPartitionBudget == 0) {
                heapSort(values, low, high, metrics);
                metrics.recordHeapFallback();
                return;
            }

            if (breakPatternsOnNextPartition) {
                breakPatterns(values, low, high);
                breakPatternsOnNextPartition = false;
            }
            int pivot = choosePivot(values, low, high, metrics);
            Partition partition = partitionThreeWay(values, low, high, pivot, mechanismBEnabled, metrics);
            metrics.recordPartition();

            int leftSize = partition.equalStart - low;
            int rightSize = high - partition.equalEnd;
            int length = high - low;
            boolean balanced = Math.max(leftSize, rightSize) <= length - length / 8;
            if (!balanced) {
                badPartitionBudget--;
                breakPatternsOnNextPartition = true;
            }

            if (mechanismAEnabled && balanced && partition.alreadyPartitioned) {
                if (partialInsertionSort(values, low, partition.equalStart, metrics)
                        && partialInsertionSort(values, partition.equalEnd, high, metrics)) {
                    metrics.recordPartialInsertion();
                    return;
                }
            }

            if (leftSize < rightSize) {
                sortRange(values, low, partition.equalStart, badPartitionBudget, depth + 1,
                    breakPatternsOnNextPartition, mechanismAEnabled, mechanismBEnabled, metrics);
                low = partition.equalEnd;
            } else {
                sortRange(values, partition.equalEnd, high, badPartitionBudget, depth + 1,
                    breakPatternsOnNextPartition, mechanismAEnabled, mechanismBEnabled, metrics);
                high = partition.equalStart;
            }
        }

        insertionSort(values, low, high, metrics);
    }

    private static int choosePivot(int[] values, int low, int high, SortMetrics metrics) {
        int length = high - low;
        int middle = low + length / 2;
        if (length < NINTHER_THRESHOLD) {
            return values[medianIndex(values, low, middle, high - 1, metrics)];
        }

        int step = length / 8;
        int first = medianIndex(values, low, low + step, low + 2 * step, metrics);
        int second = medianIndex(values, middle - step, middle, middle + step, metrics);
        int third = medianIndex(values, high - 1 - 2 * step, high - 1 - step, high - 1, metrics);
        return values[medianIndex(values, first, second, third, metrics)];
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

    private static Partition partitionThreeWay(
            int[] values, int low, int high, int pivot, boolean mechanismBEnabled, SortMetrics metrics) {
        int less = low;
        int current = low;
        int greater = high;
        boolean alreadyPartitioned = true;
        while (current < greater) {
            metrics.recordComparison();
            if (values[current] < pivot) {
                alreadyPartitioned &= less == current;
                swap(values, less++, current++);
            } else {
                metrics.recordComparison();
                if (values[current] > pivot) {
                    while (current < greater) {
                        metrics.recordComparison();
                        if (values[greater - 1] <= pivot) {
                            break;
                        }
                        greater--;
                    }
                    if (current < greater) {
                        int destination = --greater;
                        alreadyPartitioned = false;
                        swap(values, current, destination);
                    }
                } else {
                    current++;
                }
            }
        }
        if (!mechanismBEnabled) {
            int singleEqualEnd = Math.min(greater, less + 1);
            return new Partition(less, singleEqualEnd, alreadyPartitioned);
        }
        return new Partition(less, greater, alreadyPartitioned);
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

    private static void heapSort(int[] values, int low, int high, SortMetrics metrics) {
        int size = high - low;
        for (int root = size / 2 - 1; root >= 0; root--) {
            siftDown(values, low, root, size, metrics);
        }
        for (int end = size - 1; end > 0; end--) {
            swap(values, low, low + end);
            siftDown(values, low, 0, end, metrics);
        }
    }

    private static void siftDown(int[] values, int offset, int root, int size, SortMetrics metrics) {
        while (root * 2 + 1 < size) {
            int child = root * 2 + 1;
            if (child + 1 < size) {
                metrics.recordComparison();
                if (values[offset + child] < values[offset + child + 1]) {
                    child++;
                }
            }
            metrics.recordComparison();
            if (values[offset + root] >= values[offset + child]) {
                return;
            }
            swap(values, offset + root, offset + child);
            root = child;
        }
    }

    private static void swap(int[] values, int first, int second) {
        int value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private record Partition(int equalStart, int equalEnd, boolean alreadyPartitioned) {
    }
}