package edu.csd201.sort;

import java.lang.management.ManagementFactory;
import java.lang.management.MemoryPoolMXBean;
import java.lang.management.MemoryType;
import java.lang.management.MemoryUsage;
import java.nio.ByteBuffer;
import java.security.MessageDigest;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Tiến trình con: mỗi lượt đo là một JVM riêng chạy lớp này (do {@link MeasureRunner} khởi động).
 * Đây là bộ đo theo quy định chung (mỗi lượt một JVM, làm nóng, SHA-256, oracle Arrays.sort, heap).
 * Thêm thuật toán mới: thêm một nhánh trong {@link #pick}.
 *  Hợp đồng với Runner: in ra stdout ba loại dòng
 *    ENV,<jdk>,<os>,<cores>,<ram_mb>
 *    SHA,<sha256 của input>
 *    RESULT,status,elapsed_ms,heap_before,heap_peak,output_check,ops_count,array_reads,array_writes )
 *
 * Không có luồng phụ nào trong Worker: timeout do Runner xử lý (waitFor + destroyForcibly).
 */
public final class MeasureWorker {

    interface Sorter { void sort(int[] a); }

    /** Bộ đếm của lượt --count (chỉ Radix có). */
    static final RadixSort.RadixMetrics RADIX_METRICS = new RadixSort.RadixMetrics();

    static Sorter pick(String algo, String variant, boolean count) {
        switch (algo) {
            case "radix":
                requireVariant(algo, variant, "base");
                return count ? a -> RadixSort.sort(a, RADIX_METRICS) : a -> RadixSort.sort(a);
            case "lib-Arrays.sort":
                requireVariant(algo, variant, "base");
                if (count) throw new IllegalArgumentException("--count chỉ hỗ trợ radix");
                return a -> Arrays.sort(a);
            // TODO (mỗi thành viên thêm thuật toán của mình, gọi thẳng API trong package này):
            //   "quick", "merge", "heap", "pdq" (variant base/noA/noB). Lưu ý bản đo thời gian phải KHÔNG đếm:
            //   MergeSort.sort(int[]) hiện luôn đếm so sánh nên cần một bản sort không đếm trước khi đo chính thức.
            default:
                throw new IllegalArgumentException("Chưa tích hợp thuật toán: " + algo);
        }
    }

    /** Không để một biến thể gõ sai bị bỏ qua âm thầm: chỉ nhận các tên đã được hỗ trợ. */
    static void requireVariant(String algo, String variant, String... allowed) {
        for (String v : allowed) if (v.equals(variant)) return;
        throw new IllegalArgumentException("Biến thể '" + variant + "' không hợp lệ cho " + algo
                + " (chấp nhận: " + Arrays.toString(allowed) + ")");
    }

    /** --key value  hoặc cờ --flag (không có giá trị). */
    static Map<String, String> parse(String[] args) {
        Map<String, String> m = new HashMap<String, String>();
        for (int i = 0; i < args.length; i++) {
            if (!args[i].startsWith("--")) continue;
            String key = args[i].substring(2);
            if (i + 1 < args.length && !args[i + 1].startsWith("--")) m.put(key, args[++i]);
            else m.put(key, "true");
        }
        return m;
    }

    public static void main(String[] args) throws Exception {
        Map<String, String> o = parse(args);
        String algo = o.get("algo");
        String variant = o.containsKey("variant") ? o.get("variant") : "base";
        String dataset = o.get("dataset");
        int n = Integer.parseInt(o.get("n"));
        if (n < 0) throw new IllegalArgumentException("n phải >= 0");
        int k = o.containsKey("k") ? Integer.parseInt(o.get("k")) : 0;
        long seed = Long.parseLong(o.get("seed"));
        boolean count = o.containsKey("count");
        Sorter sorter = pick(algo, variant, count);

        System.out.println(envLine());

        try {
            // 1) Làm nóng: 3 lần trên dữ liệu nhỏ (10^5 phần tử <= 1 MiB), không ghi CSV
            for (int w = 0; w < 3; w++) sorter.sort(DataGenerator.generateSpec("D1", 100000, 0, 12345L + w));
            RADIX_METRICS.reset();

            // 2) Sinh dữ liệu, băm input, tạo bản oracle (đều NGOÀI vùng đo)
            int[] a = DataGenerator.generateSpec(dataset, n, k, seed);
            System.out.println("SHA," + sha256(a));
            System.out.flush();
            int[] expect = a.clone();      // oracle nằm trong heap_before nên không tính vào bộ nhớ phụ
            Arrays.sort(expect);

            // 3) Chuẩn bị đo bộ nhớ rồi đo thời gian
            System.gc();
            resetHeapPeaks();
            long before = heapBytes(false);
            long t0 = System.nanoTime();
            sorter.sort(a);
            long t1 = System.nanoTime();
            long peak = heapBytes(true);

            // 4) Kiểm tra đúng + tổng kiểm tra (ngoài vùng đo)
            boolean ok = Arrays.equals(a, expect);
            long h = 0;
            for (int i = 0; i < a.length; i++) h = 31 * h + a[i];

            String ms = String.format(Locale.ROOT, "%.3f", (t1 - t0) / 1e6);
            String ops = count ? String.valueOf(RADIX_METRICS.operations()) : "";
            String rd = count ? String.valueOf(RADIX_METRICS.reads()) : "";
            String wr = count ? String.valueOf(RADIX_METRICS.writes()) : "";
            System.out.println("RESULT," + (ok ? "ok" : "wrong") + "," + ms + "," + before + ","
                    + peak + "," + h + "," + ops + "," + rd + "," + wr);
        } catch (OutOfMemoryError e) {
            System.out.println("RESULT,oom,,,,,,,");
        } catch (StackOverflowError e) {
            System.out.println("RESULT,crash,,,,,,,");
        }
        System.out.flush();
    }

    // ---- thông tin môi trường (để ghi vào CSV theo "Quy ước chung") ----
    static String clean(String s) { return s == null ? "" : s.replace(',', ' ').trim(); }

    static String envLine() {
        String os = System.getProperty("os.name") + " " + System.getProperty("os.version")
                + " " + System.getProperty("os.arch");
        long ramMb = -1;
        try { // dùng reflection để không phụ thuộc gói com.sun.management khi biên dịch
            Object b = ManagementFactory.getOperatingSystemMXBean();
            Class<?> c = Class.forName("com.sun.management.OperatingSystemMXBean");
            ramMb = ((Long) c.getMethod("getTotalPhysicalMemorySize").invoke(b)) / (1024L * 1024L);
        } catch (Throwable ignored) { }
        return "ENV," + clean(System.getProperty("java.version")) + "," + clean(os) + ","
                + Runtime.getRuntime().availableProcessors() + "," + ramMb;
    }

    static String sha256(int[] a) throws Exception {
        MessageDigest md = MessageDigest.getInstance("SHA-256");
        ByteBuffer bb = ByteBuffer.allocate(1 << 16); // mặc định big-endian
        for (int i = 0; i < a.length; i++) {
            if (bb.remaining() < 4) { md.update(bb.array(), 0, bb.position()); bb.clear(); }
            bb.putInt(a[i]);
        }
        md.update(bb.array(), 0, bb.position());
        StringBuilder sb = new StringBuilder();
        for (byte b : md.digest()) sb.append(String.format("%02x", b));
        return sb.toString();
    }

    // Đoạn mã mẫu của quy định chung (mục 6.4)
    static long heapBytes(boolean peak) {
        long sum = 0;
        for (MemoryPoolMXBean p : ManagementFactory.getMemoryPoolMXBeans()) {
            if (p.getType() != MemoryType.HEAP) continue;
            MemoryUsage u = peak ? p.getPeakUsage() : p.getUsage();
            if (u != null) sum += u.getUsed();
        }
        return sum;
    }

    static void resetHeapPeaks() {
        for (MemoryPoolMXBean p : ManagementFactory.getMemoryPoolMXBeans()) {
            if (p.getType() == MemoryType.HEAP) p.resetPeakUsage();
        }
    }
}
