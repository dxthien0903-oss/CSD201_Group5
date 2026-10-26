package sorting;

import java.util.Arrays;
import java.util.SplittableRandom;

/**
 * Dataset generators owned by TV1: D1 (uniform random) and D2 (ascending).
 *
 * Team convention:
 *   - D1: uniform random keys in [0, 2^31 - 1], reproducible from SplittableRandom(seed)
 *   - D2: clone of D1, sorted ascending, so both datasets share the same multiset of keys
 */
public final class DataGenerator {

    /** Highest key allowed by the spec: 2^31 - 1. */
    public static final int MAX_KEY = Integer.MAX_VALUE;

    private DataGenerator() {
    }

    /**
     * D1: uniform random keys in [0, 2^31 - 1].
     * Same seed produces the same array on any machine (SplittableRandom contract).
     */
    public static int[] d1(int n, long seed) {
        if (n < 0) {
            throw new IllegalArgumentException("n must be >= 0: " + n);
        }
        SplittableRandom rnd = new SplittableRandom(seed);
        int[] a = new int[n];
        for (int i = 0; i < n; i++) {
            a[i] = rnd.nextInt() & 0x7FFFFFFF;
        }
        return a;
    }

    /** D2: ascending copy of the given D1 array; keeps exactly the same key multiset. */
    public static int[] d2(int[] d1) {
        if (d1 == null) {
            throw new NullPointerException("d1");
        }
        int[] a = d1.clone();
        Arrays.sort(a);
        return a;
    }

    /** Convenience: generate D1(n, seed) and sort a clone of it into D2. */
    public static int[] d2(int n, long seed) {
        return d2(d1(n, seed));
    }
}
