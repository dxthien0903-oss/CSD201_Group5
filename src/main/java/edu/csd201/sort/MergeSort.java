package edu.csd201.sort;

import java.util.Arrays;
import java.util.Objects;

/**
 * Top-down Merge Sort cho int[] với tracking metrics:
 *   - số phép so sánh (comparisons)
 *   - số phép gán/di chuyển (assignments)
 *   - bộ nhớ phụ cực đại dùng thêm (extraMemoryBytes)
 *
 * Tính đúng đắn được kiểm tra qua {@link #verify(int[])} so với Arrays.sort.
 */
public final class MergeSort {

    private MergeSort() {
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    /**
     * Bộ đếm metrics cho một lần chạy MergeSort.
     * Package-private mutators để chỉ MergeSort mới được ghi.
     */
    public static final class MergeMetrics {
        private long comparisons;
        private long assignments;
        private long extraMemoryBytes; // peak phụ trợ (temp array)

        // --- mutators (package-private) ---
        void addComparison()                   { comparisons++; }
        void addAssignment()                   { assignments++; }
        void setExtraMemory(long bytes)        { extraMemoryBytes = bytes; }

        // --- accessors ---
        public long comparisons()              { return comparisons; }
        public long assignments()              { return assignments; }
        /** Số byte của mảng phụ được cấp phát (= n * 4 byte). */
        public long extraMemoryBytes()         { return extraMemoryBytes; }

        /** Reset về 0 để tái sử dụng object. */
        public void reset() {
            comparisons = 0;
            assignments = 0;
            extraMemoryBytes = 0;
        }

        @Override
        public String toString() {
            return String.format("comparisons=%d assignments=%d extraMem=%d B",
                    comparisons, assignments, extraMemoryBytes);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /**
     * Sắp xếp mảng tại chỗ (in-place view); dùng mảng phụ O(n).
     * Metrics được ghi vào {@code metrics}.
     */
    public static void sort(int[] arr, MergeMetrics metrics) {
        Objects.requireNonNull(arr,     "arr");
        Objects.requireNonNull(metrics, "metrics");
        if (arr.length < 2) {
            return; // 0 hoặc 1 phần tử: đã sắp xếp
        }
        int[] temp = new int[arr.length];
        // Bộ nhớ phụ: mảng temp kiểu int (4 byte/phần tử)
        metrics.setExtraMemory((long) arr.length * Integer.BYTES);
        mergeSort(arr, temp, 0, arr.length - 1, metrics);
    }

    /** Tiện lợi – không cần truyền metrics. */
    public static void sort(int[] arr) {
        sort(arr, new MergeMetrics());
    }

    /**
     * Kiểm tra tính đúng đắn: so sánh kết quả với Arrays.sort.
     *
     * @param input mảng gốc (KHÔNG bị thay đổi)
     * @return {@code true} nếu MergeSort cho kết quả khớp Arrays.sort
     */
    public static boolean verify(int[] input) {
        int[] expected = input.clone();
        int[] actual   = input.clone();
        Arrays.sort(expected);
        sort(actual);
        return Arrays.equals(expected, actual);
    }

    // -------------------------------------------------------------------------
    // Core algorithm
    // -------------------------------------------------------------------------

    /**
     * Phân chia đệ quy (top-down merge sort).
     *
     * @param arr   mảng cần sắp xếp
     * @param temp  mảng phụ cùng kích thước arr
     * @param left  chỉ số đầu đoạn (inclusive)
     * @param right chỉ số cuối đoạn (inclusive)
     */
    private static void mergeSort(int[] arr, int[] temp,
                                  int left, int right, MergeMetrics m) {
        if (left >= right) {
            return; // đoạn 1 phần tử
        }
        int mid = left + (right - left) / 2;
        mergeSort(arr, temp, left,    mid,   m);
        mergeSort(arr, temp, mid + 1, right, m);
        merge(arr, temp, left, mid, right, m);
    }

    /**
     * Trộn hai nửa đã được sắp xếp [left..mid] và [mid+1..right].
     *
     * <p>Counting conventions:
     * <ul>
     *   <li><b>assignment</b>: mỗi lần copy một phần tử (cả copy sang temp lẫn copy về arr).
     *   <li><b>comparison</b>: mỗi lần so sánh temp[i] với temp[j].
     * </ul>
     */
    private static void merge(int[] arr, int[] temp,
                               int left, int mid, int right, MergeMetrics m) {
        // Bước 1 – Sao chép đoạn [left..right] sang temp
        for (int k = left; k <= right; k++) {
            temp[k] = arr[k];
            m.addAssignment();
        }

        int i = left;       // con trỏ nửa trái (trong temp)
        int j = mid + 1;    // con trỏ nửa phải (trong temp)
        int k = left;       // vị trí ghi vào arr

        // Bước 2 – Trộn
        while (i <= mid && j <= right) {
            m.addComparison();              // so sánh temp[i] vs temp[j]
            if (temp[i] <= temp[j]) {
                arr[k++] = temp[i++];
            } else {
                arr[k++] = temp[j++];
            }
            m.addAssignment();              // ghi kết quả vào arr
        }

        // Bước 3 – Copy phần còn lại của nửa trái
        while (i <= mid) {
            arr[k++] = temp[i++];
            m.addAssignment();
        }

        // Bước 4 – Copy phần còn lại của nửa phải
        while (j <= right) {
            arr[k++] = temp[j++];
            m.addAssignment();
        }
    }
}
