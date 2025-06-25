/*
 * Copyright (c) 2025 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.script.v2.api;

import gaiasky.data.group.DatasetOptions;
import gaiasky.script.v2.impl.BaseModule;
import gaiasky.script.v2.impl.DataModule;
import gaiasky.util.CatalogInfo;

import java.util.List;

/**
 * API definition for the data module, {@link DataModule}.
 * <p>
 * The data module provides calls and methods to handle datasets and catalogs.
 */
public interface DataAPI {
    /**
     * Return the current datasets location on disk. This is stored in a setting in the configuration file, and points to
     * the actual location where datasets are stored.
     * <p>
     * If you want the default datasets location, use {@link BaseModule#get_default_datasets_dir()}.
     *
     * @return The path to the current location used to store datasets.
     */
    String get_datasets_directory();

    /**
     * Load a VOTable, FITS, CSV or JSON dataset file with the given name.
     * In this version, the loading happens synchronously, so the catalog is available to Gaia Sky immediately after
     * this call returns.
     * The actual loading process is carried out
     * making educated guesses about semantics using UCDs and column names.
     * Please check <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     *
     * @param dsName The name of the dataset, used to identify the subsequent operations on the
     *               dataset.
     * @param path   Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code> file to load.
     *
     * @return False if the dataset could not be loaded, true otherwise.
     */
    boolean load_dataset(String dsName,
                         String path);

    /**
     * Load a VOTable, FITS, CSV or JSON dataset file with the given name.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call acts exactly like
     * {@link #load_dataset(String, String)}.<br/>
     * If <code>sync</code> is false, the loading happens
     * in a new thread and the call returns immediately. In this case, you can use
     * {@link #dataset_exists(String)}
     * to check whether the dataset is already loaded and available.
     * The actual loading process is carried out making educated guesses about semantics using UCDs and column names.
     * Please check <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     *
     * @param dsName The name of the dataset, used to identify the subsequent operations on the
     *               dataset.
     * @param path   Absolute path (or relative to the working path of Gaia Sky) to the file to load.
     * @param sync   Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_dataset(final String dsName,
                         final String path,
                         final boolean sync);

    /**
     * Load a VOTable, FITS, CSV or JSON dataset file with the given name.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call acts exactly like
     * {@link #load_dataset(String, String, boolean)}.<br/>
     * If <code>sync</code> is false, the loading happens
     * in a new thread and the call returns immediately. In this case, you can use
     * {@link #dataset_exists(String)}
     * to check whether the dataset is already loaded and available.
     * The actual loading process is carried out making educated guesses about semantics using UCDs and column names.
     * Please check <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/SAMP.html#stil-data-provider">the
     * official documentation</a> for a complete reference on what can and what can't be loaded.
     * This version includes the catalog info type.
     *
     * @param dsName  The name of the dataset, used to identify the subsequent operations on the
     *                dataset.
     * @param path    Absolute path (or relative to the working path of Gaia Sky) to the file to load.
     * @param type    The {@link CatalogInfo.CatalogInfoSource} object to use as the dataset type.
     * @param options The {@link DatasetOptions} object holding the options for this dataset.
     * @param sync    Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_dataset(final String dsName,
                         final String path,
                         final CatalogInfo.CatalogInfoSource type,
                         final DatasetOptions options,
                         final boolean sync);

    /**
     * Load a star dataset from a VOTable, a CSV or a FITS file.
     * The dataset does not have a label.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName The name of the dataset.
     * @param path   Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *               <code>.csv</code> or <code>.fits</code> file to load.
     * @param sync   Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_dataset(String dsName,
                              String path,
                              boolean sync);

    /**
     * Load a star dataset from a VOTable, a CSV or a FITS file.
     * The dataset does not have a label.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the star magnitudes, as in <code>appmag = appmag -
     *                       magnitudeScale</code>.
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_dataset(String dsName,
                              String path,
                              double magnitudeScale,
                              boolean sync);

    /**
     * Load a star dataset from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the star magnitudes, as in <code>appmag = appmag -
     *                       magnitudeScale</code>.
     * @param labelColor     The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_dataset(String dsName,
                              String path,
                              double magnitudeScale,
                              double[] labelColor,
                              boolean sync);

    /**
     * Load a star dataset from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the star magnitudes, as in <code>appmag = appmag -
     *                       magnitudeScale</code>.
     * @param labelColor     The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fadeIn         Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut        Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_dataset(String dsName,
                              String path,
                              double magnitudeScale,
                              double[] labelColor,
                              double[] fadeIn,
                              double[] fadeOut,
                              boolean sync);

    /**
     * Load a particle dataset (point cloud) from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param profileDecay  The profile decay of the particles as in 1 - distCentre^decay.
     * @param particleColor The base color of the particles, as an array of RGBA (red, green, blue, alpha) values in
     *                      [0,1].
     * @param colorNoise    In [0,1], the noise to apply to the color so that each particle gets a slightly different
     *                      tone. Set to 0 so that all particles get the same color.
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param particleSize  The size of the particles in pixels.
     * @param ct            The name of the component type to use like "Stars", "Galaxies", etc. (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_particle_dataset(String dsName,
                                  String path,
                                  double profileDecay,
                                  double[] particleColor,
                                  double colorNoise,
                                  double[] labelColor,
                                  double particleSize,
                                  String ct,
                                  boolean sync);

    /**
     * Load a particle dataset (point cloud) from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param profileDecay  The profile decay of the particles as in 1 - distCentre^decay.
     * @param particleColor The base color of the particles, as an array of RGBA (red, green, blue, alpha) values in
     *                      [0,1].
     * @param colorNoise    In [0,1], the noise to apply to the color so that each particle gets a slightly different
     *                      tone. Set to 0 so that all particles get the same color.
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param particleSize  The size of the particles in pixels.
     * @param ct            The name of the component type to use like "Stars", "Galaxies", etc. (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_particle_dataset(String dsName,
                                  String path,
                                  double profileDecay,
                                  double[] particleColor,
                                  double colorNoise,
                                  double[] labelColor,
                                  double particleSize,
                                  String ct,
                                  double[] fadeIn,
                                  double[] fadeOut,
                                  boolean sync);

    /**
     * Load a particle dataset (point cloud) from a VOTable, a CSV or a FITS file.
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName             The name of the dataset.
     * @param path               Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                           <code>.csv</code> or <code>.fits</code> file to load.
     * @param profileDecay       The profile decay of the particles as in 1 - distCentre^decay.
     * @param particleColor      The base color of the particles, as an array of RGBA (red, green, blue, alpha) values
     *                           in [0,1].
     * @param colorNoise         In [0,1], the noise to apply to the color so that each particle gets a slightly
     *                           different tone. Set to 0 so that all particles get the same color.
     * @param labelColor         The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param particleSize       The size of the particles in pixels.
     * @param particleSizeLimits The minimum and maximum size of the particles in pixels.
     * @param ct                 The name of the component type to use like "Stars", "Galaxies", etc. (see
     *                           {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn             Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                           camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut            Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                           camera to the Sun) of this dataset. Set to null to disable.
     * @param sync               Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_particle_dataset(String dsName,
                                  String path,
                                  double profileDecay,
                                  double[] particleColor,
                                  double colorNoise,
                                  double[] labelColor,
                                  double particleSize,
                                  double[] particleSizeLimits,
                                  String ct,
                                  double[] fadeIn,
                                  double[] fadeOut,
                                  boolean sync);

    /**
     * Load a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel. Uses the same color for
     * clusters and labels.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles and labels, as an array of RGBA (red, green, blue, alpha)
     *                      values in [0,1].
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_cluster_dataset(String dsName,
                                      String path,
                                      double[] particleColor,
                                      double[] fadeIn,
                                      double[] fadeOut,
                                      boolean sync);

    /**
     * Load a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles, as an array of RGBA (red, green, blue, alpha) values in
     *                      [0,1].
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_cluster_dataset(String dsName,
                                      String path,
                                      double[] particleColor,
                                      double[] labelColor,
                                      double[] fadeIn,
                                      double[] fadeOut,
                                      boolean sync);

    /**
     * Load a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel. Uses the same color
     * for clusters and labels.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles and labels, as an array of RGBA (red, green, blue, alpha)
     *                      values in [0,1].
     * @param ct            The name of the component type to use (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_cluster_dataset(String dsName,
                                      String path,
                                      double[] particleColor,
                                      String ct,
                                      double[] fadeIn,
                                      double[] fadeOut,
                                      boolean sync);

    /**
     * Load a star cluster dataset from a CSV, VOTable or FITS file. The file needs the columns with the
     * following names: name, ra, dec, dist, pmra, pmdec, radius, radvel.
     * The call can be made synchronous or asynchronous.
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName        The name of the dataset.
     * @param path          Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                      <code>.csv</code> or <code>.fits</code> file to load.
     * @param particleColor The base color of the particles and labels, as an array of RGBA (red, green, blue, alpha)
     *                      values in [0,1].
     * @param labelColor    The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param ct            The name of the component type to use (see
     *                      {@link gaiasky.render.ComponentTypes.ComponentType}).
     * @param fadeIn        Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut       Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                      camera to the Sun) of this dataset. Set to null to disable.
     * @param sync          Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_star_cluster_dataset(String dsName,
                                      String path,
                                      double[] particleColor,
                                      double[] labelColor,
                                      String ct,
                                      double[] fadeIn,
                                      double[] fadeOut,
                                      boolean sync);

    /**
     * Load a variable star dataset from a VOTable, CSV or FITS file.
     * The variable star table must have the following columns representing the light curve:
     * <ul>
     *     <li><code>g_transit_time</code>: list of times as Julian days since J2010 for each of the magnitudes</li>
     *     <li><code>g_transit_mag</code>: list of magnitudes corresponding to the times in <code>g_transit_times</code></li>
     *     <li><code>pf</code>: the period in days</li>
     * </ul>
     * The call can be made synchronous or asynchronous.<br/>
     * If <code>sync</code> is true, the call waits until the dataset is loaded and then returns.
     * If <code>sync</code> is false, the loading happens in a new thread and
     * the call returns immediately. It includes some parameters to apply to the new star group.
     *
     * @param dsName         The name of the dataset.
     * @param path           Absolute path (or relative to the working path of Gaia Sky) to the <code>.vot</code>,
     *                       <code>.csv</code> or <code>.fits</code> file to load.
     * @param magnitudeScale Scaling additive factor to apply to the magnitudes in the light curve, as in <code>appmag =
     *                       appmag - magnitudeScale</code>.
     * @param labelColor     The color of the labels, as an array of RGBA (red, green, blue, alpha) values in [0,1].
     * @param fadeIn         Two values which represent the fade in mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param fadeOut        Two values which represent the fade out mapping distances (in parsecs, as distance from
     *                       camera to the Sun) of this dataset. Set to null to disable.
     * @param sync           Whether the load must happen synchronously or asynchronously.
     *
     * @return False if the dataset could not be loaded (sync mode). True if it could not be loaded (sync mode), or
     *         <code>sync</code> is false.
     */
    boolean load_variable_star_dataset(String dsName,
                                       String path,
                                       double magnitudeScale,
                                       double[] labelColor,
                                       double[] fadeIn,
                                       double[] fadeOut,
                                       boolean sync);

    /**
     * Load a Gaia Sky JSON dataset file asynchronously. The call returns immediately, and the
     * dataset becomes available when it finished loading.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean load_json_dataset(String dsName,
                              String path);

    /**
     * Load a Gaia Sky JSON dataset file in a synchronous or asynchronous manner.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     * @param sync   If true, the call does not return until the dataset is loaded and available in Gaia Sky.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean load_json_dataset(String dsName,
                              String path,
                              boolean sync);

    /**
     * Load a Gaia Sky JSON dataset file in a synchronous or asynchronous manner.
     * The Gaia Sky JSON data format is described
     * <a href="https://gaia.ari.uni-heidelberg.de/gaiasky/docs/master/Data-format.html#json-data-format">here</a>.
     *
     * @param dsName The name of the dataset.
     * @param path   The absolute path, or the path in the data directory, of the dataset file.
     * @param select If true, focus the first object in the dataset after loading.
     * @param sync   If true, the call does not return until the dataset is loaded and available in Gaia Sky.
     *
     * @return False if the dataset could not be loaded. True otherwise.
     */
    boolean load_json_dataset(String dsName,
                              String path,
                              boolean select,
                              boolean sync);

    /**
     * Remove the dataset identified by the given name, if it exists.
     *
     * @param dsName The name of the dataset to remove.
     *
     * @return False if the dataset could not be found.
     */
    boolean remove_dataset(String dsName);

    /**
     * Hide the dataset identified by the given name, if it exists and is not hidden.
     *
     * @param dsName The name of the dataset to hide.
     *
     * @return False if the dataset could not be found.
     */
    boolean hide_dataset(String dsName);

    /**
     * Return the names of all datasets currently loaded.
     *
     * @return A list with all the names of the loaded datasets.
     */
    List<String> list_datasets();

    /**
     * Check whether the dataset identified by the given name is loaded
     *
     * @param dsName The name of the dataset to query.
     *
     * @return True if the dataset is loaded, false otherwise.
     */
    boolean dataset_exists(String dsName);

    /**
     * Show (un-hide) the dataset identified by the given name, if it exists and is hidden
     *
     * @param dsName The name of the dataset to show.
     *
     * @return False if the dataset could not be found.
     */
    boolean show_dataset(String dsName);

    /**
     * Set the given 4x4 matrix (in column-major order) as the transformation matrix to apply
     * to all the data points in the dataset identified by the given name.
     *
     * @param dsName The name of the dataset.
     * @param matrix The 16 values of the 4x4 transformation matrix in column-major order.
     *
     * @return True if the dataset was found and the transformation matrix could be applied. False otherwise.
     */
    boolean set_dataset_transform_matrix(String dsName,
                                         double[] matrix);

    /**
     * Clear the transformation matrix (if any) in the dataset identified by the given name.
     *
     * @param dsName The name of the dataset.
     *
     * @return True if the dataset was found and the transformations cleared.
     */
    boolean clear_dataset_transform_matrix(String dsName);

    /**
     * Enable or disable the dataset highlight, using a plain color given by the color index:
     * <ul>
     *     <li>0 - blue</li>
     *     <li>1 - red</li>
     *     <li>2 - yellow</li>
     *     <li>3 - green</li>
     *     <li>4 - pink</li>
     *     <li>5 - orange</li>
     *     <li>6 - purple</li>
     *     <li>7 - brown</li>
     *     <li>8 - magenta</li>
     * </ul>
     *
     * @param dsName     The dataset name.
     * @param colorIndex Color index in [0,8].
     * @param highlight  Whether to highlight or not.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlight_dataset(String dsName,
                              int colorIndex,
                              boolean highlight);

    /**
     * Enable or disable the dataset highlight using a plain color chosen by the system.
     *
     * @param dsName    The dataset name.
     * @param highlight State.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlight_dataset(String dsName,
                              boolean highlight);

    /**
     * Enable or disable the dataset highlight, using a given plain color.
     *
     * @param dsName    The dataset name.
     * @param r         Red component.
     * @param highlight State.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlight_dataset(String dsName,
                              float r,
                              float g,
                              float b,
                              float a,
                              boolean highlight);

    /**
     * Enable or disable the dataset highlight, using the given color map on the given attribute with the given
     * maximum and minimum mapping values.
     *
     * @param dsName        The dataset name.
     * @param attributeName The attribute name. You can use basic attributes (please mind the case!):
     *                      <ul><li>RA</li><li>DEC</li><li>Distance</li><li>GalLatitude</li><li>GalLongitude</li><li>EclLatitude</li><li>EclLongitude</li></ul>
     *                       Or star-only attributes (if your dataset contains stars, mind the case!):
     *                       <ul><li>Mualpha</li><li>Mudelta</li><li>Radvel</li><li>Absmag</li><li>Appmag</li></ul>
     *                       Or even extra attributes (if you loaded the dataset yourself), matching by column name.
     * @param colorMap      The color map to use, in
     *                      ["reds"|"greens"|"blues"|"rainbow18"|"rainbow"|"seismic"|"carnation"|"hotmeal"|"cool"].
     * @param minMap        The minimum mapping value.
     * @param maxMap        The maximum mapping value.
     * @param highlight     State.
     *
     * @return False if the dataset could not be found.
     */
    boolean highlight_dataset(String dsName,
                              String attributeName,
                              String colorMap,
                              double minMap,
                              double maxMap,
                              boolean highlight);

    /**
     * Set the size increase factor of this dataset when highlighted.
     *
     * @param dsName     The dataset name.
     * @param sizeFactor The size factor to apply to the particles when highlighted, must be in
     *                   [{@link gaiasky.util.Constants#MIN_DATASET_SIZE_FACTOR},
     *                   {@link gaiasky.util.Constants#MAX_DATASET_SIZE_FACTOR}].
     *
     * @return False if the dataset could not be found.
     */
    boolean set_dataset_highlight_size_factor(String dsName,
                                              float sizeFactor);

    /**
     * Set the 'all visible' property of datasets when highlighted. If set to true, all stars in the dataset have an
     * increased minimum
     * opacity when highlighted, so that they are all visible. Otherwise, stars retain their minimum opacity and base
     * brightness.
     *
     * @param dsName     The dataset name.
     * @param allVisible Whether all stars in the dataset should be visible when highlighted or not.
     *
     * @return False if the dataset could not be found.
     */
    boolean set_dataset_highlight_all_visible(String dsName,
                                              boolean allVisible);

    /**
     * Set the dataset point size multiplier.
     *
     * @param dsName     The dataset name.
     * @param multiplier The multiplier, as a positive floating point number.
     */
    void set_dataset_point_size_factor(String dsName,
                                       double multiplier);
}
