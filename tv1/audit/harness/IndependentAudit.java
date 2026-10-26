package audit;

import sorting.DataGenerator;
import sorting.QuickSort;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;
import java.util.SplittableRandom;

public final class IndependentAudit {
    private static long alphabetCount;
    private static long permutationCount;
    private static long randomCount;
    private static long adversarialCount;
    private static long segmentCount;
    private static long trivialSegmentCount;
    private static long compareBad;
    private static String firstFailure;

    public static void main(String[] args) {
        System.out.println("java.version=" + System.getProperty("java.version"));
        System.out.println("java.vm.name=" + System.getProperty("java.vm.name"));
        System.out.println("java.class.path=" + System.getProperty("java.class.path"));

        checkApi();
        checkGenerators();
        checkFixedCases();
        checkAlphabet();
        checkPermutations();
        checkRandomAndAdversarial();
        checkSegments();
        checkCountedAndOutput();

        System.out.println("TOTAL alphabet=" + alphabetCount
                + " permutations=" + permutationCount
                + " random=" + randomCount
                + " adversarial=" + adversarialCount
                + " segments=" + segmentCount
                + " bad=" + compareBad);
        if (firstFailure == null) {
            System.out.println("RESULT PASS");
        } else {
            System.out.println("RESULT FAIL firstFailure=" + firstFailure);
        }
    }

    private static void checkApi() {
        try {
            Method whole = QuickSort.class.getMethod("sort", int[].class);
            Method range = QuickSort.class.getMethod("sort", int[].class, int.class, int.class);
            Method countedWhole = QuickSort.class.getMethod("sortCounted", int[].class);
            Method countedRange = QuickSort.class.getMethod("sortCounted", int[].class, int.class, int.class);
            boolean ok = Modifier.isStatic(whole.getModifiers())
                    && Modifier.isPublic(whole.getModifiers())
                    && Modifier.isStatic(range.getModifiers())
                    && Modifier.isPublic(range.getModifiers())
                    && Modifier.isStatic(countedWhole.getModifiers())
                    && Modifier.isPublic(countedWhole.getModifiers())
                    && Modifier.isStatic(countedRange.getModifiers())
                    && Modifier.isPublic(countedRange.getModifiers())
                    && countedWhole.getReturnType() == QuickSort.Stats.class
                    && countedRange.getReturnType() == QuickSort.Stats.class;
            System.out.println("API_EXPECTED_SIGNATURES_PRESENT=" + ok);
        } catch (ReflectiveOperationException e) {
            recordFailure("api reflection", e);
        }
    }

    private static void checkGenerators() {
        int n = 1_000_000;
        long seed = 20261016L;
        int[] own = independentD1(n, seed);
        int[] delivered = DataGenerator.d1(n, seed);
        int[] sortedOwn = own.clone();
        Arrays.sort(sortedOwn);
        boolean equal = Arrays.equals(own, delivered);
        long ownCheck = outputCheck(own);
        long sortedOwnCheck = outputCheck(sortedOwn);
        long deliveredCheck = outputCheck(delivered);
        System.out.println("D1_EQUAL_OWN_DELIVERED n=" + n + " seed=" + seed + " equal=" + equal);
        System.out.println("D1_OUTPUT_CHECK n=" + n + " seed=" + seed
                + " own=" + ownCheck
                + " sorted_own=" + sortedOwnCheck
                + " delivered=" + deliveredCheck
                + " expected_csv=-4074488443990278306"
                + " sorted_matches_csv=" + (sortedOwnCheck == -4074488443990278306L));
        System.out.println("D1_PREFIX_10=" + Arrays.toString(Arrays.copyOf(own, 10)));
        System.out.println("D1_LAST_10=" + Arrays.toString(Arrays.copyOfRange(own, own.length - 10, own.length)));

        int[] d1 = DataGenerator.d1(5000, 7L);
        int[] d1Again = DataGenerator.d1(5000, 7L);
        int[] d1Other = DataGenerator.d1(5000, 8L);
        int[] d2 = DataGenerator.d2(d1);
        int[] expected = d1.clone();
        Arrays.sort(expected);
        System.out.println("D1_REPRO_SAME_SEED=" + Arrays.equals(d1, d1Again));
        System.out.println("D1_DIFF_OTHER_SEED=" + !Arrays.equals(d1, d1Other));
        System.out.println("D2_ASCENDING=" + isAscending(d2));
        System.out.println("D2_SAME_MULTISET=" + Arrays.equals(expected, d2));
    }

    private static void checkAlphabet() {
        for (int n = 0; n <= 8; n++) {
            long combos = 1;
            for (int i = 0; i < n; i++) {
                combos *= 3L;
            }
            int[] a = new int[n];
            for (long code = 0; code < combos; code++) {
                long value = code;
                for (int i = 0; i < n; i++) {
                    a[i] = (int) (value % 3L);
                    value /= 3L;
                }
                alphabetCount++;
                checkAgainstArraysSort(a, "alphabet n=" + n + " code=" + code);
            }
        }
        System.out.println("ALPHABET_TOTAL=" + alphabetCount);
    }

    private static void checkFixedCases() {
        int[][] cases = {
                {7},
                {2, 1},
                {3, 3, 3},
                {0, Integer.MAX_VALUE, 0},
                {5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5},
        };
        for (int i = 0; i < cases.length; i++) {
            checkAgainstArraysSort(cases[i], "fixed#" + (i + 1));
        }
        System.out.println("FIXED_CASES=5");
    }

    private static void checkPermutations() {
        for (int n = 1; n <= 9; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = i + 1;
            }
            permute(a, 0);
        }
        System.out.println("PERMUTATION_TOTAL=" + permutationCount);
    }

    private static void permute(int[] a, int k) {
        if (k == a.length) {
            permutationCount++;
            checkAgainstArraysSort(a, "permutation n=" + a.length);
            return;
        }
        for (int i = k; i < a.length; i++) {
            int tmp = a[k];
            a[k] = a[i];
            a[i] = tmp;
            permute(a, k + 1);
            tmp = a[k];
            a[k] = a[i];
            a[i] = tmp;
        }
    }

    private static void checkRandomAndAdversarial() {
        SplittableRandom rnd = new SplittableRandom(0x5EEDC0DEL);
        for (int i = 0; i < 10000; i++) {
            int n = rnd.nextInt(0, 501);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt() & 0x7FFFFFFF;
            }
            randomCount++;
            checkAgainstArraysSort(a, "random full i=" + i);
        }
        for (int i = 0; i < 10000; i++) {
            int n = rnd.nextInt(0, 501);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt(4);
            }
            randomCount++;
            checkAgainstArraysSort(a, "random duplicate-heavy i=" + i);
        }
        for (int i = 0; i < 5000; i++) {
            int n = rnd.nextInt(0, 501);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = (j & 1) == 0 ? 0 : Integer.MAX_VALUE;
            }
            randomCount++;
            checkAgainstArraysSort(a, "random two-extremes i=" + i);
        }
        for (int i = 0; i < 200; i++) {
            int n = rnd.nextInt(1, 20001);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt() & 0x7FFFFFFF;
            }
            randomCount++;
            checkAgainstArraysSort(a, "random larger n=" + n + " i=" + i);
        }

        int[] equal = new int[100000];
        Arrays.fill(equal, 7);
        checkAdversarial("all-equal-100000", equal);
        int[] ascending = new int[100000];
        int[] descending = new int[100000];
        for (int i = 0; i < 100000; i++) {
            ascending[i] = i;
            descending[i] = 100000 - i;
        }
        checkAdversarial("ascending-100000", ascending);
        checkAdversarial("descending-100000", descending);
        int[] organ = new int[100000];
        for (int i = 0; i < organ.length; i++) {
            organ[i] = i <= organ.length / 2 ? i : organ.length - i;
        }
        checkAdversarial("organ-pipe-100000", organ);
        int[] extremes = new int[100000];
        for (int i = 0; i < extremes.length; i++) {
            extremes[i] = (i & 1) == 0 ? 0 : Integer.MAX_VALUE;
        }
        checkAdversarial("extremes-100000", extremes);
        int[] duplicateHeavy = new int[100000];
        for (int i = 0; i < duplicateHeavy.length; i++) {
            duplicateHeavy[i] = i % 5;
        }
        checkAdversarial("duplicate-heavy-100000", duplicateHeavy);
        int[] sawtooth = new int[100000];
        for (int i = 0; i < sawtooth.length; i++) {
            sawtooth[i] = (i * 37) % 101;
        }
        checkAdversarial("sawtooth-100000", sawtooth);
    }

    private static void checkSegments() {
        SplittableRandom rnd = new SplittableRandom(0x5EED5E6L);
        for (int i = 0; i < 10000; i++) {
            int n = rnd.nextInt(0, 501);
            int lo = n == 0 ? 0 : rnd.nextInt(0, n + 1);
            int hi = n == 0 ? 0 : rnd.nextInt(lo, n + 1);
            int[] input = new int[n];
            for (int j = 0; j < n; j++) {
                input[j] = rnd.nextInt(10);
            }
            for (int j = 0; j < lo; j++) {
                input[j] = 100000 - j;
            }
            for (int j = hi; j < n; j++) {
                input[j] = 100000 - j;
            }
            int[] expected = input.clone();
            Arrays.sort(expected, lo, hi);
            int[] actual = input.clone();
            int[] countedActual = input.clone();
            try {
                QuickSort.sort(actual, lo, hi);
                QuickSort.Stats countedStats = QuickSort.sortCounted(countedActual, lo, hi);
                IndependentStats ownStats = independentSortCounted(input, lo, hi);
                segmentCount++;
                if (!Arrays.equals(expected, actual) || !Arrays.equals(expected, countedActual)) {
                    recordFailure("segment n=" + n + " lo=" + lo + " hi=" + hi
                            + " input=" + Arrays.toString(input)
                            + " expected=" + Arrays.toString(expected)
                            + " actual=" + Arrays.toString(actual)
                            + " countedActual=" + Arrays.toString(countedActual), null);
                }
                if (hi - lo < 2) {
                    // Delivered code short-circuits ranges with < 2 elements and returns
                    // Stats(0, 0) without entering the recursion counter; the audit-side
                    // mirror always enters once, so only the delivered trivial contract is
                    // asserted here and depth is compared solely for real ranges.
                    trivialSegmentCount++;
                    if (countedStats.comparisons != 0L || countedStats.maxDepth != 0
                            || ownStats.comparisons != 0L) {
                        recordFailure("trivial segment stats n=" + n + " lo=" + lo + " hi=" + hi
                                + " delivered=" + countedStats
                                + " independent=" + ownStats, null);
                    }
                } else if (countedStats.comparisons != ownStats.comparisons
                        || countedStats.maxDepth != ownStats.maxDepth) {
                    recordFailure("counted segment mismatch n=" + n + " lo=" + lo + " hi=" + hi
                            + " delivered=" + countedStats
                            + " independent=" + ownStats, null);
                }
            } catch (Throwable thrown) {
                segmentCount++;
                recordFailure("segment n=" + n + " lo=" + lo + " hi=" + hi, thrown);
            }
        }
        System.out.println("SEGMENT_TOTAL=" + segmentCount);
        System.out.println("TRIVIAL_SEGMENTS=" + trivialSegmentCount);
    }

    private static void checkCountedAndOutput() {
        long seed = 20261016L;
        int n = 100000;
        int[] input = independentD1(n, seed);
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] deliveredCounted = input.clone();
        QuickSort.Stats stats = QuickSort.sortCounted(deliveredCounted);
        int[] independentCounted = input.clone();
        IndependentStats ownStats = independentSortCounted(independentCounted);
        System.out.println("COUNT n=" + n + " seed=" + seed
                + " delivered=" + stats.comparisons
                + " independent=" + ownStats.comparisons
                + " delta=" + (stats.comparisons - ownStats.comparisons)
                + " expected_csv=2426192"
                + " delivered_matches_csv=" + (stats.comparisons == 2426192L)
                + " independent_matches_csv=" + (ownStats.comparisons == 2426192L));
        System.out.println("COUNT_DEPTH n=" + n + " delivered=" + stats.maxDepth
                + " independent=" + ownStats.maxDepth);
        System.out.println("COUNT_SORTED=" + Arrays.equals(expected, deliveredCounted)
                + " independent_sorted=" + Arrays.equals(expected, independentCounted));
        System.out.println("COUNT_SAME_RESULT_AS_TIME=" + Arrays.equals(sortClone(input), deliveredCounted));

        int[] small = {2, 1};
        QuickSort.Stats smallStats = QuickSort.sortCounted(small.clone());
        IndependentStats smallOwn = independentSortCounted(small.clone());
        System.out.println("SMALL_COUNT data=[2,1] delivered=" + smallStats.comparisons
                + " independent=" + smallOwn.comparisons);
    }

    private static void checkAgainstArraysSort(int[] input, String label) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] actual = input.clone();
        try {
            QuickSort.sort(actual);
            if (!Arrays.equals(expected, actual)) {
                recordFailure(label + " expected=" + Arrays.toString(expected)
                        + " actual=" + Arrays.toString(actual), null);
            }
        } catch (Throwable thrown) {
            recordFailure(label + " input=" + Arrays.toString(input), thrown);
        }
    }

    private static void checkAdversarial(String label, int[] input) {
        adversarialCount++;
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] actual = input.clone();
        try {
            QuickSort.Stats stats = QuickSort.sortCounted(actual);
            boolean ok = Arrays.equals(expected, actual);
            if (!ok) {
                recordFailure(label + " wrong result", null);
            }
            System.out.println("ADVERSARIAL " + label + " comparisons=" + stats.comparisons
                    + " maxDepth=" + stats.maxDepth + " sorted=" + ok);
        } catch (Throwable thrown) {
            recordFailure(label, thrown);
        }
    }

    private static int[] sortClone(int[] input) {
        int[] copy = input.clone();
        QuickSort.sort(copy);
        return copy;
    }

    private static int[] independentD1(int n, long seed) {
        if (n < 0) {
            throw new IllegalArgumentException("negative n");
        }
        SplittableRandom random = new SplittableRandom(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = random.nextInt() & 0x7FFFFFFF;
        }
        return a;
    }

    private static long outputCheck(int[] a) {
        long h = 0L;
        for (int i = 0; i < a.length; i++) {
            h = 31L * h + a[i];
        }
        return h;
    }

    private static boolean isAscending(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static void recordFailure(String message, Throwable thrown) {
        compareBad++;
        if (firstFailure == null) {
            firstFailure = thrown == null ? message : message + " thrown=" + thrown.getClass().getName()
                    + ":" + thrown.getMessage();
            System.out.println("FIRST_FAILURE " + firstFailure);
        }
    }

    private static final class IndependentStats {
        final long comparisons;
        final int maxDepth;

        IndependentStats(long comparisons, int maxDepth) {
            this.comparisons = comparisons;
            this.maxDepth = maxDepth;
        }

        @Override
        public String toString() {
            return "comparisons=" + comparisons + ",maxDepth=" + maxDepth;
        }
    }

    private static final class IndependentCounter {
        long comparisons;
        int depth;
        int maxDepth;

        void enter() {
            depth++;
            if (depth > maxDepth) {
                maxDepth = depth;
            }
        }

        void leave() {
            depth--;
        }
    }

    private static IndependentStats independentSortCounted(int[] a) {
        return independentSortCounted(a, 0, a.length);
    }

    private static IndependentStats independentSortCounted(int[] a, int lo, int hi) {
        IndependentCounter counter = new IndependentCounter();
        independentQuickSort(a, lo, hi, counter);
        return new IndependentStats(counter.comparisons, counter.maxDepth);
    }

    private static void independentQuickSort(int[] a, int lo, int hi, IndependentCounter counter) {
        counter.enter();
        if (hi - lo > 1) {
            int p = independentPartition(a, lo, hi, counter);
            independentQuickSort(a, lo, p, counter);
            independentQuickSort(a, p, hi, counter);
        }
        counter.leave();
    }

    private static int independentPartition(int[] a, int lo, int hi, IndependentCounter counter) {
        int mid = lo + ((hi - lo) >>> 1);
        int pivot = independentMedian(a[lo], a[mid], a[hi - 1], counter);
        int i = lo - 1;
        int j = hi;
        while (true) {
            do {
                i++;
                counter.comparisons++;
            } while (a[i] < pivot);
            do {
                j--;
                counter.comparisons++;
            } while (a[j] > pivot);
            if (i >= j) {
                return i;
            }
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    private static int independentMedian(int x, int y, int z, IndependentCounter counter) {
        if (independentLess(x, y, counter)) {
            if (independentLess(y, z, counter)) {
                return y;
            }
            return independentLess(x, z, counter) ? z : x;
        }
        if (independentLess(x, z, counter)) {
            return x;
        }
        return independentLess(y, z, counter) ? z : y;
    }

    private static boolean independentLess(int x, int y, IndependentCounter counter) {
        counter.comparisons++;
        return x < y;
    }
}
