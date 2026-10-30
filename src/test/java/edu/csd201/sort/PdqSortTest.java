package edu.csd201.sort;

import java.util.stream.IntStream;

import org.junit.Test;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertTrue;

public final class PdqSortTest {

    @Test
    public void sortsAscendingInputWithoutMetrics() {
        int[] values = IntStream.range(0, 100).toArray();
        int[] expected = values.clone();

        PdqSort.sort(values);

        assertArrayEquals(expected, values);
    }

    @Test
    public void sortsAscendingInputWithoutMechanismAAndMetrics() {
        int[] values = IntStream.range(0, 100).toArray();
        int[] expected = values.clone();

        PdqSort.sortWithoutMechanismA(values);

        assertArrayEquals(expected, values);
    }

    @Test
    public void instrumentedSortRecordsPartialInsertionOnAscendingInput() {
        int[] values = IntStream.range(0, 100).toArray();
        int[] expected = values.clone();
        SortMetrics metrics = new SortMetrics();

        PdqSort.sort(values, true, metrics);

        assertArrayEquals(expected, values);
        assertTrue(metrics.partialInsertionCount() > 0);
    }
}
