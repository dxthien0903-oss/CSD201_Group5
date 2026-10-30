package edu.csd201.sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public final class ExperimentRunner {
    private static final int DEFAULT_SEED = 20261016;
    private static final int DEFAULT_CASES = 10_000;
    private static final int DEFAULT_RQ3_SIZE = 4_096;
    private static final int DEFAULT_RQ3_RUNS = 3;
    private static final int RQ3_WARMUP_RUNS = 1;
    private static final String RQ3_CSV_HEADER =
            "experiment,dataset,n,seed,algorithm,variant,run,elapsed_ms,output_check,"
                    + "max_depth,partitions,heap_fallbacks,partial_insertion,status\n";
    private static final String[] VARIANT_NAMES = {"full", "noA", "noB", "noA+noB"};
    private static final boolean[][] MECHANISM_CONFIGURATIONS = {
            {true, true},
            {false, true},
            {true, false},
            {false, false}
    };

    private ExperimentRunner() {
    }

    public static void main(String[] args) throws IOException {
        if (args.length == 0 || "randomized-test".equals(args[0])) {
            runRandomizedTests(args);
            return;
        }
        if ("count".equals(args[0]) || "--count".equals(args[0])) {
            runCount(args);
            return;
        }
        if ("rq3".equals(args[0])) {
            runRq3(args);
            return;
        }
        System.err.println("Usage: randomized-test [seed] [cases] [log-file]"
                + " | count [full|noA|noB|noA+noB] <integers...>"
                + " | rq3 [seed] [size] [runs] [csv-file]");
        System.exit(2);
    }

    private static void runRandomizedTests(String[] args) throws IOException {
        long seed = args.length > 1 ? Long.parseLong(args[1]) : DEFAULT_SEED;
        int cases = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_CASES;
        Path logPath = Path.of(args.length > 3 ? args[3] : "results/randomized-test.log");
        if (cases < 1) {
            throw new IllegalArgumentException("cases must be positive");
        }

        Random random = new Random(seed);
        StringBuilder log = new StringBuilder();
        log.append("seed,").append(seed).append('\n');
        log.append("random_cases,").append(cases).append('\n');
        log.append("mechanism_variants,").append(VARIANT_NAMES.length).append('\n');
        log.append("distribution_note,D1-D5 provisional; see README\n");
        int[][] edgeCases = {
                {},
                {Integer.MIN_VALUE},
                {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE},
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
                {9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
                java.util.stream.IntStream.range(0, 23).toArray(),
                java.util.stream.IntStream.range(0, 24).toArray(),
                java.util.stream.IntStream.range(0, 25).toArray(),
                java.util.stream.IntStream.range(0, 127).toArray(),
                java.util.stream.IntStream.range(0, 128).toArray(),
                java.util.stream.IntStream.range(0, 129).toArray(),
                java.util.stream.IntStream.range(0, 127).map(i -> 126 - i).toArray(),
                java.util.stream.IntStream.range(0, 128).map(i -> 127 - i).toArray(),
                java.util.stream.IntStream.range(0, 129).map(i -> 128 - i).toArray(),
                new int[4_096]
        };
        log.append("edge_cases,").append(edgeCases.length).append('\n');
        int failures = 0;
        SortMetrics comparisonMetrics = new SortMetrics();
        PdqSort.sort(new int[] {1, 2, 3}, true, true, comparisonMetrics);
        if (comparisonMetrics.comparisonCount() != 2) {
            failures++;
            log.append("failure,comparison-counter,expected-2-actual-")
                    .append(comparisonMetrics.comparisonCount()).append('\n');
        }
        SortMetrics timingMetrics = new SortMetrics(false);
        PdqSort.sort(new int[] {1, 2, 3}, true, true, timingMetrics);
        if (timingMetrics.comparisonCount() != 0 || timingMetrics.partitionCount() != 0
                || timingMetrics.maxDepth() != 0 || timingMetrics.heapFallbackCount() != 0
                || timingMetrics.partialInsertionCount() != 0) {
            failures++;
            log.append("failure,timing-metrics-enabled\n");
        }
        for (int test = 0; test < edgeCases.length; test++) {
            failures += validateArray(edgeCases[test], "edge-" + test, log);
        }
        int[] fallbackProbe = createHeapFallbackProbe();
        failures += validateArray(fallbackProbe, "heap-fallback-probe", log);
        int[] expectedFallback = fallbackProbe.clone();
        Arrays.sort(expectedFallback);
        long fallbackCount = 0;
        for (int variant = 0; variant < VARIANT_NAMES.length; variant++) {
            int[] fallbackResult = fallbackProbe.clone();
            SortMetrics fallbackMetrics = new SortMetrics();
            PdqSort.sort(fallbackResult, MECHANISM_CONFIGURATIONS[variant][0],
                    MECHANISM_CONFIGURATIONS[variant][1], fallbackMetrics);
            fallbackCount += fallbackMetrics.heapFallbackCount();
            if (!Arrays.equals(expectedFallback, fallbackResult)) {
                failures++;
                log.append("failure,heap-fallback-probe,").append(VARIANT_NAMES[variant])
                        .append(",wrong-output\n");
            }
            if (fallbackMetrics.heapFallbackCount() == 0) {
                failures++;
                log.append("failure,heap-fallback-probe,").append(VARIANT_NAMES[variant])
                        .append(",heapsort-not-reached\n");
            }
        }
        for (int test = 0; test < cases; test++) {
            String distribution = "D" + (test % 5 + 1);
            int[] input = createArray(distribution, random);
            failures += validateArray(input, test + "," + distribution, log);
        }
        log.append("failures,").append(failures).append('\n');
        Files.createDirectories(logPath.toAbsolutePath().getParent());
        Files.writeString(logPath, log);
        System.out.printf(Locale.ROOT,
            "seed=%d random_cases=%d edge_cases=%d variants=%d heap_fallbacks=%d failures=%d log=%s%n",
            seed, cases, edgeCases.length, VARIANT_NAMES.length, fallbackCount, failures, logPath);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static int validateArray(int[] input, String label, StringBuilder log) {
        int[] expected = input.clone();
        Arrays.sort(expected);
        int failures = 0;
        int[] heapSorted = input.clone();
        HeapSort.sort(heapSorted);
        if (!Arrays.equals(expected, heapSorted)) {
            failures++;
            log.append("failure,").append(label).append(",heapsort,")
                    .append(Arrays.toString(input)).append('\n');
        }
        for (int variant = 0; variant < VARIANT_NAMES.length; variant++) {
            boolean mechanismAEnabled = MECHANISM_CONFIGURATIONS[variant][0];
            boolean mechanismBEnabled = MECHANISM_CONFIGURATIONS[variant][1];
            int[] actual = input.clone();
            SortMetrics metrics = new SortMetrics();
            PdqSort.sort(actual, mechanismAEnabled, mechanismBEnabled, metrics);
            if (!Arrays.equals(expected, actual)) {
                failures++;
                log.append("failure,").append(label).append(',').append(VARIANT_NAMES[variant]).append(',')
                        .append(Arrays.toString(input)).append('\n');
            }
            if (input.length > 1 && metrics.comparisonCount() == 0) {
                failures++;
                log.append("failure,").append(label).append(',').append(VARIANT_NAMES[variant])
                        .append(",comparisons-not-counted\n");
            }
            if (!mechanismAEnabled && metrics.partialInsertionCount() != 0) {
                failures++;
                log.append("failure,").append(label).append(',').append(VARIANT_NAMES[variant])
                        .append(",mechanism-A-enabled\n");
            }
        }
        return failures;
    }

    private static int[] createHeapFallbackProbe() {
        int size = 16_384;
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = index <= size / 2 ? 2 * index : 2 * (size - index) - 1;
        }
        return values;
    }

    private static int[] createArray(String distribution, Random random) {
        int length = random.nextInt(101);
        int[] values = new int[length];
        for (int index = 0; index < length; index++) {
            values[index] = random.nextInt(201) - 100;
        }
        switch (distribution) {
            case "D1" -> {
            }
            case "D2" -> Arrays.sort(values);
            case "D3" -> {
                Arrays.sort(values);
                reverse(values);
            }
            case "D4" -> {
                Arrays.sort(values);
                if (length > 1) {
                    int swaps = Math.max(1, length / 1000); // N/1000 theo đặc tả (khớp DataGenerator)
                    for (int index = 0; index < swaps; index++) {
                        swap(values, random.nextInt(length), random.nextInt(length));
                    }
                }
            }
            case "D5" -> {
                for (int index = 0; index < length; index++) {
                    values[index] = random.nextInt(5);
                }
            }
            default -> throw new IllegalArgumentException("Unknown distribution: " + distribution);
        }
        return values;
    }

    private static void runCount(String[] args) {
        int firstValue = 1;
        boolean mechanismAEnabled = true;
        boolean mechanismBEnabled = true;
        if (args.length > 1) {
            switch (args[1].toLowerCase(Locale.ROOT)) {
                case "full" -> firstValue = 2;
                case "noa" -> {
                    firstValue = 2;
                    mechanismAEnabled = false;
                }
                case "nob" -> {
                    firstValue = 2;
                    mechanismBEnabled = false;
                }
                case "noa+nob" -> {
                    firstValue = 2;
                    mechanismAEnabled = false;
                    mechanismBEnabled = false;
                }
                default -> {
                }
            }
        }
        if (args.length <= firstValue) {
            throw new IllegalArgumentException("count requires at least one integer");
        }
        int[] values = Arrays.stream(args, firstValue, args.length).mapToInt(Integer::parseInt).toArray();
        SortMetrics metrics = new SortMetrics();
        PdqSort.sort(values, mechanismAEnabled, mechanismBEnabled, metrics);
        System.out.printf(Locale.ROOT,
                "variant=%s comparisons=%d max_depth=%d partitions=%d heap_fallbacks=%d%n",
                variantName(mechanismAEnabled, mechanismBEnabled), metrics.comparisonCount(),
                metrics.maxDepth(), metrics.partitionCount(), metrics.heapFallbackCount());
        System.out.println(Arrays.toString(values));
    }

    private static void runRq3(String[] args) throws IOException {
        long seed = args.length > 1 ? Long.parseLong(args[1]) : DEFAULT_SEED;
        int size = args.length > 2 ? Integer.parseInt(args[2]) : DEFAULT_RQ3_SIZE;
        int runs = args.length > 3 ? Integer.parseInt(args[3]) : DEFAULT_RQ3_RUNS;
        Path csvPath = Path.of(args.length > 4 ? args[4] : "results/rq3_benchmark.csv").toAbsolutePath();
        if (size < 0) {
            throw new IllegalArgumentException("RQ3 size must be >= 0");
        }
        if (runs < 1) {
            throw new IllegalArgumentException("RQ3 runs must be positive");
        }
        if (Files.exists(csvPath)) {
            throw new java.nio.file.FileAlreadyExistsException(csvPath.toString());
        }

        StringBuilder csv = new StringBuilder(RQ3_CSV_HEADER);
        int failures = 0;
        String[][] datasets = {
                {"D1", "D1"}, {"D2", "D2"}, {"D3", "D3"}, {"D4", "D4"},
                {"D5_K2", "D5_K2"}, {"KILLER_DUPLICATE", "KILLER_DUPLICATE"}
        };
        String[] algorithms = {"pdqsort-full", "pdqsort-noB", "Arrays.sort"};
        for (String[] dataset : datasets) {
            int[] input = createRq3Dataset(dataset[0], size, seed);
            int[] expected = input.clone();
            Arrays.sort(expected);

            for (int warmup = 0; warmup < RQ3_WARMUP_RUNS; warmup++) {
                for (String algorithm : algorithms) {
                    sortForRq3(algorithm, input.clone(), null);
                }
            }

            for (String algorithm : algorithms) {
                String variant = algorithm.equals("pdqsort-full") ? "full"
                        : algorithm.equals("pdqsort-noB") ? "noB" : "JDK";
                for (int run = 1; run <= runs; run++) {
                    int[] actual = input.clone();
                    SortMetrics metrics = algorithm.equals("Arrays.sort")
                            ? null : new SortMetrics();
                    long elapsedNanos = sortForRq3(algorithm, actual, metrics);
                    boolean correct = Arrays.equals(expected, actual);
                    if (!correct) {
                        failures++;
                    }
                    csv.append(String.format(Locale.ROOT,
                            "RQ3,%s,%d,%d,%s,%s,%d,%.3f,%s,%s,%s,%s,%s,%s%n",
                            dataset[0], size, seed, algorithm, variant, run,
                            elapsedNanos / 1_000_000.0, correct ? "ok" : "wrong",
                            metrics == null ? "" : metrics.maxDepth(),
                            metrics == null ? "" : metrics.partitionCount(),
                            metrics == null ? "" : metrics.heapFallbackCount(),
                            metrics == null ? "" : metrics.partialInsertionCount(),
                            correct ? "PASS" : "FAIL"));
                }
            }
        }
        if (failures != 0) {
            throw new IllegalStateException("RQ3 output validation failed for " + failures + " runs");
        }

        Path parent = csvPath.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        Files.writeString(csvPath, csv, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);
        System.out.printf(Locale.ROOT, "RQ3 seed=%d size=%d runs=%d failures=%d csv=%s%n",
                seed, size, runs, failures, csvPath);
    }

    private static int[] createRq3Dataset(String dataset, int size, long seed) {
        if ("KILLER_DUPLICATE".equals(dataset)) {
            return createKillerAdversary(size);
        }
        if ("D5_K2".equals(dataset)) {
            return DataGenerator.generateManyDuplicatesK(size, 2, new Random(seed));
        }
        return DataGenerator.generate(dataset, size, new Random(seed));
    }

    private static int[] createKillerAdversary(int size) {
        int[] values = new int[size];
        if (size > 0) {
            Arrays.fill(values, size / 2);
        }
        return values;
    }

    private static long sortForRq3(String algorithm, int[] values, SortMetrics metrics) {
        switch (algorithm) {
            case "pdqsort-full":
                long fullStart = System.nanoTime();
                if (metrics == null) {
                    PdqSort.sort(values);
                } else {
                    PdqSort.sort(values, true, true, metrics);
                }
                return System.nanoTime() - fullStart;
            case "pdqsort-noB":
                long noBStart = System.nanoTime();
                if (metrics == null) {
                    PdqSort.sortWithoutMechanismB(values);
                } else {
                    PdqSort.sort(values, true, false, metrics);
                }
                return System.nanoTime() - noBStart;
            case "Arrays.sort":
                long arraysSortStart = System.nanoTime();
                Arrays.sort(values);
                return System.nanoTime() - arraysSortStart;
            default:
                throw new IllegalArgumentException("Unknown RQ3 algorithm: " + algorithm);
        }
    }

    private static String variantName(boolean mechanismAEnabled, boolean mechanismBEnabled) {
        if (!mechanismAEnabled && !mechanismBEnabled) {
            return "noA+noB";
        }
        if (!mechanismAEnabled) {
            return "noA";
        }
        return mechanismBEnabled ? "full" : "noB";
    }

    private static void reverse(int[] values) {
        for (int left = 0, right = values.length - 1; left < right; left++, right--) {
            swap(values, left, right);
        }
    }

    private static void swap(int[] values, int first, int second) {
        int value = values[first];
        values[first] = values[second];
        values[second] = value;
    }
}