package edu.csd201.sort;

import java.util.Arrays;
import java.util.Random;

/**
 * Tạo dữ liệu theo 5 phân phối chuẩn dùng cho benchmark thuật toán sắp xếp.
 *
 * <pre>
 * D1 – Random (ngẫu nhiên hoàn toàn)
 * D2 – Sorted (tăng dần)
 * D3 – D2 đảo ngược (giảm dần – worst-case điển hình)
 * D4 – D2 sau khi hoán đổi ngẫu nhiên N/1.000 cặp (gần tăng dần)
 * D5 – Many duplicates (phạm vi giá trị K → nhiều trùng lặp, mặc định K=√n)
 * </pre>
 *
 * Mỗi phương thức {@code generate*()} trả về mảng MỚI, không thay đổi tham số.
 *
 * <p><b>Lịch sử sửa đổi:</b>
 * <ul>
 *   <li>D4: swaps = N/1000 (theo đặc tả đề bài), trước đây dùng N/20</li>
 *   <li>D5: thêm {@link #generateManyDuplicatesK} hỗ trợ K tuỳ chỉnh cho RQ2</li>
 * </ul>
 */
public final class DataGenerator {

    /** Nhãn tất cả phân phối theo thứ tự. */
    public static final String[] DISTRIBUTIONS = {"D1", "D2", "D3", "D4", "D5"};

    private DataGenerator() {
    }

    // -------------------------------------------------------------------------
    // Dispatcher chính
    // -------------------------------------------------------------------------

    /**
     * Tạo mảng {@code int[size]} theo phân phối {@code distribution}.
     *
     * @param distribution "D1" .. "D5"
     * @param size         số phần tử (phải &gt;= 0)
     * @param random       nguồn ngẫu nhiên (để đảm bảo reproducibility, dùng seed cố định)
     * @return mảng mới, sẵn sàng để sắp xếp
     */
    public static int[] generate(String distribution, int size, Random random) {
        return switch (distribution) {
            case "D1" -> generateRandom(size, random);
            case "D2" -> generateSorted(size);
            case "D3" -> generateReverse(size);
            case "D4" -> generateNearlySorted(size, random);
            case "D5" -> generateManyDuplicates(size, random);
            default   -> throw new IllegalArgumentException(
                    "Unknown distribution: " + distribution + ". Expected D1..D5");
        };
    }

    // -------------------------------------------------------------------------
    // D1 – Random
    // -------------------------------------------------------------------------

    /**
     * D1: Mảng ngẫu nhiên hoàn toàn.
     * Giá trị lấy từ toàn bộ dải int (Integer.MIN_VALUE .. Integer.MAX_VALUE).
     */
    public static int[] generateRandom(int size, Random random) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt();
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // D2 – Sorted (Tăng dần)
    // -------------------------------------------------------------------------

    /**
     * D2: Mảng đã sắp xếp tăng dần (0, 1, 2, ..., size-1).
     * Best-case điển hình cho nhiều thuật toán.
     */
    public static int[] generateSorted(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = i;
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // D3 – Reverse Sorted (Giảm dần)
    // -------------------------------------------------------------------------

    /**
     * D3: Mảng đã sắp xếp giảm dần (size-1, size-2, ..., 1, 0).
     * Worst-case với insertion sort và các thuật toán nhạy cảm với thứ tự.
     */
    public static int[] generateReverse(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = size - 1 - i;
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // D4 – Nearly Sorted (Gần tăng dần)
    // -------------------------------------------------------------------------

    /**
     * D4: D2 sau khi hoán đổi ngẫu nhiên N/1.000 cặp vị trí.
     *
     * <p>Số cặp hoán vị = {@code max(1, size / 1000)} (làm tròn xuống).
     * Phối hợp TV1: D4 là D2 (sorted) với N/1.000 cặp bị hoán vị ngẫu nhiên.
     *
     * <p><b>Lưu ý:</b> Phiên bản cũ dùng {@code size/20} (~5%) – đã sửa thành
     * {@code size/1000} (0.1%) theo đặc tả đề bài.
     */
    public static int[] generateNearlySorted(int size, Random random) {
        int[] arr = generateSorted(size);
        if (size < 2) {
            return arr; // không thể hoán vị khi chưa đủ 2 phần tử
        }
        int swaps = Math.max(1, size / 1000); // N/1.000 cặp theo đặc tả
        for (int i = 0; i < swaps; i++) {
            int a = random.nextInt(size);
            int b = random.nextInt(size);
            int tmp = arr[a];
            arr[a]  = arr[b];
            arr[b]  = tmp;
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // D5 – Many Duplicates (Nhiều giá trị trùng lặp)
    // -------------------------------------------------------------------------

    /**
     * D5: Mảng ngẫu nhiên với phạm vi giá trị nhỏ ≈ √n (mặc định).
     *
     * <p>Ví dụ n=10 000 → phạm vi ≈ 100, tức trung bình mỗi giá trị xuất hiện ~100 lần.
     * Test case căng thẳng cho partition three-way và so sánh bằng nhau.
     *
     * @see #generateManyDuplicatesK(int, int, Random) cho K tuỳ chỉnh (dùng trong RQ2)
     */
    public static int[] generateManyDuplicates(int size, Random random) {
        int range = Math.max(1, (int) Math.sqrt(size)); // ~√n giá trị khác nhau
        return generateManyDuplicatesK(size, range, random);
    }

    /**
     * D5 với K tuỳ chỉnh: mảng ngẫu nhiên trong phạm vi [0, K).
     *
     * <p>Dùng cho RQ2 với K = 1.000, K = 10, K = 2.
     * K = 2 tương đương mảng chỉ gồm 0 và 1 (căng thẳng nhất).
     *
     * @param size   số phần tử
     * @param k      số giá trị phân biệt (phạm vi [0, k))
     * @param random nguồn ngẫu nhiên
     * @return mảng mới kích thước {@code size}
     */
    public static int[] generateManyDuplicatesK(int size, int k, Random random) {
        if (k < 1) throw new IllegalArgumentException("k must be >= 1, got: " + k);
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = random.nextInt(k);
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // Tiện ích
    // -------------------------------------------------------------------------

    /**
     * Kiểm tra nhanh tất cả 5 phân phối cho một kích thước.
     * Dùng trong unit test hoặc debug.
     */
    public static void selfTest(int size) {
        Random rng = new Random(42);
        for (String dist : DISTRIBUTIONS) {
            int[] arr = generate(dist, size, rng);
            if (arr.length != size) {
                throw new AssertionError(dist + ": length mismatch");
            }
        }
        System.out.println("DataGenerator self-test OK for size=" + size);
    }

    /** Entry point tiện lợi: in mẫu nhỏ ra console. */
    public static void main(String[] args) {
        Random rng = new Random(20261016L);
        for (String dist : DISTRIBUTIONS) {
            int[] sample = generate(dist, 20, rng);
            System.out.println(dist + ": " + Arrays.toString(sample));
        }
        selfTest(10_000);
    }
}
