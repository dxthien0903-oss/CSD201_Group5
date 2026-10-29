package edu.csd201.sort;


public final class QuickSort {

    public static final String ALGORITHM = "quick";
    public static final String VARIANT = "hoare-med3";

    private QuickSort() {
    }

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

    public static void sort(int[] a) {
        if (a == null || a.length < 2) {
            return;
        }
        quickSort(a, 0, a.length, null);
    }

    public static void sort(int[] a, int lo, int hi) {
        rangeCheck(a, lo, hi);
        if (hi - lo < 2) {
            return;
        }
        quickSort(a, lo, hi, null);
    }

    public static Stats sortCounted(int[] a) {
        if (a == null || a.length < 2) {
            return new Stats(0L, 0);
        }
        Counter c = new Counter();
        quickSort(a, 0, a.length, c);
        return new Stats(c.comparisons, c.maxDepth);
    }

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
