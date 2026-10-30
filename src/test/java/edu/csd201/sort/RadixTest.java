package edu.csd201.sort;

import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Random;

/**
 * Kiểm thử Radix Sort (TV4). Chạy từ thư mục gốc repo:
 *   java -cp target/classes edu.csd201.sort.RadixTest [duong_dan_log]
 * - Ghi test log ra file (mặc định results/radix_test_log.txt) và thoát với mã 1 nếu có lỗi.
 * - Nhóm 1: 5 ca cố định của đề (+ mảng rỗng, giá trị biên).
 * - Nhóm 2: 1.000 mảng ngẫu nhiên kích thước 0-100, đủ D1-D5 (D5 với K = 1000, 10, 2).
 * - Nhóm 3: 100.000 phần tử toàn bằng nhau / tăng / giảm.
 * - Nhóm 4: bộ đếm: reads = 8N, writes = 4N (N >= 2), và bằng 0 khi N < 2.
 * - Nhóm 5: tính chất của generator D1-D5 (kể cả D3 hoán đổi thật, tham số sai bị từ chối).
 * - Nhóm 6: mẫu đặc biệt theo byte (chỉ byte cao khác nhau, bội của 2^24, cận 2^31-1, xen kẽ 0/MAX...).
 * - Nhóm 7: khóa ÂM và toàn dải int, D1-D5 của DataGenerator.generate (có số âm), D5 đúng đề (K giá trị phân biệt).
 * Đối chiếu bằng Arrays.sort (chỉ trong test, ngoài vùng đo).
 */
public final class RadixTest {

    private RadixTest() {
    }

    static PrintWriter log;
    static int total = 0, fails = 0;

    static void check(boolean ok, String name) {
        total++;
        if (!ok) { fails++; say("FAIL  " + name); }
    }

    static void say(String s) { System.out.println(s); log.println(s); }

    /** Chạy cả bản không đếm và bản có đếm trên bản sao của a, so với Arrays.sort. */
    static boolean sortsCorrectly(int[] a) {
        int[] e = a.clone();
        Arrays.sort(e);
        int[] b = a.clone();
        RadixSort.sort(b);
        int[] c = a.clone();
        RadixSort.sort(c, new RadixSort.RadixMetrics());
        return Arrays.equals(b, e) && Arrays.equals(c, e);
    }

    static boolean sameMultiset(int[] x, int[] y) {
        int[] a = x.clone(), b = y.clone();
        Arrays.sort(a);
        Arrays.sort(b);
        return Arrays.equals(a, b);
    }

    public static void main(String[] args) throws IOException {
        Path logPath = Path.of(args.length > 0 ? args[0] : "results/radix_test_log.txt");
        if (logPath.toAbsolutePath().getParent() != null) Files.createDirectories(logPath.toAbsolutePath().getParent());
        log = new PrintWriter(new OutputStreamWriter(new FileOutputStream(logPath.toFile()), "UTF-8"), true);
        say("=== RadixTest ===");

        // Nhóm 1: ca cố định
        int[][] fixed = {{7}, {2, 1}, {3, 3, 3}, {0, 2147483647, 0}, {5, 1, 4, 1, 5, 9, 2, 6, 5, 3, 5},
                {}, {2147483647, 2147483647}, {256, 255, 65536, 65535, 16777216, 16777215, 0}};
        for (int i = 0; i < fixed.length; i++) check(sortsCorrectly(fixed[i]), "fixed#" + i + " " + Arrays.toString(fixed[i]));
        say("Nhóm 1 (ca cố định): xong");

        // Nhóm 2: 1.000 mảng ngẫu nhiên 0-100, đủ D1-D5
        Random r = new Random(1);
        String[] ds = {"D1", "D2", "D3", "D4", "D5"};
        int[] ks = {1000, 10, 2};
        for (int t = 0; t < 1000; t++) {
            int n = r.nextInt(101);
            String d = ds[t % 5];
            int k = ks[(t / 5) % 3];
            int[] a = DataGenerator.generateSpec(d, n, k, t);
            check(sortsCorrectly(a), "random#" + t + " " + d + " n=" + n + " k=" + k + " seed=" + t);
        }
        say("Nhóm 2 (1.000 mảng ngẫu nhiên, seed = chỉ số lượt): xong");

        // Nhóm 3: 100.000 phần tử đặc biệt
        int big = 100000;
        int[] eq = new int[big], inc = new int[big], dec = new int[big];
        for (int i = 0; i < big; i++) { eq[i] = 5; inc[i] = i; dec[i] = big - i; }
        check(sortsCorrectly(eq), "100000 toàn bằng nhau");
        check(sortsCorrectly(inc), "100000 tăng dần");
        check(sortsCorrectly(dec), "100000 giảm dần");
        say("Nhóm 3 (100.000 phần tử): xong");

        // Nhóm 4: bộ đếm
        int[] sizes = {0, 1, 2, 3, 1000, 100000};
        for (int n : sizes) {
            RadixSort.RadixMetrics m = new RadixSort.RadixMetrics();
            RadixSort.sort(DataGenerator.generateSpec("D1", n, 0, 7), m);
            long er = n < 2 ? 0 : 8L * n, ew = n < 2 ? 0 : 4L * n, em = n < 2 ? 0 : 4L * n;
            check(m.reads() == er && m.writes() == ew && m.operations() == er + ew && m.extraMemoryBytes() == em,
                    "đếm n=" + n + ": reads=" + m.reads() + " (cần " + er + "), writes="
                    + m.writes() + " (cần " + ew + "), mảng phụ=" + m.extraMemoryBytes() + " B (cần " + em + ")");
        }
        say("Nhóm 4 (bộ đếm 8N / 4N): xong");

        // Nhóm 5: generator
        int n = 100000;
        int[] d1 = DataGenerator.generateSpec("D1", n, 0, 3), d2 = DataGenerator.generateSpec("D2", n, 0, 3),
                d3 = DataGenerator.generateSpec("D3", n, 0, 3), d4 = DataGenerator.generateSpec("D4", n, 0, 3);
        check(Arrays.equals(d1, DataGenerator.generateSpec("D1", n, 0, 3)), "D1 tái lập được với cùng seed");
        check(!Arrays.equals(d1, DataGenerator.generateSpec("D1", n, 0, 4)), "D1 khác nhau khi đổi seed");
        boolean inRange = true;
        for (int v : d1) if (v < 0) inRange = false;
        check(inRange, "D1 trong [0, 2^31-1]");
        int[] sorted = d1.clone();
        Arrays.sort(sorted);
        check(Arrays.equals(d2, sorted), "D2 = D1 đã sắp tăng");
        int diff = 0;
        for (int i = 0; i < n; i++) if (d3[i] != d2[i]) diff++;
        check(sameMultiset(d3, d2) && diff <= 2 * (n / 1000), "D3 cùng tập giá trị với D2, lệch <= 2*N/1000 vị trí (lệch " + diff + ")");
        boolean rev = true;
        for (int i = 0; i < n; i++) if (d4[i] != d2[n - 1 - i]) rev = false;
        check(rev, "D4 = D2 đảo ngược");
        for (int k : ks) {
            int[] d5 = DataGenerator.generateSpec("D5", n, k, 3);
            HashSet<Integer> s = new HashSet<Integer>();
            boolean ok = true;
            for (int v : d5) { s.add(v); if (v < 0) ok = false; }
            check(ok && s.size() == k, "D5 K=" + k + " có đúng " + k + " giá trị phân biệt (có " + s.size() + ")");
            check(Arrays.equals(d5, DataGenerator.generateSpec("D5", n, k, 3)), "D5 K=" + k + " tái lập được");
        }
        // D3: n < 1000 -> 0 cặp -> trùng D2; n >= 1000 -> có hoán đổi thật
        check(Arrays.equals(DataGenerator.generateSpec("D3", 999, 0, 1), DataGenerator.generateSpec("D2", 999, 0, 1)), "D3 với N < 1000 trùng D2 (0 cặp)");
        check(!Arrays.equals(DataGenerator.generateSpec("D3", 1000, 0, 1), DataGenerator.generateSpec("D2", 1000, 0, 1)), "D3 với N = 1000 có hoán đổi thật");
        for (int nn : new int[]{0, 1, 2}) {
            check(DataGenerator.generateSpec("D3", nn, 0, 1).length == nn && DataGenerator.generateSpec("D5", nn, 2, 1).length == nn,
                    "generator với N = " + nn);
        }
        // tham số sai bị từ chối
        boolean threw1 = false, threw2 = false, threw3 = false;
        try { DataGenerator.generateSpec("D9", 10, 0, 1); } catch (IllegalArgumentException e) { threw1 = true; }
        try { DataGenerator.generateSpec("D5", 10, 0, 1); } catch (IllegalArgumentException e) { threw2 = true; }
        try { DataGenerator.generateSpec("D1", -1, 0, 1); } catch (IllegalArgumentException e) { threw3 = true; }
        check(threw1 && threw2 && threw3, "dataset sai / K = 0 / N âm bị từ chối");
        say("Nhóm 5 (generator): xong");

        // Mẫu đặc biệt: kiểm tra từng lượt (byte) của Radix
        int m = 50000;
        int[] lowOnly = new int[m], highOnly = new int[m], mult24 = new int[m], altern = new int[m],
                nearMax = new int[m], oneOdd = new int[m], pow2 = new int[m];
        Random rr = new Random(9);
        for (int i = 0; i < m; i++) {
            lowOnly[i] = rr.nextInt(256);                       // chỉ byte 0 khác nhau
            highOnly[i] = rr.nextInt(128) << 24;                // chỉ byte 3 khác nhau
            mult24[i] = rr.nextInt(128) << 24 | rr.nextInt(2) << 16;
            altern[i] = (i & 1) == 0 ? 0 : Integer.MAX_VALUE;   // xen kẽ cận dưới/cận trên
            nearMax[i] = Integer.MAX_VALUE - rr.nextInt(1000);  // sát 2^31 - 1
            oneOdd[i] = 1000;                                   // tất cả bằng nhau trừ một phần tử
            pow2[i] = 1 << (i % 31);                            // các lũy thừa của 2
        }
        oneOdd[m / 2] = 3;
        check(sortsCorrectly(lowOnly), "mẫu: chỉ byte thấp khác nhau");
        check(sortsCorrectly(highOnly), "mẫu: chỉ byte cao khác nhau");
        check(sortsCorrectly(mult24), "mẫu: bội của 2^16/2^24");
        check(sortsCorrectly(altern), "mẫu: xen kẽ 0 và 2^31-1");
        check(sortsCorrectly(nearMax), "mẫu: sát 2^31-1");
        check(sortsCorrectly(oneOdd), "mẫu: bằng nhau trừ một phần tử");
        check(sortsCorrectly(pow2), "mẫu: lũy thừa của 2");
        // sort hai lần liên tiếp (đã sắp) không đổi; đúng cả khi mảng dài lẻ
        int[] odd = DataGenerator.generateSpec("D1", 99999, 0, 5);
        RadixSort.sort(odd);
        int[] odd2 = odd.clone();
        RadixSort.sort(odd2);
        check(Arrays.equals(odd, odd2), "sort lần hai trên mảng đã sắp không đổi");
        say("Nhóm 6 (mẫu đặc biệt theo byte): xong");

        // Nhóm 7: khóa âm / toàn dải int (đề chỉ dùng khóa không âm, nhưng test và phân phối tạm của nhóm có số âm)
        int[][] neg = {{-1, 0, 1}, {Integer.MIN_VALUE, Integer.MAX_VALUE}, {Integer.MAX_VALUE, Integer.MIN_VALUE, 0, -1, 1},
                {-5, -5, -5}, {Integer.MIN_VALUE, Integer.MIN_VALUE + 1, -1, 0}, {-256, -255, 255, 256, -65536, 65536}};
        for (int i = 0; i < neg.length; i++) check(sortsCorrectly(neg[i]), "âm#" + i + " " + Arrays.toString(neg[i]));
        Random rn = new Random(11);
        int[] fullRange = new int[100000];
        for (int i = 0; i < fullRange.length; i++) fullRange[i] = rn.nextInt();
        check(sortsCorrectly(fullRange), "100000 giá trị toàn dải int (có âm)");
        check(RadixSort.verify(fullRange), "RadixSort.verify trên toàn dải int");
        int[] legacySizes = {0, 1, 2, 3, 10, 1000, 100000};
        for (String dist : DataGenerator.DISTRIBUTIONS) {
            for (int sz : legacySizes) {
                check(sortsCorrectly(DataGenerator.generate(dist, sz, new Random(sz + 3L))),
                        "DataGenerator.generate " + dist + " n=" + sz);
            }
        }
        for (int k : ks) { // D5 của DataGenerator (dùng trong Benchmark) cũng phải đúng đề: đúng K giá trị phân biệt
            int[] d5 = DataGenerator.generateManyDuplicatesK(100000, k, new Random(5));
            HashSet<Integer> s = new HashSet<Integer>();
            boolean ok = true;
            for (int v : d5) { s.add(v); if (v < 0) ok = false; }
            check(ok && s.size() == k && sortsCorrectly(d5), "DataGenerator.generateManyDuplicatesK K=" + k + " (có " + s.size() + " giá trị phân biệt)");
        }
        say("Nhóm 7 (khóa âm, DataGenerator chung): xong");

        say("TỔNG: " + total + " kiểm tra, lỗi = " + fails);
        say(fails == 0 ? "KẾT QUẢ: ĐẠT" : "KẾT QUẢ: KHÔNG ĐẠT");
        log.close();
        System.exit(fails == 0 ? 0 : 1);
    }
}
