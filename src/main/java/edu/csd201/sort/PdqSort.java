package edu.csd201.sort;

import java.util.Objects;

public final class PdqSort {
    private static final int INSERTION_SORT_THRESHOLD = 24;
    private static final int NINTHER_THRESHOLD = 128;
    private static final int PARTIAL_INSERTION_LIMIT = 8;

    private PdqSort() {
    }

    public static void sort(int[] values) {
        Objects.requireNonNull(values, "values");
        sortInternal(values, true, null);
    }

    public static void sortWithoutMechanismA(int[] values) {
        Objects.requireNonNull(values, "values");
        sortInternal(values, false, null);
    }

    public static void sort(int[] values, boolean mechanismAEnabled, SortMetrics metrics) {
        Objects.requireNonNull(values, "values");
        Objects.requireNonNull(metrics, "metrics");
        sortInternal(values, mechanismAEnabled, metrics);
    }

    private static void sortInternal(int[] values, boolean mechanismAEnabled, SortMetrics metrics) {
        if (values.length < 2) {
            return;
        }

        int badPartitionBudget = 2 * (31 - Integer.numberOfLeadingZeros(values.length));
        sortRange(values, 0, values.length, badPartitionBudget, 1, false,
            mechanismAEnabled, metrics);
    }

    private static void sortRange(
            int[] values,
            int low,
            int high,
            int badPartitionBudget,
            int depth,
            boolean breakPatternsBeforePartition,
            boolean mechanismAEnabled,
            SortMetrics metrics) {
        if (metrics != null) {
            metrics.recordDepth(depth);
        }
        boolean breakPatternsOnNextPartition = breakPatternsBeforePartition;

        while (high - low >= INSERTION_SORT_THRESHOLD) {
            if (badPartitionBudget == 0) {
                HeapSort.sortSubsegment(values, low, high);
                if (metrics != null) {
                    metrics.recordHeapFallback();
                }
                return;
            }

            if (breakPatternsOnNextPartition) {
                breakPatterns(values, low, high);
                breakPatternsOnNextPartition = false;
            }
            int pivot = choosePivot(values, low, high);
            Partition partition = partitionThreeWay(values, low, high, pivot);
            if (metrics != null) {
                metrics.recordPartition();
            }

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
                    if (metrics != null) {
                        metrics.recordPartialInsertion();
                    }
                    return;
                }
            }

            if (leftSize < rightSize) {
                sortRange(values, low, partition.equalStart, badPartitionBudget, depth + 1,
                    breakPatternsOnNextPartition, mechanismAEnabled, metrics);
                low = partition.equalEnd;
            } else {
                sortRange(values, partition.equalEnd, high, badPartitionBudget, depth + 1,
                    breakPatternsOnNextPartition, mechanismAEnabled, metrics);
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

    private static Partition partitionThreeWay(int[] values, int low, int high, int pivot) {
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

    private static void swap(int[] values, int first, int second) {
        int value = values[first];
        values[first] = values[second];
        values[second] = value;
    }

    private record Partition(int equalStart, int equalEnd, boolean alreadyPartitioned) {
    }
}