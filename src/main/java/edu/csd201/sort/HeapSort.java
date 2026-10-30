package edu.csd201.sort;

import java.util.Objects;

public final class HeapSort {

    private HeapSort() {}

    public static final class HeapMetrics {
        private long comparisons;
        private long assignments;

        private void recordComparison() {
            comparisons++;
        }

        private void recordAssignment() {
            assignments++;
        }

        public long comparisons() {
            return comparisons;
        }

        public long assignments() {
            return assignments;
        }

        public void reset() {
            comparisons = 0;
            assignments = 0;
        }
    }

    public static void sort(int[] a) {
        sortSubsegment(a, 0, a.length);
    }

    public static void sort(int[] a, HeapMetrics metrics) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(metrics, "metrics");
        sortSubsegmentInternal(a, 0, a.length, metrics);
    }

    /** Sắp tăng dần đoạn [lo, hi). */
    public static void sortSubsegment(int[] a, int lo, int hi) {
        Objects.requireNonNull(a, "a");
        sortSubsegmentInternal(a, lo, hi, null);
    }

    /**
     * Sắp tăng dần đoạn [lo, hi), đếm phép so sánh khóa và số lần ghi phần tử vào mảng.
     * Không tính phép gán biến cục bộ vào assignment count.
     */
    public static void sortSubsegment(int[] a, int lo, int hi, HeapMetrics metrics) {
        Objects.requireNonNull(a, "a");
        Objects.requireNonNull(metrics, "metrics");
        sortSubsegmentInternal(a, lo, hi, metrics);
    }

    private static void sortSubsegmentInternal(int[] a, int lo, int hi, HeapMetrics metrics) {
        int n = hi - lo;
        if (n < 2) return;

        for (int i = (n >>> 1) - 1; i >= 0; i--) {
            siftDown(a, lo, i, n, metrics);
        }

        for (int end = n - 1; end > 0; end--) {
            int t = a[lo];
            a[lo] = a[lo + end];
            a[lo + end] = t;
            if (metrics != null) {
                metrics.recordAssignment();
                metrics.recordAssignment();
            }
            siftDown(a, lo, 0, end, metrics);
        }
    }

    private static void siftDown(int[] a, int lo, int i, int n, HeapMetrics metrics) {
        int v = a[lo + i];
        while (true) {
            int c = 2 * i + 1;
            if (c >= n) break;
            if (c + 1 < n) {
                if (metrics != null) metrics.recordComparison();
                if (a[lo + c + 1] > a[lo + c]) c++;
            }
            if (metrics != null) metrics.recordComparison();
            if (a[lo + c] <= v) break;
            a[lo + i] = a[lo + c];
            if (metrics != null) metrics.recordAssignment();
            i = c;
        }
        a[lo + i] = v;
        if (metrics != null) metrics.recordAssignment();
    }
}
