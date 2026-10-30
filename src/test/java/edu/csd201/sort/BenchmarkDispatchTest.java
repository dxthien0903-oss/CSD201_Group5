package edu.csd201.sort;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
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

    private static void assertAdapterSorts(String algorithm) {
        int[] values = {3, -1, 2, -1, Integer.MIN_VALUE, Integer.MAX_VALUE};
        Benchmark.sorterFor(algorithm).sort(values);
        assertArrayEquals(algorithm,
                new int[] {Integer.MIN_VALUE, -1, -1, 2, 3, Integer.MAX_VALUE}, values);
    }
}
