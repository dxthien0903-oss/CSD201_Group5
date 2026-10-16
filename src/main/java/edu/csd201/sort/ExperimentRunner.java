package edu.csd201.sort;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Locale;
import java.util.Random;

public final class ExperimentRunner {
    private static final int DEFAULT_SEED = 20261016;
    private static final int DEFAULT_CASES = 1_000;

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
        System.err.println("Usage: randomized-test [seed] [cases] [log-file] | --count <integers...>");
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
        log.append("distribution_note,D1-D5 provisional; see README\n");
        int[][] edgeCases = {
                {},
                {Integer.MIN_VALUE},
                {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1, Integer.MIN_VALUE, Integer.MAX_VALUE},
                {3, 3, 3, 3, 3, 3, 3, 3, 3, 3},
                {9, 8, 7, 6, 5, 4, 3, 2, 1, 0},
                java.util.stream.IntStream.range(0, 24).toArray(),
                java.util.stream.IntStream.range(0, 25).toArray(),
                java.util.stream.IntStream.range(0, 128).toArray(),
                java.util.stream.IntStream.range(0, 129).toArray()
        };
        log.append("edge_cases,").append(edgeCases.length).append('\n');
        int failures = 0;
        for (int test = 0; test < edgeCases.length; test++) {
            failures += validateArray(edgeCases[test], "edge-" + test, log);
        }
        int[] fallbackProbe = createHeapFallbackProbe();
        failures += validateArray(fallbackProbe, "heap-fallback-probe", log);
        int[] fallbackResult = fallbackProbe.clone();
        SortMetrics fallbackMetrics = new SortMetrics();
        PdqSort.sort(fallbackResult, false, fallbackMetrics);
        if (fallbackMetrics.heapFallbackCount() == 0) {
            failures++;
            log.append("failure,heap-fallback-probe,heapsort-not-reached\n");
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
            "seed=%d random_cases=%d edge_cases=%d heap_fallbacks=%d failures=%d log=%s%n",
            seed, cases, edgeCases.length, fallbackMetrics.heapFallbackCount(), failures, logPath);
        if (failures != 0) {
            System.exit(1);
        }
    }

    private static int validateArray(int[] input, String label, StringBuilder log) {
        int[] expected = input.clone();
        int[] pdq = input.clone();
        int[] noA = input.clone();
        SortMetrics noAMetrics = new SortMetrics();
        Arrays.sort(expected);
        PdqSort.sort(pdq, true, new SortMetrics());
        PdqSort.sort(noA, false, noAMetrics);
        int failures = 0;
        if (!Arrays.equals(expected, pdq)) {
            failures++;
            log.append("failure,").append(label).append(",pdqsort,")
                    .append(Arrays.toString(input)).append('\n');
        }
        if (!Arrays.equals(expected, noA)) {
            failures++;
            log.append("failure,").append(label).append(",noA,")
                    .append(Arrays.toString(input)).append('\n');
        }
        if (noAMetrics.partialInsertionCount() != 0) {
            failures++;
            log.append("failure,").append(label).append(",noA-enabled-mechanism-A\n");
        }
        return failures;
    }

    private static int[] createHeapFallbackProbe() {
        int size = 16_384;
        int[] values = new int[size];
        for (int index = 0; index < size; index++) {
            values[index] = ((index * 31) ^ (index >>> 3)) & (size - 1);
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
                    int swaps = Math.max(1, length / 20);
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
        if (args.length < 2) {
            throw new IllegalArgumentException("count requires at least one integer");
        }
        int[] values = Arrays.stream(args, 1, args.length).mapToInt(Integer::parseInt).toArray();
        SortMetrics metrics = new SortMetrics();
        PdqSort.sort(values, true, metrics);
        System.out.printf(Locale.ROOT, "max_depth=%d partitions=%d heap_fallbacks=%d%n",
                metrics.maxDepth(), metrics.partitionCount(), metrics.heapFallbackCount());
        System.out.println(Arrays.toString(values));
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