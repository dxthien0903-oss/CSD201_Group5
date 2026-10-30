package edu.csd201.sort;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Benchmark so sánh MergeSort với PdqSort trên 5 phân phối D1–D5.
 * Các chế độ rq1/rq2 chạy các thuật toán trong {@link #ALGOS} (hiện có MergeSort và RadixSort);
 * thêm thuật toán khác bằng cách thêm tên vào ALGOS và một nhánh trong runOne/verifyQuick.
 *
 * <h2>Chế độ chạy</h2>
 * <ul>
 *   <li><b>rq1</b>  – D1 với N = 10⁵, 10⁶, 5×10⁶, 10⁷, 2×10⁷ (và 5×10⁷ nếu đủ RAM)</li>
 *   <li><b>rq2</b>  – D1–D4 + D5(K=1000,10,2), N=10⁶ và N=10⁷, 3 seed cho D1/D3/D5</li>
 *   <li><b>full</b> – Chạy cả hai RQ (mặc định)</li>
 *   <li><b>verify-only</b> – Chỉ kiểm tra tính đúng đắn</li>
 * </ul>
 *
 * <h2>CSV Schema</h2>
 * <pre>
 * algorithm, distribution, size, run, timeMs, comparisons, assignments,
 * extraMemBytes, max_depth, output_check, status
 * </pre>
 *
 * <h2>Cách chạy</h2>
 * <pre>
 * mvn -q package -DskipTests
 * java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark rq1
 * java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark rq2
 * java -Xmx6g -cp target/classes edu.csd201.sort.Benchmark full
 * </pre>
 */
public final class Benchmark {

    // -------------------------------------------------------------------------
    // Cấu hình RQ1
    // -------------------------------------------------------------------------

    /** Kích thước cho RQ1: D1, N = 10⁵ .. 2×10⁷ (5×10⁷ nếu đủ RAM). */
    private static final long[] RQ1_SIZES = {
        100_000L, 1_000_000L, 5_000_000L, 10_000_000L, 20_000_000L
        // thêm 50_000_000L nếu máy >= 4 GB heap
    };

    // -------------------------------------------------------------------------
    // Cấu hình RQ2
    // -------------------------------------------------------------------------

    /** Kích thước cho RQ2 theo đặc tả. */
    private static final long[] RQ2_SIZES = {1_000_000L, 10_000_000L};

    /**
     * Phân phối D1–D4 dùng 1 seed cố định;
     * D5 chạy 3 lần với K = 1000, 10, 2 và mỗi K dùng 3 seed.
     * D1, D3 dùng 3 seed theo đặc tả.
     */
    private static final long[] RQ2_SEEDS = {20261016L, 20261017L, 20261018L};

    /** K-values cho D5 trong RQ2. */
    private static final int[] D5_K_VALUES = {1000, 10, 2};

    // -------------------------------------------------------------------------
    // Cấu hình chung
    // -------------------------------------------------------------------------

    /** Các thuật toán chạy trong rq1/rq2 (tên khớp nhánh trong runOne và verifyQuick). */
    static final String[] ALGOS = {"QuickSort", "MergeSort", "RadixSort"};

    private static final int    DEFAULT_WARMUP   = 2;
    private static final int    DEFAULT_RUNS     = 5;
    private static final long   DEFAULT_SEED     = 20261016L;
    private static final String CSV_RQ1          = "results/rq1_benchmark.csv";
    private static final String CSV_RQ2          = "results/rq2_benchmark.csv";
    private static final String CSV_FULL         = "results/benchmark.csv";
    private static final String DEFAULT_VERIFY_LOG = "results/verify.log";

    private static final String CSV_HEADER =
        "algorithm,distribution,size,run,timeMs,comparisons,assignments," +
        "extraMemBytes,max_depth,output_check,status\n";

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        // Parse chế độ chạy
        String mode    = "full";
        int    warmup  = DEFAULT_WARMUP;
        int    runs    = DEFAULT_RUNS;
        boolean addBig = false; // thêm 5×10⁷ cho RQ1

        for (String arg : args) {
            switch (arg) {
                case "rq1"         -> mode = "rq1";
                case "rq2"         -> mode = "rq2";
                case "full"        -> mode = "full";
                case "verify-only" -> mode = "verify-only";
                case "big"         -> addBig = true;
                default -> {
                    if (arg.startsWith("warmup=")) warmup = Integer.parseInt(arg.substring(7));
                    else if (arg.startsWith("runs="))  runs   = Integer.parseInt(arg.substring(5));
                }
            }
        }

        // 1. Kiểm tra tính đúng đắn
        System.out.println("=== Correctness Verification ===");
        boolean ok = runVerification(DEFAULT_SEED, DEFAULT_VERIFY_LOG);
        if (!ok) {
            System.err.println("[FATAL] Correctness check FAILED. Aborting.");
            System.exit(1);
        }
        System.out.println("All correctness checks PASSED.\n");
        if ("verify-only".equals(mode)) return;

        // 2. Chạy theo chế độ
        long[] rq1Sizes = addBig
            ? appendSize(RQ1_SIZES, 50_000_000L)
            : RQ1_SIZES;

        switch (mode) {
            case "rq1"  -> runRQ1(warmup, runs, rq1Sizes, CSV_RQ1);
            case "rq2"  -> runRQ2(warmup, runs, RQ2_SIZES, CSV_RQ2);
            default     -> {                     // "full"
                StringBuilder all = new StringBuilder(CSV_HEADER);
                all.append(runRQ1body(warmup, runs, rq1Sizes));
                all.append(runRQ2body(warmup, runs, RQ2_SIZES));
                writeCSV(CSV_FULL, all.toString());
                System.out.println("\nFull CSV saved → " + Path.of(CSV_FULL).toAbsolutePath());
            }
        }
    }

    // =========================================================================
    // CORRECTNESS VERIFICATION
    // =========================================================================

    static boolean runVerification(long seed, String logPath) throws IOException {
        Random rng = new Random(seed);
        StringBuilder log = new StringBuilder();
        log.append("seed,").append(seed).append('\n');

        int failures = 0;

        // --- 5 ca cố định (edge cases) ---
        int[][] edgeCases = {
            {},
            {42},
            {0, 0},
            {Integer.MIN_VALUE, Integer.MAX_VALUE},
            {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1},
            {5, 5, 5, 5, 5},
            {9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
            java.util.stream.IntStream.range(0, 25).toArray(),
            java.util.stream.IntStream.rangeClosed(0, 128).map(i -> 128 - i).toArray()
        };

        for (int t = 0; t < edgeCases.length; t++) {
            failures += verifyOne(edgeCases[t], "edge-" + t, log);
        }

        // --- 1.000 mảng ngẫu nhiên kích thước 0–100 ---
        Random randTest = new Random(seed + 1);
        for (int t = 0; t < 1000; t++) {
            int sz = randTest.nextInt(101); // 0..100
            int[] input = new int[sz];
            for (int i = 0; i < sz; i++) input[i] = randTest.nextInt(201) - 100;
            failures += verifyOne(input, "rand-" + t + "-sz" + sz, log);
        }

        // --- Mảng 100.000 phần tử đặc biệt ---
        failures += verifyOne(DataGenerator.generateSorted(100_000),  "100k-sorted",   log);
        failures += verifyOne(DataGenerator.generateReverse(100_000), "100k-reversed",  log);
        failures += verifyOne(DataGenerator.generateManyDuplicatesK(100_000, 1, rng),
                              "100k-all-equal", log);

        // --- Phân phối D1–D5 nhiều kích thước ---
        int[] verifySizes = {0, 1, 2, 3, 10, 100, 1000};
        for (int size : verifySizes) {
            for (String dist : DataGenerator.DISTRIBUTIONS) {
                int[] input = DataGenerator.generate(dist, size, rng);
                failures += verifyOne(input, "size=" + size + ",dist=" + dist, log);
            }
        }

        log.append("failures,").append(failures).append('\n');
        Path out = Path.of(logPath);
        Files.createDirectories(out.getParent());
        Files.writeString(out, log);

        if (failures > 0) {
            System.err.println("[ERROR] " + failures + " failure(s). See " + logPath);
        }
        return failures == 0;
    }

    private static int verifyOne(int[] input, String label, StringBuilder log) {
        int[] expected = input.clone();
        int[] actual   = input.clone();
        Arrays.sort(expected);
        MergeSort.sort(actual);
        int[] quick = input.clone();
        QuickSort.sort(quick);
        if (!Arrays.equals(expected, quick)) {
            log.append("FAIL,").append(label).append(",quick\n");
            System.err.println("  FAIL (quick): " + label);
            return 1;
        }
        if (!Arrays.equals(expected, actual)) {
            log.append("FAIL,").append(label).append('\n');
            System.err.println("  FAIL: " + label);
            return 1;
        }
        int[] radix = input.clone();
        RadixSort.sort(radix);
        if (!Arrays.equals(expected, radix)) {
            log.append("FAIL,").append(label).append(",radix\n");
            System.err.println("  FAIL (radix): " + label);
            return 1;
        }
        log.append("PASS,").append(label).append('\n');
        return 0;
    }

    // =========================================================================
    // RQ1 – Scalability: D1, N tăng dần
    // =========================================================================

    private static void runRQ1(int warmup, int runs, long[] sizes, String csvPath) throws IOException {
        System.out.println("=== RQ1: Scalability (D1) ===");
        String body = runRQ1body(warmup, runs, sizes);
        writeCSV(csvPath, CSV_HEADER + body);
        System.out.println("RQ1 CSV saved → " + Path.of(csvPath).toAbsolutePath());
    }

    /** Trả về phần body CSV của RQ1 (không có header). */
    private static String runRQ1body(int warmup, int runs, long[] sizes) {
        StringBuilder sb = new StringBuilder();
        printTableHeader();

        for (long size : sizes) {
            if (size > Integer.MAX_VALUE) {
                System.err.println("[SKIP] size=" + size + " vượt quá Integer.MAX_VALUE");
                continue;
            }
            int n = (int) size;

            // Chạy mọi thuật toán trong ALGOS cho RQ1
            runAll(sb, "D1", n, warmup, runs, DEFAULT_SEED, -1);
            System.out.println();
        }
        return sb.toString();
    }

    // =========================================================================
    // RQ2 – Distribution: D1–D4 + D5(K), N=10⁶ và 10⁷
    // =========================================================================

    private static void runRQ2(int warmup, int runs, long[] sizes, String csvPath) throws IOException {
        System.out.println("=== RQ2: Distribution Comparison ===");
        String body = runRQ2body(warmup, runs, sizes);
        writeCSV(csvPath, CSV_HEADER + body);
        System.out.println("RQ2 CSV saved → " + Path.of(csvPath).toAbsolutePath());
    }

    /** Trả về phần body CSV của RQ2 (không có header). */
    private static String runRQ2body(int warmup, int runs, long[] sizes) {
        StringBuilder sb = new StringBuilder();
        printTableHeader();

        for (long size : sizes) {
            if (size > Integer.MAX_VALUE) {
                System.err.println("[SKIP] size=" + size + " vượt quá Integer.MAX_VALUE");
                continue;
            }
            int n = (int) size;

            // D1 – 3 seed
            for (long seed : RQ2_SEEDS) {
                runAll(sb, "D1", n, warmup, runs, seed, -1);
            }

            // D2 – 1 seed (deterministic, seed không ảnh hưởng)
            runAll(sb, "D2", n, warmup, runs, DEFAULT_SEED, -1);

            // D3 – 3 seed
            for (long seed : RQ2_SEEDS) {
                runAll(sb, "D3", n, warmup, runs, seed, -1);
            }

            // D4 – 3 seed
            for (long seed : RQ2_SEEDS) {
                runAll(sb, "D4", n, warmup, runs, seed, -1);
            }

            // D5 – 3 K-values × 3 seed
            for (int k : D5_K_VALUES) {
                for (long seed : RQ2_SEEDS) {
                    runAll(sb, "D5_K" + k, n, warmup, runs, seed, k);
                }
            }

            System.out.println();
        }
        return sb.toString();
    }

    /** Chạy cùng một cấu hình (dist, size, seed, k) cho mọi thuật toán trong {@link #ALGOS}. */
    private static void runAll(StringBuilder sb, String dist, int size, int warmup, int runs,
                               long seed, int k) {
        for (String algo : ALGOS) {
            sb.append(runDistribution(algo, dist, size, warmup, runs, seed, k));
        }
    }

    // =========================================================================
    // Core: chạy một (algo, dist, size) toàn bộ warmup + runs
    // =========================================================================

    /**
     * Chạy đủ warmup + runs cho một cấu hình, trả về các dòng CSV.
     *
     * @param k  nếu >= 1 thì dùng generateManyDuplicatesK(size, k, rng) thay vì generate("D5",...)
     *           (chỉ áp dụng khi dist bắt đầu bằng "D5"); truyền -1 để dùng K mặc định (√n)
     */
    private static String runDistribution(String algo, String dist,
                                          int size, int warmup, int runs,
                                          long seed, int k) {
        // Seed riêng cho từng (size, dist, seed, k)
        long cellSeed = seed ^ ((long) size * 31L) ^ dist.hashCode() ^ (long) k;

        // Warmup
        for (int w = 0; w < warmup; w++) {
            runOne(algo, dist, size, new Random(cellSeed + w), k, null, null);
        }

        StringBuilder sb   = new StringBuilder();
        String fmt = "%-12s %-10s %12s  %10s  %16s  %16s  %12s%n";

        long totalNs  = 0;
        long totalCmp = 0;
        long totalAsn = 0;
        long totalMem = 0;
        int  okRuns   = 0;

        for (int r = 0; r < runs; r++) {
            Random rng    = new Random(cellSeed + r);
            long[] timeNs = new long[1];
            long[] met    = new long[4]; // [cmp, asn, mem, max_depth]

            String status = "OK";
            try {
                runOne(algo, dist, size, rng, k, timeNs, met);
                totalNs  += timeNs[0];
                totalCmp += met[0];
                totalAsn += met[1];
                totalMem += met[2];
                okRuns++;
            } catch (OutOfMemoryError oom) {
                status = "OOM";
                System.err.println("[OOM] " + algo + " " + dist + " N=" + size);
            } catch (Exception ex) {
                status = "ERROR:" + ex.getClass().getSimpleName();
                System.err.println("[ERR] " + algo + " " + dist + " N=" + size + " " + ex);
            }

            // output_check: sắp xếp có đúng không (verify run 0)
            String outputCheck = (r == 0) ? verifyQuick(algo, dist, size, cellSeed, k) : "skipped";

            sb.append(String.format(Locale.ROOT,
                "%s,%s,%d,%d,%.4f,%d,%d,%d,%d,%s,%s\n",
                algo, dist, size, r + 1,
                timeNs[0] / 1e6,
                met[0], met[1], met[2], met[3],
                outputCheck, status));
        }

        // In dòng tóm tắt trung bình ra console
        if (okRuns > 0) {
            System.out.printf(fmt,
                algo, dist,
                String.format(Locale.ROOT, "%,d", (long) size),
                String.format(Locale.ROOT, "%.2f", totalNs / 1e6 / okRuns),
                String.format(Locale.ROOT, "%,d", totalCmp / okRuns),
                String.format(Locale.ROOT, "%,d", totalAsn / okRuns),
                String.format(Locale.ROOT, "%.1f", totalMem / 1024.0 / okRuns));
        } else {
            System.out.printf(fmt, algo, dist,
                String.format(Locale.ROOT, "%,d", (long) size),
                "FAILED", "-", "-", "-");
        }

        return sb.toString();
    }

    /**
     * Chạy một lần đo.
     *
     * @param k          K cho D5 ({@code k >= 1}), hoặc -1 để dùng mặc định
     * @param timeNsOut  nếu != null, ghi ns vào [0]
     * @param metricsOut nếu != null, ghi [comparisons, assignments, extraMemBytes, max_depth]
     */
    private static void runOne(String algo, String dist, int size,
                                Random rng, int k,
                                long[] timeNsOut, long[] metricsOut) {
        int[] data = generateData(dist, size, rng, k);

        if ("QuickSort".equals(algo)) {
            int[] forCount = (metricsOut != null) ? data.clone() : null;
            long t0 = System.nanoTime();
            QuickSort.sort(data);
            long t1 = System.nanoTime();
            if (timeNsOut  != null) timeNsOut[0]  = t1 - t0;
            if (metricsOut != null) {
                QuickSort.Stats stats = QuickSort.sortCounted(forCount);
                metricsOut[0] = stats.comparisons;
                metricsOut[1] = 0L;
                metricsOut[2] = 0L;
                metricsOut[3] = stats.maxDepth;
            }
        } else if ("MergeSort".equals(algo)) {
            // Clone TRƯỚC khi sort để lượt sortCounted chạy trên dữ liệu gốc (đúng distribution)
            int[] forCount = (metricsOut != null) ? data.clone() : null;
            MergeSort.MergeMetrics m = new MergeSort.MergeMetrics();
            long t0 = System.nanoTime();
            MergeSort.sort(data, m);
            long t1 = System.nanoTime();
            if (timeNsOut  != null) timeNsOut[0]  = t1 - t0;
            if (metricsOut != null) {
                // Lượt đếm riêng (ngoài vùng đo thời gian) – chạy trên bản sao chưa sort
                MergeSort.MergeMetrics counted = new MergeSort.MergeMetrics();
                MergeSort.sortCounted(forCount, counted);
                metricsOut[0] = counted.comparisons();
                metricsOut[1] = counted.assignments();
                metricsOut[2] = m.extraMemoryBytes();   // bộ nhớ phụ lấy từ lượt timing (đã cấp phát N*4 B)
                metricsOut[3] = counted.maxDepth();     // chiều sâu đệ quy thực
            }
        } else if ("RadixSort".equals(algo)) {
            // Đo thời gian bằng bản KHÔNG đếm; số thao tác lấy từ một lượt đếm riêng, ngoài vùng đo.
            int[] forCount = (metricsOut != null) ? data.clone() : null;
            long t0 = System.nanoTime();
            RadixSort.sort(data);
            long t1 = System.nanoTime();
            if (timeNsOut  != null) timeNsOut[0]  = t1 - t0;
            if (metricsOut != null) {
                RadixSort.RadixMetrics m = new RadixSort.RadixMetrics();
                RadixSort.sort(forCount, m);
                metricsOut[0] = 0L;                    // Radix không so sánh các khóa
                metricsOut[1] = m.writes();            // số lần ghi mảng khóa (4N); số lần đọc = 2 × ghi
                metricsOut[2] = m.extraMemoryBytes();  // mảng phụ N phần tử (4N byte)
                metricsOut[3] = -1;                    // không đệ quy
            }
        } else { // PdqSort
            SortMetrics m = new SortMetrics();
            long t0 = System.nanoTime();
            PdqSort.sort(data, true, m);
            long t1 = System.nanoTime();
            if (timeNsOut  != null) timeNsOut[0]  = t1 - t0;
            if (metricsOut != null) {
                metricsOut[0] = m.partitionCount();
                metricsOut[1] = m.maxDepth();
                metricsOut[2] = 0L;
                metricsOut[3] = m.maxDepth();
            }
        }
    }

    /**
     * Tạo mảng dữ liệu đúng đặc tả, nhất quán với {@link DataGenerator#generateSpec}.
     *
     * <ul>
     *   <li>D1: N giá trị ngẫu nhiên không âm từ {@code rng}</li>
     *   <li>D2: clone của D1 sắp tăng dần</li>
     *   <li>D3: D2 sau đó hoán đổi N/1000 cặp vị trí khác nhau (seed phụ = nextLong)</li>
     *   <li>D4: D2 đảo ngược</li>
     *   <li>D5: {@link DataGenerator#generateManyDuplicatesK} với K tuỳ chỉnh, hoặc √n nếu k &lt; 1</li>
     * </ul>
     *
     * <p>D3 và D4 đều dùng D2 thực (giá trị ngẫu nhiên đã sắp xếp), không phải dãy 0,1,...,n-1.
     */
    private static int[] generateData(String dist, int size, Random rng, int k) {
        if (dist.startsWith("D5")) {
            int kk = (k >= 1) ? k : Math.max(1, (int) Math.sqrt(size));
            return DataGenerator.generateManyDuplicatesK(size, kk, rng);
        }
        switch (dist) {
            case "D1": {
                int[] a = new int[size];
                for (int i = 0; i < size; i++) a[i] = rng.nextInt() & 0x7FFFFFFF;
                return a;
            }
            case "D2": {
                int[] a = new int[size];
                for (int i = 0; i < size; i++) a[i] = rng.nextInt() & 0x7FFFFFFF;
                java.util.Arrays.sort(a);
                return a;
            }
            case "D3": {
                // D2 thực (ngẫu nhiên, đã sort) + N/1000 hoán đổi cặp khác nhau
                int[] a = new int[size];
                for (int i = 0; i < size; i++) a[i] = rng.nextInt() & 0x7FFFFFFF;
                java.util.Arrays.sort(a);
                if (size >= 2) {
                    long swapSeed = rng.nextLong(); // seed phụ tách biệt với D1 seed
                    Random sr = new Random(swapSeed);
                    int swaps = size / 1000;
                    for (int s = 0; s < swaps; s++) {
                        int i = sr.nextInt(size);
                        int j = sr.nextInt(size - 1);
                        if (j >= i) j++;            // đảm bảo i != j
                        int t = a[i]; a[i] = a[j]; a[j] = t;
                    }
                }
                return a;
            }
            case "D4": {
                // D2 thực (ngẫu nhiên, đã sort) đảo ngược
                int[] a = new int[size];
                for (int i = 0; i < size; i++) a[i] = rng.nextInt() & 0x7FFFFFFF;
                java.util.Arrays.sort(a);
                for (int lo = 0, hi = size - 1; lo < hi; lo++, hi--) {
                    int t = a[lo]; a[lo] = a[hi]; a[hi] = t;
                }
                return a;
            }
            default:
                return DataGenerator.generate(dist, size, rng);
        }
    }

    /**
     * Kiểm tra nhanh: sắp xếp mảng bằng MergeSort rồi so sánh với Arrays.sort.
     * Chỉ thực hiện nếu size <= 100_000 (để tránh mất quá nhiều thời gian).
     *
     * @return "PASS", "FAIL", hoặc "skip-large"
     */
    private static String verifyQuick(String algo, String dist, int size, long seed, int k) {
        if (size > 100_000) return "skip-large";
        try {
            int[] input    = generateData(dist, size, new Random(seed), k);
            int[] expected = input.clone();
            int[] actual   = input.clone();
            Arrays.sort(expected);
            if ("QuickSort".equals(algo)) {
                QuickSort.sort(actual);
            } else if ("MergeSort".equals(algo)) {
                MergeSort.sort(actual);
            } else if ("RadixSort".equals(algo)) {
                RadixSort.sort(actual);
            } else {
                PdqSort.sort(actual, true, new SortMetrics());
            }
            return Arrays.equals(expected, actual) ? "PASS" : "FAIL";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    // =========================================================================
    // Tiện ích
    // =========================================================================

    private static void writeCSV(String path, String content) throws IOException {
        Path p = Path.of(path);
        Files.createDirectories(p.getParent());
        Files.writeString(p, content);
    }

    private static long[] appendSize(long[] arr, long extra) {
        long[] result = Arrays.copyOf(arr, arr.length + 1);
        result[arr.length] = extra;
        return result;
    }

    private static void printTableHeader() {
        String fmt = "%-12s %-10s %12s  %10s  %16s  %16s  %12s%n";
        System.out.printf(fmt,
            "Algorithm", "Dist", "Size",
            "Time(ms)", "Comparisons", "Assignments", "ExtraMem(KB)");
        System.out.println("-".repeat(85));
    }

    // =========================================================================
    // Legacy runVerification / runBenchmark (dùng cho backward-compat)
    // =========================================================================

    /**
     * Giữ lại để tương thích với code cũ / test runner.
     * Chạy benchmark với 4 kích thước nhỏ: 1K, 10K, 100K, 1M.
     */
    static void runBenchmark(long seed, int warmup, int runs,
                             int[] sizes, String csvPath) throws IOException {
        StringBuilder sb = new StringBuilder(CSV_HEADER);
        printTableHeader();

        for (int size : sizes) {
            for (String dist : DataGenerator.DISTRIBUTIONS) {
                for (String algo : new String[]{"MergeSort", "PdqSort"}) {
                    sb.append(runDistribution(algo, dist, size, warmup, runs, seed, -1));
                }
            }
            System.out.println();
        }

        writeCSV(csvPath, sb.toString());
        System.out.println("\nCSV saved → " + Path.of(csvPath).toAbsolutePath());
    }
}
