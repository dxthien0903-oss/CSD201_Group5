package edu.csd201.sort;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

/** Correctness checks for the pdqsort variant with mechanism B disabled. */
public final class PdqSortTest {
    private static int checks;

    private PdqSortTest() {
    }

    public static void main(String[] args) {
        runChecks();
    }

    @Test
    void testPdqSortWithoutMechanismB() {
        runChecks();
    }

    private static void runChecks() {
        checks = 0;
        checkSmallInputMetrics();
        checkMechanismAMetrics();
        checkNoBMatchesArrays("empty", new int[0]);
        checkNoBMatchesArrays("single element", new int[]{42});
        checkNoBMatchesArrays("duplicates", createDuplicates());
        checkNoBMatchesArrays("large D1", DataGenerator.d1(200_000, 20261016L));

        int[] allEqualInput = createAllEqualInput(4_096);
        checkNoBMatchesArrays("all-equal input", allEqualInput);
        SortMetrics fullMetrics = new SortMetrics();
        SortMetrics noBMetrics = new SortMetrics();
        PdqSort.sort(allEqualInput.clone(), true, true, fullMetrics);
        PdqSort.sort(allEqualInput.clone(), true, false, noBMetrics);
        check("all-equal input causes more noB partitions",
                noBMetrics.partitionCount() > fullMetrics.partitionCount());
        check("all-equal input reaches the noB HeapSort fallback",
                noBMetrics.heapFallbackCount() > 0);

        System.out.printf("PdqSortTest: %d checks passed.%n", checks);
    }

    private static void checkSmallInputMetrics() {
        int[] values = {2, 1};
        SortMetrics metrics = new SortMetrics();
        PdqSort.sort(values, true, true, metrics);
        check("two-element input matches Arrays.sort", Arrays.equals(values, new int[]{1, 2}));
        check("two-element comparison count is exact", metrics.comparisonCount() == 1);
        check("two-element depth is recorded", metrics.maxDepth() == 1);
        check("two-element input uses no partition or fallback",
                metrics.partitionCount() == 0 && metrics.heapFallbackCount() == 0);
        check("two-element input uses no partial insertion", metrics.partialInsertionCount() == 0);
    }

    private static void checkMechanismAMetrics() {
        int[] values = new int[25];
        for (int index = 0; index < values.length; index++) {
            values[index] = index;
        }
        SortMetrics metrics = new SortMetrics();
        PdqSort.sort(values, true, true, metrics);
        check("sorted input uses Mechanism A", metrics.partialInsertionCount() > 0);
        check("sorted input is ordered after Mechanism A",
                Arrays.equals(values, java.util.stream.IntStream.range(0, values.length).toArray()));
    }

    private static void checkNoBMatchesArrays(String label, int[] input) {
        int[] expected = input.clone();
        int[] actual = input.clone();
        Arrays.sort(expected);
        PdqSort.sortWithoutMechanismB(actual);
        check(label + " matches Arrays.sort", Arrays.equals(expected, actual));
    }

    private static int[] createDuplicates() {
        Random random = new Random(20261016L);
        int[] values = new int[512];
        for (int index = 0; index < values.length; index++) {
            values[index] = random.nextInt(4);
        }
        return values;
    }

    private static int[] createAllEqualInput(int size) {
        int[] values = new int[size];
        if (size > 0) {
            Arrays.fill(values, size / 2);
        }
        return values;
    }

    private static void check(String label, boolean condition) {
        checks++;
        if (!condition) {
            throw new AssertionError(label);
        }
        System.out.println("PASS " + label);
    }
}
