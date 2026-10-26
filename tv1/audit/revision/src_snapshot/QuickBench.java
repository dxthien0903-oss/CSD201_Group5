package sorting.bench;

import sorting.DataGenerator;
import sorting.QuickSort;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

/**
 * TV1 RQ1 runner for Quick Sort (Hoare + median-of-three).
 *
 * Timing mode measures elapsed_ms of the sort call only: dataset generation, the
 * verification copy and the output check all happen outside the measured window.
 * Counted mode runs as a separate invocation and fills comparisons/max_depth only.
 *
 * Provisional note: heap_before_bytes / heap_peak_bytes are sampled with
 * Runtime.totalMemory() - freeMemory(). The official numbers must come from TV3's core
 * benchmark framework so every algorithm shares one measurement method.
 *
 * Usage:
 *   java -Xmx4g sorting.bench.QuickBench --mode=time  --out=out/quick_sort_results.csv
 *   java -Xmx4g sorting.bench.QuickBench --mode=count --out=out/quick_sort_results.csv
 * Options: --ns=1e5 list, --dataset=d1|d2, --seed=N, --reps=N, --include-50m,
 *          --out=path, --meta=path, --run-id=text, --tolerate-errors
 */
public final class QuickBench {

    private static final String CSV_HEADER = "algorithm,variant,dataset,N,K,seed,elapsed_ms,"
            + "heap_before_bytes,heap_peak_bytes,output_check,comparisons,max_depth,mode,"
            + "aux_bytes,sorted_ok,rep,jdk,os,arch,cores,max_heap_bytes,run_id";

    private QuickBench() {
    }

    public static void main(String[] args) throws Exception {
        Options o = Options.parse(args);

        System.out.println("QuickBench algorithm=" + QuickSort.ALGORITHM + " variant=" + QuickSort.VARIANT);
        System.out.println("mode=" + o.mode + " dataset=" + o.dataset + " seed=" + o.seed
                + " reps=" + o.reps + " ns=" + o.nsList());
        System.out.println("jdk=" + System.getProperty("java.version")
                + " os=" + System.getProperty("os.name") + " arch=" + System.getProperty("os.arch")
                + " cores=" + Runtime.getRuntime().availableProcessors()
                + " maxHeap=" + Runtime.getRuntime().maxMemory());

        if (o.meta != null) {
            writeMeta(o);
        }
        for (int i = 0; i < o.ns.length; i++) {
            int n = o.ns[i];
            for (int rep = 1; rep <= o.reps; rep++) {
                try {
                    if (Options.MODE_COUNT.equals(o.mode)) {
                        runCounted(o, n, rep);
                    } else {
                        runTimed(o, n, rep);
                    }
                } catch (Throwable thrown) {
                    if (!o.tolerateErrors) {
                        if (thrown instanceof Exception) {
                            throw (Exception) thrown;
                        }
                        if (thrown instanceof Error) {
                            throw (Error) thrown;
                        }
                        throw new RuntimeException(thrown);
                    }
                    writeErrorRow(o, n, rep, thrown);
                }
            }
        }
    }

    /**
     * Records a failed configuration (stack overflow, OOM, timeout wrapper, unexpected error)
     * in a separate file so the main CSV keeps one stable schema. Enabled only with
     * --tolerate-errors, which is meant for killer-adversary collaboration runs.
     */
    private static void writeErrorRow(Options o, int n, int rep, Throwable thrown) throws IOException {
        Path path = errorPath(o);
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        boolean newFile = !Files.exists(path);
        StackTraceElement[] trace = thrown.getStackTrace();
        String topFrame = trace.length == 0 ? "" : trace[0].toString();
        StringBuilder sb = new StringBuilder();
        if (newFile) {
            sb.append("algorithm,variant,dataset,N,seed,mode,rep,error,message,top_frame")
                    .append(System.lineSeparator());
        }
        sb.append(String.join(",", QuickSort.ALGORITHM, QuickSort.VARIANT, o.dataset,
                String.valueOf(n), String.valueOf(o.seed), o.mode, String.valueOf(rep),
                thrown.getClass().getName(), quote(String.valueOf(thrown.getMessage())),
                quote(topFrame))).append(System.lineSeparator());
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        System.out.println("ERROR n=" + n + " rep=" + rep + " mode=" + o.mode + " -> "
                + thrown.getClass().getName() + ": " + thrown.getMessage()
                + " (row written to " + path.getFileName() + ")");
    }

    private static Path errorPath(Options o) {
        Path out = Paths.get(o.out).toAbsolutePath();
        String name = out.getFileName().toString();
        int dot = name.lastIndexOf('.');
        String errorName = (dot > 0 ? name.substring(0, dot) : name) + ".errors.csv";
        return out.getParent() == null ? Paths.get(errorName) : out.getParent().resolve(errorName);
    }

    private static String quote(String value) {
        return "\"" + value.replace("\"", "\"\"") + "\"";
    }

    private static void runTimed(Options o, int n, int rep) throws IOException {
        int[] data = generate(o, n);
        int[] reference = data.clone();
        Arrays.sort(reference);

        System.gc();
        sleepQuietly(200L);
        long heapBefore = usedHeap();

        HeapSampler sampler = new HeapSampler();
        sampler.start();
        long start = System.nanoTime();
        QuickSort.sort(data);
        long stop = System.nanoTime();
        sampler.stop();
        long heapPeak = sampler.peak();

        double elapsedMs = (stop - start) / 1_000_000.0;
        long outputCheck = outputCheck(data);
        boolean sortedOk = Arrays.equals(reference, data);

        String row = row(o, n, rep, "time", format(elapsedMs), String.valueOf(heapBefore),
                String.valueOf(heapPeak), String.valueOf(outputCheck), "", "",
                String.valueOf(Math.max(0L, heapPeak - heapBefore)), String.valueOf(sortedOk));
        appendRow(o, row);
        System.out.println("n=" + n + " rep=" + rep + " elapsed_ms=" + format(elapsedMs)
                + " heap_before=" + heapBefore + " heap_peak=" + heapPeak
                + " output_check=" + outputCheck + " sorted_ok=" + sortedOk);
    }

    private static void runCounted(Options o, int n, int rep) throws IOException {
        int[] data = generate(o, n);
        int[] reference = data.clone();
        Arrays.sort(reference);

        QuickSort.Stats stats = QuickSort.sortCounted(data);
        long outputCheck = outputCheck(data);
        boolean sortedOk = Arrays.equals(reference, data);

        String row = row(o, n, rep, "count", "", "", "", String.valueOf(outputCheck),
                String.valueOf(stats.comparisons), String.valueOf(stats.maxDepth), "",
                String.valueOf(sortedOk));
        appendRow(o, row);
        System.out.println("n=" + n + " rep=" + rep + " comparisons=" + stats.comparisons
                + " max_depth=" + stats.maxDepth + " output_check=" + outputCheck
                + " sorted_ok=" + sortedOk);
    }

    private static int[] generate(Options o, int n) {
        if (Options.DATASET_D2.equals(o.dataset)) {
            return DataGenerator.d2(n, o.seed);
        }
        return DataGenerator.d1(n, o.seed);
    }

    /** Shared output_check helper: h = 31 * h + a[i], long with allowed overflow. */
    public static long outputCheck(int[] a) {
        long h = 0L;
        for (int i = 0; i < a.length; i++) {
            h = 31L * h + a[i];
        }
        return h;
    }

    private static String row(Options o, int n, int rep, String mode, String elapsedMs,
            String heapBefore, String heapPeak, String outputCheck, String comparisons,
            String maxDepth, String auxBytes, String sortedOk) {
        String[] fields = {
                QuickSort.ALGORITHM,
                QuickSort.VARIANT,
                o.dataset,
                String.valueOf(n),
                "",                                  // K: only filled for D5 (TV4)
                String.valueOf(o.seed),
                elapsedMs,
                heapBefore,
                heapPeak,
                outputCheck,
                comparisons,
                maxDepth,
                mode,
                auxBytes,
                sortedOk,
                String.valueOf(rep),
                System.getProperty("java.version"),
                System.getProperty("os.name"),
                System.getProperty("os.arch"),
                String.valueOf(Runtime.getRuntime().availableProcessors()),
                String.valueOf(Runtime.getRuntime().maxMemory()),
                o.runId,
        };
        return String.join(",", fields);
    }

    private static void appendRow(Options o, String row) throws IOException {
        Path path = Paths.get(o.out).toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        boolean newFile = !Files.exists(path);
        StringBuilder sb = new StringBuilder();
        if (newFile) {
            sb.append(CSV_HEADER).append(System.lineSeparator());
        }
        sb.append(row).append(System.lineSeparator());
        Files.write(path, sb.toString().getBytes(StandardCharsets.UTF_8),
                StandardOpenOption.CREATE, StandardOpenOption.APPEND);
    }

    private static void writeMeta(Options o) throws IOException {
        Path path = Paths.get(o.meta).toAbsolutePath();
        Path parent = path.getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }
        String json = "{\n"
                + "  \"run_id\": \"" + o.runId + "\",\n"
                + "  \"mode\": \"" + o.mode + "\",\n"
                + "  \"dataset\": \"" + o.dataset + "\",\n"
                + "  \"seed\": " + o.seed + ",\n"
                + "  \"ns\": [" + o.nsList() + "],\n"
                + "  \"reps\": " + o.reps + ",\n"
                + "  \"java_version\": \"" + System.getProperty("java.version") + "\",\n"
                + "  \"java_vm\": \"" + System.getProperty("java.vm.name") + "\",\n"
                + "  \"os\": \"" + System.getProperty("os.name") + " "
                + System.getProperty("os.version") + "\",\n"
                + "  \"arch\": \"" + System.getProperty("os.arch") + "\",\n"
                + "  \"available_processors\": " + Runtime.getRuntime().availableProcessors() + ",\n"
                + "  \"max_heap_bytes\": " + Runtime.getRuntime().maxMemory() + ",\n"
                + "  \"note\": \"TV1 provisional harness; official memory numbers must come from the shared TV3 framework\"\n"
                + "}\n";
        Files.write(path, json.getBytes(StandardCharsets.UTF_8), StandardOpenOption.CREATE);
    }

    private static String format(double value) {
        return String.format(Locale.ROOT, "%.3f", value);
    }

    private static long usedHeap() {
        Runtime runtime = Runtime.getRuntime();
        return runtime.totalMemory() - runtime.freeMemory();
    }

    private static void sleepQuietly(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    /** Samples used heap while the sort runs; peak is retried on each poll. */
    private static final class HeapSampler {
        private final Thread thread;
        private volatile boolean running;
        private volatile long peak;

        HeapSampler() {
            this.thread = new Thread(new Runnable() {
                @Override
                public void run() {
                    while (running) {
                        long used = usedHeap();
                        if (used > peak) {
                            peak = used;
                        }
                        sleepQuietly(1L);
                    }
                }
            }, "heap-sampler");
            this.thread.setDaemon(true);
        }

        void start() {
            peak = usedHeap();
            running = true;
            thread.start();
        }

        void stop() {
            running = false;
            try {
                thread.join(500L);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        long peak() {
            return peak;
        }
    }

    private static final class Options {
        static final String MODE_TIME = "time";
        static final String MODE_COUNT = "count";
        static final String DATASET_D1 = "d1";
        static final String DATASET_D2 = "d2";

        int[] ns = {100_000, 1_000_000, 5_000_000, 10_000_000, 20_000_000};
        String dataset = DATASET_D1;
        long seed = 20261016L;
        String mode = MODE_TIME;
        int reps = 1;
        String out = "out/quick_sort_results.csv";
        String meta;
        String runId = defaultRunId();
        boolean tolerateErrors;

        String nsList() {
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < ns.length; i++) {
                if (i > 0) {
                    sb.append(',');
                }
                sb.append(ns[i]);
            }
            return sb.toString();
        }

        static String defaultRunId() {
            return "quick-" + System.currentTimeMillis();
        }

        static Options parse(String[] args) {
            Options o = new Options();
            List<Integer> sizes = null;
            boolean include50m = false;
            for (String arg : args) {
                if (!arg.startsWith("--")) {
                    throw new IllegalArgumentException("unexpected argument: " + arg);
                }
                String body = arg.substring(2);
                int eq = body.indexOf('=');
                String key = eq < 0 ? body : body.substring(0, eq);
                String value = eq < 0 ? "" : body.substring(eq + 1);
                if ("ns".equals(key)) {
                    sizes = new ArrayList<>();
                    for (String part : value.split(",")) {
                        sizes.add(Integer.valueOf(part.trim()));
                    }
                } else if ("dataset".equals(key)) {
                    o.dataset = value;
                } else if ("seed".equals(key)) {
                    o.seed = Long.parseLong(value);
                } else if ("mode".equals(key)) {
                    o.mode = value;
                } else if ("reps".equals(key)) {
                    o.reps = Integer.parseInt(value);
                } else if ("out".equals(key)) {
                    o.out = value;
                } else if ("meta".equals(key)) {
                    o.meta = value;
                } else if ("run-id".equals(key)) {
                    o.runId = value;
                } else if ("include-50m".equals(key)) {
                    include50m = true;
                } else if ("tolerate-errors".equals(key)) {
                    o.tolerateErrors = true;
                } else {
                    throw new IllegalArgumentException("unknown option: " + arg);
                }
            }
            if (sizes != null) {
                o.ns = new int[sizes.size()];
                for (int i = 0; i < sizes.size(); i++) {
                    o.ns[i] = sizes.get(i);
                }
            } else if (include50m) {
                int[] extended = Arrays.copyOf(o.ns, o.ns.length + 1);
                extended[extended.length - 1] = 50_000_000;
                o.ns = extended;
            }
            if (!MODE_TIME.equals(o.mode) && !MODE_COUNT.equals(o.mode)) {
                throw new IllegalArgumentException("mode must be time or count");
            }
            if (!DATASET_D1.equals(o.dataset) && !DATASET_D2.equals(o.dataset)) {
                throw new IllegalArgumentException("dataset must be d1 or d2");
            }
            if (o.reps < 1) {
                throw new IllegalArgumentException("reps must be >= 1");
            }
            return o;
        }
    }
}
