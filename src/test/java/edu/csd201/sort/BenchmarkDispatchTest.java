package edu.csd201.sort;

import org.junit.Test;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

public final class BenchmarkDispatchTest {

    @Test
    public void dispatchesEachSupportedAlgorithmForVerification() {
        String[] algorithms = {"MergeSort", "HeapSort", "PdqSort"};
        int[] expected = {-1, -1, 2, 3};

        for (String algorithm : algorithms) {
            int[] values = {3, -1, 2, -1};
            Benchmark.sortForVerification(algorithm, values);
            assertArrayEquals(algorithm, expected, values);
        }
    }

    @Test
    public void rejectsUnsupportedAlgorithm() {
        try {
            Benchmark.sortForVerification("QuickSort", new int[] {2, 1});
            fail("unsupported algorithms must not fall through to another sorter");
        } catch (IllegalArgumentException exception) {
            assertEquals("Unsupported benchmark algorithm: QuickSort", exception.getMessage());
        }
    }

    @Test
    public void mergeSortAdapterSortsValues() {
        assertAdapterSorts("MergeSort");
    }

    @Test
    public void heapSortAdapterSortsValues() {
        assertAdapterSorts("HeapSort");
    }

    @Test
    public void adaptersHandleEmptyAndSingleElementArrays() {
        for (String algorithm : new String[] {"MergeSort", "HeapSort"}) {
            int[] empty = {};
            Benchmark.sorterFor(algorithm).sort(empty);
            assertArrayEquals(algorithm + " empty", new int[0], empty);

            int[] single = {-7};
            Benchmark.sorterFor(algorithm).sort(single);
            assertArrayEquals(algorithm + " single element", new int[] {-7}, single);
        }
    }

    @Test
    public void rejectsUnsupportedSorterAdapter() {
        try {
            Benchmark.sorterFor("QuickSort");
            fail("unsupported sorter adapters must be rejected");
        } catch (IllegalArgumentException exception) {
            assertEquals("Unsupported benchmark sorter: QuickSort", exception.getMessage());
        }
    }

    @Test
    public void timedTrialsSortFreshClonesAndPreserveBaseline() {
        int[] baseline = {4, -2, 4, 0, -9};
        int[] original = baseline.clone();
        List<int[]> trialInputs = new ArrayList<>();

        for (int trial = 0; trial < 3; trial++) {
            Benchmark.TimedSortResult result = Benchmark.runSortTrial(values -> {
                trialInputs.add(values);
                Arrays.sort(values);
            }, baseline);

            assertSame(trialInputs.get(trial), result.values());
            assertNotSame(baseline, result.values());
            assertArrayEquals(new int[] {-9, -2, 0, 4, 4}, result.values());
            assertArrayEquals(original, baseline);
        }

        for (int first = 0; first < trialInputs.size(); first++) {
            for (int second = first + 1; second < trialInputs.size(); second++) {
                assertNotSame(trialInputs.get(first), trialInputs.get(second));
            }
        }
    }

    @Test
    public void timedTrialsRejectIncorrectSorterOutput() {
        int[] baseline = {2, 1};

        try {
            Benchmark.runSortTrial(values -> values[0] = 2, baseline);
            fail("incorrect sorter output must be rejected");
        } catch (IllegalStateException exception) {
            assertEquals("Sorter output does not match Arrays.sort", exception.getMessage());
        }

        assertArrayEquals(new int[] {2, 1}, baseline);
    }

    @Test
    public void runOneCollectsMetricsFromTheOriginalInput() {
        String[] algorithms = {"MergeSort", "HeapSort", "PdqSort"};
        int size = 256;
        long seed = 12345L;

        for (String algorithm : algorithms) {
            int[] original = DataGenerator.generate("D1", size, new java.util.Random(seed));
            long[] expectedMetrics = expectedMetrics(algorithm, original.clone());
            long[] elapsedNanos = {-1L};
            long[] metrics = new long[4];

            Benchmark.runOne(algorithm, "D1", size, new java.util.Random(seed), -1,
                    elapsedNanos, metrics);

            assertTrue(algorithm + " elapsed time", elapsedNanos[0] >= 0);
            assertArrayEquals(algorithm, expectedMetrics, metrics);
        }
    }

    @Test
    public void runOneAcceptsNullMetricsOutput() {
        for (String algorithm : new String[] {"MergeSort", "HeapSort", "PdqSort"}) {
            long[] elapsedNanos = {-1L};

            Benchmark.runOne(algorithm, "D1", 32, new java.util.Random(12345L), -1,
                    elapsedNanos, null);

            assertTrue(algorithm + " elapsed time", elapsedNanos[0] >= 0);
        }
    }

    @Test
    public void missingMetricsAreExcludedFromAverages() {
        Benchmark.MetricsSummary summary = new Benchmark.MetricsSummary();
        summary.add(100, 40);
        summary.add(-1, -1);
        summary.add(200, -1);

        assertEquals(Long.valueOf(150), summary.averageComparisons());
        assertEquals(Long.valueOf(40), summary.averageAssignments());
    }

    @Test
    public void summaryWithoutAvailableMetricsHasNoAverage() {
        Benchmark.MetricsSummary summary = new Benchmark.MetricsSummary();
        summary.add(-1, -1);

        assertNull(summary.averageComparisons());
        assertNull(summary.averageAssignments());
        assertEquals("", Benchmark.formatCsvMetric(-1));
        assertEquals("0", Benchmark.formatCsvMetric(0));
        assertEquals("-", Benchmark.formatSummaryMetric(summary.averageComparisons()));
    }

    private static long[] expectedMetrics(String algorithm, int[] values) {
        switch (algorithm) {
            case "MergeSort" -> {
                MergeSort.MergeMetrics metrics = new MergeSort.MergeMetrics();
                MergeSort.sort(values, metrics);
                return new long[] {
                        metrics.comparisons(), metrics.assignments(), metrics.extraMemoryBytes(), -1
                };
            }
            case "HeapSort" -> {
                HeapSort.HeapMetrics metrics = new HeapSort.HeapMetrics();
                HeapSort.sort(values, metrics);
                return new long[] {metrics.comparisons(), metrics.assignments(), 0, -1};
            }
            case "PdqSort" -> {
                SortMetrics metrics = new SortMetrics();
                PdqSort.sort(values, true, metrics);
                return new long[] {-1, -1, 0, metrics.maxDepth()};
            }
            default -> throw new IllegalArgumentException("Unsupported algorithm: " + algorithm);
        }
    }

    private static void assertAdapterSorts(String algorithm) {
        int[] values = {3, -1, 2, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        Benchmark.sorterFor(algorithm).sort(values);
        assertArrayEquals(algorithm,
                new int[] {Integer.MIN_VALUE, -1, -1, 2, 3, Integer.MAX_VALUE}, values);
    }
}
