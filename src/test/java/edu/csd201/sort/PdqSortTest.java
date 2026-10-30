package edu.csd201.sort;

import java.util.Arrays;
import java.util.Random;

/** Correctness checks for the pdqsort variant with mechanism B disabled. */
public final class PdqSortTest {
    private static int checks;

    private PdqSortTest() {
    }

    public static void main(String[] args) {
        checkNoBMatchesArrays("empty", new int[0]);
        checkNoBMatchesArrays("single element", new int[]{42});
        checkNoBMatchesArrays("duplicates", createDuplicates());
        checkNoBMatchesArrays("large D1", DataGenerator.d1(200_000, 20261016L));

        int[] killerAdversary = createKillerAdversary(4_096);
        checkNoBMatchesArrays("killer adversary", killerAdversary);
        SortMetrics fullMetrics = new SortMetrics();
        SortMetrics noBMetrics = new SortMetrics();
        PdqSort.sort(killerAdversary.clone(), true, true, fullMetrics);
        PdqSort.sort(killerAdversary.clone(), true, false, noBMetrics);
        check("killer adversary causes more noB partitions",
                noBMetrics.partitionCount() > fullMetrics.partitionCount());
        check("killer adversary reaches the noB HeapSort fallback",
                noBMetrics.heapFallbackCount() > 0);

        System.out.printf("PdqSortTest: %d checks passed.%n", checks);
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

    private static int[] createKillerAdversary(int size) {
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
