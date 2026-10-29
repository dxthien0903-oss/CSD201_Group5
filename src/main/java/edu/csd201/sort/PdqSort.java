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
                heapSort(values, low, high);
                metrics.recordHeapFallback();
                return;
            }

            if (breakPatternsOnNextPartition) {
                breakPatterns(values, low, high);
                breakPatternsOnNextPartition = false;
            }
            int pivot = choosePivot(values, low, high);
            Partition partition = partitionThreeWay(values, low, high, pivot, mechanismBEnabled);
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
                if (partialInsertionSort(values, low, partition.equalStart)
                        && partialInsertionSort(values, partition.equalEnd, high)) {
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

        insertionSort(values, low, high);
    }

    private static int choosePivot(int[] values, int low, int high) {
        int length = high - low;
        int middle = low + length / 2;
        if (length < NINTHER_THRESHOLD) {
            return medianIndex(values, low, middle, high - 1);
        }

        int step = length / 8;
        int first = medianIndex(values, low, low + step, low + 2 * step);
        int second = medianIndex(values, middle - step, middle, middle + step);
        int third = medianIndex(values, high - 1 - 2 * step, high - 1 - step, high - 1);
        return medianIndex(values, first, second, third);
    }

    private static int medianIndex(int[] values, int first, int second, int third) {
        if (values[first] < values[second]) {
            if (values[second] < values[third]) {
                return second;
            }
            return values[first] < values[third] ? third : first;
        }
        if (values[first] < values[third]) {
            return first;
        }
        return values[second] < values[third] ? third : second;
    }

    private static Partition partitionThreeWay(int[] values, int low, int high, int pivot, boolean mechanismBEnabled) {
        int less = low;
        int current = low;
        int greater = high;
        boolean alreadyPartitioned = true;
        while (current < greater) {
            if (values[current] < pivot) {
                alreadyPartitioned &= less == current;
                swap(values, less++, current++);
            } else if (values[current] > pivot) {
                while (current < greater && values[greater - 1] > pivot) {
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
        if (!mechanismBEnabled) {
            int singleEqualEnd = Math.min(greater, less + 1);
            return new Partition(less, singleEqualEnd, alreadyPartitioned);
        }
        return new Partition(less, greater, alreadyPartitioned);
    }

    private static boolean partialInsertionSort(int[] values, int low, int high) {
        int moves = 0;
        for (int index = low + 1; index < high; index++) {
            int value = values[index];
            int destination = index;
            while (destination > low && value < values[destination - 1]) {
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

    private static void insertionSort(int[] values, int low, int high) {
        for (int index = low + 1; index < high; index++) {
            int value = values[index];
            int destination = index;
            while (destination > low && value < values[destination - 1]) {
                values[destination] = values[destination - 1];
                destination--;
            }
            values[destination] = value;
        }
    }

    private static void heapSort(int[] values, int low, int high) {
        int size = high - low;
        for (int root = size / 2 - 1; root >= 0; root--) {
            siftDown(values, low, root, size);
        }
        for (int end = size - 1; end > 0; end--) {
            swap(values, low, low + end);
            siftDown(values, low, 0, end);
        }
    }

    private static void siftDown(int[] values, int offset, int root, int size) {
        while (root * 2 + 1 < size) {
            int child = root * 2 + 1;
            if (child + 1 < size && values[offset + child] < values[offset + child + 1]) {
                child++;
            }
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