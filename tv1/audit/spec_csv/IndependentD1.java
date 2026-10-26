import java.util.Arrays;

/**
 * Independent D1 generator used only by the spec_csv audit.
 *
 * The implementation below is an independent transcription of the
 * java.util.SplittableRandom algorithm as observed in the runtime javap
 * bytecode.  It does not call sorting.DataGenerator or sorting.QuickSort.
 */
public final class IndependentD1 {
    private static final long GOLDEN_GAMMA = -7046029254386353131L;
    private static final long MIX32_M1 = 7109453100751455733L;
    private static final long MIX32_M2 = -3808689974395783757L;

    private IndependentD1() {
    }

    private static long mix32(long z) {
        z = (z ^ (z >>> 33)) * MIX32_M1;
        z = (z ^ (z >>> 28)) * MIX32_M2;
        return z >>> 32;
    }

    private static long outputCheck(int[] a) {
        long h = 0L;
        for (int i = 0; i < a.length; i++) {
            h = 31L * h + a[i];
        }
        return h;
    }

    private static int[] d1(int n, long seed) {
        int[] a = new int[n];
        long state = seed;
        for (int i = 0; i < n; i++) {
            state += GOLDEN_GAMMA;
            a[i] = (int) mix32(state) & 0x7FFFFFFF;
        }
        return a;
    }

    private static void selfCheck(long seed) {
        java.util.SplittableRandom jdk = new java.util.SplittableRandom(seed);
        int[] own = d1(4096, seed);
        for (int i = 0; i < own.length; i++) {
            int expected = jdk.nextInt() & 0x7FFFFFFF;
            if (own[i] != expected) {
                throw new IllegalStateException("own SplittableRandom mismatch at index " + i
                        + ": own=" + own[i] + " jdk=" + expected);
            }
        }
    }

    public static void main(String[] args) {
        long seed = 20261016L;
        int[] sizes = {100_000, 1_000_000, 5_000_000, 10_000_000, 20_000_000, 50_000_000};
        selfCheck(seed);
        System.out.println("SELF_CHECK=PASS version=" + System.getProperty("java.version")
                + " seed=" + seed);
        for (int n : sizes) {
            int[] a = d1(n, seed);
            long raw = outputCheck(a);
            Arrays.sort(a);
            long sorted = outputCheck(a);
            System.out.println("D1 n=" + n + " raw_output_check=" + raw
                    + " sorted_output_check=" + sorted);
        }
    }
}
