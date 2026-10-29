package edu.csd201.sort;

/**
 * Radix Sort LSD, cơ số 256, 4 lượt cho khóa int không âm (32-bit).
 * Mỗi lượt là một counting sort ổn định, dùng MỘT mảng phụ kích thước N.
 *
 * Quy ước đếm thao tác (--count): chỉ đếm đọc/ghi trên mảng khóa (src, dst),
 * KHÔNG đếm mảng đếm cnt[257]. Phải ghi quy ước này vào paper.
 * Mỗi lượt đọc 2N lần (histogram + rải) và ghi N lần, nên tổng là 8N đọc, 4N ghi, 12N thao tác
 * với N >= 2, không phụ thuộc dữ liệu. Với N < 2 hàm trả về ngay, không thao tác gì.
 *
 * Điều kiện đầu vào (theo đề): mọi khóa trong [0, 2^31 - 1]. Với khóa âm, kết quả sai vì bit dấu
 * không được xử lý; Worker phát hiện bằng bước so với Arrays.sort (status = wrong).
 */
public final class RadixSort {

    public static long reads, writes;

    public static void reset() { reads = 0; writes = 0; }

    /** Bản dùng để đo thời gian: không có bộ đếm. */
    public static void sort(int[] a) {
        final int n = a.length;
        if (n < 2) return;
        int[] src = a, dst = new int[n];
        final int[] cnt = new int[257];
        for (int shift = 0; shift < 32; shift += 8) {
            for (int r = 0; r < 257; r++) cnt[r] = 0;
            // 1) histogram: cnt[d + 1]++
            for (int i = 0; i < n; i++) cnt[((src[i] >>> shift) & 0xFF) + 1]++;
            // 2) tổng tích lũy: cnt[d] = vị trí bắt đầu của chữ số d
            for (int r = 0; r < 256; r++) cnt[r + 1] += cnt[r];
            // 3) rải phần tử theo thứ tự cũ -> ổn định
            for (int i = 0; i < n; i++) {
                int v = src[i];
                dst[cnt[(v >>> shift) & 0xFF]++] = v;
            }
            int[] t = src; src = dst; dst = t;
        }
        // 4 lượt (số chẵn) -> kết quả cuối nằm lại trong a, không cần copy.
    }

    /** Bản dùng cho lượt --count: giống hệt sort() nhưng có đếm đọc/ghi. */
    public static void sortCounting(int[] a) {
        final int n = a.length;
        if (n < 2) return;
        long r = 0, w = 0;
        int[] src = a, dst = new int[n];
        final int[] cnt = new int[257];
        for (int shift = 0; shift < 32; shift += 8) {
            for (int d = 0; d < 257; d++) cnt[d] = 0;
            for (int i = 0; i < n; i++) { r++; cnt[((src[i] >>> shift) & 0xFF) + 1]++; }
            for (int d = 0; d < 256; d++) cnt[d + 1] += cnt[d];
            for (int i = 0; i < n; i++) {
                r++;
                int v = src[i];
                w++;
                dst[cnt[(v >>> shift) & 0xFF]++] = v;
            }
            int[] t = src; src = dst; dst = t;
        }
        reads += r;
        writes += w;
    }
}
