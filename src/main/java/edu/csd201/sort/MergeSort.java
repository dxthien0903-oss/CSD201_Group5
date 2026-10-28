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
        private int  maxDepth;         // chiều sâu đệ quy cực đại (chỉ có ở lượt count)

        // --- mutators (package-private) ---
        void addComparison()                   { comparisons++; }
        void addAssignment()                   { assignments++; }
        void setExtraMemory(long bytes)        { extraMemoryBytes = bytes; }
        void updateDepth(int depth)            { if (depth > maxDepth) maxDepth = depth; }

        // --- accessors ---
        public long comparisons()              { return comparisons; }
        public long assignments()              { return assignments; }
        /** Số byte của mảng phụ được cấp phát (= n * 4 byte). */
        public long extraMemoryBytes()         { return extraMemoryBytes; }
        /**
         * Chiều sâu đệ quy cực đại (bắt đầu từ 1).
         * Chỉ có ý nghĩa sau khi gọi {@link MergeSort#sortCounted}.
         * Trong lượt đo thời gian ({@link MergeSort#sort}), giá trị này = 0.
         */
        public int maxDepth()                  { return maxDepth; }

        /** Reset về 0 để tái sử dụng object. */
        public void reset() {
            comparisons = 0;
            assignments = 0;
            extraMemoryBytes = 0;
            maxDepth = 0;
        }

        @Override
        public String toString() {
            return String.format("comparisons=%d assignments=%d extraMem=%d B maxDepth=%d",
                    comparisons, assignments, extraMemoryBytes, maxDepth);
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
     * Chế độ đếm (count mode) – chạy riêng, KHÔNG dùng để đo thời gian.
     *
     * <p>Đếm {@code comparisons}, {@code assignments}, {@code extraMemoryBytes}
     * và {@code maxDepth} (chiều sâu đệ quy cực đại).
     *
     * <p><b>Quy ước nhóm:</b> lượt {@code --count} phải tách biệt hoàn toàn
     * với lượt đo thời gian để không nhiễu kết quả của nhau.
     *
     * @param arr     mảng cần sắp xếp (sẽ bị thay đổi tại chỗ)
     * @param metrics đối tượng ghi metrics; sau khi trả về, {@code maxDepth()} > 0
     */
    public static void sortCounted(int[] arr, MergeMetrics metrics) {
        Objects.requireNonNull(arr,     "arr");
        Objects.requireNonNull(metrics, "metrics");
        if (arr.length < 2) {
            metrics.setExtraMemory((long) arr.length * Integer.BYTES);
            return;
        }
        int[] temp = new int[arr.length];
        metrics.setExtraMemory((long) arr.length * Integer.BYTES);
        mergeSortCounted(arr, temp, 0, arr.length - 1, metrics, 1);
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

    // -------------------------------------------------------------------------
    // Count mode helpers (tách biệt khỏi timing mode)
    // -------------------------------------------------------------------------

    /**
     * Đệ quy top-down với tracking chiều sâu – dùng trong lượt count.
     *
     * <p>Cách đo bộ nhớ phụ: {@code extraMemoryBytes = N × 4 byte}
     * (mảng temp kiểu int[], cấp phát 1 lần duy nhất trong {@link #sortCounted}).
     *
     * <p>Cách đo max_depth: bất đầu từ {@code depth=1}, mỗi lần gọi đệ quy
     * thêm 1. max_depth = {@code ⌈log₂(N)⌉} với top-down merge sort.
     *
     * @param depth độ sâu hiện tại (bắt đầu từ 1 ở lần gọi đầu tiên)
     */
    private static void mergeSortCounted(int[] arr, int[] temp,
                                          int left, int right, MergeMetrics m, int depth) {
        m.updateDepth(depth);
        if (left >= right) {
            return;
        }
        int mid = left + (right - left) / 2;
        mergeSortCounted(arr, temp, left,    mid,   m, depth + 1);
        mergeSortCounted(arr, temp, mid + 1, right, m, depth + 1);
        merge(arr, temp, left, mid, right, m);
    }
}
