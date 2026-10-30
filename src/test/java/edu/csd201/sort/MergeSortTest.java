package edu.csd201.sort;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Bộ kiểm thử Merge Sort (TV2).
 *
 * <p>
 * Chạy bằng JUnit: {@code mvn test -Dtest=MergeSortTest}
 * <p>
 * Chạy bằng CLI (sinh test log):
 * {@code java -cp target/classes:target/test-classes
 * edu.csd201.sort.MergeSortTest --log results/merge_sort_test_log.txt}
 * (Windows dùng dấu {@code ;} thay cho {@code :}).
 *
 * <p>
 * Mỗi nhóm kiểm thử được viết một lần dưới dạng "section"; JUnit và CLI cùng
 * gọi lại section đó.
 */
public final class MergeSortTest {

    private static final int LARGE_N = 100_000;
    private static final int SPEC_N = 1_000_000;
    private static final int RANDOM_CASES = 1_000;
    private static final int RANDOM_DUPLICATE_CASES = 200;
    private static final long SEED = 20261016L;

    private static final StringBuilder LOG = new StringBuilder();
    private static final StringBuilder FAILS = new StringBuilder();
    private static int passed;
    private static int failed;

    public MergeSortTest() {
    }

    // =========================================================================
    // JUnit entry points
    // =========================================================================

    @Test
    @DisplayName("Nhóm 1: Ca cố định theo đề + ca biên (rỗng, 1 phần tử, trùng, 0 và 2^31-1)")
    public void testFixedCases() {
        asJUnit(MergeSortTest::fixedCases);
    }

    @Test
    @DisplayName("Nhóm 2: Mảng 100.000 phần tử (toàn bằng nhau / tăng / giảm / hai cực trị)")
    public void testLargeCases() {
        asJUnit(MergeSortTest::largeCases);
    }

    @Test
    @DisplayName("Nhóm 3: Randomized — 1.000 mảng kích thước 0..100 thuộc D1–D5 + 200 mảng trùng nhiều, đối chiếu Arrays.sort")
    public void testRandomizedCases() {
        asJUnit(MergeSortTest::randomizedCases);
    }

    @Test
    @DisplayName("Nhóm 4: Đúng quy ước Merge Sort (không bỏ merge, đếm thao tác, max_depth, mảng phụ N×4 byte)")
    public void testMergeContract() {
        asJUnit(MergeSortTest::mergeContractCases);
    }

    @Test
    @DisplayName("Nhóm 5: Mảng phụ chỉ cấp phát đúng một lần (đo bộ nhớ cấp phát của luồng)")
    public void testSingleAllocation() {
        asJUnit(MergeSortTest::allocationCase);
    }

    @Test
    @DisplayName("Nhóm 6: Vét cạn mảng nhỏ (3^n, n<=7) và mọi hoán vị độ dài 1..8")
    public void testExhaustiveSweep() {
        asJUnit(MergeSortTest::exhaustiveCases);
    }

    @Test
    @DisplayName("Nhóm 7: Generator D1–D5 đúng quy tắc đề (D3 = D2 + N/1000 hoán đổi, D4 = D2 đảo ngược)")
    public void testGeneratorRules() {
        asJUnit(MergeSortTest::generatorCases);
    }

    @Test
    @DisplayName("Nhóm 8: Dữ liệu lớn N=10^6 trên D1–D4 và D5 (K=1000, 10, 2) + output_check")
    public void testSpecDatasetsAtScale() {
        asJUnit(MergeSortTest::specScaleCases);
    }

    private static void asJUnit(Runnable section) {
        int failedBefore = failed;
        int failsLen = FAILS.length();
        section.run();
        if (failed != failedBefore) {
            throw new AssertionError(FAILS.substring(failsLen));
        }
    }

    // =========================================================================
    // CLI entry point (sinh test log)
    // =========================================================================

    public static void main(String[] args) throws IOException {
        String logPath = parseLogPath(args);

        line("MergeSort correctness suite (TV2)");
        line("java.version=" + System.getProperty("java.version")
                + " java.vm.name=" + System.getProperty("java.vm.name"));
        line("seed=" + SEED);
        line("");

        runSection("fixed cases", MergeSortTest::fixedCases);
        runSection("large fixed-shape cases", MergeSortTest::largeCases);
        runSection("randomized cases D1-D5", MergeSortTest::randomizedCases);
        runSection("merge contract (no skip-merge, counters, max_depth, extra memory)",
                MergeSortTest::mergeContractCases);
        runSection("single allocation of temp array", MergeSortTest::allocationCase);
        runSection("exhaustive sweep", MergeSortTest::exhaustiveCases);
        runSection("D1-D5 generator rules", MergeSortTest::generatorCases);
        runSection("spec datasets at N=10^6", MergeSortTest::specScaleCases);

        line("");
        line("SUMMARY passed=" + passed + " failed=" + failed);
        line(failed == 0 ? "RESULT ALL_TESTS_PASSED" : "RESULT TESTS_FAILED");
        String text = LOG.toString();
        System.out.print(text);
        if (logPath != null) {
            Path target = Paths.get(logPath).toAbsolutePath();
            Path parent = target.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Files.write(target, text.getBytes(StandardCharsets.UTF_8));
            System.out.println("log written: " + target);
        }
        if (failed > 0) {
            System.exit(1);
        }
    }

    private static String parseLogPath(String[] args) {
        for (int i = 0; i < args.length; i++) {
            if (args[i].equals("--log") && i + 1 < args.length) {
                return args[i + 1];
            }
            if (args[i].startsWith("--log=")) {
                return args[i].substring("--log=".length());
            }
        }
        return null;
    }

    private static void runSection(String name, Runnable section) {
        line("[" + name + "]");
        try {
            section.run();
        } catch (Throwable thrown) {
            failed++;
            line("FATAL section [" + name + "] " + thrown.getClass().getName() + ": " + thrown.getMessage());
        }
    }

    // =========================================================================
    // Sections
    // =========================================================================

    private static void fixedCases() {
        int[][] cases = {
                { 7 },
                { 2, 1 },
                { 3, 3, 3 },
                { 0, 2147483647, 0 },
                { 5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5 },
                {},
                { 0 },
                { 1, 0 },
                { 0, 2147483647 },
                { 2147483647, 0, 2147483647 },
                { 1, 2, 3, 4, 5 },
                { 5, 4, 3, 2, 1 },
        };
        for (int i = 0; i < cases.length; i++) {
            checkSorted("fixed#" + (i + 1) + " " + Arrays.toString(cases[i]), cases[i]);
        }
    }

    private static void largeCases() {
        int[] equal = new int[LARGE_N];
        Arrays.fill(equal, 123456789);
        checkSorted("N=100000 all-equal", equal);

        int[] ascending = new int[LARGE_N];
        for (int i = 0; i < ascending.length; i++) {
            ascending[i] = i;
        }
        checkSorted("N=100000 ascending", ascending);

        int[] descending = new int[LARGE_N];
        for (int i = 0; i < descending.length; i++) {
            descending[i] = LARGE_N - i;
        }
        checkSorted("N=100000 descending", descending);

        int[] extremes = new int[LARGE_N];
        for (int i = 0; i < extremes.length; i++) {
            extremes[i] = (i % 2 == 0) ? 0 : Integer.MAX_VALUE;
        }
        checkSorted("N=100000 two-value extremes (0 / 2^31-1)", extremes);
    }

    private static void randomizedCases() {
        SplittableRandom rnd = new SplittableRandom(SEED);
        int[] ks = { 1000, 10, 2, 1 };
        int[] badByDataset = new int[DataGenerator.DISTRIBUTIONS.length];
        int[] countByDataset = new int[DataGenerator.DISTRIBUTIONS.length];
        for (int i = 0; i < RANDOM_CASES; i++) {
            int d = i % DataGenerator.DISTRIBUTIONS.length;
            int n = rnd.nextInt(0, 101);
            int k = ks[(i / DataGenerator.DISTRIBUTIONS.length) % ks.length];
            int[] a = DataGenerator.generateSpec(DataGenerator.DISTRIBUTIONS[d], n, k, rnd.nextLong());
            countByDataset[d]++;
            if (!sortMatchesArraysSort(a)) {
                badByDataset[d]++;
            }
        }
        for (int d = 0; d < DataGenerator.DISTRIBUTIONS.length; d++) {
            expect(countByDataset[d] + " random arrays of " + DataGenerator.DISTRIBUTIONS[d]
                    + ", N in [0, 100] match Arrays.sort", badByDataset[d] == 0);
        }

        int badSmallDomain = 0;
        for (int i = 0; i < RANDOM_DUPLICATE_CASES; i++) {
            int n = rnd.nextInt(0, 101);
            int[] a = new int[n];
            for (int j = 0; j < n; j++) {
                a[j] = rnd.nextInt(0, 4);
            }
            if (!sortMatchesArraysSort(a)) {
                badSmallDomain++;
            }
        }
        expect(RANDOM_DUPLICATE_CASES + " random arrays over domain {0..3} match Arrays.sort", badSmallDomain == 0);
    }

    private static void mergeContractCases() {
        // --- Không bỏ qua bước merge khi hai nửa đã có thứ tự
        // -------------------------------
        // N=1024=2^10: mỗi tầng có N/2 phép so sánh (nửa trái <= nửa phải) và 2N phép
        // gán (copy sang temp + ghi về arr).
        int n = 1024;
        int[] sorted = new int[n];
        for (int i = 0; i < n; i++) {
            sorted[i] = i;
        }
        MergeSort.MergeMetrics ms = new MergeSort.MergeMetrics();
        MergeSort.sortCounted(sorted, ms);
        expect("sorted N=1024: merge không bị bỏ qua, comparisons = (N/2)·log2N = 5120 (got " + ms.comparisons() + ")",
                ms.comparisons() == 5120L);
        expect("sorted N=1024: assignments = 2·N·log2N = 20480 (got " + ms.assignments() + ")",
                ms.assignments() == 20480L);
        line("  sorted N=1024 -> " + ms);

        // Đường sort() (lượt đo thời gian) là một nhánh đệ quy riêng, không đi qua
        // sortCounted nên phải kiểm tra riêng.
        // (sort(int[]) là bản không đếm nên kiểm tra bằng sort(arr, metrics); bản không
        // đếm được kiểm bằng kết quả
        // trên mọi nhóm khác và bằng so sánh với bản đếm ngay bên dưới.)
        int[] sortedForTiming = sorted.clone();
        MergeSort.MergeMetrics mt = new MergeSort.MergeMetrics();
        MergeSort.sort(sortedForTiming, mt);
        expect("sort() trên mảng đã sắp N=1024 cũng không bỏ merge: comparisons=5120, assignments=20480 (got "
                + mt.comparisons() + ", " + mt.assignments() + ")",
                mt.comparisons() == 5120L && mt.assignments() == 20480L);

        int[] reversed = new int[n];
        for (int i = 0; i < n; i++) {
            reversed[i] = n - i;
        }
        MergeSort.MergeMetrics mr = new MergeSort.MergeMetrics();
        MergeSort.sortCounted(reversed, mr);
        expect("reversed N=1024 sorted correctly", isAscending(reversed));
        expect("reversed N=1024: comparisons = 5120 (got " + mr.comparisons() + ")", mr.comparisons() == 5120L);
        line("  reversed N=1024 -> " + mr);

        // Bản không đếm và bản có đếm cho cùng kết quả trên mọi dataset
        for (String ds : DataGenerator.DISTRIBUTIONS) {
            int[] in = DataGenerator.generateSpec(ds, 20_000, 10, 99L);
            int[] p = in.clone();
            int[] c = in.clone();
            MergeSort.sort(p);
            MergeSort.sortCounted(c, new MergeSort.MergeMetrics());
            expect(ds + " N=20000: sort(int[]) không đếm cho cùng kết quả với sortCounted", Arrays.equals(p, c));
        }

        // --- Cận số phép so sánh trên dữ liệu ngẫu nhiên
        // ---------------------------------------
        int rn = 5_000;
        int[] random = DataGenerator.d1(rn, 7L);
        int[] expected = random.clone();
        Arrays.sort(expected);
        MergeSort.MergeMetrics mrand = new MergeSort.MergeMetrics();
        MergeSort.sortCounted(random, mrand);
        expect("random N=5000 counted mode sorts correctly", Arrays.equals(expected, random));
        double log2n = Math.log(rn) / Math.log(2);
        double lower = rn * log2n - 1.45 * rn; // ~ log2(n!)
        double upper = (double) rn * Math.ceil(log2n);
        expect("random N=5000: log2(N!) <= comparisons <= N·ceil(log2N) (got " + mrand.comparisons() + ")",
                mrand.comparisons() >= lower && mrand.comparisons() <= upper);
        line("  random N=5000 seed=7 -> " + mrand);

        // --- max_depth: ceil(log2 N) + 1, bắt đầu đếm từ 1 ở lần gọi đầu tiên
        // --------------------
        int[][] depthCases = { { 2, 2 }, { 3, 3 }, { 4, 3 }, { 1024, 11 }, { LARGE_N, 18 }, { SPEC_N, 21 } };
        for (int[] c : depthCases) {
            int size = c[0];
            int[] arr = DataGenerator.d1(size, 11L);
            MergeSort.MergeMetrics m = new MergeSort.MergeMetrics();
            MergeSort.sortCounted(arr, m);
            expect("N=" + size + " maxDepth=" + c[1] + " (got " + m.maxDepth() + ")", m.maxDepth() == c[1]);
        }
        MergeSort.MergeMetrics m1 = new MergeSort.MergeMetrics();
        MergeSort.sortCounted(new int[] { 42 }, m1);
        expect("N=1 sortCounted không đệ quy: maxDepth = 0 (got " + m1.maxDepth() + ")", m1.maxDepth() == 0);

        // --- Bộ nhớ phụ = N × 4 byte
        // -------------------------------------------------------------
        for (int size : new int[] { 2, 1_000, LARGE_N }) {
            MergeSort.MergeMetrics a = new MergeSort.MergeMetrics();
            MergeSort.sort(DataGenerator.d1(size, 3L), a);
            MergeSort.MergeMetrics b = new MergeSort.MergeMetrics();
            MergeSort.sortCounted(DataGenerator.d1(size, 3L), b);
            expect("N=" + size + " extraMemoryBytes = 4·N (sort & sortCounted)",
                    a.extraMemoryBytes() == 4L * size && b.extraMemoryBytes() == 4L * size);
        }

        // --- Lượt đo thời gian không ghi max_depth
        // -----------------------------------------------
        MergeSort.MergeMetrics timing = new MergeSort.MergeMetrics();
        int[] t = DataGenerator.d1(10_000, 5L);
        MergeSort.sort(t, timing);
        expect("timing mode sort(): maxDepth = 0", timing.maxDepth() == 0);
        expect("timing mode sort(): sorted correctly", isAscending(t));

        // --- reset()
        // ------------------------------------------------------------------------------
        timing.reset();
        expect("reset() đưa mọi bộ đếm về 0", timing.comparisons() == 0 && timing.assignments() == 0
                && timing.extraMemoryBytes() == 0 && timing.maxDepth() == 0);

        // --- verify() không làm thay đổi mảng gốc
        // ------------------------------------------------
        int[] input = { 5, 3, 9, 1, 1, 0 };
        int[] copy = input.clone();
        expect("verify() trả về true với mảng hợp lệ", MergeSort.verify(input));
        expect("verify() không sửa mảng đầu vào", Arrays.equals(copy, input));

        // --- Tham số null
        // --------------------------------------------------------------------------
        expect("sort(null) ném NullPointerException", throwsNpe(() -> MergeSort.sort(null)));
        expect("sort(arr, null) ném NullPointerException",
                throwsNpe(() -> MergeSort.sort(new int[] { 2, 1 }, null)));
        expect("sortCounted(null, m) ném NullPointerException",
                throwsNpe(() -> MergeSort.sortCounted(null, new MergeSort.MergeMetrics())));
    }

    private static void allocationCase() {
        try {
            com.sun.management.ThreadMXBean mx = (com.sun.management.ThreadMXBean) ManagementFactory.getThreadMXBean();
            if (!mx.isThreadAllocatedMemorySupported()) {
                line("  SKIP: JVM không hỗ trợ đo bộ nhớ cấp phát theo luồng");
                return;
            }
            mx.setThreadAllocatedMemoryEnabled(true);
            MergeSort.sort(DataGenerator.d1(10_000, 1L)); // khởi động lớp trước khi đo

            int n = SPEC_N;
            int[] a = DataGenerator.d1(n, 5L);
            long id = Thread.currentThread().getId();
            long before = mx.getThreadAllocatedBytes(id);
            MergeSort.sort(a);
            long used = mx.getThreadAllocatedBytes(id) - before;
            long arrayBytes = 4L * n;
            expect("N=10^6: sort() cấp phát ≈ một mảng int[N] (" + arrayBytes + " B), thực tế " + used + " B",
                    used >= arrayBytes && used <= arrayBytes + 64 * 1024);
            expect("N=10^6: sorted correctly", isAscending(a));
        } catch (Throwable unsupported) {
            line("  SKIP: không đo được bộ nhớ cấp phát (" + unsupported + ")");
        }
    }

    private static long permTotal;
    private static int permBad;

    private static void exhaustiveCases() {
        long total = 0;
        int bad = 0;
        for (int n = 0; n <= 7; n++) {
            long combos = 1;
            for (int i = 0; i < n; i++) {
                combos *= 3;
            }
            int[] a = new int[n];
            for (long code = 0; code < combos; code++) {
                long value = code;
                for (int i = 0; i < n; i++) {
                    a[i] = (int) (value % 3);
                    value /= 3;
                }
                total++;
                if (!sortMatchesArraysSort(a)) {
                    bad++;
                }
            }
        }
        expect("all " + total + " arrays of length 0..7 over {0,1,2} sort correctly", bad == 0);

        permTotal = 0;
        permBad = 0;
        for (int n = 1; n <= 8; n++) {
            int[] a = new int[n];
            for (int i = 0; i < n; i++) {
                a[i] = i + 1;
            }
            permutations(a, 0);
        }
        expect("all " + permTotal + " permutations of length 1..8 sort correctly", permBad == 0);
    }

    private static void generatorCases() {
        final long seed = 42L;
        final int n = LARGE_N;

        int[] d1 = DataGenerator.generateSpec("D1", n, 0, seed);
        int[] d2 = DataGenerator.generateSpec("D2", n, 0, seed);
        int[] d3 = DataGenerator.generateSpec("D3", n, 0, seed);
        int[] d4 = DataGenerator.generateSpec("D4", n, 0, seed);

        // D1 / D2
        expect("D1 (generateSpec) trùng DataGenerator.d1 cùng seed", Arrays.equals(d1, DataGenerator.d1(n, seed)));
        expect("D1 nằm trong [0, 2^31-1]", allNonNegative(d1));
        int[] d1Sorted = d1.clone();
        Arrays.sort(d1Sorted);
        expect("D2 = D1 đã sắp tăng dần (cùng multiset)", Arrays.equals(d1Sorted, d2));

        // D3
        int[] d3Sorted = d3.clone();
        Arrays.sort(d3Sorted);
        expect("D3 giữ nguyên multiset của D2", Arrays.equals(d2, d3Sorted));
        int swaps = n / 1000;
        int diff = 0;
        for (int i = 0; i < n; i++) {
            if (d2[i] != d3[i]) {
                diff++;
            }
        }
        expect("D3 khác D2 tại 1.." + (2 * swaps) + " vị trí (N/1000=" + swaps + " cặp hoán đổi), thực tế " + diff,
                diff >= 1 && diff <= 2 * swaps);
        expect("D3 tái lập theo seed", Arrays.equals(d3, DataGenerator.generateSpec("D3", n, 0, seed)));
        expect("D3 khác nhau khi đổi seed", !Arrays.equals(d3, DataGenerator.generateSpec("D3", n, 0, seed + 1)));
        expect("D3 với N<1000 không hoán đổi (giống D2)",
                Arrays.equals(DataGenerator.generateSpec("D2", 999, 0, seed),
                        DataGenerator.generateSpec("D3", 999, 0, seed)));

        // D4
        boolean reverse = true;
        for (int i = 0; i < n; i++) {
            if (d4[i] != d2[n - 1 - i]) {
                reverse = false;
                break;
            }
        }
        expect("D4 = D2 đảo ngược", reverse);
        expect("D4 giảm dần (không tăng)", isDescending(d4));

        // D5
        for (int k : new int[] { 1000, 10, 2, 1 }) {
            int[] d5 = DataGenerator.generateSpec("D5", n, k, seed);
            expect("D5 K=" + k + ": có đúng " + k + " giá trị phân biệt", distinctCount(d5) == k);
            expect("D5 K=" + k + ": khóa trong [0, 2^31-1]", allNonNegative(d5));
            expect("D5 K=" + k + ": tái lập theo seed",
                    Arrays.equals(d5, DataGenerator.generateSpec("D5", n, k, seed)));
        }

        // Kích thước biên
        for (String ds : DataGenerator.DISTRIBUTIONS) {
            expect(ds + " với N=0 trả về mảng rỗng", DataGenerator.generateSpec(ds, 0, 2, seed).length == 0);
            expect(ds + " với N=1 trả về đúng 1 phần tử", DataGenerator.generateSpec(ds, 1, 1, seed).length == 1);
        }
        expect("Dataset lạ bị từ chối", throwsIae(() -> DataGenerator.generateSpec("D9", 10, 1, seed)));
        expect("N âm bị từ chối", throwsIae(() -> DataGenerator.generateSpec("D1", -1, 1, seed)));
    }

    private static void specScaleCases() {
        String[] names = { "D1", "D2", "D3", "D4", "D5/K=1000", "D5/K=10", "D5/K=2" };
        String[] datasets = { "D1", "D2", "D3", "D4", "D5", "D5", "D5" };
        int[] ks = { 0, 0, 0, 0, 1000, 10, 2 };
        for (int i = 0; i < names.length; i++) {
            int[] input = DataGenerator.generateSpec(datasets[i], SPEC_N, ks[i], SEED);
            int[] expected = input.clone();
            Arrays.sort(expected);
            int[] actual = input.clone();
            MergeSort.sort(actual);
            boolean equal = Arrays.equals(expected, actual);
            long hashExpected = outputHash(expected);
            long hashActual = outputHash(actual);
            expect(names[i] + " N=10^6 seed=" + SEED + " khớp Arrays.sort (output_check h=" + hashActual + ")",
                    equal && hashExpected == hashActual);
        }
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private static void checkSorted(String name, int[] input) {
        int[] actual = input.clone();
        int[] expected = input.clone();
        Arrays.sort(expected);
        MergeSort.sort(actual);
        expect(name + " sorted correctly", Arrays.equals(expected, actual));
    }

    private static boolean sortMatchesArraysSort(int[] input) {
        int[] expected = input.clone();
        int[] actual = input.clone();
        int[] counted = input.clone();
        Arrays.sort(expected);
        MergeSort.sort(actual); // bản không đếm (lượt đo thời gian)
        MergeSort.sort(counted, new MergeSort.MergeMetrics()); // bản có đếm
        return Arrays.equals(expected, actual) && Arrays.equals(expected, counted);
    }

    /**
     * output_check theo quy ước chung của nhóm: h = 31*h + a[i], h kiểu long, cho
     * phép tràn.
     */
    private static long outputHash(int[] a) {
        long h = 0;
        for (int v : a) {
            h = 31 * h + v;
        }
        return h;
    }

    private static boolean isAscending(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] > a[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean isDescending(int[] a) {
        for (int i = 1; i < a.length; i++) {
            if (a[i - 1] < a[i]) {
                return false;
            }
        }
        return true;
    }

    private static boolean allNonNegative(int[] a) {
        for (int v : a) {
            if (v < 0) {
                return false;
            }
        }
        return true;
    }

    private static int distinctCount(int[] a) {
        int[] copy = a.clone();
        Arrays.sort(copy);
        int count = copy.length == 0 ? 0 : 1;
        for (int i = 1; i < copy.length; i++) {
            if (copy[i] != copy[i - 1]) {
                count++;
            }
        }
        return count;
    }

    private static boolean throwsNpe(Runnable r) {
        try {
            r.run();
            return false;
        } catch (NullPointerException expected) {
            return true;
        }
    }

    private static boolean throwsIae(Runnable r) {
        try {
            r.run();
            return false;
        } catch (IllegalArgumentException expected) {
            return true;
        }
    }

    private static void permutations(int[] a, int k) {
        if (k == a.length) {
            permTotal++;
            if (!sortMatchesArraysSort(a)) {
                permBad++;
            }
            return;
        }
        for (int i = k; i < a.length; i++) {
            int tmp = a[k];
            a[k] = a[i];
            a[i] = tmp;
            permutations(a, k + 1);
            tmp = a[k];
            a[k] = a[i];
            a[i] = tmp;
        }
    }

    private static void expect(String name, boolean condition) {
        if (condition) {
            passed++;
            line("PASS " + name);
        } else {
            failed++;
            FAILS.append("FAIL ").append(name).append('\n');
            line("FAIL " + name);
        }
    }

    private static void line(String text) {
        LOG.append(text).append('\n');
    }
}