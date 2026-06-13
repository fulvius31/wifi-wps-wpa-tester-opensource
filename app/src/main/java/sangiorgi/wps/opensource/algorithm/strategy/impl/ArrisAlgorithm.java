package sangiorgi.wps.opensource.algorithm.strategy.impl;

import java.util.Locale;
import sangiorgi.wps.opensource.algorithm.ChecksumCalculator;
import sangiorgi.wps.opensource.algorithm.strategy.AlgorithmException;
import sangiorgi.wps.opensource.algorithm.strategy.BaseWpsAlgorithm;

/** Arris router WPS algorithm implementation */
public class ArrisAlgorithm extends BaseWpsAlgorithm {

  // Memoized Fibonacci cache
  private static final long[] FIB_CACHE = new long[50];

  static {
    FIB_CACHE[0] = 1;
    FIB_CACHE[1] = 1;
    FIB_CACHE[2] = 1;
    for (int i = 3; i < FIB_CACHE.length; i++) {
      FIB_CACHE[i] = FIB_CACHE[i - 1] + FIB_CACHE[i - 2];
    }
  }

  public ArrisAlgorithm() {
    super("Arris");
  }

  @Override
  public String generatePin(String bssid, String ssid) throws AlgorithmException {
    String[] macParts = bssid.split(":");
    if (macParts.length != 6) {
      throw new AlgorithmException("Invalid MAC address format");
    }

    long[] macBytes = new long[6];
    for (int i = 0; i < 6; i++) {
      macBytes[i] = parseHexSafe(macParts[i], 0);
    }

    long[] fibNum = new long[6];
    long[] tmp = macBytes.clone();

    for (int i = 0; i < 6; i++) {
      int counter = 0;

      if (tmp[i] > 30) {
        while (tmp[i] > 31) {
          tmp[i] -= 16;
          counter++;
        }
      }

      if (counter == 0) {
        if (tmp[i] < 3) {
          long sum = 0;
          for (long b : tmp) {
            sum += b;
          }
          tmp[i] = (sum - tmp[i]) & 0xff;
          tmp[i] = (tmp[i] % 28) + 3;
        }
        fibNum[i] = fibonacci(tmp[i]);
      } else {
        fibNum[i] = fibonacci(tmp[i]) + fibonacci(counter);
      }
    }

    long fibSum = 0;
    for (int i = 0; i < 6; i++) {
      fibSum += (fibNum[i] * fibonacci(i + 16)) + macBytes[i];
    }

    fibSum = fibSum % PIN_MODULO;
    long checksum = ChecksumCalculator.calculateLong(fibSum);
    fibSum = (fibSum * 10) + checksum;

    return String.format(Locale.ROOT, "%08d", fibSum);
  }

  private static long fibonacci(long n) {
    if (n < 0) {
      return 1;
    }
    if (n < FIB_CACHE.length) {
      return FIB_CACHE[(int) n];
    }
    // For larger values, calculate recursively (shouldn't happen in practice)
    return fibonacci(n - 1) + fibonacci(n - 2);
  }
}
