import java.util.Arrays;
import java.util.HashSet;
import java.util.SplittableRandom;

/**
 * Generator D1-D5 theo đề. D5 là phần của TV4; D1-D4 ở đây là BẢN TẠM
 * để chạy thử end-to-end, hãy thay bằng generator của TV1/TV2 khi nhóm chốt.
 */
public final class Datasets {

    public static final String GENERATOR_VERSION = "gen-1.1";

    public static int[] make(String code, int n, int k, long seed) {
        if (n < 0) throw new IllegalArgumentException("n phải >= 0, nhận được " + n);
        switch (code) {
            case "D1": return d1(n, seed);
            case "D2": return d2(n, seed);
            case "D3": return d3(n, seed);
            case "D4": return d4(n, seed);
            case "D5": return d5(n, k, seed);
            default: throw new IllegalArgumentException("Dataset không hợp lệ: " + code);
        }
    }

    /** D1: N giá trị đều trên [0, 2^31 - 1]. */
    public static int[] d1(int n, long seed) {
        SplittableRandom r = new SplittableRandom(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = r.nextInt() & 0x7FFFFFFF;
        return a;
    }

    /** D2: D1 sắp tăng dần (cùng tập giá trị với D1). */
    public static int[] d2(int n, long seed) {
        int[] a = d1(n, seed);
        Arrays.sort(a); // chỉ trong generator, không nằm trong vùng đo
        return a;
    }

    /** D3: D2 rồi hoán đổi N/1000 cặp vị trí ngẫu nhiên. */
    public static int[] d3(int n, long seed) {
        int[] a = d2(n, seed);
        SplittableRandom r = new SplittableRandom(seed ^ 0x9E3779B97F4A7C15L);
        int swaps = n / 1000;           // n < 1000 -> 0 cặp -> D3 trùng D2 (đúng theo đề)
        for (int s = 0; s < swaps; s++) {
            // hai vị trí PHÂN BIỆT (n >= 1000 nên n >= 2), để mỗi lần là một hoán đổi thật
            int i = r.nextInt(n), j = r.nextInt(n - 1);
            if (j >= i) j++;
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
        return a;
    }

    /** D4: D2 đảo ngược. */
    public static int[] d4(int n, long seed) {
        int[] a = d2(n, seed);
        for (int i = 0, j = n - 1; i < j; i++, j--) {
            int t = a[i]; a[i] = a[j]; a[j] = t;
        }
        return a;
    }

    /**
     * D5: K giá trị phân biệt (sinh từ seed), mỗi phần tử chọn đều trong K giá trị.
     * HashSet chỉ dùng để loại trùng trong generator; các giá trị giữ theo thứ tự sinh (mảng vals),
     * nên kết quả tái lập được với cùng seed. K > N vẫn hợp lệ (khi đó không đủ K giá trị xuất hiện).
     */
    public static int[] d5(int n, int k, long seed) {
        if (n < 0) throw new IllegalArgumentException("n phải >= 0, nhận được " + n);
        if (k < 1) throw new IllegalArgumentException("D5 cần k >= 1");
        SplittableRandom r = new SplittableRandom(seed);
        int[] vals = new int[k];
        HashSet<Integer> used = new HashSet<Integer>();
        for (int i = 0; i < k; ) {
            int v = r.nextInt() & 0x7FFFFFFF;
            if (used.add(v)) vals[i++] = v;
        }
        int[] a = new int[n];
        for (int i = 0; i < n; i++) a[i] = vals[r.nextInt(k)];
        return a;
    }
}
