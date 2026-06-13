package sangiorgi.wps.opensource.algorithm;

import java.util.Locale;

/** Utility class for WPS PIN checksum calculations */
public final class ChecksumCalculator {

  private ChecksumCalculator() {}

  /**
   * Calculates WPS PIN checksum using the standard algorithm
   *
   * @param pin 7-digit PIN
   * @return checksum digit (0-9)
   */
  public static int calculate(int pin) {
    int accum = 0;

    accum += 3 * ((pin / 10000000) % 10);
    accum += ((pin / 1000000) % 10);
    accum += 3 * ((pin / 100000) % 10);
    accum += ((pin / 10000) % 10);
    accum += 3 * ((pin / 1000) % 10);
    accum += ((pin / 100) % 10);
    accum += 3 * ((pin / 10) % 10);

    int digit = accum % 10;
    return (10 - digit) % 10;
  }

  /**
   * Calculates WPS PIN checksum for a pre-multiplied PIN
   *
   * @param pin PIN already multiplied by 10
   * @return checksum digit (0-9)
   */
  public static int calculatePreMultiplied(int pin) {
    pin *= 10;
    int accum = 0;

    accum += 3 * ((pin / 10000000) % 10);
    accum += ((pin / 1000000) % 10);
    accum += 3 * ((pin / 100000) % 10);
    accum += ((pin / 10000) % 10);
    accum += 3 * ((pin / 1000) % 10);
    accum += ((pin / 100) % 10);
    accum += 3 * ((pin / 10) % 10);

    int digit = accum % 10;
    return (10 - digit) % 10;
  }

  /** Calculates checksum for long PIN values */
  public static long calculateLong(long pin) {
    pin *= 10;
    long accum = 0;

    accum += 3 * ((pin / 10000000) % 10);
    accum += ((pin / 1000000) % 10);
    accum += 3 * ((pin / 100000) % 10);
    accum += ((pin / 10000) % 10);
    accum += 3 * ((pin / 1000) % 10);
    accum += ((pin / 100) % 10);
    accum += 3 * ((pin / 10) % 10);

    long digit = accum % 10;
    return (10 - digit) % 10;
  }

  /** Formats a PIN with its checksum */
  public static String formatWithChecksum(int pin) {
    return String.format(Locale.ROOT, "%07d%d", pin, calculatePreMultiplied(pin));
  }

  /** Formats a PIN with its checksum and algorithm name */
  public static String formatWithChecksum(int pin, String algorithmName) {
    return String.format(
        Locale.ROOT, "%07d%d --%s", pin, calculatePreMultiplied(pin), algorithmName);
  }
}
