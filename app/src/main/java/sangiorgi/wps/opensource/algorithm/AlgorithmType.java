package sangiorgi.wps.opensource.algorithm;

/** Enum defining all supported WPS PIN generation algorithms */
public enum AlgorithmType {
  PIN(101, "24-bit"),
  AIROCON_REALTEK(102, "Airocon"),
  ARCADYAN(103, "EasyBox"),
  ARRIS(104, "Arris"),
  ASUS(105, "Asus"),
  BELKIN(106, "Belkin"),
  DLINK(107, "DLink"),
  DLINK_PLUS_ONE(108, "DLink+1"),
  FORTY_BIT(109, "40-bit"),
  FORTY_EIGHT_BIT(110, "48-bit"),
  ORANGE(111, "Orange"),
  FORTY_FOUR_BIT(112, "44-bit"),
  THIRTY_SIX_BIT(113, "36-bit"),
  THIRTY_TWO_BIT(114, "32-bit"),
  TWENTY_EIGHT_BIT(115, "28-bit"),
  TRENDNET(116, "TrendNet"),
  FTE(117, "FTE");

  private final int code;
  private final String displayName;

  AlgorithmType(int code, String displayName) {
    this.code = code;
    this.displayName = displayName;
  }

  public int getCode() {
    return code;
  }

  public String getDisplayName() {
    return displayName;
  }

  /**
   * Returns the algorithm type matching the given numeric code.
   *
   * @param code the algorithm code
   * @return the matching type, or null if no type has that code
   */
  public static AlgorithmType fromCode(int code) {
    for (AlgorithmType type : values()) {
      if (type.code == code) {
        return type;
      }
    }
    return null;
  }
}
