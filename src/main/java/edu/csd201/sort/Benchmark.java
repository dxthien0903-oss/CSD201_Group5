package edu.csd201.sort;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Benchmark so sánh MergeSort với PdqSort trên 5 phân phối D1–D5
 * và 4 kích thước mảng: 1K, 10K, 100K, 1M.
 *
 * <h2>Metrics đo lường</h2>
 * <ul>
 *   <li><b>Time (ms)</b>       – thời gian chạy trung bình qua nhiều lần lặp</li>
 *   <li><b>Comparisons</b>     – số phép so sánh phần tử</li>
 *   <li><b>Assignments</b>     – số phép gán / di chuyển phần tử</li>
 *   <li><b>ExtraMemory (B)</b> – bộ nhớ phụ cấp phát (byte)</li>
 * </ul>
 *
 * <h2>Output</h2>
 * <ul>
 *   <li>In bảng kết quả ra console</li>
 *   <li>Xuất CSV vào {@code results/benchmark.csv}</li>
 * </ul>
 *
 * <h2>Cách chạy</h2>
 * <pre>
 * mvn -q package
 * java -cp target/classes edu.csd201.sort.Benchmark
 * java -cp target/classes edu.csd201.sort.Benchmark verify-only
 * java -cp target/classes edu.csd201.sort.Benchmark seed=12345 warmup=3 runs=5
 * </pre>
 */
public final class Benchmark {

    // -------------------------------------------------------------------------
    // Cấu hình mặc định
    // -------------------------------------------------------------------------

    private static final int[]   DEFAULT_SIZES      = {1_000, 10_000, 100_000, 1_000_000};
    private static final int     DEFAULT_WARMUP      = 3;   // lần JVM warmup (không tính vào kết quả)
    private static final int     DEFAULT_RUNS        = 5;   // lần đo thực sự → lấy trung bình
    private static final long    DEFAULT_SEED        = 20261016L;
    private static final String  DEFAULT_CSV_PATH    = "results/benchmark.csv";
    private static final String  DEFAULT_VERIFY_LOG  = "results/verify.log";

    private static final String[] ALGORITHMS = {"MergeSort", "PdqSort"};

    // -------------------------------------------------------------------------
    // Entry point
    // -------------------------------------------------------------------------

    public static void main(String[] args) throws IOException {
        // --- parse args ---
        long   seed   = DEFAULT_SEED;
        int    warmup = DEFAULT_WARMUP;
        int    runs   = DEFAULT_RUNS;
        String csvPath = DEFAULT_CSV_PATH;
        boolean verifyOnly = false;

        for (String arg : args) {
            if ("verify-only".equals(arg)) {
                verifyOnly = true;
            } else if (arg.startsWith("seed=")) {
                seed = Long.parseLong(arg.substring(5));
            } else if (arg.startsWith("warmup=")) {
                warmup = Integer.parseInt(arg.substring(7));
            } else if (arg.startsWith("runs=")) {
                runs = Integer.parseInt(arg.substring(5));
            } else if (arg.startsWith("csv=")) {
                csvPath = arg.substring(4);
            }
        }

        // --- 1. Kiểm tra tính đúng đắn trước khi benchmark ---
        System.out.println("=== Correctness Verification ===");
        boolean ok = runVerification(seed, DEFAULT_VERIFY_LOG);
        if (!ok) {
            System.err.println("[FATAL] Correctness check FAILED. Aborting benchmark.");
            System.exit(1);
        }
        System.out.println("All correctness checks PASSED.\n");

        if (verifyOnly) {
            return;
        }

        // --- 2. Chạy benchmark ---
        System.out.printf(Locale.ROOT,
                "=== Benchmark  seed=%d  warmup=%d  runs=%d ===%n", seed, warmup, runs);
        runBenchmark(seed, warmup, runs, DEFAULT_SIZES, csvPath);
    }

    // =========================================================================
    // CORRECTNESS VERIFICATION
    // =========================================================================

    /**
     * Kiểm tra MergeSort cho kết quả đúng trên nhiều loại input.
     *
     * @return {@code true} nếu tất cả đều pass
     */
    static boolean runVerification(long seed, String logPath) throws IOException {
        Random rng     = new Random(seed);
        StringBuilder log = new StringBuilder();
        log.append("seed,").append(seed).append('\n');

        int failures = 0;

        // --- Edge cases ---
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

        // --- Distribution cases – nhiều kích thước ---
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

    /**
     * Kiểm tra một mảng: sort bằng MergeSort, so sánh với Arrays.sort.
     *
     * @return 0 nếu pass, 1 nếu fail
     */
    private static int verifyOne(int[] input, String label, StringBuilder log) {
        int[] expected = input.clone();
        int[] actual   = input.clone();
        Arrays.sort(expected);
        MergeSort.sort(actual);
        if (!Arrays.equals(expected, actual)) {
            log.append("FAIL,").append(label).append(',')
               .append(Arrays.toString(input)).append('\n');
            System.err.println("  FAIL: " + label);
            return 1;
        }
        log.append("PASS,").append(label).append('\n');
        return 0;
    }

    // =========================================================================
    // BENCHMARK
    // =========================================================================

    /**
     * Chạy benchmark đầy đủ, in bảng console và ghi CSV.
     */
    static void runBenchmark(long seed, int warmup, int runs,
                             int[] sizes, String csvPath) throws IOException {

        Path csv = Path.of(csvPath);
        Files.createDirectories(csv.getParent());

        // Header CSV
        String csvHeader = "algorithm,distribution,size,run," +
                           "timeMs,comparisons,assignments,extraMemBytes\n";
        StringBuilder csvBody = new StringBuilder(csvHeader);

        // Header console
        String fmt = "%-12s %-6s %10s  %10s  %14s  %14s  %12s%n";
        System.out.printf(fmt,
            "Algorithm", "Dist", "Size",
            "Time(ms)", "Comparisons", "Assignments", "ExtraMem(KB)");
        System.out.println("-".repeat(80));

        for (int size : sizes) {
            for (String dist : DataGenerator.DISTRIBUTIONS) {
                // Seed riêng cho từng (size, dist) để đảm bảo reproducibility
                long cellSeed = seed ^ ((long) size * 31) ^ dist.hashCode();

                // Warmup – không ghi kết quả
                for (int w = 0; w < warmup; w++) {
                    runOneAlgorithm("MergeSort", dist, size,
                                    new Random(cellSeed + w), null, null);
                    runOneAlgorithm("PdqSort",   dist, size,
                                    new Random(cellSeed + w), null, null);
                }

                // Đo thực sự
                for (String algo : ALGORITHMS) {
                    long   totalTimeNs   = 0;
                    long   totalCmp      = 0;
                    long   totalAsn      = 0;
                    long   totalMem      = 0;

                    for (int r = 0; r < runs; r++) {
                        Random rng = new Random(cellSeed + r);
                        long[] timeBuf  = new long[1];
                        long[] metrics  = new long[3]; // [cmp, asn, mem]

                        runOneAlgorithm(algo, dist, size, rng, timeBuf, metrics);

                        totalTimeNs += timeBuf[0];
                        totalCmp    += metrics[0];
                        totalAsn    += metrics[1];
                        totalMem    += metrics[2];

                        // Ghi từng run vào CSV
                        csvBody.append(String.format(Locale.ROOT,
                            "%s,%s,%d,%d,%.4f,%d,%d,%d\n",
                            algo, dist, size, r + 1,
                            timeBuf[0] / 1e6, metrics[0], metrics[1], metrics[2]));
                    }

                    double avgMs  = totalTimeNs / 1e6 / runs;
                    long   avgCmp = totalCmp / runs;
                    long   avgAsn = totalAsn / runs;
                    double avgKB  = totalMem / 1024.0 / runs;

                    System.out.printf(fmt,
                        algo, dist,
                        formatNumber(size),
                        String.format(Locale.ROOT, "%.2f", avgMs),
                        formatNumber(avgCmp),
                        formatNumber(avgAsn),
                        String.format(Locale.ROOT, "%.1f", avgKB));
                }
            }
            System.out.println();
        }

        Files.writeString(csv, csvBody);
        System.out.println("\nCSV saved → " + csv.toAbsolutePath());
    }

    // -------------------------------------------------------------------------
    // Chạy một lần duy nhất cho một thuật toán
    // -------------------------------------------------------------------------

    /**
     * Chạy một lần đo cho thuật toán và bộ phân phối đã chọn.
     *
     * @param algo     "MergeSort" | "PdqSort"
     * @param dist     "D1".."D5"
     * @param size     số phần tử
     * @param rng      nguồn random để tạo data
     * @param timeOut  nếu != null, ghi thời gian nanosecond vào [0]
     * @param metrics  nếu != null, ghi [comparisons, assignments, extraMemBytes]
     */
    private static void runOneAlgorithm(String algo, String dist, int size,
                                        Random rng,
                                        long[] timeOut,
                                        long[] metrics) {
        int[] data = DataGenerator.generate(dist, size, rng);

        long startNs, endNs;

        if ("MergeSort".equals(algo)) {
            MergeSort.MergeMetrics m = new MergeSort.MergeMetrics();
            startNs = System.nanoTime();
            MergeSort.sort(data, m);
            endNs = System.nanoTime();
            if (timeOut  != null) timeOut[0]   = endNs - startNs;
            if (metrics  != null) {
                metrics[0] = m.comparisons();
                metrics[1] = m.assignments();
                metrics[2] = m.extraMemoryBytes();
            }

        } else { // PdqSort
            SortMetrics m = new SortMetrics();
            startNs = System.nanoTime();
            PdqSort.sort(data, true, m);
            endNs = System.nanoTime();
            if (timeOut != null) timeOut[0]   = endNs - startNs;
            if (metrics != null) {
                // PdqSort không đếm comparisons/assignments theo nghĩa chuẩn;
                // dùng partitionCount thay comparisons, maxDepth thay assignments.
                metrics[0] = m.partitionCount();    // ~đại diện comparisons
                metrics[1] = m.maxDepth();          // ~đại diện depth/assignments
                metrics[2] = 0L;                    // in-place: không dùng bộ nhớ phụ O(n)
            }
        }
    }

    // -------------------------------------------------------------------------
    // Tiện ích định dạng
    // -------------------------------------------------------------------------

    private static String formatNumber(long n) {
        // Phân cách hàng nghìn bằng dấu phẩy
        return String.format(Locale.ROOT, "%,d", n);
    }
}
