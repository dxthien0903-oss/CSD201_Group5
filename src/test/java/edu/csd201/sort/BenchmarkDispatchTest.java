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
    public void timingAndMetricsTrialsUseFreshClonesAndPreserveBaseline() {
        int[] baseline = {4, -2, 4, 0, -9};
        int[] original = baseline.clone();
        List<int[]> timingInputs = new ArrayList<>();
        List<int[]> metricsInputs = new ArrayList<>();
        HeapSort.HeapMetrics metrics = new HeapSort.HeapMetrics();

        for (int trial = 0; trial < 3; trial++) {
            Benchmark.TimedSortResult result = Benchmark.runSortTrial(values -> {
                timingInputs.add(values);
                Arrays.sort(values);
            }, baseline);
            int[] metricsValues = Benchmark.runMetricsTrial(values -> {
                metricsInputs.add(values);
                HeapSort.sort(values, metrics);
            }, baseline);

            assertSame(timingInputs.get(trial), result.values());
            assertSame(metricsInputs.get(trial), metricsValues);
            assertNotSame(baseline, result.values());
            assertArrayEquals(new int[] {-9, -2, 0, 4, 4}, result.values());
            assertNotSame(baseline, metricsValues);
            assertNotSame(result.values(), metricsValues);
            assertArrayEquals(new int[] {-9, -2, 0, 4, 4}, metricsValues);
            assertArrayEquals(original, baseline);
        }

        for (int first = 0; first < timingInputs.size(); first++) {
            for (int second = first + 1; second < timingInputs.size(); second++) {
                assertNotSame(timingInputs.get(first), timingInputs.get(second));
                assertNotSame(metricsInputs.get(first), metricsInputs.get(second));
            }
        }
    }

    @Test
    public void validationRejectsIncorrectTimingAndMetricsOutputs() {
        int[] baseline = {2, 1};
        int[] expected = Benchmark.expectedOutput(baseline);
        int[] timedOutput = Benchmark.runSortTrial(values -> values[0] = 2, baseline).values();

        try {
            Benchmark.validateSortResult(expected, timedOutput);
            fail("incorrect timing output must be rejected");
        } catch (IllegalStateException exception) {
            assertEquals("Sorter output does not match Arrays.sort", exception.getMessage());
        }

        int[] metricsOutput = Benchmark.runMetricsTrial(values -> values[0] = 2, baseline);
        try {
            Benchmark.validateSortResult(expected, metricsOutput);
            fail("incorrect metrics output must be rejected");
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
    public void runOneAcceptsNullMetricsOutputWithoutChangingInputBehavior() {
        for (String algorithm : new String[] {"MergeSort", "HeapSort", "PdqSort"}) {
            long[] elapsedNanos = {-1L};

            Benchmark.runOne(algorithm, "D1", 32, new java.util.Random(12345L), -1,
                    elapsedNanos, null);

            assertTrue(algorithm + " elapsed time", elapsedNanos[0] >= 0);
        }
    }

    @Test
    public void runOneSortsPdqD2WithoutMetrics() {
        long[] elapsedNanos = {-1L};

        Benchmark.runOne("PdqSort", "D2", 100, new java.util.Random(12345L), -1,
                elapsedNanos, null);

        assertTrue("PdqSort D2 elapsed time", elapsedNanos[0] >= 0);
    }

    @Test
    public void runOneSortsPdqD2WithMetrics() {
        long[] elapsedNanos = {-1L};
        long[] metrics = new long[4];

        Benchmark.runOne("PdqSort", "D2", 100, new java.util.Random(12345L), -1,
                elapsedNanos, metrics);

        assertTrue("PdqSort D2 elapsed time", elapsedNanos[0] >= 0);
        assertArrayEquals(new long[] {-1, -1, 0, 1}, metrics);
    }

    @Test
    public void uninstrumentedMergeSortAndPdqSortProduceSortedOutput() {
        int[] input = {9, -1, 4, 4, Integer.MIN_VALUE, 0, Integer.MAX_VALUE};
        int[] expected = {Integer.MIN_VALUE, -1, 0, 4, 4, 9, Integer.MAX_VALUE};

        int[] mergeValues = input.clone();
        MergeSort.sort(mergeValues);
        assertArrayEquals(expected, mergeValues);

        int[] pdqValues = input.clone();
        PdqSort.sort(pdqValues);
        assertArrayEquals(expected, pdqValues);

        int[] noMechanismAValues = input.clone();
        PdqSort.sortWithoutMechanismA(noMechanismAValues);
        assertArrayEquals(expected, noMechanismAValues);
    }

    @Test
    public void knownMergeSortMetricsMatchIndependentExpectedCounts() {
        int[] values = {4, 1, 3, 2};
        MergeSort.MergeMetrics metrics = new MergeSort.MergeMetrics();

        MergeSort.sort(values, metrics);

        assertEquals(5, metrics.comparisons());
        assertEquals(16, metrics.assignments());
        assertEquals(16, metrics.extraMemoryBytes());
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
