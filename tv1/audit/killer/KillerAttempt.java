package audit.killer;

import sorting.QuickSort;

import java.util.Arrays;
import java.util.Random;

/**
 * Bounded adversary attempt against the delivered QuickSort at N=10000.
 *
 * Scope: structured worst-case families, 200 fixed-seed random permutations, and a small
 * hill-climb search that mutates arrays to maximise the counted key comparisons. This is NOT
 * the adaptive McIlroy/Musser anti-quicksort oracle (which needs an instrumented copy of the
 * algorithm); it is a bounded search for which this document reports the exact outcome.
 */
public final class KillerAttempt {
    private static final int N = 10_000;
    private static int cases;
    private static long worstComparisons;
    private static int worstDepth;
    private static String worstLabel = "none";
    private static int failures;

    public static void main(String[] args) throws Exception {
        family("all-equal", filled(N, 7));
        family("ascending", ascending(N));
        family("descending", descending(N));
        family("organ-pipe", organPipe(N));
        family("sawtooth-101", sawtooth(N, 101));
        family("sawtooth-997", sawtooth(N, 997));
        family("two-extremes", twoExtremes(N));
        family("descending-triples", triples(N, 2, 1, 0));
        family("ascending-triples", triples(N, 0, 1, 2));
        family("median-middle-triples", triples(N, 0, 2, 1));
        family("median-last-triples", triples(N, 1, 0, 2));
        family("three-values-0-1-2", threeValues(N));
        family("blocks-of-8-descending", blocks(N, 8));
        family("blocks-of-32-descending", blocks(N, 32));

        for (int seed = 1; seed <= 200; seed++) {
            Random rnd = new Random(seed);
            int[] a = new int[N];
            for (int i = 0; i < N; i++) {
                a[i] = rnd.nextInt() & 0x7FFFFFFF;
            }
            family("random-seed-" + seed, a);
        }
        for (int seed = 1; seed <= 50; seed++) {
            Random rnd = new Random(seed);
            int[] a = new int[N];
            for (int i = 0; i < N; i++) {
                a[i] = rnd.nextInt(8);
            }
            family("random-8-values-seed-" + seed, a);
        }

        hillClimb(512, 4000, 12345L);
        hillClimb(2048, 1500, 98765L);

        System.out.println("KILLER_CASES=" + cases);
        System.out.println("KILLER_FAILURES=" + failures);
        System.out.println("KILLER_WORST=" + worstLabel + " comparisons=" + worstComparisons
                + " maxDepth=" + worstDepth + " n=" + N);
    }

    private static void family(String label, int[] a) {
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

    private static void hillClimb(int n, int evaluations, long seed) {
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
