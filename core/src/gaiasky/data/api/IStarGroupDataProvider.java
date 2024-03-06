/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.api;

import com.badlogic.gdx.utils.LongMap;
import gaiasky.scene.api.IParticleRecord;

import java.io.InputStream;
import java.util.List;
import java.util.Set;

public interface IStarGroupDataProvider extends IParticleGroupDataProvider {
    LongMap<float[]> getColors();

    /**
     * Loads the data applying a factor using a memory mapped file for improved speed.
     *
     * @param file   The file to load.
     * @param factor Factor to apply to the positions.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadDataMapped(String file, double factor);

    /**
     * Loads the data applying a factor.
     *
     * @param file   The file to load.
     * @param factor Factor to apply to the positions.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadData(String file, double factor);

    /**
     * Loads the data applying a factor.
     *
     * @param is     Input stream to load the data from.
     * @param factor Factor to apply to the positions.
     *
     * @return The array of particle records.
     */
    List<IParticleRecord> loadData(InputStream is, double factor);

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. This applies to
     * faint stars (gmag &ge; 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     *
     * @param parallaxErrorFactor The percentage value of parallax errors with respect to parallax.
     */
    void setParallaxErrorFactorFaint(double parallaxErrorFactor);

    /**
     * <p>
     * The loader will only load stars for which the parallax error is
     * at most the percentage given here, in [0..1]. This applies to
     * bright stars (gmag &lt; 13.1)
     * More specifically, the following must be met:
     * </p>
     * <code>pllx_err &lt; pllx * pllxErrFactor</code>
     *
     * @param parallaxErrorFactor The percentage value of parallax errors with respect to parallax.
     */
    void setParallaxErrorFactorBright(double parallaxErrorFactor);

    /**
     * Whether to use an adaptive threshold, relaxing it for bright (appmag &ge; 13) stars to let more
     * bright stars in.
     */
    void setAdaptiveParallax(boolean adaptive);

    /**
     * Sets the zero point of the parallax as an addition to the parallax
     * values, in [mas].
     *
     * @param parallaxZeroPoint The parallax zero point.
     */
    void setParallaxZeroPoint(double parallaxZeroPoint);

    /**
     * Sets the flag to apply magnitude and color corrections for extinction and
     * reddening.
     *
     * @param magCorrections Whether to apply the corrections.
     */
    void setMagCorrections(boolean magCorrections);

    /**
     * Set location of additional columns file or directory.
     *
     * @param additionalFile File or directory with additional columns per sourceId.
     */
    void setAdditionalFiles(String additionalFile);

    /**
     * Sets the RUWE criteria. RUWE file must have been set.
     *
     * @param RUWE The criteria (usually 1.4).
     */
    void setRUWECap(double RUWE);

    /**
     * Sets a distance cap. Stars beyond this distance will not be loaded.
     *
     * @param distCap The distance cap, in parsecs.
     */
    void setDistanceCap(double distCap);

    /**
     * Gets the star counts per magnitude.
     **/
    long[] getCountsPerMag();

    /**
     * Adds a set with all the ids which will be loaded regardless of any other
     * conditions (i.e. parallax error thresholds).
     *
     * @param ids The ids that must be loaded.
     */
    void setMustLoadIds(Set<Long> ids);

    /**
     * List of column names, separated by commas, indicating the position of each
     * field to load.
     *
     * @param columns The column name list.
     */
    void setColumns(String columns);

    /**
     * Set the preferred output format version, if applicable.
     *
     * @param version The version number.
     */
    void setOutputFormatVersion(int version);
}