package edu.csd201.sort;

import java.io.IOException;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

/**
 * Benchmark các thuật toán MergeSort, HeapSort và PdqSort trên 5 phân phối D1–D5.
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

    enum SortAlgorithm {
        MERGE_SORT("MergeSort"),
        HEAP_SORT("HeapSort"),
        PDQ_SORT("PdqSort");

        private final String name;

        SortAlgorithm(String name) {
            this.name = name;
        }

        static SortAlgorithm fromName(String name) {
            for (SortAlgorithm algorithm : values()) {
                if (algorithm.name.equals(name)) {
                    return algorithm;
                }
            }
            throw new IllegalArgumentException("Unsupported benchmark algorithm: " + name);
        }
    }

    static IntSorter sorterFor(String algorithmName) {
        return switch (algorithmName) {
            case "MergeSort" -> MergeSort::sort;
            case "HeapSort" -> HeapSort::sort;
            default -> throw new IllegalArgumentException(
                    "Unsupported benchmark sorter: " + algorithmName);
        };
    }

    record TimedSortResult(int[] values, long elapsedNanos) {
    }

    static TimedSortResult runSortTrial(IntSorter sorter, int[] baseline) {
        int[] values = baseline.clone();
        long start = System.nanoTime();
        sorter.sort(values);
        long elapsed = System.nanoTime() - start;

        return new TimedSortResult(values, elapsed);
    }

    static int[] runMetricsTrial(IntSorter sorter, int[] baseline) {
        int[] values = baseline.clone();
        sorter.sort(values);
        return values;
    }

    static int[] expectedOutput(int[] baseline) {
        int[] expected = baseline.clone();
        Arrays.sort(expected);
        return expected;
    }

    static void validateSortResult(int[] expected, int[] actual) {
        if (!Arrays.equals(expected, actual)) {
            throw new IllegalStateException("Sorter output does not match Arrays.sort");
        }
    }

    static final class MetricsSummary {
        private long comparisonTotal;
        private long assignmentTotal;
        private int comparisonRuns;
        private int assignmentRuns;

        void add(long comparisons, long assignments) {
            if (comparisons >= 0) {
                comparisonTotal += comparisons;
                comparisonRuns++;
            }
            if (assignments >= 0) {
                assignmentTotal += assignments;
                assignmentRuns++;
            }
        }

        Long averageComparisons() {
            return comparisonRuns == 0 ? null : comparisonTotal / comparisonRuns;
        }

        Long averageAssignments() {
            return assignmentRuns == 0 ? null : assignmentTotal / assignmentRuns;
        }
    }

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
        if (!Arrays.equals(expected, actual)) {
            log.append("FAIL,").append(label).append('\n');
            System.err.println("  FAIL: " + label);
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

            sb.append(runCoreAlgorithm("D1", n, warmup, runs, DEFAULT_SEED, -1));
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
                sb.append(runCoreAlgorithm("D1", n, warmup, runs, seed, -1));
            }

            // D2 – 1 seed (deterministic, seed không ảnh hưởng)
            sb.append(runCoreAlgorithm("D2", n, warmup, runs, DEFAULT_SEED, -1));

            // D3 – 3 seed
            for (long seed : RQ2_SEEDS) {
                sb.append(runCoreAlgorithm("D3", n, warmup, runs, seed, -1));
            }

            // D4 – 3 seed
            for (long seed : RQ2_SEEDS) {
                sb.append(runCoreAlgorithm("D4", n, warmup, runs, seed, -1));
            }

            // D5 – 3 K-values × 3 seed
            for (int k : D5_K_VALUES) {
                for (long seed : RQ2_SEEDS) {
                    sb.append(runCoreAlgorithm("D5_K" + k, n, warmup, runs, seed, k));
                }
            }

            System.out.println();
        }
        return sb.toString();
    }

    private static String runCoreAlgorithm(String dist, int size, int warmup, int runs,
                                           long seed, int k) {
        return runDistribution("MergeSort", dist, size, warmup, runs, seed, k)
                + runDistribution("HeapSort", dist, size, warmup, runs, seed, k);
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
        MetricsSummary metricsSummary = new MetricsSummary();
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
                metricsSummary.add(met[0], met[1]);
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
                "%s,%s,%d,%d,%.4f,%s,%s,%d,%d,%s,%s\n",
                algo, dist, size, r + 1,
                timeNs[0] / 1e6,
                formatCsvMetric(met[0]), formatCsvMetric(met[1]), met[2], met[3],
                outputCheck, status));
        }

        // In dòng tóm tắt trung bình ra console
        if (okRuns > 0) {
            System.out.printf(fmt,
                algo, dist,
                String.format(Locale.ROOT, "%,d", (long) size),
                String.format(Locale.ROOT, "%.2f", totalNs / 1e6 / okRuns),
                formatSummaryMetric(metricsSummary.averageComparisons()),
                formatSummaryMetric(metricsSummary.averageAssignments()),
                String.format(Locale.ROOT, "%.1f", totalMem / 1024.0 / okRuns));
        } else {
            System.out.printf(fmt, algo, dist,
                String.format(Locale.ROOT, "%,d", (long) size),
                "FAILED", "-", "-", "-");
        }

        return sb.toString();
    }

    static String formatCsvMetric(long metric) {
        return metric < 0 ? "" : Long.toString(metric);
    }

    static String formatSummaryMetric(Long average) {
        return average == null ? "-" : String.format(Locale.ROOT, "%,d", average);
    }

    /**
     * Chạy một lần đo.
     *
     * @param k          K cho D5 ({@code k >= 1}), hoặc -1 để dùng mặc định
     * @param timeNsOut  nếu != null, ghi ns vào [0]
     * @param metricsOut nếu != null, ghi [comparisons, assignments, extraMemBytes, max_depth]
     */
    static void runOne(String algo, String dist, int size,
                       Random rng, int k,
                       long[] timeNsOut, long[] metricsOut) {
        SortAlgorithm algorithm = SortAlgorithm.fromName(algo);
        int[] baseline = generateData(dist, size, rng, k);

        IntSorter sorter = switch (algorithm) {
            case MERGE_SORT -> sorterFor("MergeSort");
            case HEAP_SORT -> sorterFor("HeapSort");
            case PDQ_SORT -> PdqSort::sort;
        };
        TimedSortResult result = runSortTrial(sorter, baseline);
        int[] expected = expectedOutput(baseline);
        validateSortResult(expected, result.values());
        if (timeNsOut != null) timeNsOut[0] = result.elapsedNanos();

        if (metricsOut != null) {
            long[] measuredMetrics = new long[4];
            int[] metricsInput = switch (algorithm) {
                case MERGE_SORT -> {
                    MergeSort.MergeMetrics metrics = new MergeSort.MergeMetrics();
                    int[] sorted = runMetricsTrial(values -> MergeSort.sort(values, metrics), baseline);
                    measuredMetrics[0] = metrics.comparisons();
                    measuredMetrics[1] = metrics.assignments();
                    measuredMetrics[2] = metrics.extraMemoryBytes();
                    measuredMetrics[3] = -1;
                    yield sorted;
                }
                case HEAP_SORT -> {
                    HeapSort.HeapMetrics metrics = new HeapSort.HeapMetrics();
                    int[] sorted = runMetricsTrial(values -> HeapSort.sort(values, metrics), baseline);
                    measuredMetrics[0] = metrics.comparisons();
                    measuredMetrics[1] = metrics.assignments();
                    measuredMetrics[2] = 0L;
                    measuredMetrics[3] = -1;
                    yield sorted;
                }
                case PDQ_SORT -> {
                    SortMetrics metrics = new SortMetrics();
                    int[] sorted = runMetricsTrial(values -> PdqSort.sort(values, true, metrics), baseline);
                    measuredMetrics[0] = -1L;
                    measuredMetrics[1] = -1L;
                    measuredMetrics[2] = 0L;
                    measuredMetrics[3] = metrics.maxDepth();
                    yield sorted;
                }
            };
            validateSortResult(expected, metricsInput);
            System.arraycopy(measuredMetrics, 0, metricsOut, 0, measuredMetrics.length);
        }
    }

    /**
     * Tạo mảng dữ liệu. Nếu dist bắt đầu bằng "D5" và k >= 1,
     * dùng {@link DataGenerator#generateManyDuplicatesK(int, int, Random)}.
     */
    private static int[] generateData(String dist, int size, Random rng, int k) {
        if (dist.startsWith("D5") && k >= 1) {
            return DataGenerator.generateManyDuplicatesK(size, k, rng);
        }
        // D5 không có K → dùng dist gốc "D5"
        String baseDist = dist.startsWith("D5") ? "D5" : dist;
        return DataGenerator.generate(baseDist, size, rng);
    }

    /**
     * Kiểm tra nhanh: sắp xếp mảng bằng thuật toán được chọn rồi so sánh với Arrays.sort.
     * Chỉ thực hiện nếu size <= 100_000 (để tránh mất quá nhiều thời gian).
     *
     * @return "PASS", "FAIL", hoặc "skip-large"
     */
    private static String verifyQuick(String algo, String dist, int size, long seed, int k) {
        SortAlgorithm algorithm = SortAlgorithm.fromName(algo);
        if (size > 100_000) return "skip-large";
        try {
            int[] input    = generateData(dist, size, new Random(seed), k);
            int[] expected = input.clone();
            int[] actual   = input.clone();
            Arrays.sort(expected);
            sortForVerification(algorithm, actual);
            return Arrays.equals(expected, actual) ? "PASS" : "FAIL";
        } catch (Exception e) {
            return "ERROR";
        }
    }

    static void sortForVerification(String algorithmName, int[] values) {
        sortForVerification(SortAlgorithm.fromName(algorithmName), values);
    }

    private static void sortForVerification(SortAlgorithm algorithm, int[] values) {
        switch (algorithm) {
            case MERGE_SORT -> sorterFor("MergeSort").sort(values);
            case HEAP_SORT -> sorterFor("HeapSort").sort(values);
            case PDQ_SORT -> PdqSort.sort(values);
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
                for (String algo : new String[]{"MergeSort", "HeapSort", "PdqSort"}) {
                    sb.append(runDistribution(algo, dist, size, warmup, runs, seed, -1));
                }
            }
            System.out.println();
        }

        writeCSV(csvPath, sb.toString());
        System.out.println("\nCSV saved → " + Path.of(csvPath).toAbsolutePath());
    }
}
