/*
 * Copyright (c) 2026 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.data.api.IParticleGroupDataProvider;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.ParticleKepler;
import gaiasky.util.Constants;
import gaiasky.util.Logger;
import gaiasky.util.Settings;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.Matrix4D;
import gaiasky.util.ucd.UCD;
import uk.ac.starlink.table.StarTableFactory;
import uk.ac.starlink.table.TableBuilder;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.logging.Level;

/**
 * Loader for Keplerian orbital elements from a VOTable, CSV, or ASCII source.
 * Data is parsed into a {@link List} of {@link IParticleRecord} objects.
 * * <p><b>VOTable Column Mapping:</b></p>
 * This provider relies on fixed positional indexing. The source table must
 * provide the following columns in this exact sequence:
 * * <table border="1">
 * <tr><th>Index</th><th>Internal ID</th><th>Type</th><th>Units / Description</th></tr>
 * <tr><td>0</td><td>sourceId</td><td>long</td><td>Unique Gaia source identifier</td></tr>
 * <tr><td>1</td><td>number_mp</td><td>long</td><td>MPC Minor Planet number (0 for satellites)</td></tr>
 * <tr><td>2</td><td>name</td><td>String</td><td>Object designation or name</td></tr>
 * <tr><td>3</td><td>typeShort</td><td>short</td><td>Type code: 3 for "A" (Asteroid), else "C" (Comet)</td></tr>
 * <tr><td>4</td><td>nObs</td><td>int</td><td>Total number of observations</td></tr>
 * <tr><td>5</td><td>epoch</td><td>double</td><td>Reference epoch in Julian Days (JD)</td></tr>
 * <tr><td>6</td><td>semiMajorAxis</td><td>double</td><td>Semi-major axis in AU</td></tr>
 * <tr><td>7</td><td>eccentricity</td><td>double</td><td>Orbital eccentricity</td></tr>
 * <tr><td>8</td><td>inclination</td><td>double</td><td>Inclination to the ecliptic (degrees)</td></tr>
 * <tr><td>9</td><td>ascendingNode</td><td>double</td><td>Longitude of ascending node Ω (degrees)</td></tr>
 * <tr><td>10</td><td>argOfPericenter</td><td>double</td><td>Argument of perihelion ω (degrees)</td></tr>
 * <tr><td>11</td><td>meanAnomaly</td><td>double</td><td>Mean anomaly M at epoch (degrees)</td></tr>
 * <tr><td>12</td><td>period</td><td>double</td><td>Orbital period in Days</td></tr>
 * </table>
 * * <p><b>Data Processing:</b></p>
 * <ul>
 * <li><b>Units:</b> The semi-major axis is converted from AU to Kilometers internally using
 * {@code AU_TO_U * U_TO_KM}.</li>
 * <li><b>Metadata:</b> Columns 3 and 4 must have valid UCD (Unified Content Descriptor)
 * strings as they are used to initialize the metadata {@link ObjectMap}.</li>
 * <li><b>Object Type:</b> A hardcoded mapping is used where a short value of 3
 * designates an Asteroid ("A"), and all other values default to Comet ("C").</li>
 * </ul>
 *
 * @deprecated Use {@link STILDataProvider} instead.
 * @see ParticleKepler
 */
@Deprecated
public class ElementsGroupVOTableProvider implements IParticleGroupDataProvider {
    private static final Logger.Log logger = Logger.getLogger(ElementsGroupVOTableProvider.class);

    /** Table factory. **/
    private StarTableFactory factory;

    /** Do all particles have the same epoch? **/
    private boolean uniformEpoch = true;


    public ElementsGroupVOTableProvider() {
        try {
            java.util.logging.Logger.getLogger("uk.ac.starlink")
                    .setLevel(Level.WARNING);
            java.util.logging.Logger.getLogger("org.astrogrid")
                    .setLevel(Level.WARNING);
            factory = new StarTableFactory();
        } catch (Exception e) {
            factory = null;
            logger.error(e);
        }
    }

    @Override
    public List<IParticleRecord> loadData(String file) {
        return loadData(file, 1.0);
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file, double factor) {
        return null;
    }

    @Override
    public List<IParticleRecord> loadData(String file, double factor) {
        logger.info(I18n.msg("notif.datafile", file));
        List<IParticleRecord> list = null;
        try {
            list = loadData(new FileDataSource(Settings.settings.data.dataFile(file)), factor);
        } catch (Exception e1) {
            try {
                logger.info("File " + file + " not found in data folder, trying relative path");
                list = loadData(new FileDataSource(file), factor);
            } catch (Exception e2) {
                logger.error(e1);
                logger.error(e2);
            }
        }
        if (list != null)
            logger.info(I18n.msg("notif.nodeloader", list.size(), file));
        return list;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is, double factor) {
        return null;
    }

    private List<IParticleRecord> loadData(DataSource ds, double factor) {

        if (factory != null) {
            // Add extra builders
            List<TableBuilder> builders = factory.getDefaultBuilders();
            builders.add(new CsvTableBuilder());
            builders.add(new AsciiTableBuilder());
            try (var table = factory.makeStarTable(ds)) {
                long nRows = table.getRowCount();
                List<IParticleRecord> list = new ArrayList<>((int) nRows);

                var typeUCD = new UCD(table.getColumnInfo(3).getUCD(), "type", null, 4);
                var obsUCD = new UCD(table.getColumnInfo(4).getUCD(), "n_observations", null, 5);

                double prevEpoch = -1;
                try (var rs = table.getRowSequence()) {
                    while (rs.next()) {
                        var row = rs.getRow();
                        var sourceId = (long) row[0];
                        var name = (String) row[2];
                        var epoch = (double) row[5];
                        var semiMajorAxis = (double) row[6] * Constants.AU_TO_U * Constants.U_TO_KM * factor;
                        var e = (double) row[7];
                        var i = (double) row[8];
                        var ascendingNode = (double) row[9];
                        var argOfPericenter = (double) row[10];
                        var meanAnomaly = (double) row[11];
                        var period = (double) row[12];

                        var typeShort = (short) row[3];
                        var type = (int) typeShort;
                        var nObs = (int) row[4];
                        var map = new ObjectMap<UCD, Object>();
                        map.put(typeUCD, type);
                        map.put(obsUCD, nObs);

                        // Is the epoch uniform?
                        if (prevEpoch > 0 && epoch != prevEpoch) {
                            uniformEpoch = false;
                        }
                        prevEpoch = epoch;

                        var particle = new ParticleKepler(sourceId,
                                                          name,
                                                          epoch,
                                                          meanAnomaly,
                                                          semiMajorAxis,
                                                          e,
                                                          argOfPericenter,
                                                          ascendingNode,
                                                          i,
                                                          period,
                                                          map);
                        list.add(particle);
                    }
                }

                return list;
            } catch (Exception e) {
                logger.error(e);
            }
        }

        return List.of();
    }

    @Override
    public void setFileNumberCap(int cap) {

    }

    @Override
    public void setStarNumberCap(int cap) {

    }

    @Override
    public void setProviderParams(Map<String, Object> params) {

    }

    @Override
    public boolean isUniformEpoch() {
        return uniformEpoch;
    }

    @Override
    public void setTransformMatrix(Matrix4D matrix) {
    }
}
