import java.util.*;

/**
 * Helper class for making operations on numbers with a certain bit-length.
 */
public class Helper {
    /// Number of bits to use.
    private static final int bits = 4;
    private static final Random random = new Random();

    /**
     * Get the maximum number allowed + 1 according to bits
     */
    public static int getMax() {
        return 1 << bits;
    }

    /**
     * Get a random number within the interval 0..2 ** bits - 1
     */
    public static int random() {
        return random.nextInt(getMax());
    }

    /**
     * Return whether a < k <= b modulo 2 ** bits
     */
    public static boolean between(int k, int a, int b) {
        k = k % getMax();
        if (b <= a) { // We go past 0
            b += getMax();
            if (k <= a)
                k += getMax();
        }
        return (a < k && k <= b);
    }
}
