package sorting;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * TV1 correctness suite: fixed cases, large edge cases, randomized comparison against
 * Arrays.sort, counted-mode checks, sub-segment contract and D1/D2 generator rules.
 *
 * Usage: java sorting.QuickSortTest [--log=out/quick_sort_test_log.txt]
 */
public final class QuickSortTest {

    private static final int LARGE_N = 100_000;
    private static final int RANDOM_CASES = 1_000;
    private static final int RANDOM_DUPLICATE_CASES = 200;

    private static final StringBuilder LOG = new StringBuilder();
    private static int passed;
    private static int failed;
    private static String stage = "init";

    private QuickSortTest() {
    }

    public static void main(String[] args) throws IOException {
        String logPath = parseLogPath(args);

        line("QuickSort correctness suite (TV1)");
        line("algorithm=" + QuickSort.ALGORITHM + " variant=" + QuickSort.VARIANT);
        line("java.version=" + System.getProperty("java.version")
                + " java.vm.name=" + System.getProperty("java.vm.name"));
        line("");

        for (int section = 0; section < 7; section++) {
            try {
                switch (section) {
                    case 0:
                        fixedCases();
                        break;
                    case 1:
                        subSegmentCases();
                        break;
                    case 2:
                        largeCases();
                        break;
                    case 3:
                        randomizedCases();
                        break;
                    case 4:
                        countedModeCases();
                        break;
                    case 5:
                        exhaustiveCases();
                        break;
                    default:
                        generatorCases();
                        break;
                }
            } catch (Throwable thrown) {
                failed++;
                line("FATAL section#" + section + " stage=" + stage + " "
                        + thrown.getClass().getName() + ": " + thrown.getMessage());
                StackTraceElement[] trace = thrown.getStackTrace();
                for (int i = 0; i < trace.length && i < 6; i++) {
                    line("  at " + trace[i]);
                }
            }
        }

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

    private static void fixedCases() {
        line("[fixed cases]");
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

    private static void subSegmentCases() {
        line("[sub-segment contract: sort(a, lo, hi)]");
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
            QuickSort.sort(new int[] {1, 2}, 0, 3);
        } catch (IndexOutOfBoundsException e) {
            threw = true;
        }
        expect("out-of-range hi is rejected", threw);
    }

    private static void largeCases() {
        line("[large fixed-shape cases, N=" + LARGE_N + "]");

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

    private static void randomizedCases() {
        line("[randomized comparison against Arrays.sort]");
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
        expect("200 random arrays, N in [0, 100], duplicate-heavy domain {0..3}",
                badSmallDomain == 0);

        int[] empty = new int[0];
        expect("empty array stays empty", sortMatchesArraysSort(empty));
    }

    private static void countedModeCases() {
        line("[counted mode]");

        int[] data = DataGenerator.d1(5_000, 7L);
        int[] expected = data.clone();
        Arrays.sort(expected);
        stage = "counted D1 n=5000";
        QuickSort.Stats stats = QuickSort.sortCounted(data);
        expect("counted run sorts correctly", Arrays.equals(expected, data));
        expect("counted run reports comparisons > 0", stats.comparisons > 0);
        expect("counted run reports maxDepth >= 1", stats.maxDepth >= 1);
        line("  n=5000 seed=7 -> " + stats);

        int[] equal = new int[LARGE_N];
        Arrays.fill(equal, 7);
        stage = "counted all-equal N=100000";
        QuickSort.Stats equalStats = QuickSort.sortCounted(equal);
        expect("all-equal N=100000 stays correct in counted mode", isAscending(equal));
        expect("all-equal N=100000 keeps depth balanced (maxDepth <= 64)",
                equalStats.maxDepth <= 64);
        line("  all-equal N=" + LARGE_N + " -> " + equalStats);

        int[] ascending = new int[LARGE_N];
        for (int i = 0; i < ascending.length; i++) {
            ascending[i] = i;
        }
        stage = "counted ascending N=100000";
        QuickSort.Stats ascendingStats = QuickSort.sortCounted(ascending);
        expect("ascending N=100000 stays correct in counted mode", isAscending(ascending));
        expect("ascending N=100000 keeps depth balanced (maxDepth <= 64)",
                ascendingStats.maxDepth <= 64);
        line("  ascending N=" + LARGE_N + " -> " + ascendingStats);

        int[] descending = new int[LARGE_N];
        for (int i = 0; i < descending.length; i++) {
            descending[i] = LARGE_N - i;
        }
        stage = "counted descending N=100000";
        QuickSort.Stats descendingStats = QuickSort.sortCounted(descending);
        expect("descending N=100000 stays correct in counted mode", isAscending(descending));
        expect("descending N=100000 keeps depth balanced (maxDepth <= 64)",
                descendingStats.maxDepth <= 64);
        line("  descending N=" + LARGE_N + " -> " + descendingStats);

        int[] segment = {9, 9, 4, 3, 2, 9, 9};
        int[] segmentExpected = segment.clone();
        Arrays.sort(segmentExpected, 1, 5);
        stage = "counted sub-segment";
        QuickSort.Stats segmentStats = QuickSort.sortCounted(segment, 1, 5);
        expect("counted sub-segment sorts only the requested range",
                Arrays.equals(segmentExpected, segment));
        expect("counted sub-segment reports statistics", segmentStats.maxDepth >= 1);
    }

    private static void generatorCases() {
        line("[D1/D2 generators]");

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

    /**
     * Exhaustive small-input sweep: every array of length 0..7 over the alphabet {0,1,2}
     * and every permutation of length 1..8. This is the regression net for the Hoare
     * crossing-index recursion: a non-progressing partition would hang or overflow here.
     */
    private static void exhaustiveCases() {
        line("[exhaustive small-input sweep]");

        stage = "exhaustive alphabet {0,1,2}, length 0..7";
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

        stage = "exhaustive permutations, length 1..8";
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
        stage = name;
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
