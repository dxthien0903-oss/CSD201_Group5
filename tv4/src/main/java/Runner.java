import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.TimeUnit;

/**
 * Runner (tiến trình cha) của TV4: sinh plan, chạy cả ma trận cấu hình, kiểm tra và tổng hợp CSV.
 *
 *   gen-plan  --plan bench/plan_radix.csv [--big]
 *   run       --plan bench/plan_radix.csv --out results/raw/radix.csv --xmx 2g --machine-id M1
 *             [--timeout-sec 60] [--jar duong_dan.jar]
 *   check     --plan ... --out ... [--strict]
 *   summary   --out results/raw/radix.csv [--summary-out results/radix_summary.csv]
 *   selftest                                (tự kiểm tra toàn bộ quy trình, vài chục giây)
 *
 * --big    : thêm N = 5x10^7 vào RQ1 (chỉ khi máy đủ RAM).
 *
 * Lệnh đo đúng như quy định: `java -Xmx<xmx> -jar <jar> worker ...` — KHÔNG thêm cờ JVM nào khác.
 * Thực nghiệm: thêm -Xms lớn (>= 1g) làm heap_peak - heap_before ra 0 ở N nhỏ (TLAB cỡ hàng chục MB
 * che mất mảng phụ), còn đúng lệnh trên thì bộ nhớ phụ đúng ~4N ở mọi N; `selftest` kiểm tra điều này
 * trên chính máy đo. Timeout: waitFor(timeoutSec) rồi destroyForcibly() (mặc định 60 s, tính cả khởi động
 * JVM và sinh dữ liệu; với Radix phần chuẩn bị << 60 s).
  * Mỗi lượt đo chạy trong MỘT JVM riêng; các lượt chạy tuần tự, không song song.
 */
public final class Runner {

    static final String HEADER = "run_id,rq,algorithm,variant,dataset,n,params,seed,input_sha256,"
            + "generator_version,repeat,elapsed_ms,heap_before_bytes,heap_peak_bytes,ops_count,"
            + "output_check,status,xmx,machine_id,git_commit,array_reads,array_writes,max_depth,"
            + "k,jdk,os,cpu_cores,ram_mb,jvm_flags";
    static final String[] COLS = HEADER.split(",");
    static final Map<String, Integer> IDX = new HashMap<String, Integer>();
    static {
        for (int i = 0; i < COLS.length; i++) IDX.put(COLS[i], i);
    }

    static final String PLAN_HEADER = "run_id,rq,algo,variant,dataset,n,k,seed,repeat,count";

    static volatile Process current;

    public static void main(String[] args) throws Exception {
        Map<String, String> o = Worker.parse(args);
        String cmd = args.length > 0 ? args[0] : "";
        Path plan = Paths.get(o.getOrDefault("plan", "bench/plan_radix.csv"));
        Path out = Paths.get(o.getOrDefault("out", "results/raw/radix.csv"));
        if (cmd.equals("gen-plan")) {
            genPlan(plan, o.containsKey("big"));
        } else if (cmd.equals("run")) {
            String jar = o.containsKey("jar") ? o.get("jar") : ownJar();
            String xmxArg = o.getOrDefault("xmx", "2g"), mid = o.getOrDefault("machine-id", "M1");
            if (!xmxArg.matches("[0-9]+[kKmMgG]")) throw new IllegalArgumentException("--xmx phải có dạng 2g, 512m, ...");
            if (!mid.matches("[A-Za-z0-9_.-]+")) throw new IllegalArgumentException("--machine-id chỉ gồm chữ, số, _ . - (không có dấu phẩy/khoảng trắng)");
            runPlan(plan, out, jar, o.getOrDefault("xmx", "2g"), o.getOrDefault("machine-id", "M1"),
                    Integer.parseInt(o.getOrDefault("timeout-sec", "60"))); // đo chính thức: để 60
        } else if (cmd.equals("check")) {
            requireFile(plan, "plan");
            requireFile(out, "CSV kết quả");
            System.exit(check(plan, out, o.containsKey("strict")) ? 0 : 1);
        } else if (cmd.equals("summary")) {
            requireFile(out, "CSV kết quả");
            summary(out, o.containsKey("summary-out") ? Paths.get(o.get("summary-out")) : null);
        } else if (cmd.equals("selftest")) {
            System.exit(selfTest(o.containsKey("jar") ? o.get("jar") : ownJar()) ? 0 : 1);
        } else {
            System.out.println("Dùng: gen-plan | run | check | summary | selftest | worker");
        }
    }

    // ===================== Sinh plan =====================
    static void genPlan(Path out, boolean big) throws IOException {
        List<String> rows = new ArrayList<String>();
        rows.add(PLAN_HEADER);
        int[] id = {0};
        int[] rq1N = big
                ? new int[]{100000, 1000000, 5000000, 10000000, 20000000, 50000000}
                : new int[]{100000, 1000000, 5000000, 10000000, 20000000};
        for (int n : rq1N) {
            for (int seed = 1; seed <= 3; seed++)
                for (int rep = 1; rep <= 3; rep++) rows.add(row(id, "RQ1", "D1", n, 0, seed, rep, false));
            rows.add(row(id, "RQ1", "D1", n, 0, 1, 1, true)); // lượt --count
        }
        int[] rq2N = {1000000, 10000000};
        String[] dsA = {"D1", "D2", "D3", "D4"};
        for (int n : rq2N) {
            for (String ds : dsA) {
                int seeds = (ds.equals("D1") || ds.equals("D3")) ? 3 : 1;
                for (int seed = 1; seed <= seeds; seed++)
                    for (int rep = 1; rep <= 3; rep++) rows.add(row(id, "RQ2", ds, n, 0, seed, rep, false));
                rows.add(row(id, "RQ2", ds, n, 0, 1, 1, true));
            }
            for (int k : new int[]{1000, 10, 2}) {
                for (int seed = 1; seed <= 3; seed++)
                    for (int rep = 1; rep <= 3; rep++) rows.add(row(id, "RQ2", "D5", n, k, seed, rep, false));
                rows.add(row(id, "RQ2", "D5", n, k, 1, 1, true));
            }
        }
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        Files.write(out, rows, StandardCharsets.UTF_8);
        System.out.println("Đã ghi " + (rows.size() - 1) + " lượt vào " + out);
    }

    static String row(int[] id, String rq, String ds, int n, int k, int seed, int rep, boolean count) {
        id[0]++;
        return String.format(Locale.ROOT, "R%04d,%s,radix,base,%s,%d,%d,%d,%d,%s",
                id[0], rq, ds, n, k, seed, rep, count);
    }

    // ===================== Đọc và kiểm tra plan =====================
    static List<String[]> readPlan(Path plan) throws IOException {
        List<String> lines = Files.readAllLines(plan, StandardCharsets.UTF_8);
        List<String[]> rows = new ArrayList<String[]>();
        Set<String> ids = new HashSet<String>();
        boolean headerSeen = false;
        for (int i = 0; i < lines.size(); i++) {
            String line = lines.get(i).replace("\uFEFF", "").trim(); // bỏ BOM và \r của Windows
            if (line.isEmpty()) continue;
            if (!headerSeen) {
                if (!line.equals(PLAN_HEADER)) {
                    throw new IllegalArgumentException("Dòng đầu của plan phải là: " + PLAN_HEADER);
                }
                headerSeen = true;
                continue;
            }
            String[] f = line.split(",", -1);
            validateRow(f, i + 1);
            if (!ids.add(f[0])) throw new IllegalArgumentException("Dòng " + (i + 1) + ": trùng run_id " + f[0]);
            rows.add(f);
        }
        if (!headerSeen) throw new IllegalArgumentException("Plan rỗng: " + plan);
        return rows;
    }

    static void validateRow(String[] f, int line) {
        String where = "Plan dòng " + line + ": ";
        if (f.length != 10) throw new IllegalArgumentException(where + "cần 10 cột, có " + f.length);
        if (f[0].isEmpty()) throw new IllegalArgumentException(where + "run_id rỗng");
        if (!f[4].matches("D[1-5]")) throw new IllegalArgumentException(where + "dataset phải là D1..D5");
        try {
            int n = Integer.parseInt(f[5]);
            int k = Integer.parseInt(f[6]);
            Long.parseLong(f[7]);
            int rep = Integer.parseInt(f[8]);
            if (n < 0) throw new IllegalArgumentException(where + "n < 0");
            if (rep < 1) throw new IllegalArgumentException(where + "repeat < 1");
            if (f[4].equals("D5") && k < 1) throw new IllegalArgumentException(where + "D5 cần k >= 1");
            if (!f[4].equals("D5") && k != 0) throw new IllegalArgumentException(where + "chỉ D5 mới có k");
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException(where + "n/k/seed/repeat phải là số: " + e.getMessage());
        }
        if (!f[9].equals("true") && !f[9].equals("false")) {
            throw new IllegalArgumentException(where + "cột count phải là true hoặc false");
        }
    }

    // ===================== Chuẩn bị file CSV đầu ra (chạy lại sau khi bị ngắt) =====================
    /** Trả về tập run_id đã có đủ cột. Dòng bị cắt dở, dòng trùng run_id bị loại và ghi lại file. */
    static Set<String> prepareOut(Path out) throws IOException {
        Set<String> done = new HashSet<String>();
        if (out.getParent() != null) Files.createDirectories(out.getParent());
        if (!Files.exists(out) || Files.size(out) == 0) {
            Files.write(out, Arrays.asList(HEADER), StandardCharsets.UTF_8);
            return done;
        }
        List<String> old = Files.readAllLines(out, StandardCharsets.UTF_8);
        if (!old.get(0).equals(HEADER)) {
            throw new IllegalStateException("Header của " + out + " khác schema hiện tại; "
                    + "hãy ghi ra file --out mới để không trộn hai schema.");
        }
        byte[] all = Files.readAllBytes(out);
        boolean endsWithNewline = all[all.length - 1] == '\n';
        List<String> keep = new ArrayList<String>();
        keep.add(HEADER);
        int dropped = 0;
        for (int i = 1; i < old.size(); i++) {
            String l = old.get(i);
            if (l.trim().isEmpty()) continue;
            String[] c = l.split(",", -1);
            if (c.length == COLS.length && done.add(c[0])) keep.add(l);
            else dropped++;
        }
        if (dropped > 0 || !endsWithNewline) {
            if (dropped > 0) System.out.println("Cảnh báo: bỏ " + dropped + " dòng hỏng/trùng trong " + out);
            Files.write(out, keep, StandardCharsets.UTF_8);
        }
        return done;
    }

    // ===================== Chạy plan =====================
    static void runPlan(Path plan, Path out, String jar, String xmx, String machine,
                        int timeoutSec) throws Exception {
        List<String[]> rows = readPlan(plan);       // kiểm tra TOÀN BỘ plan trước khi chạy lượt đầu
        Set<String> done = prepareOut(out);
        final String commit = gitCommit();
        if (commit.endsWith("-dirty")) {
            System.out.println("Cảnh báo: có thay đổi chưa commit trong mã nguồn (" + commit
                    + "). Đo chính thức nên commit trước để cột git_commit đúng bản đã đo.");
        }
        Runtime.getRuntime().addShutdownHook(new Thread(new Runnable() {
            public void run() {
                Process p = current;                 // không để JVM con mồ côi làm nhiễu lượt sau
                if (p != null) p.destroyForcibly();
            }
        }));
        int total = rows.size(), idx = 0;
        for (String[] f : rows) {
            idx++;
            if (done.contains(f[0])) continue;
            String csv = executeOne(f, jar, xmx, machine, commit, timeoutSec);
            Files.write(out, Arrays.asList(csv), StandardCharsets.UTF_8, StandardOpenOption.APPEND);
            String[] c = csv.split(",", -1);
            System.out.println("[" + idx + "/" + total + "] " + f[0] + " " + f[1] + " " + f[4]
                    + " n=" + f[5] + " seed=" + f[7] + " -> " + c[IDX.get("status")] + " "
                    + c[IDX.get("elapsed_ms")] + " ms");
        }
    }

    /** Chạy một lượt trong JVM riêng và trả về một dòng CSV theo HEADER. */
    static String executeOne(String[] f, String jar, String xmx, String machine, String commit,
                             int timeoutSec) throws Exception {
        String runId = f[0], rq = f[1], algo = f[2], variant = f[3], ds = f[4], n = f[5];
        int k = Integer.parseInt(f[6]);
        String seed = f[7], rep = f[8];
        boolean count = Boolean.parseBoolean(f[9]);

        List<String> cmd = new ArrayList<String>();
        cmd.add(System.getProperty("java.home") + File.separator + "bin" + File.separator + "java");
        cmd.add("-Xmx" + xmx);                   // đúng lệnh đo của quy định; không thêm cờ JVM khác
        String flags = "-Xmx" + xmx;
        cmd.add("-jar");
        cmd.add(jar);
        cmd.add("worker");
        cmd.addAll(Arrays.asList("--algo", algo, "--variant", variant, "--dataset", ds,
                "--n", n, "--seed", seed));
        if (k > 0) cmd.addAll(Arrays.asList("--k", String.valueOf(k)));
        if (count) cmd.add("--count");

        // Ghi stdout/stderr ra file tạm để tránh nghẽn đường ống khi chờ
        File so = File.createTempFile("radix-out", ".txt");
        File se = File.createTempFile("radix-err", ".txt");
        String sha = "", err = "";
        String[] res = null, env = null;
        String status = null;
        try {
            ProcessBuilder pb = new ProcessBuilder(cmd);
            pb.redirectOutput(so);
            pb.redirectError(se);
            Process p = pb.start();
            current = p;
            if (!p.waitFor(timeoutSec, TimeUnit.SECONDS)) {   // quy định: waitFor(60) rồi destroyForcibly()
                p.destroyForcibly();
                p.waitFor();
                status = "timeout";
            }
            current = null;
            for (String line : Files.readAllLines(so.toPath(), StandardCharsets.UTF_8)) {
                if (line.startsWith("SHA,")) sha = line.substring(4).trim();
                else if (line.startsWith("ENV,")) env = line.split(",", -1);
                else if (line.startsWith("RESULT,") && res == null) res = line.split(",", -1);
            }
            err = new String(Files.readAllBytes(se.toPath()), StandardCharsets.UTF_8);
        } finally {
            current = null;
            so.delete();
            se.delete();
        }

        String elapsed = "", before = "", peak = "", check = "", ops = "", rd = "", wr = "";
        if (status == null) {
            if (res != null && res.length >= 9) {
                status = res[1];
                elapsed = res[2]; before = res[3]; peak = res[4]; check = res[5];
                ops = res[6]; rd = res[7]; wr = res[8];
            } else if (err.contains("OutOfMemoryError")) {
                status = "oom";
            } else {
                status = "crash"; // JVM chết mà không in RESULT (StackOverflowError, tham số sai, ...)
            }
        }
        if (count) elapsed = ""; // lượt --count không dùng để đo thời gian

        StringBuilder params = new StringBuilder();
        if (k > 0) params.append("k=").append(k);
        if (count) params.append(params.length() > 0 ? ";" : "").append("mode=count");

        String jdk = "", os = "", cores = "", ram = "";
        if (env != null && env.length >= 5) { jdk = env[1]; os = env[2]; cores = env[3]; ram = env[4]; }

        return String.join(",", runId, rq, algo, variant, ds, n, params.toString(), seed, sha,
                Datasets.GENERATOR_VERSION, rep, elapsed, before, peak, ops, check, status, xmx,
                machine, commit, rd, wr, "",
                k > 0 ? String.valueOf(k) : "", jdk, os, cores, ram, flags);
    }

    // ===================== check: CSV đủ hàng, đúng nội dung =====================
    static boolean check(Path plan, Path out, boolean strict) throws IOException {
        List<String[]> pl = readPlan(plan);
        Map<String, String[]> want = new LinkedHashMap<String, String[]>();
        for (String[] f : pl) want.put(f[0], f);

        List<String> ol = Files.readAllLines(out, StandardCharsets.UTF_8);
        if (ol.isEmpty() || !ol.get(0).equals(HEADER)) {
            System.out.println("Header CSV khác schema hiện tại.");
            return false;
        }
        Map<String, Integer> byStatus = new LinkedHashMap<String, Integer>();
        Set<String> have = new HashSet<String>();
        int badCols = 0, dup = 0, missingSha = 0, mismatch = 0, badOps = 0, noMeasure = 0, wrong = 0;
        for (int i = 1; i < ol.size(); i++) {
            if (ol.get(i).trim().isEmpty()) continue;
            String[] c = ol.get(i).split(",", -1);
            if (c.length != COLS.length) { badCols++; continue; }
            String id = c[0];
            if (!have.add(id)) dup++;
            String status = c[IDX.get("status")];
            Integer v = byStatus.get(status);
            byStatus.put(status, v == null ? 1 : v + 1);
            if (c[IDX.get("input_sha256")].isEmpty()) missingSha++;
            if (status.equals("wrong")) wrong++;
            String[] f = want.get(id);
            if (f != null) { // đối chiếu cấu hình trong CSV với plan
                boolean same = c[IDX.get("rq")].equals(f[1]) && c[IDX.get("algorithm")].equals(f[2])
                        && c[IDX.get("variant")].equals(f[3]) && c[IDX.get("dataset")].equals(f[4])
                        && c[IDX.get("n")].equals(f[5]) && c[IDX.get("seed")].equals(f[7])
                        && c[IDX.get("repeat")].equals(f[8]);
                if (!same) mismatch++;
            }
            if (status.equals("ok")) {
                boolean isCount = c[IDX.get("params")].contains("mode=count");
                if (c[IDX.get("output_check")].isEmpty() || c[IDX.get("heap_before_bytes")].isEmpty()
                        || c[IDX.get("heap_peak_bytes")].isEmpty()
                        || (!isCount && c[IDX.get("elapsed_ms")].isEmpty())) noMeasure++;
                if (isCount && c[IDX.get("algorithm")].equals("radix")) {
                    long n = Long.parseLong(c[IDX.get("n")]);
                    long er = n < 2 ? 0 : 8 * n, ew = n < 2 ? 0 : 4 * n;
                    if (!(c[IDX.get("array_reads")].equals(String.valueOf(er))
                            && c[IDX.get("array_writes")].equals(String.valueOf(ew)))) badOps++;
                }
            }
        }
        List<String> missing = new ArrayList<String>();
        for (String id : want.keySet()) if (!have.contains(id)) missing.add(id);
        Collections.sort(missing);

        int notOk = 0;
        for (Map.Entry<String, Integer> e : byStatus.entrySet()) if (!e.getKey().equals("ok")) notOk += e.getValue();
        System.out.println("Kế hoạch: " + want.size() + " lượt | Đã có: " + have.size()
                + " | Thiếu: " + missing.size() + " | Trùng run_id: " + dup + " | Sai số cột: " + badCols);
        System.out.println("Thiếu SHA: " + missingSha + " | Lệch cấu hình so với plan: " + mismatch
                + " | Thiếu số đo ở lượt ok: " + noMeasure + " | Sai số đếm Radix (8N/4N): " + badOps
                + " | Kết quả sai (wrong): " + wrong);
        System.out.println("Theo trạng thái: " + byStatus);
        if (!missing.isEmpty()) System.out.println("Run_id còn thiếu: " + missing);
        boolean ok = missing.isEmpty() && dup == 0 && badCols == 0 && missingSha == 0 && mismatch == 0
                && noMeasure == 0 && badOps == 0 && wrong == 0 && (!strict || notOk == 0);
        if (ok && notOk > 0) System.out.println("Lưu ý: có " + notOk + " lượt không ok (dùng --strict để coi là lỗi).");
        System.out.println(ok ? "CSV ĐẠT." : "CSV CHƯA ĐẠT (xem các số liệu ở trên).");
        return ok;
    }

    // ===================== summary: tổng hợp số liệu cho phần Results =====================
    static double median(List<Double> v) {
        List<Double> s = new ArrayList<Double>(v);
        Collections.sort(s);
        int m = s.size();
        return m % 2 == 1 ? s.get(m / 2) : (s.get(m / 2 - 1) + s.get(m / 2)) / 2.0;
    }

    static void summary(Path out, Path summaryOut) throws IOException {
        List<String> ol = Files.readAllLines(out, StandardCharsets.UTF_8);
        Map<String, List<Double>> ms = new LinkedHashMap<String, List<Double>>();
        Map<String, List<Double>> aux = new HashMap<String, List<Double>>();
        Map<String, Integer> total = new HashMap<String, Integer>();
        Map<String, double[]> ops = new HashMap<String, double[]>(); // reads/n, writes/n
        Map<String, String[]> keyParts = new HashMap<String, String[]>();
        for (int i = 1; i < ol.size(); i++) {
            String[] c = ol.get(i).split(",", -1);
            if (c.length != COLS.length) continue;
            boolean isCount = c[IDX.get("params")].contains("mode=count");
            String[] kp = {c[IDX.get("rq")], c[IDX.get("algorithm")], c[IDX.get("variant")],
                    c[IDX.get("dataset")], c[IDX.get("k")], c[IDX.get("n")]};
            String key = String.join("|", kp);
            keyParts.put(key, kp);
            if (!ms.containsKey(key)) {
                ms.put(key, new ArrayList<Double>());
                aux.put(key, new ArrayList<Double>());
                total.put(key, 0);
            }
            if (isCount) {
                if (c[IDX.get("status")].equals("ok") && !c[IDX.get("array_reads")].isEmpty()) {
                    double n = Double.parseDouble(c[IDX.get("n")]);
                    ops.put(key, new double[]{Double.parseDouble(c[IDX.get("array_reads")]) / n,
                            Double.parseDouble(c[IDX.get("array_writes")]) / n});
                }
                continue;
            }
            total.put(key, total.get(key) + 1);
            if (c[IDX.get("status")].equals("ok")) {
                ms.get(key).add(Double.parseDouble(c[IDX.get("elapsed_ms")]));
                aux.get(key).add(Double.parseDouble(c[IDX.get("heap_peak_bytes")])
                        - Double.parseDouble(c[IDX.get("heap_before_bytes")]));
            }
        }
        List<String> lines = new ArrayList<String>();
        lines.add("rq,algorithm,variant,dataset,k,n,runs_ok,runs_total,median_ms,min_ms,max_ms,cv_percent,"
                + "throughput_mps,aux_median_bytes,aux_bytes_per_element,array_reads_per_n,array_writes_per_n");
        for (Map.Entry<String, List<Double>> e : ms.entrySet()) {
            String key = e.getKey();
            String[] kp = keyParts.get(key);
            List<Double> v = e.getValue();
            String rd = ops.containsKey(key) ? fmt(ops.get(key)[0]) : "";
            String wr = ops.containsKey(key) ? fmt(ops.get(key)[1]) : "";
            if (v.isEmpty()) {
                lines.add(String.join(",", kp[0], kp[1], kp[2], kp[3], kp[4], kp[5], "0",
                        String.valueOf(total.get(key)), "", "", "", "", "", "", "", rd, wr));
                continue;
            }
            double med = median(v), min = Collections.min(v), max = Collections.max(v);
            double mean = 0;
            for (double d : v) mean += d;
            mean /= v.size();
            double var = 0;
            for (double d : v) var += (d - mean) * (d - mean);
            double sd = v.size() > 1 ? Math.sqrt(var / (v.size() - 1)) : 0;
            double n = Double.parseDouble(kp[5]);
            double auxMed = median(aux.get(key));
            lines.add(String.join(",", kp[0], kp[1], kp[2], kp[3], kp[4], kp[5],
                    String.valueOf(v.size()), String.valueOf(total.get(key)),
                    fmt(med), fmt(min), fmt(max), fmt(mean > 0 ? 100 * sd / mean : 0),
                    med > 0 ? fmt(n / (med * 1000.0)) : "",       // triệu phần tử / giây
                    String.valueOf(Math.round(auxMed)), n > 0 ? fmt(auxMed / n) : "", rd, wr));
        }
        for (String l : lines) System.out.println(l);
        if (summaryOut != null) {
            if (summaryOut.getParent() != null) Files.createDirectories(summaryOut.getParent());
            Files.write(summaryOut, lines, StandardCharsets.UTF_8);
            System.out.println("Đã ghi " + summaryOut);
        }
    }

    static void requireFile(Path p, String what) {
        if (!Files.exists(p)) throw new IllegalArgumentException("Không thấy file " + what + ": " + p);
    }

    static String fmt(double d) { return String.format(Locale.ROOT, "%.3f", d); }

    // ===================== selftest =====================
    static boolean selfTest(String jar) throws Exception {
        int[] fails = {0};
        String commit = "selftest";
        // run_id, rq, algo, variant, dataset, n, k, seed, repeat, count
        String[][] cases = {
            {"S01", "T", "radix", "base", "D1", "0", "0", "1", "1", "false"},
            {"S02", "T", "radix", "base", "D1", "1", "0", "1", "1", "false"},
            {"S03", "T", "radix", "base", "D1", "2", "0", "1", "1", "true"},
            {"S04", "T", "radix", "base", "D1", "1000", "0", "1", "1", "true"},
            {"S05", "T", "radix", "base", "D2", "100000", "0", "2", "1", "false"},
            {"S06", "T", "radix", "base", "D3", "100000", "0", "3", "1", "false"},
            {"S07", "T", "radix", "base", "D4", "100000", "0", "1", "1", "true"},
            {"S08", "T", "radix", "base", "D5", "100000", "2", "1", "1", "true"},
            {"S09", "T", "radix", "base", "D5", "100000", "1000", "1", "2", "false"},
            {"S10", "T", "radix", "base", "D1", "1000000", "0", "1", "1", "false"},
        };
        Path tmp = Files.createTempFile("selftest-plan", ".csv");
        Path tmpOut = Files.createTempFile("selftest-out", ".csv");
        List<String> planLines = new ArrayList<String>();
        planLines.add(PLAN_HEADER);
        List<String> outLines = new ArrayList<String>();
        outLines.add(HEADER);
        for (String[] f : cases) {
            planLines.add(String.join(",", f));
            String csv = executeOne(f, jar, "2g", "SELF", commit, 60); // đúng lệnh và cỡ heap của đo chính thức
            outLines.add(csv);
            String[] c = csv.split(",", -1);
            boolean isCount = f[9].equals("true");
            long n = Long.parseLong(f[5]);
            boolean ok = c.length == COLS.length && c[IDX.get("status")].equals("ok")
                    && !c[IDX.get("input_sha256")].isEmpty() && !c[IDX.get("output_check")].isEmpty();
            if (ok && isCount) {
                long er = n < 2 ? 0 : 8 * n, ew = n < 2 ? 0 : 4 * n;
                ok = c[IDX.get("array_reads")].equals(String.valueOf(er))
                        && c[IDX.get("array_writes")].equals(String.valueOf(ew))
                        && c[IDX.get("ops_count")].equals(String.valueOf(er + ew))
                        && c[IDX.get("elapsed_ms")].isEmpty();
            }
            if (ok && !isCount && n >= 100000) { // bộ nhớ phụ ~ 4N byte (một mảng int phụ), ở MỌI N
                long auxB = Long.parseLong(c[IDX.get("heap_peak_bytes")]) - Long.parseLong(c[IDX.get("heap_before_bytes")]);
                ok = auxB >= 4 * n && auxB <= 4 * n + (2L << 20);
                if (!ok) System.out.println("  bộ nhớ phụ ngoài khoảng kỳ vọng: " + auxB);
            }
            report(f[0] + " " + f[4] + " n=" + n + " k=" + f[6] + (isCount ? " --count" : ""), ok, fails);
        }
        // check phải ĐẠT với dữ liệu đủ, và KHÔNG ĐẠT khi thiếu một dòng
        Files.write(tmp, planLines, StandardCharsets.UTF_8);
        Files.write(tmpOut, outLines, StandardCharsets.UTF_8);
        report("check: CSV đủ -> ĐẠT", check(tmp, tmpOut, true), fails);
        outLines.remove(outLines.size() - 1);
        Files.write(tmpOut, outLines, StandardCharsets.UTF_8);
        report("check: thiếu một dòng -> phải KHÔNG đạt", !check(tmp, tmpOut, true), fails);
        tmp.toFile().delete();
        tmpOut.toFile().delete();

        // Các trường hợp lỗi phải được ghi đúng trạng thái
        String[] oom = {"E1", "T", "radix", "base", "D1", "20000000", "0", "1", "1", "false"};
        report("E1 hết bộ nhớ (xmx 32m) -> status oom",
                executeOne(oom, jar, "32m", "SELF", commit, 60).split(",", -1)[IDX.get("status")].equals("oom"), fails);
        String[] badVariant = {"E2", "T", "radix", "noA", "D1", "1000", "0", "1", "1", "false"};
        report("E2 biến thể sai -> status crash",
                executeOne(badVariant, jar, "256m", "SELF", commit, 60).split(",", -1)[IDX.get("status")].equals("crash"), fails);
        String[] badAlgo = {"E3", "T", "quick", "base", "D1", "1000", "0", "1", "1", "false"};
        report("E3 thuật toán chưa tích hợp -> status crash",
                executeOne(badAlgo, jar, "256m", "SELF", commit, 60).split(",", -1)[IDX.get("status")].equals("crash"), fails);
        try {
            validateRow("R1,RQ1,radix,base,D6,10,0,1,1,false".split(",", -1), 2);
            report("plan sai dataset phải bị từ chối", false, fails);
        } catch (IllegalArgumentException e) {
            report("plan sai dataset bị từ chối", true, fails);
        }
        System.out.println(fails[0] == 0 ? "SELFTEST: ĐẠT" : "SELFTEST: " + fails[0] + " mục LỖI");
        return fails[0] == 0;
    }

    static void report(String name, boolean ok, int[] fails) {
        System.out.println((ok ? "  ok    " : "  LỖI   ") + name);
        if (!ok) fails[0]++;
    }

    // ===================== Tiện ích =====================
    static String ownJar() throws Exception {
        String p = new File(Runner.class.getProtectionDomain().getCodeSource().getLocation().toURI())
                .getPath();
        if (!p.endsWith(".jar")) {
            throw new IllegalStateException("Hãy chạy từ file jar (Clean and Build) hoặc truyền --jar.");
        }
        return p;
    }

    /** Mã commit ngắn; thêm "-dirty" nếu có thay đổi chưa commit ở các file đã được git theo dõi. */
    static String gitCommit() {
        try {
            String h = gitOut("rev-parse", "--short", "HEAD");
            if (h == null || h.isEmpty()) return "unknown";
            String dirty = gitOut("status", "--porcelain", "--untracked-files=no");
            return (dirty != null && !dirty.isEmpty()) ? h + "-dirty" : h;
        } catch (Exception e) {
            return "unknown";
        }
    }

    private static String gitOut(String... a) throws Exception {
        List<String> cmd = new ArrayList<String>();
        cmd.add("git");
        cmd.addAll(Arrays.asList(a));
        Process p = new ProcessBuilder(cmd).redirectErrorStream(true).start();
        BufferedReader r = new BufferedReader(new InputStreamReader(p.getInputStream(), StandardCharsets.UTF_8));
        String s = r.readLine();
        p.waitFor();
        return p.exitValue() != 0 ? null : (s == null ? "" : s.trim());
    }
}
