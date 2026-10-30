package edu.csd201.sort;

import java.util.Arrays;
import java.util.Random;

/**
 * Tạo dữ liệu theo 5 phân phối chuẩn dùng cho benchmark thuật toán sắp xếp.
 *
 * <pre>
 * D1 – Random (ngẫu nhiên hoàn toàn)
 * D2 – Sorted (tăng dần)
 * D3 – D2 sau khi hoán đổi ngẫu nhiên N/1.000 cặp (gần tăng dần)
 * D4 – D2 đảo ngược (giảm dần – worst-case điển hình)
 * D5 – Many duplicates (phạm vi giá trị K → nhiều trùng lặp, mặc định K=√n)
 * </pre>
 *
 * Mỗi phương thức {@code generate*()} trả về mảng MỚI, không thay đổi tham số.
 *
 * <p><b>Lịch sử sửa đổi:</b>
 * <ul>
 *   <li>D3: swaps = N/1000 theo đặc tả – D2 + hoán đổi N/1.000 cặp ngẫu nhiên</li>
 *   <li>D4: D2 đảo ngược (giảm dần) theo đặc tả đề bài</li>
 *   <li>D5: thêm {@link #generateManyDuplicatesK} hỗ trợ K tuỳ chỉnh cho RQ2</li>
 *   <li>TV4: D5 sinh K giá trị phân biệt (miền 0 .. 2^31-1) từ nguồn ngẫu nhiên rồi chọn đều trong K giá trị đó,
 *       đúng đề; thêm {@link #generateSpec} (D1-D5 đúng đề, tái lập theo seed) cho bộ đo chạy từng JVM riêng</li>
 * </ul>
 *
 * <p>{@link #generate} (D1-D4 đã sửa đúng đề) dùng cho Benchmark; {@link #generateSpec} dùng cho MeasureRunner (seed riêng từng lượt).
 */
public final class DataGenerator {

    /** Nhãn tất cả phân phối theo thứ tự. */
    public static final String[] DISTRIBUTIONS = {"D1", "D2", "D3", "D4", "D5"};

    /** Phiên bản của {@link #generateSpec}; ghi vào cột generator_version của CSV đo. */
    public static final String SPEC_GENERATOR_VERSION = "gen-1.2";

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
            case "D3" -> generateNearlySorted(size, random);
            case "D4" -> generateReverse(size);
            case "D5" -> generateManyDuplicates(size, random);
            default   -> throw new IllegalArgumentException(
                    "Unknown distribution: " + distribution + ". Expected D1..D5");
        };
    }

    // TV1 D1/D2 helpers (SplittableRandom)
    public static int[] d1(int n, long seed) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0: " + n);
        java.util.SplittableRandom rnd = new java.util.SplittableRandom(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = rnd.nextInt() & 0x7FFFFFFF;
        return a;
    }

    public static int[] d2(int[] d1) {
        if (d1 == null) throw new NullPointerException("d1");
        int[] a = d1.clone();
        java.util.Arrays.sort(a);
        return a;
    }

    public static int[] d2(int n, long seed) {
        return d2(d1(n, seed));
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
            arr[i] = random.nextInt() & 0x7FFFFFFF;
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
    // D4 – Reverse Sorted (Giảm dần)
    // -------------------------------------------------------------------------

    /**
     * D4: D2 đảo ngược – mảng giảm dần (size-1, size-2, ..., 1, 0).
     *
     * <p>Đúng theo đặc tả: D4 = D2 đảo ngược.
     * Worst-case với insertion sort và các thuật toán nhạy cảm thứ tự.
     */
    public static int[] generateReverse(int size) {
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = size - 1 - i;
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // D3 – Nearly Sorted (Gần tăng dần)
    // -------------------------------------------------------------------------

    /**
     * D3: D2 sau khi hoán đổi ngẫu nhiên N/1.000 cặp vị trí.
     *
     * <p>Số cặp hoán vị = {@code max(1, size / 1000)} (làm tròn xuống).
     * Đúng theo đặc tả đề bài: D3 = lấy D2 rồi hoán đổi N/1.000 cặp ngẫu nhiên.
     *
     * <p><b>Phối hợp TV1:</b> D3 phụ thuộc D2 do TV1 sinh; cùng seed và multiset.
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
     * D5 với K tuỳ chỉnh (đúng đề): sinh trước K giá trị PHÂN BIỆT trong [0, 2^31 - 1] từ {@code random},
     * sau đó mỗi phần tử chọn đều ngẫu nhiên trong K giá trị đó.
     *
     * <p>Dùng cho RQ2 với K = 1.000, K = 10, K = 2. K = 1 cho mảng toàn bằng nhau.
     * Cùng trạng thái của {@code random} cho cùng mảng (tái lập theo seed). Nếu K &gt; size thì không đủ
     * K giá trị xuất hiện, vẫn hợp lệ.
     *
     * @param size   số phần tử
     * @param k      số giá trị phân biệt (k &gt;= 1)
     * @param random nguồn ngẫu nhiên
     * @return mảng mới kích thước {@code size}
     */
    public static int[] generateManyDuplicatesK(int size, int k, Random random) {
        if (k < 1) throw new IllegalArgumentException("k must be >= 1, got: " + k);
        int[] values = new int[k];
        java.util.HashSet<Integer> used = new java.util.HashSet<>(); // chỉ để loại trùng khi sinh K giá trị
        for (int i = 0; i < k; ) {
            int v = random.nextInt() & Integer.MAX_VALUE; // miền 0 .. 2^31 - 1
            if (used.add(v)) values[i++] = v;
        }
        int[] arr = new int[size];
        for (int i = 0; i < size; i++) {
            arr[i] = values[random.nextInt(k)];
        }
        return arr;
    }

    // -------------------------------------------------------------------------
    // D1-D5 ĐÚNG ĐỀ (dùng cho bộ đo chạy từng JVM riêng: MeasureRunner / MeasureWorker)
    // -------------------------------------------------------------------------

    /**
     * Sinh dữ liệu đúng bảng D1-D5 của đề, tái lập hoàn toàn theo {@code seed}.
     *
     * <ul>
     *   <li>D1: N giá trị đều trên [0, 2^31 - 1], {@link java.util.SplittableRandom}(seed)</li>
     *   <li>D2: bản sao của D1 sắp tăng dần</li>
     *   <li>D3: D2 rồi hoán đổi N/1.000 cặp vị trí khác nhau chọn ngẫu nhiên (N &lt; 1.000 thì giống D2)</li>
     *   <li>D4: D2 đảo ngược</li>
     *   <li>D5: K giá trị phân biệt sinh từ seed, mỗi phần tử chọn đều trong K giá trị đó (cần k &gt;= 1)</li>
     * </ul>
     *
     * @param dataset "D1" .. "D5"
     * @param n       số phần tử (&gt;= 0)
     * @param k       chỉ dùng cho D5; các dataset khác bỏ qua
     * @param seed    seed của lượt chạy
     */
    public static int[] generateSpec(String dataset, int n, int k, long seed) {
        if (n < 0) throw new IllegalArgumentException("n must be >= 0, got: " + n);
        switch (dataset) {
            case "D1": return specD1(n, seed);
            case "D2": return specD2(n, seed);
            case "D3": return specD3(n, seed);
            case "D4": return specD4(n, seed);
            case "D5": return generateManyDuplicatesK(n, k, new Random(seed));
            default: throw new IllegalArgumentException("Unknown dataset: " + dataset + ". Expected D1..D5");
        }
    }

    private static int[] specD1(int n, long seed) {
        java.util.SplittableRandom r = new java.util.SplittableRandom(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = r.nextInt() & Integer.MAX_VALUE;
        return a;
    }

    private static int[] specD2(int n, long seed) {
        int[] a = specD1(n, seed);
        Arrays.sort(a); // chỉ trong generator, không nằm trong vùng đo
        return a;
    }

    private static int[] specD3(int n, long seed) {
        int[] a = specD2(n, seed);
        java.util.SplittableRandom r = new java.util.SplittableRandom(seed ^ 0x9E3779B97F4A7C15L);
        int swaps = n / 1000;
        for (int s = 0; s < swaps; s++) {
            int i = r.nextInt(n), j = r.nextInt(n - 1); // n >= 1000 nên n - 1 >= 1
            if (j >= i) j++;                              // hai vị trí khác nhau: mỗi lần là một hoán đổi thật
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
        return a;
    }

    private static int[] specD4(int n, long seed) {
        int[] a = specD2(n, seed);
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
        return a;
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
