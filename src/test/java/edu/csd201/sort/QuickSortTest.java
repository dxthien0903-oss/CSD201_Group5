package edu.csd201.sort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.SplittableRandom;

import static org.junit.jupiter.api.Assertions.*;

/**
 * TV1 correctness suite: fixed cases, large edge cases, randomized comparison against
 * Arrays.sort, counted-mode checks, sub-segment contract, exhaustive permutations,
 * and D1/D2 generator rules.
 *
 * Supports both JUnit 5 test runner (mvn test) and standalone CLI execution:
 *   mvn test -Dtest=QuickSortTest
 *   java -cp "target/classes;target/test-classes" edu.csd201.sort.QuickSortTest [--log=results/quick_sort_test_log.txt]
 */
public final class QuickSortTest {

    private static final int LARGE_N = 100_000;
    private static final int RANDOM_CASES = 1_000;
    private static final int RANDOM_DUPLICATE_CASES = 200;

    private static final StringBuilder LOG = new StringBuilder();
    private static int passed;
    private static int failed;

    public QuickSortTest() {
    }

    // -------------------------------------------------------------------------
    // JUnit 5 Test Methods
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Nhóm 1: Fixed edge cases (mảng rỗng, 1 phần tử, 2 phần tử, 3 phần tử giống nhau, giá trị cực trị biên)")
    public void testFixedCases() {
        int[][] cases = {
                {7},
                {2, 1},
                {3, 3, 3},
                {0, 2147483647, 0},
                {5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5},
                {},
                {0},
                {1, 0},
                {0, 2147483647},
                {2147483647, 0, 2147483647},
        };
        for (int i = 0; i < cases.length; i++) {
            assertSorted("fixed#" + (i + 1), cases[i]);
        }
    }

    @Test
    @DisplayName("Nhóm 2: Sub-segment contract sort(a, lo, hi) trên đoạn [lo, hi)")
    public void testSubSegmentContract() {
        int[] a = {9, 5, 1, 4, 1, 5, 9, 2, 6, 7};
        int[] expected = a.clone();
        Arrays.sort(expected, 2, 8);
        QuickSort.sort(a, 2, 8);
        assertArrayEquals(expected, a, "sub-segment sorts only [2, 8)");

        int[] single = {42, 7, 3};
        int[] singleCopy = single.clone();
        QuickSort.sort(single, 1, 2);
        assertArrayEquals(singleCopy, single, "sub-segment of size 1 is a no-op");

        int[] emptyRange = {4, 3, 2};
        int[] emptyCopy = emptyRange.clone();
        QuickSort.sort(emptyRange, 1, 1);
        assertArrayEquals(emptyCopy, emptyRange, "empty sub-range is a no-op");

        int[] whole = {4, 3, 2, 1, 0};
        QuickSort.sort(whole, 0, whole.length);
        assertTrue(isAscending(whole), "sub-segment covering the whole array sorts it");

        assertThrows(IndexOutOfBoundsException.class, () -> QuickSort.sort(new int[]{1, 2}, 0, 3),
                "out-of-range hi must be rejected");
    }

    @Test
    @DisplayName("Nhóm 3: Large fixed-shape cases N=100.000 (all-equal, ascending, descending, two-extremes)")
    public void testLargeCases() {
        int[] equal = new int[LARGE_N];
        Arrays.fill(equal, 123456789);
        assertSorted("N=100000 all-equal", equal);

        int[] ascending = new int[LARGE_N];
        for (int i = 0; i < ascending.length; i++) {
            ascending[i] = i;
        }
        assertSorted("N=100000 ascending", ascending);

        int[] descending = new int[LARGE_N];
        for (int i = 0; i < descending.length; i++) {
            descending[i] = LARGE_N - i;
        }
        assertSorted("N=100000 descending", descending);

        int[] extremes = new int[LARGE_N];
        for (int i = 0; i < extremes.length; i++) {
            extremes[i] = (i % 2 == 0) ? 0 : Integer.MAX_VALUE;
        }
        assertSorted("N=100000 two-value extremes", extremes);
    }

    @Test
    @DisplayName("Nhóm 4: Randomized cases vs Arrays.sort (1.000 mảng ngẫu nhiên & 200 mảng trùng lặp)")
    public void testRandomizedCases() {
        SplittableRandom rnd = new SplittableRandom(20261016L);
        for (int i = 0; i < RANDOM_CASES; i++) {
            int n = rnd.nextInt(0, 101);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt() & 0x7FFFFFFF;
            }
            assertTrue(sortMatchesArraysSort(a), "Random array #" + i + " must match Arrays.sort");
        }

        for (int i = 0; i < RANDOM_DUPLICATE_CASES; i++) {
            int n = rnd.nextInt(0, 101);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt(0, 4);
            }
            assertTrue(sortMatchesArraysSort(a), "Duplicate array #" + i + " must match Arrays.sort");
        }

        int[] empty = new int[0];
        assertTrue(sortMatchesArraysSort(empty), "empty array stays empty");
    }

    @Test
    @DisplayName("Nhóm 5: Counted mode & max recursion depth checks")
    public void testCountedMode() {
        int[] data = DataGenerator.d1(5_000, 7L);
        int[] expected = data.clone();
        Arrays.sort(expected);
        QuickSort.Stats stats = QuickSort.sortCounted(data);
        assertArrayEquals(expected, data, "counted run sorts correctly");
        assertTrue(stats.comparisons > 0, "counted run reports comparisons > 0");
        assertTrue(stats.maxDepth >= 1, "counted run reports maxDepth >= 1");

        int[] equal = new int[LARGE_N];
        Arrays.fill(equal, 7);
        QuickSort.Stats equalStats = QuickSort.sortCounted(equal);
        assertTrue(isAscending(equal), "all-equal N=100000 stays correct in counted mode");
        assertTrue(equalStats.maxDepth <= 64, "all-equal N=100000 keeps depth balanced (maxDepth <= 64)");

        int[] ascending = new int[LARGE_N];
        for (int i = 0; i < ascending.length; i++) {
            ascending[i] = i;
        }
        QuickSort.Stats ascendingStats = QuickSort.sortCounted(ascending);
        assertTrue(isAscending(ascending), "ascending N=100000 stays correct in counted mode");
        assertTrue(ascendingStats.maxDepth <= 64, "ascending N=100000 keeps depth balanced (maxDepth <= 64)");

        int[] descending = new int[LARGE_N];
        for (int i = 0; i < descending.length; i++) {
            descending[i] = LARGE_N - i;
        }
        QuickSort.Stats descendingStats = QuickSort.sortCounted(descending);
        assertTrue(isAscending(descending), "descending N=100000 stays correct in counted mode");
        assertTrue(descendingStats.maxDepth <= 64, "descending N=100000 keeps depth balanced (maxDepth <= 64)");

        int[] segment = {9, 9, 4, 3, 2, 9, 9};
        int[] segmentExpected = segment.clone();
        Arrays.sort(segmentExpected, 1, 5);
        QuickSort.Stats segmentStats = QuickSort.sortCounted(segment, 1, 5);
        assertArrayEquals(segmentExpected, segment, "counted sub-segment sorts only the requested range");
        assertTrue(segmentStats.maxDepth >= 1, "counted sub-segment reports statistics");
    }

    @Test
    @DisplayName("Nhóm 6: Exhaustive small-input sweep (3.280 mảng độ dài 0..7 & 46.233 hoán vị độ dài 1..8)")
    public void testExhaustiveSweep() {
        int bad = 0;
        for (int n = 0; n <= 7; n++) {
            long combos = 1;
            for (int i = 0; i < n; i++) {
                combos *= 3;
            }
            int[] a = new int[n];
            for (long code = 0; code < combos; code++) {
                long value = code;
                for (int i = 0; i < n; i++) {
                    a[i] = (int) (value % 3);
                    value /= 3;
                }
                if (!sortMatchesArraysSort(a)) {
                    bad++;
                }
            }
        }
        assertEquals(0, bad, "all arrays of length 0..7 over {0,1,2} sort correctly");

        permTotal = 0;
        permBad = 0;
        for (int n = 1; n <= 8; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = i + 1;
            }
            permutations(a, 0);
        }
        assertEquals(0, permBad, "all " + permTotal + " permutations of length 1..8 sort correctly");
    }

    @Test
    @DisplayName("Nhóm 7: D1 & D2 DataGenerator contract")
    public void testDataGenerators() {
        int[] a1 = DataGenerator.d1(1_000, 42L);
        int[] a2 = DataGenerator.d1(1_000, 42L);
        assertArrayEquals(a1, a2, "D1 is reproducible for the same seed");

        int[] other = DataGenerator.d1(1_000, 43L);
        assertFalse(Arrays.equals(a1, other), "D1 differs for a different seed");

        boolean inRange = true;
        for (int value : a1) {
            if (value < 0) {
                inRange = false;
                break;
            }
        }
        assertTrue(inRange, "D1 keys stay in [0, 2^31 - 1]");

        int[] d2 = DataGenerator.d2(a1);
        assertTrue(isAscending(d2), "D2 is ascending");

        int[] d1Copy = a1.clone();
        Arrays.sort(d1Copy);
        assertArrayEquals(d1Copy, d2, "D2 keeps the exact multiset of D1");

        int[] shortD1 = DataGenerator.d1(1, 0L);
        assertEquals(1, shortD1.length, "D1 of size 1 is valid");
        assertArrayEquals(shortD1, DataGenerator.d2(shortD1), "D2 of size 1 is a copy");
    }

    private static void assertSorted(String name, int[] input) {
        int[] actual = input.clone();
        int[] expected = input.clone();
        Arrays.sort(expected);
        QuickSort.sort(actual);
        assertArrayEquals(expected, actual, name + " must be sorted correctly");
    }

    // -------------------------------------------------------------------------
    // Standalone CLI Main Runner (Independent of JUnit classes at runtime)
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        String logPath = parseLogPath(args);

        line("QuickSort correctness suite (TV1)");
        line("algorithm=" + QuickSort.ALGORITHM + " variant=" + QuickSort.VARIANT);
        line("java.version=" + System.getProperty("java.version")
                + " java.vm.name=" + System.getProperty("java.vm.name"));
        line("");

        runSection("fixed cases", QuickSortTest::cliFixedCases);
        runSection("sub-segment contract", QuickSortTest::cliSubSegmentCases);
        runSection("large fixed-shape cases", QuickSortTest::cliLargeCases);
        runSection("randomized cases", QuickSortTest::cliRandomizedCases);
        runSection("counted mode", QuickSortTest::cliCountedModeCases);
        runSection("exhaustive sweep", QuickSortTest::cliExhaustiveCases);
        runSection("D1/D2 generators", QuickSortTest::cliGeneratorCases);

        line("");
        line("SUMMARY passed=" + passed + " failed=" + failed);
        line(failed == 0 ? "RESULT ALL_TESTS_PASSED" : "RESULT TESTS_FAILED");
        String text = LOG.toString();
        System.out.print(text);
        if (logPath != null) {
            Path target = Paths.get(logPath).toAbsolutePath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, text.getBytes(StandardCharsets.UTF_8));
            System.out.println("log written: " + target);
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static void runSection(String name, Runnable test) {
        try {
            line("[" + name + "]");
            test.run();
        } catch (Throwable thrown) {
            failed++;
            line("FATAL section [" + name + "] " + thrown.getClass().getName() + ": " + thrown.getMessage());
        }
    }

    private static void cliFixedCases() {
        int[][] cases = {
                {7},
                {2, 1},
                {3, 3, 3},
                {0, 2147483647, 0},
                {5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5},
                {},
                {0},
                {1, 0},
                {0, 2147483647},
                {2147483647, 0, 2147483647},
        };
        for (int i = 0; i < cases.length; i++) {
            checkSorted("fixed#" + (i + 1), cases[i]);
        }
    }

    private static void cliSubSegmentCases() {
        int[] a = {9, 5, 1, 4, 1, 5, 9, 2, 6, 7};
        int[] expected = a.clone();
        Arrays.sort(expected, 2, 8);
        QuickSort.sort(a, 2, 8);
        expect("sub-segment sorts only [2, 8)", Arrays.equals(expected, a));

        int[] single = {42, 7, 3};
        int[] singleCopy = single.clone();
        QuickSort.sort(single, 1, 2);
        expect("sub-segment of size 1 is a no-op", Arrays.equals(singleCopy, single));

        int[] emptyRange = {4, 3, 2};
        int[] emptyCopy = emptyRange.clone();
        QuickSort.sort(emptyRange, 1, 1);
        expect("empty sub-range is a no-op", Arrays.equals(emptyCopy, emptyRange));

        int[] whole = {4, 3, 2, 1, 0};
        QuickSort.sort(whole, 0, whole.length);
        expect("sub-segment covering the whole array sorts it", isAscending(whole));

        boolean threw = false;
        try {
            QuickSort.sort(new int[]{1, 2}, 0, 3);
        } catch (IndexOutOfBoundsException e) {
            threw = true;
        }
        expect("out-of-range hi is rejected", threw);
    }

    private static void cliLargeCases() {
        int[] equal = new int[LARGE_N];
        Arrays.fill(equal, 123456789);
        checkSorted("N=100000 all-equal", equal);

        int[] ascending = new int[LARGE_N];
        for (int i = 0; i < ascending.length; i++) {
            ascending[i] = i;
        }
        checkSorted("N=100000 ascending", ascending);

        int[] descending = new int[LARGE_N];
        for (int i = 0; i < descending.length; i++) {
            descending[i] = LARGE_N - i;
        }
        checkSorted("N=100000 descending", descending);

        int[] extremes = new int[LARGE_N];
        for (int i = 0; i < extremes.length; i++) {
            extremes[i] = (i % 2 == 0) ? 0 : Integer.MAX_VALUE;
        }
        checkSorted("N=100000 two-value extremes", extremes);
    }

    private static void cliRandomizedCases() {
        SplittableRandom rnd = new SplittableRandom(20261016L);
        int badFullRange = 0;
        for (int i = 0; i < RANDOM_CASES; i++) {
            int n = rnd.nextInt(0, 101);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt() & 0x7FFFFFFF;
            }
            if (!sortMatchesArraysSort(a)) {
                badFullRange++;
            }
        }
        expect("1000 random arrays, N in [0, 100], full key range", badFullRange == 0);

        int badSmallDomain = 0;
        for (int i = 0; i < RANDOM_DUPLICATE_CASES; i++) {
            int n = rnd.nextInt(0, 101);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt(0, 4);
            }
            if (!sortMatchesArraysSort(a)) {
                badSmallDomain++;
            }
        }
        expect("200 random arrays, N in [0, 100], duplicate-heavy domain {0..3}", badSmallDomain == 0);

        int[] empty = new int[0];
        expect("empty array stays empty", sortMatchesArraysSort(empty));
    }

    private static void cliCountedModeCases() {
        int[] data = DataGenerator.d1(5_000, 7L);
        int[] expected = data.clone();
        Arrays.sort(expected);
        QuickSort.Stats stats = QuickSort.sortCounted(data);
        expect("counted run sorts correctly", Arrays.equals(expected, data));
        expect("counted run reports comparisons > 0", stats.comparisons > 0);
        expect("counted run reports maxDepth >= 1", stats.maxDepth >= 1);
        line("  n=5000 seed=7 -> " + stats);

        int[] equal = new int[LARGE_N];
        Arrays.fill(equal, 7);
        QuickSort.Stats equalStats = QuickSort.sortCounted(equal);
        expect("all-equal N=100000 stays correct in counted mode", isAscending(equal));
        expect("all-equal N=100000 keeps depth balanced (maxDepth <= 64)", equalStats.maxDepth <= 64);
        line("  all-equal N=" + LARGE_N + " -> " + equalStats);

        int[] ascending = new int[LARGE_N];
        for (int i = 0; i < ascending.length; i++) {
            ascending[i] = i;
        }
        QuickSort.Stats ascendingStats = QuickSort.sortCounted(ascending);
        expect("ascending N=100000 stays correct in counted mode", isAscending(ascending));
        expect("ascending N=100000 keeps depth balanced (maxDepth <= 64)", ascendingStats.maxDepth <= 64);
        line("  ascending N=" + LARGE_N + " -> " + ascendingStats);

        int[] descending = new int[LARGE_N];
        for (int i = 0; i < descending.length; i++) {
            descending[i] = LARGE_N - i;
        }
        QuickSort.Stats descendingStats = QuickSort.sortCounted(descending);
        expect("descending N=100000 stays correct in counted mode", isAscending(descending));
        expect("descending N=100000 keeps depth balanced (maxDepth <= 64)", descendingStats.maxDepth <= 64);
        line("  descending N=" + LARGE_N + " -> " + descendingStats);

        int[] segment = {9, 9, 4, 3, 2, 9, 9};
        int[] segmentExpected = segment.clone();
        Arrays.sort(segmentExpected, 1, 5);
        QuickSort.Stats segmentStats = QuickSort.sortCounted(segment, 1, 5);
        expect("counted sub-segment sorts only the requested range", Arrays.equals(segmentExpected, segment));
        expect("counted sub-segment reports statistics", segmentStats.maxDepth >= 1);
    }

    private static void cliExhaustiveCases() {
        long total = 0;
        int bad = 0;
        for (int n = 0; n <= 7; n++) {
            long combos = 1;
            for (int i = 0; i < n; i++) {
                combos *= 3;
            }
            int[] a = new int[n];
            for (long code = 0; code < combos; code++) {
                long value = code;
                for (int i = 0; i < n; i++) {
                    a[i] = (int) (value % 3);
                    value /= 3;
                }
                total++;
                if (!sortMatchesArraysSort(a)) {
                    bad++;
                }
            }
        }
        expect("all " + total + " arrays of length 0..7 over {0,1,2} sort correctly", bad == 0);

        permTotal = 0;
        permBad = 0;
        for (int n = 1; n <= 8; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = i + 1;
            }
            permutations(a, 0);
        }
        expect("all " + permTotal + " permutations of length 1..8 sort correctly", permBad == 0);
    }

    private static void cliGeneratorCases() {
        int[] a1 = DataGenerator.d1(1_000, 42L);
        int[] a2 = DataGenerator.d1(1_000, 42L);
        expect("D1 is reproducible for the same seed", Arrays.equals(a1, a2));

        int[] other = DataGenerator.d1(1_000, 43L);
        expect("D1 differs for a different seed", !Arrays.equals(a1, other));

        boolean inRange = true;
        for (int value : a1) {
            if (value < 0) {
                inRange = false;
                break;
            }
        }
        expect("D1 keys stay in [0, 2^31 - 1]", inRange);

        int[] d2 = DataGenerator.d2(a1);
        expect("D2 is ascending", isAscending(d2));

        int[] d1Copy = a1.clone();
        Arrays.sort(d1Copy);
        expect("D2 keeps the exact multiset of D1", Arrays.equals(d1Copy, d2));

        int[] shortD1 = DataGenerator.d1(1, 0L);
        expect("D1 of size 1 is valid", shortD1.length == 1);
        expect("D2 of size 1 is a copy", Arrays.equals(shortD1, DataGenerator.d2(shortD1)));
    }

    // -------------------------------------------------------------------------
    // Helpers
    // -------------------------------------------------------------------------

    private static long permTotal;
    private static int permBad;

    private static void permutations(int[] a, int k) {
        if (k == a.length) {
            permTotal++;
            if (!sortMatchesArraysSort(a)) {
                permBad++;
            }
            return;
        }
        for (int i = k; i < a.length; i++) {
            int tmp = a[k];
            a[k] = a[i];
            a[i] = tmp;
            permutations(a, k + 1);
            tmp = a[k];
            a[k] = a[i];
            a[i] = tmp;
        }
    }

    private static boolean sortMatchesArraysSort(int[] input) {
        int[] actual = input.clone();
        int[] expected = input.clone();
        Arrays.sort(expected);
        QuickSort.sort(actual);
        return Arrays.equals(expected, actual);
    }

    private static boolean isAscending(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static void checkSorted(String name, int[] input) {
        int[] actual = input.clone();
        int[] expected = input.clone();
        Arrays.sort(expected);
        boolean ok;
        try {
            QuickSort.sort(actual);
            ok = Arrays.equals(expected, actual);
        } catch (StackOverflowError error) {
            expect(name + " (StackOverflowError)", false);
            return;
        }
        expect(name, ok);
    }

    private static String parseLogPath(String[] args) {
        for (String arg : args) {
            if (arg.startsWith("--log=")) {
                return arg.substring("--log=".length());
            }
        }
        return null;
    }

    private static void expect(String name, boolean condition) {
        if (condition) {
            passed++;
            line("PASS " + name);
        } else {
            failed++;
            line("FAIL " + name);
        }
    }

    private static void line(String text) {
        LOG.append(text).append(System.lineSeparator());
    }
}
