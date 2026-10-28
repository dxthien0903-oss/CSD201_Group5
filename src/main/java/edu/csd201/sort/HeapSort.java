package edu.csd201.sort;

public final class HeapSort {

    private HeapSort() {}

    public static void sort(int[] a) {
        sortSubsegment(a, 0, a.length);
    }

    /** Sắp tăng dần đoạn [lo, hi). */
    public static void sortSubsegment(int[] a, int lo, int hi) {
        int n = hi - lo;
        if (n < 2) return;

        for (int i = (n >>> 1) - 1; i >= 0; i--) {
            siftDown(a, lo, i, n);
        }

        for (int end = n - 1; end > 0; end--) {
            int t = a[lo];
            a[lo] = a[lo + end];
            a[lo + end] = t;
            siftDown(a, lo, 0, end);
        }
    }

    private static void siftDown(int[] a, int lo, int i, int n) {
        int v = a[lo + i];
        while (true) {
            int c = 2 * i + 1;
            if (c >= n) break;
            if (c + 1 < n && a[lo + c + 1] > a[lo + c]) c++;
            if (a[lo + c] <= v) break;
            a[lo + i] = a[lo + c];
            i = c;
        }
        a[lo + i] = v;
    }
}
