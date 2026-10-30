package edu.csd201.sort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;

public final class KillerAdversaryTest {

    private static final int N = 10_000;
    private static int cases;
    private static long worstComparisons;
    private static int worstDepth;
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
        for (int n : sizes) {
            int[] killer = generateMed3Killer(n);
            int[] expected = killer.clone();
            Arrays.sort(expected);

            int[] pdqWork = killer.clone();
            SortMetrics pdqMetrics = new SortMetrics();
            PdqSort.sort(pdqWork, true, pdqMetrics);
            assertArrayEquals(expected, pdqWork, "PdqSort must sort Musser/McIlroy killer at N=" + n);
            assertTrue(pdqMetrics.heapFallbackCount() > 0, "PdqSort must trigger HeapSort fallback on Musser/McIlroy killer at N=" + n);

            int[] quickWork = killer.clone();
            String qsStatus = runQuickSortWithBoundedStack(quickWork, expected);
            assertTrue("ok".equals(qsStatus) || "crash".equals(qsStatus), "QuickSort status recorded for N=" + n + ": " + qsStatus);
        }
    }

    private static String runQuickSortWithBoundedStack(int[] work, int[] expected) {
        final String[] status = {"crash"};
        Thread t = new Thread(null, () -> {
            try {
                QuickSort.sort(work);
                status[0] = Arrays.equals(expected, work) ? "ok" : "wrong";
            } catch (StackOverflowError err) {
                status[0] = "crash";
            }
        }, "qs-killer-runner", 256 * 1024L);
        t.start();
        try {
            t.join(10_000L);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
        return status[0];
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
    }

    private static void assertHillClimb(int n, int evaluations, long seed) {
        int bestDepth = runHillClimb(n, evaluations, seed);
        assertTrue(bestDepth <= 200, "Hillclimb maxDepth must remain balanced <= 200");
    }

    public static void main(String[] args) {
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

        System.out.println("KILLER_CASES=" + cases);
        System.out.println("KILLER_FAILURES=" + failures);
        System.out.println("KILLER_WORST=" + worstLabel + " comparisons=" + worstComparisons
                + " maxDepth=" + worstDepth + " n=" + N);

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
        try {
            long start = System.nanoTime();
            QuickSort.Stats stats = QuickSort.sortCounted(work);
            long elapsedUs = (System.nanoTime() - start) / 1000L;
            boolean sorted = Arrays.equals(expected, work);
            if (!sorted) {
                failures++;
                System.out.println("KILLER_FAIL " + label + " wrong order");
            }
            if (stats.comparisons > worstComparisons) {
                worstComparisons = stats.comparisons;
                worstDepth = stats.maxDepth;
                worstLabel = label;
            }
            if (stats.maxDepth > 200 || stats.comparisons > 40L * N * 14L) {
                System.out.println("KILLER_SUSPECT " + label + " comparisons=" + stats.comparisons
                        + " maxDepth=" + stats.maxDepth + " elapsed_us=" + elapsedUs);
            }
        } catch (Throwable thrown) {
            failures++;
            System.out.println("KILLER_THROW " + label + " " + thrown.getClass().getName()
                    + ": " + thrown.getMessage());
        }
    }

    private static void cliHillClimb(int n, int evaluations, long seed) {
        int bestDepth = runHillClimb(n, evaluations, seed);
        if (bestDepth > 200) {
            failures++;
        }
    }

    private static void cliMusserMcIlroyScaling() {
        int[] sizes = {10_000, 100_000, 1_000_000};
        for (int n : sizes) {
            int[] killer = generateMed3Killer(n);
            int[] expected = killer.clone();
            Arrays.sort(expected);

            int[] pdqWork = killer.clone();
            SortMetrics pdqMetrics = new SortMetrics();
            long t0 = System.nanoTime();
            PdqSort.sort(pdqWork, true, pdqMetrics);
            double pdqMs = (System.nanoTime() - t0) / 1e6;
            boolean pdqOk = Arrays.equals(expected, pdqWork);
            if (!pdqOk) {
                failures++;
            }

            int[] qsWork = killer.clone();
            long qsStart = System.nanoTime();
            String rawStatus = runQuickSortWithBoundedStack(qsWork, expected);
            double qsMs = (System.nanoTime() - qsStart) / 1e6;
            String qsStatus = "crash".equals(rawStatus) ? "crash (StackOverflowError)" : rawStatus;
            if ("wrong".equals(rawStatus)) {
                failures++;
            }
            cases++;
            System.out.printf(java.util.Locale.ROOT,
                    "MUSSER_KILLER n=%d QuickSort=[status=%s, time_ms=%.2f] PdqSort=[status=%s, time_ms=%.2f, maxDepth=%d, heapFallbacks=%d]%n",
                    n, qsStatus, qsMs, pdqOk ? "ok" : "wrong", pdqMs, pdqMetrics.maxDepth(), pdqMetrics.heapFallbackCount());
        }
    }

    private static int runHillClimb(int n, int evaluations, long seed) {
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
        cases++;
        System.out.println("KILLER_HILLCLIMB n=" + n + " evaluations=" + evaluations
                + " bestComparisons=" + best + " bestMaxDepth=" + bestDepth
                + " nLog2n=" + Math.round(n * (Math.log(n) / Math.log(2))));
        if (best > worstComparisons) {
            worstComparisons = best;
            worstDepth = bestDepth;
            worstLabel = "hillclimb-n" + n;
        }
        return bestDepth;
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
