package sangiorgi.wps.opensource.algorithm;

import java.util.Locale;

/** Utility class for MAC address operations */
public final class MacAddressUtil {

  private MacAddressUtil() {}

  private static final String DEFAULT_PIN = "12345670";

  /** Validates a MAC address format */
  public static boolean isValidMacAddress(String mac) {
    if (mac == null || mac.isEmpty()) {
      return false;
    }
    // Check if it matches MAC address pattern (with or without colons)
    String normalizedMac = mac.replace(":", "");
    return normalizedMac.matches("[0-9A-Fa-f]{12}");
  }

  /** Normalizes a MAC address by removing colons and converting to uppercase */
  public static String normalize(String mac) {
    if (mac == null) {
      return "";
    }
    return mac.replace(":", "").toUpperCase(Locale.ROOT);
  }

  /** Gets the last 3 bytes (6 hex characters) of a MAC address */
  public static String getLastThreeBytes(String mac) {
    String normalized = normalize(mac);
    if (normalized.length() < 12) {
      return normalized;
    }
    return normalized.substring(6, 12);
  }

  /** Gets the last 2 bytes for WAN calculation */
  public static String getLastTwoBytesWan(String mac) {
    String normalized = normalize(mac);
    if (normalized.length() < 12) {
      return "0000";
    }

    String wimac = normalized.substring(8, 12);

    try {
      switch (wimac) {
        case "0000":
          return "fffe";
        case "0001":
          return "ffff";
        default:
          int lastDigitWan = Integer.parseInt(wimac.substring(3, 4), 16) - 2;
          return wimac.substring(0, 3) + String.format(Locale.ROOT, "%01x", lastDigitWan);
      }
    } catch (NumberFormatException e) {
      return wimac;
    }
  }

  /** Splits MAC address into byte array */
  public static String[] splitIntoBytes(String mac) {
    String normalized = normalize(mac);
    String[] bytes = new String[6];

    for (int i = 0; i < 6; i++) {
      int startIndex = i * 2;
      if (startIndex + 2 <= normalized.length()) {
        bytes[i] = normalized.substring(startIndex, startIndex + 2);
      } else {
        bytes[i] = "00";
      }
    }

    return bytes;
  }

  /** Parse hex string safely */
  public static int parseHexSafe(String hex, int defaultValue) {
    try {
      return Integer.parseInt(hex, 16);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }

  /** Parse long hex string safely */
  public static long parseLongHexSafe(String hex, long defaultValue) {
    try {
      return Long.parseLong(hex, 16);
    } catch (NumberFormatException e) {
      return defaultValue;
    }
  }
}
