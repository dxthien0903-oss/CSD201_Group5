package edu.csd201.sort;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

public final class Rq3Benchmark {
    private static final long[] DEFAULT_SEEDS = {20261016L, 20261017L, 20261018L};
    private static final int[] DEFAULT_SIZES = {1_000_000, 10_000_000};
    private static final int[] D5_K_VALUES = {1_000, 10, 2};
    private static final String CSV_HEADER = "rq,dataset,size,k,seed,algorithm,variant,warmup_runs,"
            + "warmup_stack_overflows,repeat,elapsed_ms,output_check,comparisons,partitions,"
            + "partial_insertions,heap_fallbacks,max_depth,jdk_version,os,available_processors,status\n";

    private static final List<Algorithm> ALGORITHMS = List.of(
            new Algorithm("QuickSort", "hoare-med3", true, true, true),
            new Algorithm("pdqsort", "full", true, true, false),
            new Algorithm("pdqsort", "noA", false, true, false),
            new Algorithm("pdqsort", "noB", true, false, false),
            new Algorithm("pdqsort", "noA+noB", false, false, false),
            new Algorithm("Arrays.sort", "jdk", true, true, false));

    private Rq3Benchmark() {
    }

    public static void main(String[] args) throws IOException {
        int warmups = 2;
        int repeats = 3;
        int[] sizes = DEFAULT_SIZES.clone();
        long[] seeds = DEFAULT_SEEDS.clone();
        Path output = Path.of("results/rq3_pdqsort_metrics.csv");

        for (int index = 0; index < args.length; index++) {
            String option = args[index];
            if (index + 1 >= args.length) {
                throw new IllegalArgumentException("Missing value for " + option);
            }
            String value = args[++index];
            switch (option) {
                case "--output" -> output = Path.of(value);
                case "--warmup" -> warmups = Integer.parseInt(value);
                case "--repeats" -> repeats = Integer.parseInt(value);
                case "--sizes" -> sizes = parseSizes(value);
                case "--seeds" -> seeds = parseSeeds(value);
                default -> throw new IllegalArgumentException("Unknown option: " + option);
            }
        }
        validateConfiguration(output, warmups, repeats, sizes, seeds);

        Path absoluteOutput = output.toAbsolutePath();
        if (Files.exists(absoluteOutput)) {
            throw new java.nio.file.FileAlreadyExistsException(absoluteOutput.toString());
        }

        StringBuilder csv = new StringBuilder(CSV_HEADER);
        int caseCount = 0;
        int rowCount = 0;
        int stackOverflows = 0;
        for (int size : sizes) {
            for (Dataset dataset : datasets(size, seeds)) {
                DatasetResult result = runDataset(dataset, warmups, repeats);
                csv.append(result.csvRows);
                rowCount += result.rowCount;
                stackOverflows += result.stackOverflows;
                caseCount++;
                System.out.printf(Locale.ROOT,
                        "RQ3 %s N=%d k=%d seed=%d: %d rows, QuickSort stack overflows=%d%n",
                        dataset.name, dataset.size, dataset.k, dataset.seed,
                        result.rowCount, result.stackOverflows);
            }
        }

        writeNewFile(absoluteOutput, csv.toString());
        System.out.printf(Locale.ROOT,
                "RQ3 complete: datasets=%d rows=%d warmups=%d repeats=%d quicksort_stack_overflows=%d csv=%s%n",
                caseCount, rowCount, warmups, repeats, stackOverflows, absoluteOutput);
    }

    private static DatasetResult runDataset(Dataset dataset, int warmups, int repeats) {
        int[] input = DataGenerator.generateSpec(dataset.name, dataset.size, dataset.k, dataset.seed);
        int[] expected = input.clone();
        Arrays.sort(expected);

        StringBuilder rows = new StringBuilder();
        int rowCount = 0;
        int stackOverflows = 0;
        for (Algorithm algorithm : ALGORITHMS) {
            int warmupOverflows = warmup(algorithm, input, expected, warmups);
            stackOverflows += warmupOverflows;

            for (int repeat = 1; repeat <= repeats; repeat++) {
                int[] actual = input.clone();
                SortMetrics timingMetrics = new SortMetrics(false);
                long started = System.nanoTime();
                boolean completed = sort(algorithm, actual, timingMetrics);
                double elapsedMs = (System.nanoTime() - started) / 1_000_000.0;
                if (!completed) {
                    stackOverflows++;
                    rows.append(csvRow(dataset, algorithm, warmups, warmupOverflows,
                            repeat, elapsedMs, "", "", "", "", "", "", "STACK_OVERFLOW"));
                    rowCount++;
                    continue;
                }

                if (!Arrays.equals(expected, actual)) {
                    throw new IllegalStateException("Output mismatch: dataset=" + dataset.name
                            + " size=" + dataset.size + " k=" + dataset.k + " seed=" + dataset.seed
                            + " algorithm=" + algorithm.name + " variant=" + algorithm.variant
                            + " repeat=" + repeat);
                }

                String comparisons = "";
                String partitions = "";
                String partialInsertions = "";
                String heapFallbacks = "";
                String maxDepth = "";
                String status = "PASS";
                SortMetrics metrics = timingMetrics;
                if (algorithm.countComparisons) {
                    int[] counted = input.clone();
                    try {
                        QuickSort.Stats stats = QuickSort.sortCounted(counted);
                        if (!Arrays.equals(expected, counted)) {
                            throw new IllegalStateException("QuickSort counted output mismatch: dataset="
                                    + dataset.name + " size=" + dataset.size + " seed=" + dataset.seed);
                        }
                        comparisons = Long.toString(stats.comparisons);
                        maxDepth = Integer.toString(stats.maxDepth);
                    } catch (StackOverflowError error) {
                        status = "COUNT_STACK_OVERFLOW";
                        stackOverflows++;
                    }
                } else if (algorithm.name.equals("pdqsort")) {
                    int[] counted = input.clone();
                    metrics = new SortMetrics();
                    PdqSort.sort(counted, algorithm.mechanismAEnabled, algorithm.mechanismBEnabled, metrics);
                    if (!Arrays.equals(expected, counted)) {
                        throw new IllegalStateException("Counted pdqsort output mismatch: dataset="
                                + dataset.name + " size=" + dataset.size + " k=" + dataset.k
                                + " seed=" + dataset.seed + " variant=" + algorithm.variant);
                    }
                    comparisons = Long.toString(metrics.comparisonCount());
                    partitions = Long.toString(metrics.partitionCount());
                    partialInsertions = Long.toString(metrics.partialInsertionCount());
                    heapFallbacks = Long.toString(metrics.heapFallbackCount());
                    maxDepth = Integer.toString(metrics.maxDepth());
                }

                rows.append(csvRow(dataset, algorithm, warmups, warmupOverflows, repeat,
                        elapsedMs, Long.toString(checksum(actual)), comparisons, partitions,
                        partialInsertions, heapFallbacks, maxDepth, status));
                rowCount++;
            }
        }
        return new DatasetResult(rows.toString(), rowCount, stackOverflows);
    }

    private static int warmup(Algorithm algorithm, int[] input, int[] expected, int warmups) {
        int stackOverflows = 0;
        for (int run = 0; run < warmups; run++) {
            int[] actual = input.clone();
            boolean completed = sort(algorithm, actual, new SortMetrics());
            if (!completed) {
                stackOverflows++;
            } else if (!Arrays.equals(expected, actual)) {
                throw new IllegalStateException("Warm-up output mismatch: algorithm=" + algorithm.name
                        + " variant=" + algorithm.variant);
            }
        }
        return stackOverflows;
    }

    private static boolean sort(Algorithm algorithm, int[] values, SortMetrics metrics) {
        try {
            if (algorithm.name.equals("QuickSort")) {
                QuickSort.sort(values);
            } else if (algorithm.name.equals("Arrays.sort")) {
                Arrays.sort(values);
            } else {
                PdqSort.sort(values, algorithm.mechanismAEnabled, algorithm.mechanismBEnabled, metrics);
            }
            return true;
        } catch (StackOverflowError error) {
            if (!algorithm.name.equals("QuickSort")) {
                throw error;
            }
            return false;
        }
    }

    private static List<Dataset> datasets(int size, long[] seeds) {
        List<Dataset> result = new ArrayList<>();
        for (String name : List.of("D1", "D3")) {
            for (long seed : seeds) {
                result.add(new Dataset(name, size, 0, seed));
            }
        }
        result.add(new Dataset("D2", size, 0, seeds[0]));
        result.add(new Dataset("D4", size, 0, seeds[0]));
        for (int k : D5_K_VALUES) {
            for (long seed : seeds) {
                result.add(new Dataset("D5", size, k, seed));
            }
        }
        return result;
    }

    private static String csvRow(
            Dataset dataset,
            Algorithm algorithm,
            int warmups,
            int warmupOverflows,
            int repeat,
            double elapsedMs,
            String outputCheck,
            String comparisons,
            String partitions,
            String partialInsertions,
            String heapFallbacks,
            String maxDepth,
            String status) {
        return String.format(Locale.ROOT,
                "RQ3,%s,%d,%d,%d,%s,%s,%d,%d,%d,%.4f,%s,%s,%s,%s,%s,%s,%s,%s,%d,%s%n",
                dataset.name, dataset.size, dataset.k, dataset.seed, algorithm.name, algorithm.variant,
                warmups, warmupOverflows, repeat, elapsedMs, outputCheck, comparisons, partitions,
                partialInsertions, heapFallbacks, maxDepth, csvValue(System.getProperty("java.version")),
                csvValue(System.getProperty("os.name") + " " + System.getProperty("os.version")),
                Runtime.getRuntime().availableProcessors(), status);
    }

    private static String csvValue(String value) {
        return value.replace(',', ' ').replace('\r', ' ').replace('\n', ' ');
    }

    private static long checksum(int[] values) {
        long hash = 0;
        for (int value : values) {
            hash = 31 * hash + value;
        }
        return hash;
    }

    private static void writeNewFile(Path output, String contents) throws IOException {
        Path parent = output.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        boolean created = false;
        try (BufferedWriter writer = Files.newBufferedWriter(output, StandardCharsets.UTF_8,
                StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE)) {
            created = true;
            writer.write(contents);
        } catch (IOException exception) {
            if (created) {
                Files.deleteIfExists(output);
            }
            throw exception;
        }
    }

    private static int[] parseSizes(String value) {
        String[] parts = value.split(",");
        int[] sizes = new int[parts.length];
        for (int index = 0; index < parts.length; index++) {
            sizes[index] = Integer.parseInt(parts[index]);
        }
        return sizes;
    }

    private static long[] parseSeeds(String value) {
        String[] parts = value.split(",");
        long[] seeds = new long[parts.length];
        for (int index = 0; index < parts.length; index++) {
            seeds[index] = Long.parseLong(parts[index]);
        }
        return seeds;
    }

    private static void validateConfiguration(
            Path output, int warmups, int repeats, int[] sizes, long[] seeds) {
        if (output.toString().isBlank()) {
            throw new IllegalArgumentException("--output must not be blank");
        }
        if (warmups < 0 || repeats < 1 || sizes.length == 0 || seeds.length == 0) {
            throw new IllegalArgumentException("warmup >= 0, repeats >= 1, sizes and seeds must be non-empty");
        }
        for (int size : sizes) {
            if (size < 1) {
                throw new IllegalArgumentException("sizes must be positive: " + size);
            }
        }
        for (long seed : seeds) {
            if (seed < 0) {
                throw new IllegalArgumentException("seeds must be non-negative: " + seed);
            }
        }
    }

    private record Algorithm(
            String name,
            String variant,
            boolean mechanismAEnabled,
            boolean mechanismBEnabled,
            boolean countComparisons) {
    }

    private record Dataset(String name, int size, int k, long seed) {
    }

    private record DatasetResult(String csvRows, int rowCount, int stackOverflows) {
    }
}
