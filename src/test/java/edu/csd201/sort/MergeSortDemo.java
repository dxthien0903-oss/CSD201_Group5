package edu.csd201.sort;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Demo trace cho Merge Sort (dùng để chụp ảnh slide).
 * java -cp target/classes edu.csd201.sort.MergeSortDemo -> demo 8 số mặc định +
 * chứng minh
 * java -cp target/classes edu.csd201.sort.MergeSortDemo 5 2 9 2 7 1 5 -> mảng
 * tự nhập
 * java -cp target/classes edu.csd201.sort.MergeSortDemo --merge -> sorted vs
 * reversed (N = 6)
 * java -cp target/classes edu.csd201.sort.MergeSortDemo --scale -> chi phí so
 * với N log2 N
 * java -cp target/classes edu.csd201.sort.MergeSortDemo --data -> D2, D3, D4
 */
public class MergeSortDemo {

    // ---- bản trace: cùng thuật toán với MergeSort, chỉ thêm in và đếm ----
    private static int merges, comps, depthMax;

    private static void trace(int[] a, int[] t, int l, int r, int depth) {
        depthMax = Math.max(depthMax, depth);
        if (l >= r)
            return;
        int mid = l + (r - l) / 2;
        String ind = "  ".repeat(depth - 1);
        System.out.println(ind + "split " + Arrays.toString(Arrays.copyOfRange(a, l, r + 1)) + " -> "
                + Arrays.toString(Arrays.copyOfRange(a, l, mid + 1)) + " | "
                + Arrays.toString(Arrays.copyOfRange(a, mid + 1, r + 1)));
        trace(a, t, l, mid, depth + 1);
        trace(a, t, mid + 1, r, depth + 1);
        String L = Arrays.toString(Arrays.copyOfRange(a, l, mid + 1));
        String R = Arrays.toString(Arrays.copyOfRange(a, mid + 1, r + 1));
        System.arraycopy(a, l, t, l, r - l + 1);
        int i = l, j = mid + 1, k = l, c = 0;
        while (i <= mid && j <= r) {
            c++;
            a[k++] = (t[i] <= t[j]) ? t[i++] : t[j++];
        }
        while (i <= mid)
            a[k++] = t[i++];
        while (j <= r)
            a[k++] = t[j++];
        merges++;
        comps += c;
        System.out.println(ind + "merge " + L + " + " + R + " = "
                + Arrays.toString(Arrays.copyOfRange(a, l, r + 1))
                + "   (#" + merges + ", " + c + " comparisons)");
    }

    static void run(int[] input, boolean proof) {
        System.out.println("input = " + Arrays.toString(input));
        int[] a = input.clone();
        merges = 0;
        comps = 0;
        depthMax = 0;
        if (a.length > 1)
            trace(a, new int[a.length], 0, a.length - 1, 1);
        System.out.println();
        int[] expected = input.clone();
        Arrays.sort(expected);
        System.out.println("sorted        = " + Arrays.toString(a));
        if (proof) {
            MergeSort.MergeMetrics m = new MergeSort.MergeMetrics();
            MergeSort.sortCounted(input.clone(), m);
            System.out.println("group's code  : " + m);
            System.out.println("trace counts  : comparisons=" + comps + " maxDepth=" + depthMax);
            int[] s1 = input.clone();
            MergeSort.sort(s1);
            int[] s2 = input.clone();
            MergeSort.sortCounted(s2, new MergeSort.MergeMetrics());
            System.out.println("matches Arrays.sort          : " + Arrays.equals(a, expected));
            System.out.println("MergeSort.sort matches       : " + Arrays.equals(s1, expected));
            System.out.println("MergeSort.sortCounted matches: " + Arrays.equals(s2, expected));
            System.out.println("trace == real code (comparisons, depth): "
                    + (comps == m.comparisons() && depthMax == m.maxDepth()));
        } else {
            System.out.println("matches Arrays.sort          : " + Arrays.equals(a, expected));
        }
    }

    static void mergeDemo() {
        int[][] ins = { { 1, 2, 3, 4, 5, 6 }, { 6, 5, 4, 3, 2, 1 } };
        String[] names = { "sorted", "reversed" };
        System.out.printf("%-30s %-12s %-12s %-10s %s%n", "Input (N = 6)", "Comparisons", "Assignments", "Aux memory",
                "Depth");
        for (int x = 0; x < 2; x++) {
            MergeSort.MergeMetrics m = new MergeSort.MergeMetrics();
            MergeSort.sortCounted(ins[x].clone(), m);
            System.out.printf("%-30s %-12d %-12d %-10s %d%n", Arrays.toString(ins[x]) + " " + names[x],
                    m.comparisons(), m.assignments(), m.extraMemoryBytes() + " B", m.maxDepth());
        }
        System.out.println();
        for (int x = 0; x < 2; x++) {
            int[] a = ins[x].clone();
            merges = 0;
            comps = 0;
            depthMax = 0;
            java.io.PrintStream old = System.out;
            java.io.ByteArrayOutputStream bos = new java.io.ByteArrayOutputStream();
            System.setOut(new java.io.PrintStream(bos));
            trace(a, new int[a.length], 0, a.length - 1, 1);
            System.setOut(old);
            String[] lines = bos.toString().split("\\R");
            System.out.println(lines[lines.length - 1].trim() + "   <- " + names[x] + " input");
        }
    }

    static void scale() {
        System.out.printf("%10s %14s %14s %8s %10s %12s %10s%n",
                "N", "comparisons", "N*log2(N)", "ratio", "maxDepth", "extraMem(B)", "4N(B)");
        boolean ok = true;
        for (int n : new int[] { 8, 16, 1000, 100000, 1000000 }) {
            int[] a = DataGenerator.d1(n, 1);
            int[] e = a.clone();
            Arrays.sort(e);
            MergeSort.MergeMetrics m = new MergeSort.MergeMetrics();
            MergeSort.sortCounted(a, m);
            ok &= Arrays.equals(a, e);
            double nl = n * (Math.log(n) / Math.log(2));
            System.out.printf("%,10d %,14d %,14.0f %8.3f %10d %,12d %,10d%n",
                    n, m.comparisons(), nl, m.comparisons() / nl, m.maxDepth(), m.extraMemoryBytes(), 4L * n);
        }
        System.out.println(
                "maxDepth = ceil(log2 N) + 1 ; extraMem = exactly one int[N] = 4N bytes ; all sorted correctly: " + ok);
    }

    static void data() {
        int n = 5000;
        long seed = 1;
        int[] d1 = DataGenerator.generateSpec("D1", n, 0, seed);
        int[] d2 = DataGenerator.generateSpec("D2", n, 0, seed);
        int[] d3 = DataGenerator.generateSpec("D3", n, 0, seed);
        int[] d4 = DataGenerator.generateSpec("D4", n, 0, seed);
        System.out.println("=== Demo 8 - the datasets TV2 owns, N=5000 seed=1 ===");
        System.out.println("D2 first 5 : " + Arrays.toString(Arrays.copyOf(d2, 5)) + "  (sorted ascending)");
        System.out.println("D4 first 5 : " + Arrays.toString(Arrays.copyOf(d4, 5)) + "  (D2 reversed)");
        int diff = 0;
        for (int i = 0; i < n; i++)
            if (d2[i] != d3[i])
                diff++;
        System.out.println("D3 vs D2   : " + diff + " positions differ  (N/1000 = " + (n / 1000) + " swaps -> at most "
                + 2 * (n / 1000) + " positions)");
        boolean rev = true;
        for (int i = 0; i < n; i++)
            if (d4[i] != d2[n - 1 - i])
                rev = false;
        System.out.println("D4 == reverse(D2)            : " + rev);
        int[] s2 = d2.clone(), s3 = d3.clone(), s1 = d1.clone();
        Arrays.sort(s3);
        Arrays.sort(s1);
        System.out.println("D3 has the same values as D2 : " + Arrays.equals(s3, s2));
    }

    public static void main(String[] args) {
        if (args.length == 0) {
            run(new int[] { 38, 27, 43, 3, 9, 82, 10, 15 }, true);
        } else if (args[0].equals("--merge")) {
            mergeDemo();
        } else if (args[0].equals("--scale")) {
            scale();
        } else if (args[0].equals("--data")) {
            data();
        } else {
            List<Integer> l = new ArrayList<>();
            for (String s : args)
                l.add(Integer.parseInt(s));
            run(l.stream().mapToInt(Integer::intValue).toArray(), false);
        }
    }
}