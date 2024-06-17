/*
 * Copyright (c) 2023 Gaia Sky - All rights reserved.
 *  This file is part of Gaia Sky, which is released under the Mozilla Public License 2.0.
 *  You may use, distribute and modify this code under the terms of MPL2.
 *  See the file LICENSE.md in the project root for full license details.
 */

package gaiasky.data.group;

import com.badlogic.gdx.graphics.Color;
import com.badlogic.gdx.utils.Array;
import com.badlogic.gdx.utils.LongMap;
import com.badlogic.gdx.utils.ObjectMap;
import gaiasky.data.group.DatasetOptions.DatasetLoadType;
import gaiasky.scene.api.IParticleRecord;
import gaiasky.scene.record.ParticleRecord;
import gaiasky.scene.record.ParticleRecord.ParticleRecordType;
import gaiasky.scene.record.VariableRecord;
import gaiasky.scene.system.render.draw.VariableSetInstancedRenderer;
import gaiasky.util.*;
import gaiasky.util.color.BVToTeff_ballesteros;
import gaiasky.util.color.ColorUtils;
import gaiasky.util.coord.AstroUtils;
import gaiasky.util.coord.Coordinates;
import gaiasky.util.i18n.I18n;
import gaiasky.util.math.LinearInterpolator;
import gaiasky.util.math.MathUtilsDouble;
import gaiasky.util.math.Vector2d;
import gaiasky.util.math.Vector3d;
import gaiasky.util.parse.Parser;
import gaiasky.util.ucd.UCD;
import gaiasky.util.ucd.UCDParser;
import gaiasky.util.units.Position;
import gaiasky.util.units.Position.PositionType;
import gaiasky.util.units.Quantity.Angle;
import gaiasky.util.units.Quantity.Angle.AngleUnit;
import gaiasky.util.units.Quantity.Length;
import gaiasky.util.units.Quantity.Length.LengthUnit;
import net.jafama.FastMath;
import uk.ac.starlink.table.*;
import uk.ac.starlink.table.formats.AsciiTableBuilder;
import uk.ac.starlink.table.formats.CsvTableBuilder;
import uk.ac.starlink.util.DataSource;
import uk.ac.starlink.util.FileDataSource;

import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Path;
import java.util.*;
import java.util.logging.Level;

/**
 * Loads data in VOTable, CSV and FITS formats using the STIL library.
 *
 * @see <a href="https://www.star.bristol.ac.uk/~mbt/stil/">STIL Library homepage</a>
 */
public class STILDataProvider extends AbstractStarGroupDataProvider {
    static {
        logger = Logger.getLogger(STILDataProvider.class);
    }

    /** These names are not allowed **/
    private static final String[] forbiddenNameValues = { "-", "...", "nop", "nan", "?", "_", "x", "n/a" };
    /** Store already visited colName:attribute pairs. **/
    private final Map<String, Integer> stringAttributesMap;
    /** Store the last index for a given attribute. **/
    private final Map<String, Integer> lastIndexMap;
    private StarTableFactory factory;
    private long objectId = 1;
    /** Dataset options, may be null. **/
    private DatasetOptions datasetOptions;
    /** The list of {@link ColumnInfo} objects of the last table loaded by this provider. **/
    private List<ColumnInfo> columnInfoList;

    public STILDataProvider() {
        super();
        stringAttributesMap = new HashMap<>();
        lastIndexMap = new HashMap<>();
        // Logging level to WARN.
        try {
            java.util.logging.Logger.getLogger("uk.ac.starlink").setLevel(Level.WARNING);
            java.util.logging.Logger.getLogger("org.astrogrid").setLevel(Level.WARNING);
            factory = new StarTableFactory();
            countsPerMag = new long[22];
            initLists();
        } catch (Exception e) {
            factory = null;
            logger.error(e);
        }
    }

    public void setDatasetOptions(DatasetOptions datasetOptions) {
        this.datasetOptions = datasetOptions;
    }

    @Override
    public List<IParticleRecord> loadData(String file,
                                          double factor) {
        logger.info(I18n.msg("notif.datafile", file));
        try {
            loadData(new FileDataSource(Settings.settings.data.dataFile(file)), factor);
        } catch (Exception e1) {
            try {
                logger.info("File " + file + " not found in data folder, trying relative path");
                loadData(new FileDataSource(file), factor);
            } catch (Exception e2) {
                logger.error(e1);
                logger.error(e2);
            }
        }
        logger.info(I18n.msg("notif.nodeloader", list.size(), file));
        return list;
    }

    /**
     * Gets the first ucd that can be translated to a double from the set.
     *
     * @param UCDs The array of UCDs. The UCDs which coincide with the names should be first.
     * @param row  The row objects.
     *
     * @return Pair of <UCD,Double>.
     */
    private Pair<UCD, Double> getDoubleUcd(Array<UCD> UCDs,
                                           Object[] row) {
        for (UCD ucd : UCDs) {
            try {
                double num = ((Number) row[ucd.index]).doubleValue();
                if (Double.isNaN(num)) {
                    throw new Exception();
                }
                return new Pair<>(ucd, num);
            } catch (Exception e0) {
                // not working, try String
                try {
                    double num = Parser.parseDouble((String) row[ucd.index]);
                    if (Double.isNaN(num)) {
                        throw new Exception();
                    }
                    return new Pair<>(ucd, num);
                } catch (Exception e1) {
                    // Out of ideas
                }
            }
        }
        return null;
    }

    /**
     * Gets the first ucd that can be translated to a double[] from the set.
     *
     * @param UCDs The array of UCDs. The UCDs which coincide with the names should be first.
     * @param row  The row objects
     *
     * @return Pair of <UCD,double[]>
     */
    private Pair<UCD, double[]> getDoubleArrayUcd(Array<UCD> UCDs,
                                                  Object[] row) {
        for (UCD ucd : UCDs) {
            try {
                double[] nums = (double[]) row[ucd.index];
                return new Pair<>(ucd, nums);
            } catch (Exception e0) {
                // not working, try String
                try {
                    double[] nums = Parser.parseDoubleArray((String) row[ucd.index]);
                    return new Pair<>(ucd, nums);
                } catch (Exception e1) {
                    // Out of ideas
                }
            }
        }
        return null;
    }

    /**
     * Gets the first ucd as a string from the set.
     *
     * @param UCDs The set of UCD objects
     * @param row  The row
     *
     * @return A pair with the UCD and the string
     */
    private Pair<UCD, String> getStringUcd(Array<UCD> UCDs,
                                           Object[] row) {
        for (UCD ucd : UCDs) {
            try {
                String str = row[ucd.index].toString().strip();
                return new Pair<>(ucd, str);
            } catch (Exception e) {
                // not working, try next
            }
        }
        return null;
    }

    private Pair<UCD, String>[] getAllStringsUcd(Array<UCD> UCDs,
                                                 Object[] row) {
        Array<Pair<UCD, String>> strings = new Array<>(false, 2);
        for (UCD ucd : UCDs) {
            try {
                String str = row[ucd.index].toString().strip();
                strings.add(new Pair<>(ucd, str));
            } catch (Exception e) {
                // not working, try next
            }
        }
        Pair<UCD, String>[] result = new Pair[strings.size];
        int i = 0;
        for (Pair<UCD, String> value : strings) {
            result[i++] = value;
        }
        return result;

    }

    public List<IParticleRecord> loadData(DataSource ds,
                                          double factor) {
        return loadData(ds, factor, null, null, null);
    }

    /**
     * Loads a dataset (from a data source object) into a list of particle records.
     *
     * @param ds             The data source.
     * @param factor         Length factor.
     * @param preCallback    A function that runs before.
     * @param updateCallback A function that runs after each object has loaded. Gets two longs, the first holds the
     *                       current number of loaded objects and the
     *                       second holds the total number of objects to load.
     * @param postCallback   A function that runs after the data has been loaded.
     *
     * @return The list of particle records.
     */
    public List<IParticleRecord> loadData(DataSource ds,
                                          double factor,
                                          Runnable preCallback,
                                          RunnableLongLong updateCallback,
                                          Runnable postCallback) {
        try {
            if (factory != null) {
                // Add extra builders
                List<TableBuilder> builders = factory.getDefaultBuilders();
                builders.add(new CsvTableBuilder());
                builders.add(new AsciiTableBuilder());

                if (preCallback != null)
                    preCallback.run();

                // Try to load
                StarTable table = factory.makeStarTable(ds);

                long count = table.getRowCount();
                initLists((int) count);

                UCDParser ucdParser = new UCDParser();
                ucdParser.parse(table);

                final int numColumns = table.getColumnCount();
                if (columnInfoList == null) {
                    columnInfoList = new ArrayList<>(numColumns);
                } else {
                    columnInfoList.clear();
                }
                for (int i = 0; i < numColumns; i++) {
                    columnInfoList.add(table.getColumnInfo(i));
                }

                // Automatically switch to extended particles if proper motions, colors or sizes are found in the data file.
                if (datasetOptions != null) {
                    if ((ucdParser.hasPm || ucdParser.hasSize || ucdParser.hasColor) && (datasetOptions.type == null
                            || datasetOptions.type == DatasetLoadType.PARTICLES)) {
                        // Switch to extended.
                        datasetOptions.type = DatasetLoadType.PARTICLES_EXT;
                    }
                }
                boolean isStars = datasetOptions == null || isAnyType(DatasetLoadType.VARIABLES, DatasetLoadType.STARS);

                int resampledLightCurves = 0;
                int noPeriods = 0;

                if (ucdParser.hasPos) {
                    BVToTeff_ballesteros bvToTEff = new BVToTeff_ballesteros();

                    int nInvalidParallaxes = 0;
                    long i = 0L;
                    long step = FastMath.max(1L, FastMath.round(count / 100d));

                    RowSequence rs = table.getRowSequence();
                    while (rs.next()) {
                        Object[] row = rs.getRow();
                        try {
                            // POSITION
                            Pair<UCD, Double> a = getDoubleUcd(ucdParser.POS1, row);
                            Pair<UCD, Double> b = getDoubleUcd(ucdParser.POS2, row);
                            Pair<UCD, Double> c;
                            String unitC;

                            Pair<UCD, Double> pos3 = getDoubleUcd(ucdParser.POS3, row);
                            // Check missing pos3 -> Use default parallax
                            if (ucdParser.POS3.isEmpty() || pos3 == null || pos3.getSecond() == null || !Double.isFinite(pos3.getSecond())) {
                                c = new Pair<>(null, Constants.DEFAULT_PARALLAX);
                                unitC = "mas";
                                nInvalidParallaxes++;
                            } else {
                                c = getDoubleUcd(ucdParser.POS3, row);
                                assert c != null;
                                unitC = c.getFirst().unit;
                            }

                            assert a != null;
                            assert b != null;
                            PositionType pt = ucdParser.getPositionType(a.getFirst(), b.getFirst(), c.getFirst());
                            // Check negative parallaxes -> Use default for consistency
                            if (pt.isParallax() && (c.getSecond() == null || c.getSecond().isNaN() || c.getSecond() <= 0)) {
                                c.setSecond(Constants.DEFAULT_PARALLAX);
                                unitC = "mas";
                                nInvalidParallaxes++;
                            }

                            Position p = new Position(a.getSecond(), a.getFirst().unit, b.getSecond(), b.getFirst().unit, c.getSecond(), unitC, pt);

                            double distPc = p.realPosition.len();
                            if ((pt.isParallax() && c.getSecond() <= 0) || !Double.isFinite(distPc) || distPc < 0) {
                                // Next
                                break;
                            }

                            p.realPosition.scl(Constants.PC_TO_U);
                            // Transform if necessary
                            if (transform != null) {
                                p.realPosition.mul(transform);
                            }

                            // Find out RA/DEC/Dist
                            Vector3d sph = new Vector3d();
                            Coordinates.cartesianToSpherical(p.realPosition, sph);

                            // PROPER MOTION
                            Vector3d pm;
                            double muAlphaStar = 0, muDelta = 0, radVel = 0;
                            // Only supported if position is equatorial spherical coordinates (ra/dec)
                            if (pt == PositionType.EQ_SPH_DIST || pt == PositionType.EQ_SPH_PLX) {
                                Pair<UCD, Double> pma = getDoubleUcd(ucdParser.PMRA, row);
                                Pair<UCD, Double> pmb = getDoubleUcd(ucdParser.PMDEC, row);
                                Pair<UCD, Double> pmc = getDoubleUcd(ucdParser.RADVEL, row);

                                muAlphaStar = pma != null ? pma.getSecond() : 0;
                                muDelta = pmb != null ? pmb.getSecond() : 0;
                                radVel = pmc != null ? pmc.getSecond() : Float.NaN;

                                double radVelValue = Double.isFinite(radVel) ? radVel : 0;

                                double raRad = new Angle(a.getSecond(), a.getFirst().unit).get(AngleUnit.RAD);
                                double decRad = new Angle(b.getSecond(), b.getFirst().unit).get(AngleUnit.RAD);
                                pm = AstroUtils.properMotionsToCartesian(muAlphaStar, muDelta, radVelValue, raRad, decRad, distPc, new Vector3d());
                            } else {
                                pm = new Vector3d(Vector3d.Zero);
                            }

                            // MAGNITUDE
                            double appMag;
                            if (!ucdParser.MAG.isEmpty()) {
                                Pair<UCD, Double> appMagPair = getDoubleUcd(ucdParser.MAG, row);
                                if (appMagPair == null) {
                                    // Default magnitude.
                                    appMag = Constants.DEFAULT_MAG;
                                } else {
                                    appMag = appMagPair.getSecond();
                                }
                            } else {
                                // Default magnitude.
                                appMag = Constants.DEFAULT_MAG;
                            }
                            // Scale magnitude if needed.
                            double magScl = isStars && datasetOptions != null ? datasetOptions.magnitudeScale : 0f;
                            appMag = appMag - magScl;

                            // Absolute magnitude to pseudo-size.
                            final double absMag = AstroUtils.apparentToAbsoluteMagnitude(distPc, appMag);
                            double sizePc = AstroUtils.absoluteMagnitudeToPseudoSize(absMag);

                            // SIZE (DIAMETER, not RADIUS!)
                            if (!ucdParser.SIZE.isEmpty()) {
                                // We have a size in the dataset.
                                if (!isStars) {
                                    // Only particles, star datasets do not have a size.
                                    Pair<UCD, Double> sizePair = getDoubleUcd(ucdParser.SIZE, row);
                                    UCD sizeUcd = sizePair.getFirst();
                                    if (sizeUcd != null && sizeUcd.unit != null) {
                                        if (Angle.isAngle(sizeUcd.unit)) {
                                            // Solid angle in radians.
                                            double sa = new Angle(sizePair.getSecond(), sizePair.getFirst().unit).get(AngleUnit.RAD);
                                            // Size in parsecs = tan(sa) * distPc
                                            sizePc = FastMath.tan(sa) * p.realPosition.len();
                                        } else if (Length.isLength(sizeUcd.unit)) {
                                            // Size in parsecs, directly.
                                            sizePc = new Length(sizePair.getSecond(), sizePair.getFirst().unit).get(LengthUnit.PC);
                                        }
                                    } else {
                                        // We hope size is already in parsecs.
                                        sizePc = sizePair.getSecond().floatValue();
                                    }
                                    if (TextUtils.containsOrMatches(UCDParser.radiusColNames, sizeUcd.colName, true)) {
                                        // Radius, need to multiply by 2 to get diameter.
                                        sizePc *= 2.0;
                                    }
                                }
                            } else {
                                if (!isStars) {
                                    // We have particles without a size. We just clamp what we have.
                                    double maxSizePc = 3.0;
                                    if (params != null && params.containsKey("maxSizePc")) {
                                        var value = params.get("maxSizePc");
                                        if (value instanceof Number n) {
                                            maxSizePc = n.doubleValue();
                                        }
                                    }
                                    sizePc = MathUtilsDouble.clamp(sizePc, 1e-8, maxSizePc);
                                }
                            }

                            // COLOR INDEX
                            float colorIndex;
                            if (!ucdParser.COL.isEmpty()) {
                                Pair<UCD, Double> colPair = getDoubleUcd(ucdParser.COL, row);
                                if (colPair == null) {
                                    colorIndex = (float) Constants.DEFAULT_COLOR;
                                } else {
                                    colorIndex = colPair.getSecond().floatValue();
                                }
                            } else {
                                // Default color index for stars, NaN for others.
                                colorIndex = isStars ? (float) Constants.DEFAULT_COLOR : Float.NaN;
                            }

                            // VARIABILITY
                            float[] variMags = null;
                            double[] variTimes = null;
                            double pf = 0.0;
                            int nVari = 0;
                            if (ucdParser.hasVariability) {
                                Pair<UCD, Double> period = getDoubleUcd(ucdParser.VARI_PERIOD, row);
                                if (!ucdParser.hasPeriod || period == null || !Double.isFinite(period.getSecond())) {
                                    // Skip stars without period
                                    noPeriods++;
                                    continue;
                                } else {
                                    pf = period.getSecond();
                                }
                                Pair<UCD, double[]> variMagsPair = getDoubleArrayUcd(ucdParser.VARI_MAGS, row);
                                assert variMagsPair != null;
                                double[] variMagsDouble = variMagsPair.getSecond();
                                nVari = variMagsDouble.length;
                                variMags = new float[nVari];

                                Pair<UCD, double[]> variTimesPair = getDoubleArrayUcd(ucdParser.VARI_TIMES, row);
                                assert variTimesPair != null;
                                variTimes = variTimesPair.getSecond();

                                double[] auxMags = variMagsDouble;
                                double[] auxTimes = variTimes;

                                // SANITIZE (no NaNs)
                                List<Double> magnitudesList = new ArrayList<>();
                                List<Double> timesList = new ArrayList<>();
                                int idx = 0;
                                for (double mag : auxMags) {
                                    if (Double.isFinite(mag)) {
                                        magnitudesList.add(mag - magScl);
                                        timesList.add(auxTimes[idx]);
                                    }
                                    idx++;
                                }
                                variMagsDouble = magnitudesList.stream().mapToDouble(Double::doubleValue).toArray();
                                variTimes = timesList.stream().mapToDouble(Double::doubleValue).toArray();
                                nVari = variMagsDouble.length;

                                // FOLD
                                List<Vector2d> list = new ArrayList<>(nVari);
                                for (int k = 0; k < nVari; k++) {
                                    double phase = ((variTimes[k] - variTimes[0]) % pf);
                                    list.add(new Vector2d(phase, variMagsDouble[k]));
                                }
                                list.sort(Comparator.comparingDouble(o -> o.x));

                                for (int k = 0; k < nVari; k++) {
                                    Vector2d point = list.get(k);
                                    variTimes[k] = point.x + variTimes[0];
                                    variMagsDouble[k] = point.y;
                                }

                                // RESAMPLE (only if too many samples)
                                final int MAX_VARI = VariableSetInstancedRenderer.MAX_VARI;
                                if (variMagsDouble.length > MAX_VARI) {
                                    nVari = MAX_VARI;
                                    double t0 = variTimes[0];
                                    double tn = variTimes[variTimes.length - 1];
                                    double tStep = (tn - t0) / (nVari - 1);

                                    var linearInterpolator = new LinearInterpolator(variTimes, variMagsDouble);

                                    variMagsDouble = new double[nVari];
                                    variTimes = new double[nVari];

                                    for (idx = 0; idx < nVari; idx++) {
                                        double t = t0 + tStep * idx;
                                        variTimes[idx] = t;
                                        variMagsDouble[idx] = linearInterpolator.value(t);
                                    }
                                    resampledLightCurves++;
                                }

                                // Convert magnitudes to sizes
                                assert variMags.length == variTimes.length;
                                for (int j = 0; j < variMagsDouble.length; j++) {
                                    double variAbsoluteMag = AstroUtils.apparentToAbsoluteMagnitude(distPc, variMagsDouble[j]);
                                    variMags[j] = (float) AstroUtils.absoluteMagnitudeToPseudoSize(variAbsoluteMag);
                                }
                            }

                            // EFFECTIVE TEMPERATURE
                            float tEff;
                            if (!ucdParser.TEFF.isEmpty()) {
                                Pair<UCD, Double> tEffPair = getDoubleUcd(ucdParser.TEFF, row);
                                if (tEffPair != null) {
                                    // Use value from table.
                                    tEff = tEffPair.getSecond().floatValue();
                                } else {
                                    // Use color index.
                                    tEff = (float) bvToTEff.bvToTeff(colorIndex);
                                }
                            } else {
                                // Convert B-V to T_eff using Ballesteros 2012
                                tEff = (float) bvToTEff.bvToTeff(colorIndex);
                            }

                            // RGB COLOR (PACKED) from COLOR INDEX or Teff
                            float colorPacked;
                            float[] rgb = null;
                            if (Float.isFinite(colorIndex)) {
                                // Convert color index to RGB.
                                rgb = ColorUtils.BVtoRGB(colorIndex);
                                colorPacked = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);
                            } else if (Float.isFinite(tEff)) {
                                rgb = ColorUtils.tEffToRGB_harre(tEff);
                                colorPacked = Color.toFloatBits(rgb[0], rgb[1], rgb[2], 1.0f);
                            } else {
                                colorPacked = Float.NaN;
                            }

                            // IDENTIFIER
                            long id;
                            boolean idIsNotNumber = false;
                            int hip = -1;
                            if (!ucdParser.ID.isEmpty()) {
                                // We have ID
                                Pair<UCD, String> idPair = getStringUcd(ucdParser.ID, row);
                                assert idPair != null;
                                try {
                                    id = Parser.parseLongException(idPair.getSecond());
                                    if (isStars && idPair.getFirst().colName.equalsIgnoreCase("hip")) {
                                        hip = (int) id;
                                    }
                                } catch (NumberFormatException e) {
                                    // ID is not an integer.
                                    id = ++objectId;
                                    idIsNotNumber = true;
                                }
                            } else {
                                // Empty ID
                                id = ++objectId;
                            }

                            // NAME(S)
                            String[] names;
                            if (ucdParser.NAME.isEmpty()) {
                                // Name from ID.
                                if (idIsNotNumber) {
                                    Pair<UCD, String> idPair = getStringUcd(ucdParser.ID, row);
                                    names = new String[] { idPair.getSecond() };
                                } else {
                                    names = new String[] { Long.toString(id) };
                                }
                            } else {
                                // We have a name.
                                Pair<UCD, String>[] namePairs = getAllStringsUcd(ucdParser.NAME, row);
                                Array<String> namesArray = new Array<>(false, namePairs.length);
                                for (Pair<UCD, String> pair : namePairs) {
                                    String[] currNames = pair.getSecond().split(Constants.nameSeparatorRegex);
                                    for (String actualName : currNames) {
                                        if (actualName != null && !actualName.isEmpty() && !TextUtils.contains(forbiddenNameValues, actualName, true)) {
                                            namesArray.add(actualName);
                                        }
                                    }
                                }
                                names = new String[namesArray.size];
                                int k = 0;
                                for (String n : namesArray) {
                                    names[k++] = n;
                                }
                                // Default to ID.
                                if (names.length == 0) {
                                    names = new String[] { Long.toString(id) };
                                }
                            }

                            // Populate provider lists
                            if (rgb != null) {
                                colors.put(id, rgb);
                            }
                            sphericalPositions.put(id, new double[] { sph.x, sph.y, sph.z });

                            if (datasetOptions == null || datasetOptions.type == DatasetOptions.DatasetLoadType.STARS
                                    || datasetOptions.type == DatasetOptions.DatasetLoadType.VARIABLES) {
                                var type = ParticleRecordType.STAR;

                                IParticleRecord pr;
                                if (datasetOptions != null && datasetOptions.type == DatasetLoadType.VARIABLES || variMags != null) {
                                    pr = new VariableRecord();
                                    var vr = (VariableRecord) pr;
                                    vr.setNVari(nVari);
                                    vr.setPeriod(pf);
                                    vr.setVariMags(variMags);
                                    vr.setVariTimes(variTimes);
                                } else {
                                    pr = new ParticleRecord(type);
                                }
                                pr.setId(id);
                                pr.setNames(names);
                                pr.setPos(p.realPosition.x, p.realPosition.y, p.realPosition.z);
                                pr.setVelocityVector(pm.x, pm.y, pm.z);
                                pr.setProperMotion((float) muAlphaStar, (float) muDelta, (float) radVel);
                                pr.setMag((float) appMag, (float) absMag);
                                pr.setCol(colorPacked);
                                pr.setSize((float) sizePc);
                                pr.setHip(hip);
                                // Extra
                                ObjectMap<UCD, Object> extraAttributes = addExtraAttributes(ucdParser, row);
                                if (ucdParser.TEFF.isEmpty()) {
                                    UCD tEffUCD = new UCD("phys.temperature.effective", "teff", "K", -1);
                                    extraAttributes = initExtraAttributes(extraAttributes);
                                    extraAttributes.put(tEffUCD, tEff);
                                    pr.setTeff(tEff);
                                } else {
                                    extraAttributes = initExtraAttributes(extraAttributes);
                                    extraAttributes.put(ucdParser.TEFF.first(), tEff);
                                    pr.setTeff(tEff);
                                }
                                pr.setExtraAttributes(extraAttributes);

                                list.add(pr);

                                int appMagClamp = (int) MathUtilsDouble.clamp(appMag, 0, 21);
                                countsPerMag[appMagClamp] += 1;
                            } else if (datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES) {
                                var type = ParticleRecordType.PARTICLE;

                                IParticleRecord pr = new ParticleRecord(type);
                                pr.setId(id);
                                pr.setNames(names);
                                pr.setPos(p.realPosition.x, p.realPosition.y, p.realPosition.z);
                                // Extra
                                ObjectMap<UCD, Object> extraAttributes = addExtraAttributes(ucdParser, row);
                                pr.setExtraAttributes(extraAttributes);

                                list.add(pr);
                            } else if (datasetOptions.type == DatasetOptions.DatasetLoadType.PARTICLES_EXT) {
                                var type = ParticleRecordType.PARTICLE_EXT;

                                IParticleRecord pr = new ParticleRecord(type);
                                pr.setId(id);
                                pr.setNames(names);
                                pr.setPos(p.realPosition.x, p.realPosition.y, p.realPosition.z);
                                pr.setVelocityVector(pm.x, pm.y, pm.z);
                                pr.setProperMotion((float) muAlphaStar, (float) muDelta, (float) radVel);
                                pr.setMag((float) appMag, (float) absMag);
                                pr.setCol(colorPacked);
                                pr.setSize((float) (sizePc * Constants.PC_TO_U));
                                // Extra
                                ObjectMap<UCD, Object> extraAttributes = addExtraAttributes(ucdParser, row);
                                pr.setExtraAttributes(extraAttributes);

                                list.add(pr);
                            }

                        } catch (Exception e) {
                            logger.debug(e);
                            logger.debug(I18n.msg("debug.parse.row.skip", i));
                        }
                        i++;
                        if (updateCallback != null && i % step == 0) {
                            updateCallback.run(i, count);
                        }
                    }
                    if (nInvalidParallaxes > 0) {
                        logger.warn(I18n.msg("warn.star.parallax", nInvalidParallaxes, Constants.DEFAULT_PARALLAX));
                    }
                    if (resampledLightCurves > 0) {
                        logger.warn(I18n.msg("warn.star.vari.resample", resampledLightCurves, VariableSetInstancedRenderer.MAX_VARI));
                    }
                    if (noPeriods > 0) {
                        logger.warn(I18n.msg("warn.star.vari.noperiod", noPeriods));
                    }
                } else {
                    logger.error(I18n.msg("error.star.noposition"));
                }
            }
        } catch (Exception e) {
            logger.error(e);
        } finally {
            if (postCallback != null)
                postCallback.run();
        }

        return list;
    }

    private void exportCsv(double[] x,
                           double[] y,
                           int n,
                           Path p,
                           String... cols) {
        try {
            FileWriter myWriter = new FileWriter(p.toString());
            if (cols != null && cols.length >= 2) {
                myWriter.write(String.format("%1$s,%2$s\n", cols[0], cols[1]));
            }
            for (int i = 0; i < n; i++) {
                myWriter.write(String.format("%1$f,%2$f\n", x[i], y[i]));
            }
            myWriter.close();
        } catch (IOException e) {
            logger.error(e);
        }

    }

    private ObjectMap<UCD, Object> initExtraAttributes(ObjectMap<UCD, Object> extra) {
        if (extra == null)
            extra = new ObjectMap<>(5);
        return extra;
    }

    private ObjectMap<UCD, Object> addExtraAttributes(UCDParser ucdParser,
                                                      Object[] row) {
        // Extra
        ObjectMap<UCD, Object> extraAttributes = null;
        for (UCD extra : ucdParser.extra) {
            Object val = row[extra.index];
            if (extraAttributes == null)
                extraAttributes = new ObjectMap<>((int) (ucdParser.extra.size * 1.25f), 0.8f);
            extraAttributes.put(extra, val);
        }
        return extraAttributes;
    }

    private double getStringAttributeValue(UCD extra,
                                           Object o) {
        double val;
        String value = (String) o;
        if (value == null || value.isEmpty()) {
            return -1;
        }

        String key = extra.colName + ":" + value;
        int index = 0;
        if (stringAttributesMap.containsKey(key)) {
            index = stringAttributesMap.get(key);
        } else if (lastIndexMap.containsKey(extra.colName)) {
            index = lastIndexMap.get(extra.colName);
        }

        val = index;

        if (!stringAttributesMap.containsKey(key)) {
            stringAttributesMap.put(key, index);
            lastIndexMap.put(extra.colName, index + 1);
        }
        return val;
    }

    private boolean isOfType(DatasetLoadType type) {
        return datasetOptions != null && datasetOptions.type != null && datasetOptions.type == type;
    }

    private boolean isAnyType(DatasetLoadType... types) {
        for (var type : types) {
            if (isOfType(type)) {
                return true;
            }
        }
        return false;
    }

    public List<ColumnInfo> getColumnInfoList() {
        return columnInfoList;
    }

    @Override
    public List<IParticleRecord> loadData(InputStream is,
                                          double factor) {
        return null;
    }

    @Override
    public List<IParticleRecord> loadDataMapped(String file,
                                                double factor) {
        return null;
    }

    @Override
    public void setFileNumberCap(int cap) {
    }

    @Override
    public LongMap<float[]> getColors() {
        return null;
    }

    @Override
    public void setParallaxErrorFactorFaint(double parallaxErrorFactor) {

    }

    @Override
    public void setParallaxErrorFactorBright(double parallaxErrorFactor) {

    }

    @Override
    public void setParallaxZeroPoint(double parallaxZeroPoint) {
    }

    @Override
    public void setMagCorrections(boolean magCorrections) {
    }

    @Override
    public void setProviderParams(Map<String, Object> params) {
        super.setProviderParams(params);
        datasetOptionsFromParameters();
    }

    private void datasetOptionsFromParameters() {
        if (params != null) {
            DatasetOptions dOps = new DatasetOptions();
            // Catalog name
            if (params.containsKey("catalogName")) {
                dOps.catalogName = (String) params.get("catalogName");
            }

            // Type
            if (params.containsKey("type")) {
                String typeStr = (String) params.get("type");
                dOps.type = DatasetLoadType.valueOf(typeStr.toUpperCase());
            }

            // Label color
            if (params.containsKey("labelColor")) {
                dOps.labelColor = (double[]) params.get("labelColor");
            }

            // Magnitude scale
            if (params.containsKey("magnitudeScale")) {
                dOps.magnitudeScale = (double) params.get("magnitudeScale");
            }

            // Fade in
            if (params.containsKey("fadeIn")) {
                dOps.fadeIn = (double[]) params.get("fadeIn");
            }

            // Fade out
            if (params.containsKey("fadeOut")) {
                dOps.fadeOut = (double[]) params.get("fadeOut");
            }

            // Set
            this.datasetOptions = dOps;
        }

    }
}
