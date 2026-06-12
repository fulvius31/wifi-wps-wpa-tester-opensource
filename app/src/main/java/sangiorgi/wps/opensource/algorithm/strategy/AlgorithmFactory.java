package sangiorgi.wps.opensource.algorithm.strategy;

import android.content.Context;
import java.util.HashMap;
import java.util.Map;
import sangiorgi.wps.opensource.algorithm.AlgorithmType;
import sangiorgi.wps.opensource.algorithm.strategy.impl.AiroconRealtekAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.ArcadyanAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.ArrisAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.AsusAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.BelkinAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.BitBasedAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.DlinkAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.FteAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.OrangeAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.PinAlgorithm;
import sangiorgi.wps.opensource.algorithm.strategy.impl.TrendNetAlgorithm;

/**
 * Factory class for creating WPS algorithm instances Uses lazy initialization and caching for
 * performance
 */
public class AlgorithmFactory {

  private final Map<AlgorithmType, WpsAlgorithm> algorithmCache = new HashMap<>();
  private final String dataDir;

  public AlgorithmFactory(Context context) {
    this.dataDir = context.getFilesDir().getAbsolutePath() + "/Sessions/";
  }

  public AlgorithmFactory(String dataDir) {
    this.dataDir = dataDir + "Sessions/";
  }

  /**
   * Gets or creates an algorithm instance for the specified type
   *
   * @param type The algorithm type
   * @return The algorithm instance, or null if type is not supported
   */
  public synchronized WpsAlgorithm getAlgorithm(AlgorithmType type) {
    if (type == null) {
      return null;
    }

    // Check cache first
    if (algorithmCache.containsKey(type)) {
      return algorithmCache.get(type);
    }

    // Create new instance based on type
    WpsAlgorithm algorithm = createAlgorithm(type);

    // Cache the instance
    if (algorithm != null) {
      algorithmCache.put(type, algorithm);
    }

    return algorithm;
  }

  /** Creates a new algorithm instance based on type */
  private WpsAlgorithm createAlgorithm(AlgorithmType type) {
    switch (type) {
      case PIN:
        return new PinAlgorithm();

      case AIROCON_REALTEK:
        return new AiroconRealtekAlgorithm();

      case ARCADYAN:
        return new ArcadyanAlgorithm();

      case ARRIS:
        return new ArrisAlgorithm();

      case ASUS:
        return new AsusAlgorithm();

      case BELKIN:
        return new BelkinAlgorithm(dataDir);

      case DLINK:
        return new DlinkAlgorithm(false);

      case DLINK_PLUS_ONE:
        return new DlinkAlgorithm(true);

      case FORTY_BIT:
        return new BitBasedAlgorithm(BitBasedAlgorithm.BitType.FORTY);

      case FORTY_EIGHT_BIT:
        return new BitBasedAlgorithm(BitBasedAlgorithm.BitType.FORTY_EIGHT);

      case FORTY_FOUR_BIT:
        return new BitBasedAlgorithm(BitBasedAlgorithm.BitType.FORTY_FOUR);

      case THIRTY_SIX_BIT:
        return new BitBasedAlgorithm(BitBasedAlgorithm.BitType.THIRTY_SIX);

      case THIRTY_TWO_BIT:
        return new BitBasedAlgorithm(BitBasedAlgorithm.BitType.THIRTY_TWO);

      case TWENTY_EIGHT_BIT:
        return new BitBasedAlgorithm(BitBasedAlgorithm.BitType.TWENTY_EIGHT);

      case ORANGE:
        return new OrangeAlgorithm(dataDir);

      case TRENDNET:
        return new TrendNetAlgorithm();

      case FTE:
        return new FteAlgorithm();

      default:
        return null;
    }
  }

  /** Clears the algorithm cache */
  public synchronized void clearCache() {
    algorithmCache.clear();
  }
}
