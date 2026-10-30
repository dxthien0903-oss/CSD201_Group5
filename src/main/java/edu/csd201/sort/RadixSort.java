package edu.csd201.sort;

import java.util.Arrays;
import java.util.Objects;

/**
 * Radix Sort LSD, cơ số 256, 4 lượt cho khóa int 32-bit.
 *
 * <p>Mỗi lượt là một counting sort ỔN ĐỊNH theo một byte của khóa (từ byte thấp đến byte cao) và dùng
 * MỘT mảng phụ kích thước N (cấp phát một lần). 4 lượt là số chẵn nên kết quả cuối nằm lại trong mảng
 * đầu vào, không cần sao chép.
 *
 * <p>Đề quy định khóa không âm (0 .. 2^31 - 1). Để chạy đúng cả trên dữ liệu có số âm (ví dụ các
 * test và phân phối tạm của nhóm), lượt byte cao nhất đảo bit dấu (XOR 0x80): số âm có bit dấu 1
 * nên sẽ đứng trước số không âm. Khóa không âm không bị ảnh hưởng, số lượt và số thao tác không đổi.
 *
 * <p>Quy ước đếm thao tác (chỉ ở bản có {@link RadixMetrics}): chỉ đếm đọc/ghi trên hai mảng khóa
 * (nguồn và đích), KHÔNG đếm mảng đếm cnt[257]. Mỗi lượt đọc 2N lần (histogram + rải) và ghi N lần,
 * nên với N &gt;= 2 tổng là 8N lần đọc, 4N lần ghi, 12N thao tác, không phụ thuộc dữ liệu.
 * Radix Sort không so sánh các khóa với nhau nên không có "số phép so sánh".
 */
public final class RadixSort {

    private static final int RADIX = 256;
    private static final int PASSES = 4;

    private RadixSort() {
    }

    // -------------------------------------------------------------------------
    // Metrics
    // -------------------------------------------------------------------------

    /** Bộ đếm cho một lần chạy bản có đếm. Mutator package-private, chỉ RadixSort được ghi. */
    public static final class RadixMetrics {
        private long reads;
        private long writes;
        private long extraMemoryBytes;

        void addReads(long v)               { reads += v; }
        void addWrites(long v)              { writes += v; }
        void setExtraMemory(long bytes)     { extraMemoryBytes = bytes; }

        /** Số lần đọc mảng khóa (không tính mảng đếm). */
        public long reads()                 { return reads; }
        /** Số lần ghi mảng khóa (không tính mảng đếm). */
        public long writes()                { return writes; }
        /** Tổng thao tác = đọc + ghi (12N với N &gt;= 2). */
        public long operations()            { return reads + writes; }
        /** Số byte của mảng phụ N phần tử (= 4N byte), chưa tính mảng đếm 257 phần tử. */
        public long extraMemoryBytes()      { return extraMemoryBytes; }

        public void reset() {
            reads = 0;
            writes = 0;
            extraMemoryBytes = 0;
        }

        @Override
        public String toString() {
            return String.format("reads=%d writes=%d extraMem=%d B", reads, writes, extraMemoryBytes);
        }
    }

    // -------------------------------------------------------------------------
    // Public API
    // -------------------------------------------------------------------------

    /** Bản dùng để ĐO THỜI GIAN: không có bộ đếm. */
    public static void sort(int[] arr) {
        Objects.requireNonNull(arr, "arr");
        final int n = arr.length;
        if (n < 2) {
            return;
        }
        int[] src = arr;
        int[] dst = new int[n];
        final int[] cnt = new int[RADIX + 1];
        for (int pass = 0; pass < PASSES; pass++) {
            final int shift = pass * 8;
            final int flip = (pass == PASSES - 1) ? 0x80 : 0; // chỉ byte cao nhất: đảo bit dấu
            Arrays.fill(cnt, 0);
            // 1) histogram: cnt[d + 1]++
            for (int i = 0; i < n; i++) {
                cnt[(((src[i] >>> shift) ^ flip) & 0xFF) + 1]++;
            }
            // 2) cộng dồn: cnt[d] = vị trí bắt đầu của chữ số d trong mảng đích
            for (int r = 0; r < RADIX; r++) {
                cnt[r + 1] += cnt[r];
            }
            // 3) rải theo thứ tự cũ, nên ổn định
            for (int i = 0; i < n; i++) {
                int v = src[i];
                dst[cnt[((v >>> shift) ^ flip) & 0xFF]++] = v;
            }
            int[] t = src;
            src = dst;
            dst = t;
        }
        // PASSES chẵn: kết quả cuối nằm lại trong arr.
    }

    /** Bản dùng cho lượt ĐẾM: cùng thuật toán, có ghi {@code metrics}. Không dùng để đo thời gian. */
    public static void sort(int[] arr, RadixMetrics metrics) {
        Objects.requireNonNull(arr, "arr");
        Objects.requireNonNull(metrics, "metrics");
        final int n = arr.length;
        if (n < 2) {
            return;
        }
        metrics.setExtraMemory((long) n * Integer.BYTES);
        long reads = 0;
        long writes = 0;
        int[] src = arr;
        int[] dst = new int[n];
        final int[] cnt = new int[RADIX + 1];
        for (int pass = 0; pass < PASSES; pass++) {
            final int shift = pass * 8;
            final int flip = (pass == PASSES - 1) ? 0x80 : 0;
            Arrays.fill(cnt, 0);
            for (int i = 0; i < n; i++) {
                reads++;
                cnt[(((src[i] >>> shift) ^ flip) & 0xFF) + 1]++;
            }
            for (int r = 0; r < RADIX; r++) {
                cnt[r + 1] += cnt[r];
            }
            for (int i = 0; i < n; i++) {
                reads++;
                int v = src[i];
                writes++;
                dst[cnt[((v >>> shift) ^ flip) & 0xFF]++] = v;
            }
            int[] t = src;
            src = dst;
            dst = t;
        }
        metrics.addReads(reads);
        metrics.addWrites(writes);
    }

    /**
     * Kiểm tra tính đúng đắn: so sánh kết quả với Arrays.sort.
     *
     * @param input mảng gốc (KHÔNG bị thay đổi)
     * @return {@code true} nếu RadixSort cho kết quả khớp Arrays.sort
     */
    public static boolean verify(int[] input) {
        int[] expected = input.clone();
        int[] actual = input.clone();
        Arrays.sort(expected);
        sort(actual);
        return Arrays.equals(expected, actual);
    }
}
