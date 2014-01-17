/**
 * 
 */
package nippon.kawauso.chiraura.util;

import java.util.ArrayList;
import java.util.List;

/**
 * @author chirauraNoSakusha
 */
final class PrimeCalculator {

    // インスタンス化防止。
    private PrimeCalculator() {}

    public static void main(final String[] args) {
        final int numOfPrimes;
        if (args.length > 0) {
            numOfPrimes = Integer.parseInt(args[0]);
        } else {
            numOfPrimes = 1_000;
        }
        final List<Long> primes = new ArrayList<>();
        for (long value = 2L; value >= 0L; value++) {
            final long max = (long) Math.floor(Math.sqrt(value));
            boolean ok = true;
            for (int i = 0; i < primes.size(); i++) {
                final long prime = primes.get(i);
                if (max < prime) {
                    break;
                } else if (value % prime == 0) {
                    ok = false;
                    break;
                }
            }
            if (ok) {
                primes.add(value);
                System.out.println(primes.size() + " " + value);
                if (primes.size() >= numOfPrimes) {
                    break;
                }
            }
        }
    }

}
