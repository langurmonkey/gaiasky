package gaia.cu9.ari.gaiaorbit.data.group;

import com.badlogic.gdx.utils.LongMap;

/**
 * Data provider for a star group, which contains an index map with the names
 * and indices of the stars.
 *
 * @author tsagrista
 */
public interface IStarGroupDataProvider extends IParticleGroupDataProvider {
    LongMap<float[]> getColors();

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. This applies to
     * faint stars (gmag >= 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     *
     * @param parallaxErrorFactor The percentage value of parallax errors with respect to parallax
     */
    void setParallaxErrorFactorFaint(double parallaxErrorFactor);

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. This applies to
     * bright stars (gmag < 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     *
     * @param parallaxErrorFactor The percentage value of parallax errors with respect to parallax
     */
    void setParallaxErrorFactorBright(double parallaxErrorFactor);

    /**
     * Whether to use an adaptive threshold, relaxing it for bright (appmag >= 13) stars to let more
     * bright stars in.
     */
    void setAdaptiveParallax(boolean adaptive);

    /**
     * Sets the zero point of the parallax as an addition to the parallax
     * values, in [mas]
     *
     * @param parallaxZeroPoint The parallax zero point
     */
    void setParallaxZeroPoint(double parallaxZeroPoint);

    /**
     * Sets the flag to apply magnitude and color corrections for extinction and
     * reddening
     *
     * @param magCorrections Whether to apply the corrections
     */
    void setMagCorrections(boolean magCorrections);

    /**
     * Sets the location of the geometric distances file or directory
     *
     * @param geoDistFile File or directory with geometric distances per sourceId
     */
    void setGeoDistancesFile(String geoDistFile);

    /**
     * Sets the location of the gzipped RUWE file
     *
     * @param RUWEFile Gzipped file with RUWE values for each source id
     */
    void setRUWEFile(String RUWEFile);

    /**
     * Sets the RUWE criteria. RUWE file must have been set
     *
     * @param RUWE The criteria (usually 1.4)
     */
    void setRUWECap(double RUWE);

    /**
     * Sets a distance cap. Stars beyond this distance will not be loaded
     *
     * @param distCap The distance cap, in parsecs
     */
    void setDistanceCap(double distCap);

    /**
     * Gets the star counts per magnitude
     **/
    long[] getCountsPerMag();
}