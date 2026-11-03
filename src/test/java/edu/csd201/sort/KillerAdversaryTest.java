package edu.csd201.sort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public final class KillerAdversaryTest {

    private static final int N = 10_000;
    private static final String[] PDQ_VARIANTS = {"full", "noA", "noB", "noA+noB"};
    private static final boolean[][] PDQ_CONFIGURATIONS = {
            {true, true},
            {false, true},
            {true, false},
            {false, false}
    };
    private static int cases;
    private static long worstComparisons;
    private static int worstDepth;
    private static int worstN;
    private static String worstLabel = "none";
    private static int failures;

    public KillerAdversaryTest() {
    }

    @Test
    @DisplayName("Kiểm thử các họ dữ liệu trường hợp xấu (Worst-case structured families)")
    public void testWorstCaseFamilies() {
        assertFamily("all-equal", filled(N, 7));
        assertFamily("ascending", ascending(N));
        assertFamily("descending", descending(N));
        assertFamily("organ-pipe", organPipe(N));
        assertFamily("sawtooth-101", sawtooth(N, 101));
        assertFamily("sawtooth-997", sawtooth(N, 997));
        assertFamily("two-extremes", twoExtremes(N));
        assertFamily("descending-triples", triples(N, 2, 1, 0));
        assertFamily("ascending-triples", triples(N, 0, 1, 2));
        assertFamily("median-middle-triples", triples(N, 0, 2, 1));
        assertFamily("median-last-triples", triples(N, 1, 0, 2));
        assertFamily("three-values-0-1-2", threeValues(N));
        assertFamily("blocks-of-8-descending", blocks(N, 8));
        assertFamily("blocks-of-32-descending", blocks(N, 32));
    }

    @Test
    @DisplayName("Kiểm thử 200 hoán vị ngẫu nhiên toàn dải khóa (200 random permutations)")
    public void testRandomPermutations() {
        for (int seed = 1; seed <= 200; seed++) {
            Random rnd = new Random(seed);
            int[] a = new int[N];
            for (int i = 0; i < N; i++) {
                a[i] = rnd.nextInt() & 0x7FFFFFFF;
            }
            assertFamily("random-seed-" + seed, a);
        }
    }

    @Test
    @DisplayName("Kiểm thử 50 mảng miền giá trị hẹp trùng lặp cao (50 duplicate-heavy random arrays)")
    public void testRandomDuplicatesDomain() {
        for (int seed = 1; seed <= 50; seed++) {
            Random rnd = new Random(seed);
            int[] a = new int[N];
            for (int i = 0; i < N; i++) {
                a[i] = rnd.nextInt(8);
            }
            assertFamily("random-8-values-seed-" + seed, a);
        }
    }

    @Test
    @DisplayName("Metrics bật/tắt không làm thay đổi kết quả của bốn biến thể pdqsort")
    public void testMetricsTogglePreservesOutput() {
        int[][] inputs = {
                {Integer.MIN_VALUE, -1, 0, 1, Integer.MAX_VALUE, -1},
                filled(512, 7),
                descending(1_024),
                generateMed3Killer(2_048)
        };
        for (int inputIndex = 0; inputIndex < inputs.length; inputIndex++) {
            int[] expected = inputs[inputIndex].clone();
            Arrays.sort(expected);
            for (int variant = 0; variant < PDQ_VARIANTS.length; variant++) {
                int[] counted = inputs[inputIndex].clone();
                int[] uncounted = inputs[inputIndex].clone();
                SortMetrics metrics = new SortMetrics();
                SortMetrics disabledMetrics = new SortMetrics(false);
                PdqSort.sort(counted, PDQ_CONFIGURATIONS[variant][0],
                        PDQ_CONFIGURATIONS[variant][1], metrics);
                PdqSort.sort(uncounted, PDQ_CONFIGURATIONS[variant][0],
                        PDQ_CONFIGURATIONS[variant][1], disabledMetrics);

                String label = "input=" + inputIndex + ", variant=" + PDQ_VARIANTS[variant];
                assertArrayEquals(expected, counted, "Counted result must match Arrays.sort: " + label);
                assertArrayEquals(expected, uncounted, "Uncounted result must match Arrays.sort: " + label);
                assertArrayEquals(counted, uncounted, "Metrics toggle changed output: " + label);
                assertMetricsAreZero(disabledMetrics, label);
                if (!PDQ_CONFIGURATIONS[variant][0]) {
                    assertEquals(0, metrics.partialInsertionCount(),
                            "Mechanism A disabled but partial insertion was recorded: " + label);
                }
            }
        }
    }

    @Test
    @DisplayName("Kiểm thử hiệu năng noB trên dữ liệu nhiều khóa trùng tại N=10^4..10^6")
    public void testNoBDuplicateHeavyScaling() {
        int[] sizes = {10_000, 100_000, 1_000_000};
        for (int size : sizes) {
            assertNoBPerformance("all-equal", filled(size, 17));
            for (int distinctValues = 2; distinctValues <= 8; distinctValues++) {
                int[] values = new int[size];
                Random random = new Random(20261102L + distinctValues * 31L + size);
                for (int index = 0; index < size; index++) {
                    values[index] = random.nextInt(distinctValues) - distinctValues / 2;
                }
                assertNoBPerformance("random-" + distinctValues + "-values", values);
            }
        }
    }

    @Test
    @DisplayName("Bốn biến thể pdqsort trên dữ liệu nhiều khóa tại N=10^4..10^6")
    public void testAllVariantsDuplicateHeavyScaling() {
        int[] sizes = {10_000, 100_000, 1_000_000};
        for (int size : sizes) {
            for (int distinctValues : new int[]{2, 8}) {
                int[] input = new int[size];
                Random random = new Random(20261103L + distinctValues * 31L + size);
                for (int index = 0; index < size; index++) {
                    input[index] = random.nextInt(distinctValues) - distinctValues / 2;
                }
                int[] expected = input.clone();
                Arrays.sort(expected);
                assertPdqVariants("duplicate-heavy-" + distinctValues + "-values-n" + size,
                        input, expected);
            }
        }
    }

    @Test
    @DisplayName("Kiểm thử tấn công leo đồi đối kháng (Hill-climbing adversary search)")
    public void testHillClimbingAdversary() {
        assertHillClimb(512, 4000, 12345L);
        assertHillClimb(2048, 1500, 98765L);
    }

    @Test
    @DisplayName("Kiểm thử Musser/McIlroy Median-of-Three Killer trên QuickSort và PdqSort (N=10^4, 10^5, 10^6)")
    public void testMusserMcIlroyKillerScaling() {
        int[] smallKiller = generateMed3Killer(2_000);
        int[] expectedSmall = smallKiller.clone();
        Arrays.sort(expectedSmall);
        int[] qsWork = smallKiller.clone();
        QuickSort.Stats qsStats = QuickSort.sortCounted(qsWork);
        assertArrayEquals(expectedSmall, qsWork, "QuickSort must still sort Musser/McIlroy killer array at N=2000");
        assertTrue(qsStats.maxDepth >= 900, "Musser/McIlroy killer must force linear recursion depth >= N/2 (actual: " + qsStats.maxDepth + ")");
        assertTrue(qsStats.comparisons >= 900_000L, "Musser/McIlroy killer must force quadratic comparisons (actual: " + qsStats.comparisons + ")");

        int[] sizes = {10_000, 100_000, 1_000_000};
        long totalPdqHeapFallbacks = 0;
        for (int n : sizes) {
            int[] killer = generateMed3Killer(n);
            int[] expected = killer.clone();
            Arrays.sort(expected);

            totalPdqHeapFallbacks += assertPdqVariants("musser-mcilroy-killer-n" + n, killer, expected);

            int[] quickWork = killer.clone();
            String qsStatus = runQuickSortWithBoundedStack(quickWork, expected);
            System.out.printf(Locale.ROOT,
                    "QUICKSORT_KILLER n=%d status=%s timeout_is_stack_overflow=false%n",
                    n, qsStatus);
            assertTrue("ok".equals(qsStatus) || "stack-overflow".equals(qsStatus),
                    "QuickSort failed or timed out (timeout is not StackOverflowError) at N="
                            + n + ": " + qsStatus);
        }
        assertTrue(totalPdqHeapFallbacks > 0,
                "The Musser/McIlroy killer family must reach HeapSort fallback at least once");
    }

    private static String runQuickSortWithBoundedStack(int[] work, int[] expected) {
        final String[] status = {"running"};
        Thread t = new Thread(null, () -> {
            try {
                QuickSort.sort(work);
                status[0] = Arrays.equals(expected, work) ? "ok" : "wrong";
            } catch (StackOverflowError err) {
                status[0] = "stack-overflow";
            }
        }, "qs-killer-runner", 256 * 1024L);
        t.setDaemon(true);
        t.start();
        try {
            t.join(10_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return "interrupted";
        }
        return t.isAlive() ? "timeout" : status[0];
    }

    public static int[] generateMed3Killer(int n) {
        int[] pos = new int[n];
        for (int i = 0; i < n; i++) {
            pos[i] = i;
        }
        int[] out = new int[n];
        int val = 1;
        int lo = 0;
        while (n - lo >= 2) {
            int mid = lo + ((n - lo) >>> 1);
            out[pos[lo]] = val++;
            out[pos[mid]] = val++;
            int tmp = pos[lo + 1];
            pos[lo + 1] = pos[mid];
            pos[mid] = tmp;
            lo += 2;
        }
        if (lo < n) {
            out[pos[lo]] = val;
        }
        return out;
    }

    private static void assertFamily(String label, int[] a) {
        int[] expected = a.clone();
        Arrays.sort(expected);
        int[] work = a.clone();
        QuickSort.Stats stats = QuickSort.sortCounted(work);
        assertArrayEquals(expected, work, "Family [" + label + "] must be sorted correctly");
        assertTrue(stats.maxDepth <= 200, "Family [" + label + "] maxDepth must be <= 200 (actual: " + stats.maxDepth + ")");
        assertTrue(stats.comparisons <= 40L * N * 14L, "Family [" + label + "] comparisons exceeded safety threshold");
        assertPdqVariants(label, a, expected);
    }

    private static long assertPdqVariants(String label, int[] input, int[] expected) {
        long heapFallbacks = 0;
        int log2Ceiling = ceilLog2(input.length);
        for (int variant = 0; variant < PDQ_VARIANTS.length; variant++) {
            int[] work = input.clone();
            SortMetrics metrics = new SortMetrics();
            long started = System.nanoTime();
            PdqSort.sort(work, PDQ_CONFIGURATIONS[variant][0],
                    PDQ_CONFIGURATIONS[variant][1], metrics);
            long elapsedNanos = System.nanoTime() - started;

            String context = "Family [" + label + "], variant [" + PDQ_VARIANTS[variant] + "]";
            assertArrayEquals(expected, work, context + " must match Arrays.sort");
            if (input.length > 1) {
                assertTrue(metrics.comparisonCount() > 0, context + " must count comparisons");
                assertTrue(metrics.maxDepth() > 0, context + " must record max depth");
            }
            assertTrue(metrics.partitionCount() >= 0, context + " partition count must be non-negative");
            assertTrue(metrics.heapFallbackCount() >= 0, context + " fallback count must be non-negative");
            assertTrue(metrics.partialInsertionCount() >= 0,
                    context + " partial insertion count must be non-negative");
            if (!PDQ_CONFIGURATIONS[variant][0]) {
                assertEquals(0, metrics.partialInsertionCount(),
                        context + " must not run Mechanism A");
            }
            int depthLimit = input.length < 2 ? 0 : 2 * log2Ceiling + 1;
            assertTrue(metrics.maxDepth() <= depthLimit,
                    context + " exceeded depth bound: " + metrics.maxDepth());

            System.out.printf(Locale.ROOT,
                    "PDQ_METRICS family=%s variant=%s comparisons=%d max_depth=%d partitions=%d"
                            + " heap_fallbacks=%d partial_insertions=%d elapsed_ms=%.3f%n",
                    label, PDQ_VARIANTS[variant], metrics.comparisonCount(), metrics.maxDepth(),
                    metrics.partitionCount(), metrics.heapFallbackCount(),
                    metrics.partialInsertionCount(), elapsedNanos / 1_000_000.0);
            heapFallbacks += metrics.heapFallbackCount();
        }
        return heapFallbacks;
    }

    private static void assertNoBPerformance(String label, int[] input) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int[] work = input.clone();
        SortMetrics metrics = new SortMetrics();
        long started = System.nanoTime();
        PdqSort.sort(work, true, false, metrics);
        long elapsedNanos = System.nanoTime() - started;

        assertArrayEquals(expected, work, "noB must sort " + label + " N=" + input.length);
        System.out.printf(Locale.ROOT,
                "PDQ_NOB_PERF dataset=%s n=%d comparisons=%d max_depth=%d partitions=%d"
                        + " heap_fallbacks=%d partial_insertions=%d elapsed_ms=%.3f%n",
                label, input.length, metrics.comparisonCount(), metrics.maxDepth(),
                metrics.partitionCount(), metrics.heapFallbackCount(),
                metrics.partialInsertionCount(), elapsedNanos / 1_000_000.0);
    }

    private static void assertMetricsAreZero(SortMetrics metrics, String context) {
        assertEquals(0, metrics.comparisonCount(), context);
        assertEquals(0, metrics.maxDepth(), context);
        assertEquals(0, metrics.partitionCount(), context);
        assertEquals(0, metrics.heapFallbackCount(), context);
        assertEquals(0, metrics.partialInsertionCount(), context);
    }

    private static void assertHillClimb(int n, int evaluations, long seed) {
        HillClimbResult result = runHillClimb(n, evaluations, seed);
        assertTrue(result.maxDepth <= 200, "Hillclimb maxDepth must remain balanced <= 200");
    }

    public static void main(String[] args) {
        resetCliStatistics();
        System.out.println("=== Killer Adversary Test Suite (TV1) ===");

        cliFamily("all-equal", filled(N, 7));
        cliFamily("ascending", ascending(N));
        cliFamily("descending", descending(N));
        cliFamily("organ-pipe", organPipe(N));
        cliFamily("sawtooth-101", sawtooth(N, 101));
        cliFamily("sawtooth-997", sawtooth(N, 997));
        cliFamily("two-extremes", twoExtremes(N));
        cliFamily("descending-triples", triples(N, 2, 1, 0));
        cliFamily("ascending-triples", triples(N, 0, 1, 2));
        cliFamily("median-middle-triples", triples(N, 0, 2, 1));
        cliFamily("median-last-triples", triples(N, 1, 0, 2));
        cliFamily("three-values-0-1-2", threeValues(N));
        cliFamily("blocks-of-8-descending", blocks(N, 8));
        cliFamily("blocks-of-32-descending", blocks(N, 32));

        for (int seed = 1; seed <= 200; seed++) {
            Random rnd = new Random(seed);
            int[] a = new int[N];
            for (int i = 0; i < N; i++) {
                a[i] = rnd.nextInt() & 0x7FFFFFFF;
            }
            cliFamily("random-seed-" + seed, a);
        }
        for (int seed = 1; seed <= 50; seed++) {
            Random rnd = new Random(seed);
            int[] a = new int[N];
            for (int i = 0; i < N; i++) {
                a[i] = rnd.nextInt(8);
            }
            cliFamily("random-8-values-seed-" + seed, a);
        }

        cliHillClimb(512, 4000, 12345L);
        cliHillClimb(2048, 1500, 98765L);
        cliMusserMcIlroyScaling();

        System.out.println("KILLER_DATASET_CASES=" + cases);
        System.out.println("KILLER_FAILURES=" + failures);
        System.out.println("KILLER_WORST=" + worstLabel + " comparisons=" + worstComparisons
                + " maxDepth=" + worstDepth + " n=" + worstN);

        if (failures > 0) {
            System.err.println("RESULT: TESTS_FAILED");
            System.exit(1);
        } else {
            System.out.println("RESULT: ALL_KILLER_TESTS_PASSED");
        }
    }

    private static void cliFamily(String label, int[] a) {
        int[] expected = a.clone();
        Arrays.sort(expected);
        int[] work = a.clone();
        cases++;
        boolean passed = true;
        long started = System.nanoTime();
        try {
            QuickSort.Stats stats = QuickSort.sortCounted(work);
            long elapsedUs = (System.nanoTime() - started) / 1000L;
            if (!Arrays.equals(expected, work)) {
                failures++;
                passed = false;
                System.out.println("KILLER_FAIL " + label + " wrong order");
            }
            recordCliWorst(label + "/QuickSort", stats.comparisons, stats.maxDepth, a.length);
            if (stats.maxDepth > 200) {
                failures++;
                passed = false;
                System.out.printf("KILLER_FAIL dataset=%s algorithm=QuickSort metric=maxDepth actual=%d limit=200%n",
                        label, stats.maxDepth);
            }
            long comparisonLimit = 40L * N * 14L;
            if (stats.comparisons > comparisonLimit) {
                failures++;
                passed = false;
                System.out.printf("KILLER_FAIL dataset=%s algorithm=QuickSort metric=comparisons actual=%d limit=%d%n",
                        label, stats.comparisons, comparisonLimit);
            }
            System.out.printf(Locale.ROOT,
                    "QUICKSORT_CLI dataset=%s comparisons=%d maxDepth=%d elapsed_us=%d status=%s%n",
                    label, stats.comparisons, stats.maxDepth, elapsedUs, passed ? "PASS" : "FAIL");
        } catch (Throwable thrown) {
            failures++;
            System.out.printf(Locale.ROOT,
                    "QUICKSORT_CLI dataset=%s elapsed_us=%d status=FAIL exception=%s message=%s%n",
                    label, (System.nanoTime() - started) / 1_000L,
                    thrown.getClass().getName(), thrown.getMessage());
        }
        cliPdqVariants(label, a, expected);
    }

    private static void cliPdqVariants(String label, int[] input, int[] expected) {
        for (int variant = 0; variant < PDQ_VARIANTS.length; variant++) {
            String variantName = PDQ_VARIANTS[variant];
            int[] work = input.clone();
            SortMetrics metrics = new SortMetrics();
            boolean variantPassed = true;
            long started = System.nanoTime();
            try {
                PdqSort.sort(work, PDQ_CONFIGURATIONS[variant][0],
                        PDQ_CONFIGURATIONS[variant][1], metrics);
                long elapsedNanos = System.nanoTime() - started;
                if (!Arrays.equals(expected, work)) {
                    failures++;
                    variantPassed = false;
                    System.out.printf("PDQ_FAIL dataset=%s variant=%s wrong order%n",
                            label, variantName);
                }
                if (!PDQ_CONFIGURATIONS[variant][0] && metrics.partialInsertionCount() != 0) {
                    failures++;
                    variantPassed = false;
                    System.out.printf("PDQ_FAIL dataset=%s variant=%s Mechanism A ran while disabled%n",
                            label, variantName);
                }
                int log2Ceiling = ceilLog2(input.length);
                long depthLimit = input.length < 2 ? 0 : 2L * log2Ceiling + 1;
                long minimumCountedValue = input.length < 2 ? 0 : 1;
                variantPassed &= reportCliMetricCheck(label, variantName, "comparisons",
                        metrics.comparisonCount(), minimumCountedValue, Long.MAX_VALUE);
                variantPassed &= reportCliMetricCheck(label, variantName, "maxDepth",
                        metrics.maxDepth(), minimumCountedValue, depthLimit);
                variantPassed &= reportCliMetricCheck(label, variantName, "partitions",
                        metrics.partitionCount(), 0, Long.MAX_VALUE);
                variantPassed &= reportCliMetricCheck(label, variantName, "heapFallbacks",
                        metrics.heapFallbackCount(), 0, Long.MAX_VALUE);
                variantPassed &= reportCliMetricCheck(label, variantName, "partialInsertions",
                        metrics.partialInsertionCount(), 0, Long.MAX_VALUE);
                if (metrics.comparisonCount() > worstComparisons) {
                    recordCliWorst(label + "/" + variantName,
                            metrics.comparisonCount(), metrics.maxDepth(), input.length);
                }
                System.out.printf(Locale.ROOT,
                        "PDQ_METRICS dataset=%s variant=%s comparisons=%d maxDepth=%d partitions=%d"
                                + " heapFallbacks=%d partialInsertions=%d elapsed_ms=%.3f status=%s%n",
                        label, variantName, metrics.comparisonCount(), metrics.maxDepth(),
                        metrics.partitionCount(), metrics.heapFallbackCount(),
                        metrics.partialInsertionCount(), elapsedNanos / 1_000_000.0,
                        variantPassed ? "PASS" : "FAIL");
            } catch (Throwable thrown) {
                failures++;
                long elapsedNanos = System.nanoTime() - started;
                System.out.printf(Locale.ROOT,
                        "PDQ_THROW dataset=%s variant=%s exception=%s message=%s elapsed_ms=%.3f%n",
                        label, variantName, thrown.getClass().getName(), thrown.getMessage(),
                        elapsedNanos / 1_000_000.0);
                System.out.printf(Locale.ROOT,
                        "PDQ_METRICS dataset=%s variant=%s comparisons=%d maxDepth=%d partitions=%d"
                                + " heapFallbacks=%d partialInsertions=%d elapsed_ms=%.3f status=FAIL%n",
                        label, variantName, metrics.comparisonCount(), metrics.maxDepth(),
                        metrics.partitionCount(), metrics.heapFallbackCount(),
                        metrics.partialInsertionCount(), elapsedNanos / 1_000_000.0);
            }
        }
    }

    private static int ceilLog2(int value) {
        return value <= 1 ? 0 : 32 - Integer.numberOfLeadingZeros(value - 1);
    }

    private static boolean reportCliMetricCheck(
            String dataset, String variant, String metric, long actual, long minimum, long maximum) {
        if (actual < minimum || actual > maximum) {
            failures++;
            System.out.printf(Locale.ROOT,
                    "PDQ_METRIC_FAIL dataset=%s variant=%s metric=%s actual=%d allowed=[%d,%d]%n",
                    dataset, variant, metric, actual, minimum, maximum);
            return false;
        }
        return true;
    }

    private static void recordCliWorst(String label, long comparisons, int depth, int n) {
        if (comparisons <= worstComparisons) {
            return;
        }
        worstComparisons = comparisons;
        worstDepth = depth;
        worstN = n;
        worstLabel = label;
    }

    private static void resetCliStatistics() {
        cases = 0;
        worstComparisons = 0;
        worstDepth = 0;
        worstN = 0;
        worstLabel = "none";
        failures = 0;
    }

    private static void cliHillClimb(int n, int evaluations, long seed) {
        HillClimbResult result = runHillClimb(n, evaluations, seed);
        boolean passed = result.maxDepth <= 200;
        if (!passed) {
            failures++;
            System.out.printf(Locale.ROOT,
                    "KILLER_HILLCLIMB_FAIL n=%d metric=maxDepth actual=%d limit=200%n",
                    n, result.maxDepth);
        }
        cases++;
        recordCliWorst("hillclimb-n" + n, result.comparisons, result.maxDepth, n);
        System.out.println("KILLER_HILLCLIMB n=" + n + " evaluations=" + evaluations
                + " bestComparisons=" + result.comparisons + " bestMaxDepth=" + result.maxDepth
                + " depthLimit=200 nLog2n=" + Math.round(n * (Math.log(n) / Math.log(2)))
                + " status=" + (passed ? "PASS" : "FAIL"));
    }

    private static void cliMusserMcIlroyScaling() {
        int[] sizes = {10_000, 100_000, 1_000_000};
        for (int n : sizes) {
            int[] killer = generateMed3Killer(n);
            int[] expected = killer.clone();
            Arrays.sort(expected);

            cliPdqVariants("musser-mcilroy-killer-n" + n, killer, expected);

            int[] qsWork = killer.clone();
            long qsStart = System.nanoTime();
            String rawStatus = runQuickSortWithBoundedStack(qsWork, expected);
            double qsMs = (System.nanoTime() - qsStart) / 1e6;
            String qsStatus = switch (rawStatus) {
                case "stack-overflow" -> "StackOverflowError";
                case "timeout" -> "timeout";
                case "interrupted" -> "interrupted";
                default -> rawStatus;
            };
            if (!"ok".equals(rawStatus) && !"stack-overflow".equals(rawStatus)) {
                failures++;
            }
            System.out.printf(Locale.ROOT,
                    "MUSSER_KILLER n=%d QuickSort=[status=%s, time_ms=%.2f]%n",
                    n, qsStatus, qsMs);
            cases++;
        }
    }

    private static HillClimbResult runHillClimb(int n, int evaluations, long seed) {
        Random rnd = new Random(seed);
        int[] current = new int[n];
        for (int i = 0; i < n; i++) {
            current[i] = i;
        }
        for (int i = n - 1; i > 0; i--) {
            int j = rnd.nextInt(i + 1);
            int tmp = current[i];
            current[i] = current[j];
            current[j] = tmp;
        }
        long currentScore = score(current);
        long best = currentScore;
        int bestDepth = QuickSort.sortCounted(current.clone()).maxDepth;
        for (int e = 0; e < evaluations; e++) {
            int[] candidate = current.clone();
            int mode = rnd.nextInt(3);
            if (mode == 0) {
                int i = rnd.nextInt(n);
                int j = rnd.nextInt(n);
                int tmp = candidate[i];
                candidate[i] = candidate[j];
                candidate[j] = tmp;
            } else if (mode == 1) {
                int i = rnd.nextInt(n);
                candidate[i] = rnd.nextInt(n);
            } else {
                int from = rnd.nextInt(n);
                int to = rnd.nextInt(n);
                int value = candidate[from];
                if (from < to) {
                    System.arraycopy(candidate, from + 1, candidate, from, to - from);
                } else if (to < from) {
                    System.arraycopy(candidate, to, candidate, to + 1, from - to);
                }
                candidate[to] = value;
            }
            long candidateScore = score(candidate);
            if (candidateScore >= currentScore) {
                current = candidate;
                currentScore = candidateScore;
                if (candidateScore > best) {
                    best = candidateScore;
                    bestDepth = QuickSort.sortCounted(candidate.clone()).maxDepth;
                }
            }
        }
        return new HillClimbResult(best, bestDepth);
    }

    private record HillClimbResult(long comparisons, int maxDepth) {
    }

    private static long score(int[] a) {
        return QuickSort.sortCounted(a.clone()).comparisons;
    }

    private static int[] filled(int n, int value) {
        int[] a = new int[n];
        Arrays.fill(a, value);
        return a;
    }

    private static int[] ascending(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i;
        }
        return a;
    }

    private static int[] descending(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = n - i;
        }
        return a;
    }

    private static int[] organPipe(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i <= n / 2 ? i : n - i;
        }
        return a;
    }

    private static int[] sawtooth(int n, int period) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = (i * 37) % period;
        }
        return a;
    }

    private static int[] twoExtremes(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = (i & 1) == 0 ? 0 : Integer.MAX_VALUE;
        }
        return a;
    }

    private static int[] triples(int n, int first, int second, int third) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            int slot = i % 3;
            int block = i / 3;
            a[i] = block * 3 + (slot == 0 ? first : slot == 1 ? second : third);
        }
        return a;
    }

    private static int[] threeValues(int n) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = i % 3;
        }
        return a;
    }

    private static int[] blocks(int n, int block) {
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            int base = (i / block) * block;
            a[i] = base + (block - 1 - (i % block));
        }
        return a;
    }
}
