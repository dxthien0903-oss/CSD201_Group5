package sorting;

/**
 * Quick Sort: Hoare partition, median-of-three pivot (lo, mid, hi), recursion on
 * both branches, no insertion-sort cutoff, no Lomuto partition.
 *
 * Team contract:
 *   - input: int[] with non-negative keys in [0, 2^31 - 1]
 *   - output: ascending order, in place
 *   - API: sort(a), sort(a, lo, hi) with half-open range [lo, hi)
 *   - counted mode: sortCounted(a), sortCounted(a, lo, hi)
 *
 * Counting mode counts key comparisons and max recursion depth, and is meant to run
 * as a separate pass from the timing benchmark (team convention: never mix modes).
 */
public final class QuickSort {

    public static final String ALGORITHM = "quick";
    public static final String VARIANT = "hoare-med3";

    private QuickSort() {
    }

    /** Result of a counted run: key comparisons and maximum recursion depth. */
    public static final class Stats {
        public final long comparisons;
        public final int maxDepth;

        Stats(long comparisons, int maxDepth) {
            this.comparisons = comparisons;
            this.maxDepth = maxDepth;
        }

        @Override
        public String toString() {
            return "comparisons=" + comparisons + ", maxDepth=" + maxDepth;
        }
    }

    /** Sorts the whole array in ascending order (timing mode, no counters). */
    public static void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        quickSort(a, 0, a.length, null);
    }

    /** Sorts the half-open range a[lo, hi) in ascending order (timing mode). */
    public static void sort(int[] a, int lo, int hi) {
        rangeCheck(a, lo, hi);
        if (hi - lo < 2) {
            return;
        }
        quickSort(a, lo, hi, null);
    }

    /** Sorts the whole array and returns key comparisons plus max recursion depth. */
    public static Stats sortCounted(int[] a) {
        if (a == null || a.length < 2) {
            return new Stats(0L, 0);
        }
        Counter c = new Counter();
        quickSort(a, 0, a.length, c);
        return new Stats(c.comparisons, c.maxDepth);
    }

    /** Sorts the half-open range a[lo, hi) and returns counted statistics. */
    public static Stats sortCounted(int[] a, int lo, int hi) {
        rangeCheck(a, lo, hi);
        if (hi - lo < 2) {
            return new Stats(0L, 0);
        }
        Counter c = new Counter();
        quickSort(a, lo, hi, c);
        return new Stats(c.comparisons, c.maxDepth);
    }

    private static void quickSort(int[] a, int lo, int hi, Counter c) {
        if (c != null) {
            c.enter();
        }
        if (hi - lo > 1) {
            int p = partition(a, lo, hi, c);
            quickSort(a, lo, p, c);
            quickSort(a, p, hi, c);
        }
        if (c != null) {
            c.leave();
        }
    }

    /**
     * Hoare partition over a[lo, hi): the i pointer walks right while keys are below the
     * pivot, the j pointer walks left while keys are above it, and every out-of-place pair
     * is swapped. The loop stops when the pointers cross and returns the crossing index p
     * (i at the break), which is the boundary between the two halves:
     *
     *   a[lo..p-1] <= pivot <= a[p..hi-1]
     *
     * The pivot is the median of a[lo], a[mid], a[hi-1]. Because both a[mid] and a[hi-1]
     * live inside [lo+1, hi-1], that median cannot make one of the halves empty, so every
     * recursion level shrinks the range and the scans always terminate.
     */
    private static int partition(int[] a, int lo, int hi, Counter c) {
        int mid = lo + ((hi - lo) >>> 1);
        int pivot = medianOfThree(a[lo], a[mid], a[hi - 1], c);
        int i = lo - 1;
        int j = hi;
        while (true) {
            do {
                i++;
                if (c != null) {
                    c.comparisons++;
                }
            } while (a[i] < pivot);
            do {
                j--;
                if (c != null) {
                    c.comparisons++;
                }
            } while (a[j] > pivot);
            if (i >= j) {
                if (i <= lo || i >= hi) {
                    throw new IllegalStateException("partition made no progress: i=" + i
                            + ", lo=" + lo + ", hi=" + hi);
                }
                return i;
            }
            int tmp = a[i];
            a[i] = a[j];
            a[j] = tmp;
        }
    }

    /** Median value of three keys; costs 2 or 3 counted comparisons. */
    private static int medianOfThree(int x, int y, int z, Counter c) {
        if (less(x, y, c)) {
            if (less(y, z, c)) {
                return y;
            }
            return less(x, z, c) ? z : x;
        }
        if (less(x, z, c)) {
            return x;
        }
        return less(y, z, c) ? z : y;
    }

    private static boolean less(int x, int y, Counter c) {
        if (c != null) {
            c.comparisons++;
        }
        return x < y;
    }

    private static void rangeCheck(int[] a, int lo, int hi) {
        if (a == null) {
            throw new NullPointerException("a");
        }
        if (lo < 0 || hi > a.length || lo > hi) {
            throw new IndexOutOfBoundsException(
                    "lo=" + lo + ", hi=" + hi + ", length=" + a.length);
        }
    }

    private static final class Counter {
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
}
