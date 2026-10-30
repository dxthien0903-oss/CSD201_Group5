package edu.csd201.sort;

import java.util.Arrays;
import java.util.Random;

import org.junit.Test;

public final class HeapSortTest {

    @Test
    public void testFixedAssignmentCases() {
        assertSort(new int[] {7}, new int[] {7}, "assignment case [7]");
        assertSort(new int[] {2, 1}, new int[] {1, 2}, "assignment case [2, 1]");
        assertSort(new int[] {3, 3, 3}, new int[] {3, 3, 3}, "assignment case [3, 3, 3]");
        assertSort(new int[] {0, 2147483647, 0}, new int[] {0, 0, 2147483647},
                "assignment case [0, 2147483647, 0]");
        assertSort(new int[] {5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5},
                new int[] {1, 1, 2, 3, 4, 5, 5, 5, 5, 6, 9},
                "assignment case [5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5]");
    }

    @Test
    public void testRandomizedInputs() {
        Random random = new Random(20261028L);
        for (int iteration = 0; iteration < 1000; iteration++) {
            int size = random.nextInt(301);
            int[] values = new int[size];
            for (int i = 0; i < size; i++) {
                values[i] = random.nextInt();
            }
            assertMatchesArraysSort(values, "random, iteration " + iteration);

            int[] ascending = values.clone();
            Arrays.sort(ascending);
            assertMatchesArraysSort(ascending, "ascending, iteration " + iteration);

            int[] descending = ascending.clone();
            reverse(descending);
            assertMatchesArraysSort(descending, "descending, iteration " + iteration);

            int[] equal = new int[size];
            Arrays.fill(equal, random.nextInt());
            assertMatchesArraysSort(equal, "all equal, iteration " + iteration);

            int[] fewDistinct = new int[size];
            for (int i = 0; i < size; i++) {
                fewDistinct[i] = random.nextInt(7) - 3;
            }
            assertMatchesArraysSort(fewDistinct, "few distinct, iteration " + iteration);
        }
    }

    @Test
    public void testSubsegments() {
        int[] actual = {91, 4, -2, 4, 17, -100, 92};
        HeapSort.sortSubsegment(actual, 1, 6);
        assertArrayEquals(new int[] {91, -100, -2, 4, 4, 17, 92}, actual,
                "subsegment [1, 6)");

        int[] emptySegment = {8, 3, 2, 7};
        int[] emptySegmentBefore = emptySegment.clone();
        HeapSort.sortSubsegment(emptySegment, 2, 2);
        assertArrayEquals(emptySegmentBefore, emptySegment, "empty subsegment [2, 2)");

        int[] singleElementSegment = {8, 3, 2, 7};
        int[] singleElementBefore = singleElementSegment.clone();
        HeapSort.sortSubsegment(singleElementSegment, 1, 2);
        assertArrayEquals(singleElementBefore, singleElementSegment, "single-element subsegment [1, 2)");
    }

    private static void assertMatchesArraysSort(int[] input, String description) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] actual = input.clone();
        HeapSort.sort(actual);
        assertArrayEquals(expected, actual, description);
    }

    private static void assertSort(int[] input, int[] expected, String description) {
        int[] actual = input.clone();
        HeapSort.sort(actual);
        assertArrayEquals(expected, actual, description);
    }

    private static void assertArrayEquals(int[] expected, int[] actual, String description) {
        org.junit.Assert.assertArrayEquals(description, expected, actual);
    }

    private static void reverse(int[] values) {
        for (int left = 0, right = values.length - 1; left < right; left++, right--) {
            int temp = values[left];
            values[left] = values[right];
            values[right] = temp;
        }
    }
}
